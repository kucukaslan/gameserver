package tr.com.kucukaslan.dream.util;

import java.sql.SQLException;
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


    public synchronized TournamentGroup join(String countryISO2, Long userId) throws SQLException, JSONException {
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
            group.put(countryISO2, userId);
        }

        if (group.size() == 5) {
            JSONObject curTourn = getCurrentTournament();
            log.trace("joining {} from {}  to tournament {}", userId, countryISO2, curTourn.toString() );
            JSONArray js = DBService.getInstance().insertGroup(curTourn);
            
            group.setGroupJson(js.getJSONObject(0));

            log.trace("TODO group {} is full, and created in the database", group);
            notifyAll();
        }
        log.trace("{}: joined country: {} group {}", userId, countryISO2, group);
        return group;
    }

    public synchronized JSONObject getCurrentTournament() throws SQLException, JSONException {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        String tournamentCode =String.format("%d%02d%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        if(tournamentCacheByCode.containsKey(tournamentCode)) {
            log.trace("tournament {} is found in cache", tournamentCode);
            return tournamentCacheByCode.get(tournamentCode);
        }

        JSONArray js = DBService.getInstance().getCurrentTournaments();
        if(js.length() == 0) {
            log.trace("no tournament found for today creating one");
            js = DBService.getInstance().insertTournament();
        }
        return js.getJSONObject(0);
    }

    public JSONObject join(Long user_id, Long tournament_group_id) throws SQLException{
        return DBService.getInstance().insertUserGroup(user_id, tournament_group_id);
    }

}