package tr.com.kucukaslan.dream.util;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.dream.service.DBService;

@Slf4j
public class TournamentManager {

    private Map<String, ConcurrentLinkedQueue<TournamentGroup>> queueMap;
    private Map<String, JSONObject> tournamentCacheByCode;

    private static TournamentManager instance = null;

    private TournamentManager() {
        queueMap = new HashMap<>();
        queueMap.put("TR", new ConcurrentLinkedQueue<TournamentGroup>());
        queueMap.put("US", new ConcurrentLinkedQueue<TournamentGroup>());
        queueMap.put("DE", new ConcurrentLinkedQueue<TournamentGroup>());
        queueMap.put("FR", new ConcurrentLinkedQueue<TournamentGroup>());
        queueMap.put("GB", new ConcurrentLinkedQueue<TournamentGroup>());
        tournamentCacheByCode = new HashMap<>();

    }

    public synchronized static TournamentManager getInstance() {
        if (instance == null) {
            instance = new TournamentManager();
        }
        return instance;
    }

    /**
     * 
     * @param countryISO2
     * @param user
     * @return Tournament Group that user joined, null if user is already in a group
     * @throws SQLException
     * @throws JSONException
     * @throws MyException if user is already registered to a tournament or does not have enough coin
     */
    public synchronized TournamentGroup join(String countryISO2, JSONObject user) throws SQLException, JSONException, MyException {
        Long userId = user.getLong("user_id");
        Long coin ;
        Long level;
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        if( !isTournamentHour(now)) {
            SimpleDateFormat f = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" );
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            throw new MyException("It is not tournament hour cannot join to tournament " +f.format(now.getTime()));
        }
        
        // check if user has enough coin
        coin = user.getLong("coin");
        if (coin < DBService.TOURNAMENT_ENTRANCE_FEE) {
            throw new MyException("Not enough coin to join tournament. Tournament entrance fee is "
                    + DBService.TOURNAMENT_ENTRANCE_FEE + " but user only has " + coin);
        }
        
        // check if user's level entitles him to join tournament
        level = user.getLong("level");
        if (level < DBService.TOURNAMENT_MIN_LEVEL) {
            throw new MyException("User must be at least level " + DBService.TOURNAMENT_MIN_LEVEL
                    + " to join tournament but user is only level " + level);
        }


        JSONObject tournament = getTodaysTournament();
        long tournament_id = tournament.getLong("tournament_id");
        JSONObject tug = DBService.getInstance().getTournamentGroupIdByTournamentUserId(tournament_id, userId);
        if(tug != null) {
            log.trace("user {} is already joined to tournament", userId);
            throw new MyException("User is already joined to tournament");
        }
        TournamentGroup group = queueMap.get(countryISO2).poll();
        log.trace("{}: requested to join country: {} group {}", userId, countryISO2, group);
        if (group == null) {
            log.trace("{}: no group found for country: {}, creating one!", userId, countryISO2);
            group = new TournamentGroup();
            group.put(countryISO2, userId);
            // add to other queues to wait other players
            for (String country : queueMap.keySet()) {
                if (!country.equals(countryISO2)) {
                    log.trace("{}:{} adding to queue of country: {} ", userId, countryISO2, country);
                    queueMap.get(country).add(group);
                }
            }
        } else {
            // if this is going to be fifth then create a tournament group in the database
            // and set the group json to avoid race conditions
            if (group.size() == 4) {
                log.trace("joining {} from {}  to tournament {}", userId, countryISO2, tournament.toString());
                JSONArray js = DBService.getInstance().insertGroup(tournament); 
                group.setGroupJson(js.getJSONObject(0));
            }
            group.put(countryISO2, userId);
            log.trace("group {} is full, and created in the database", group);
            notifyAll();
        }
        log.trace("{}: joined country: {} group {}", userId, countryISO2, group);
        return group;
    }

    /** 
     * Returns the tournament of the day even after the tournament is finished
     * 
     */
    public synchronized JSONObject getTodaysTournament() throws SQLException, JSONException, MyException {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

        // month starts from 0 
        String tournamentCode = String.format("%d%02d%02d", c.get(Calendar.YEAR), 1 + c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));
        if (tournamentCacheByCode.containsKey(tournamentCode)) {
            log.trace("tournament {} is found in cache", tournamentCode);
            return tournamentCacheByCode.get(tournamentCode);
        }


        JSONArray js = DBService.getInstance().getTodaysTournaments();
        if (js.length() == 0) {
            log.trace("no tournament found for today creating one");
            log.info("emptying queues before creating a new tournament");
            emptyQueues();
            js = DBService.getInstance().insertTournament();
        }
        tournamentCacheByCode.put(tournamentCode, js.getJSONObject(0));
        return js.getJSONObject(0);
    }

    public JSONObject join(Long user_id, Long tournament_group_id) throws SQLException {
        return DBService.getInstance().insertUserGroup(user_id, tournament_group_id);
    }

    public boolean isTournamentHour() {
        return isTournamentHour(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
    }
    public boolean isTournamentHour(Calendar now) {
        // check if it is tournament hours 00:00 - 20:00
        // since the tournament starts at 00:00 we don't need to check the start hour
        Calendar endHour = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        endHour.set(Calendar.HOUR_OF_DAY, 20);
        endHour.set(Calendar.MINUTE, 0);
        endHour.set(Calendar.SECOND, 0);
        endHour.set(Calendar.MILLISECOND, 0);
         
        if( now.after(endHour)) {
            log.trace("{} it is not tournament hours", now);
            return false;
        }
        return true;
    }

    private void emptyQueues() {
        for (String country : queueMap.keySet()) {
            log.trace("emptying queue of country: {}, removing {} elements", country, queueMap.get(country).size());
            queueMap.get(country).clear();
        }
    }

    public void updateTournamentScore(long user_id) throws SQLException, JSONException, MyException 
    {
        if(!isTournamentHour()) {
            return;
        }

        JSONObject tournament = getTodaysTournament();
        long tournament_id = tournament.getLong("tournament_id");
        JSONObject tug = DBService.getInstance().getTournamentGroupIdByTournamentUserId(tournament_id, user_id);
        if(tug == null) {
            log.trace("user {} is not in any tournament group, so tournament score is not updated", user_id);
            return;
        }
        long tournament_group_id = tug.getLong("utg_id");
        DBService.getInstance().incrementTournamentScore(tournament_group_id);
    }

}