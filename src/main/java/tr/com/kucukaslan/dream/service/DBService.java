package tr.com.kucukaslan.dream.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Calendar;
import java.util.Properties;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.dream.util.MyException;

@Slf4j
public class DBService {
    String DB_CONFIG = "config.properties";

    String DBUSER_KEY = "DBUSER";
    String DBPASS_KEY = "DBPASS";
    String DBHOST_KEY = "DBHOST";
    String DBPORT_KEY = "DBPORT";
    String DBNAME_KEY = "DBNAME";

    // private static DBService instance;
    Properties properties;
    Connection con;

    private static final String USER_INSERT_SQL = "INSERT INTO user (countryISO2, name) VALUES (?, ?)";
    // UPDATE `user` SET `level` = '1', `coin` = '5025', `name` = 'Wow updated', `date_modified` = UTC_TIMESTAMP(6) WHERE `user`.`user_id` = 10
    private static final String USER_UPDATE_SQL = "UPDATE  `user` SET `level` = `level` + ?, `coin` = `coin` + ?, `date_modified` = UTC_TIMESTAMP(6) WHERE `user`.`user_id` = ?";
    private static final String USER_SELECT_SQL = "SELECT * FROM `user` WHERE `user_id` = ?";

    // INSERT INTO `tournament` ( `code`, `start_time`, `end_time`, `date_created`) VALUES ('20231013', '2023-10-13 00:00:00', '2023-10-13 20:00:00', UTC_TIMESTAMP(6));
    private static final String TOURNAMENT_INSERT_SQL = "INSERT INTO `tournament` ( `code`, `start_time`, `end_time`, `date_created`) VALUES (?, ?, ?, UTC_TIMESTAMP(6))";
    private static final String TOURNAMENT_SELECT_SQL = "SELECT * FROM `tournament` WHERE `tournament_id` = ?";
    private static final String TOURNAMENT_SELECT_ALL_CURRENT_TOURNAMENTS = "SELECT * FROM `tournament` WHERE `end_time` <= UTC_TIMESTAMP(6) AND `start_time` < UTC_TIMESTAMP(6)";
    private static final String TOURNAMENT_SELECT_ALL_TODAYS_TOURNAMENTS = "SELECT * FROM `tournament` WHERE `end_time` <= ? AND `start_time` >= ?";

    private static final String GROUP_INSERT_SQL = "INSERT INTO `tournament_group` (`code`,  `tournament_id`) VALUES (?,?)";

    private static final String USER_DEDUCT_COIN = "UPDATE  `user` SET `coin` = `coin` - ?, `date_modified` = UTC_TIMESTAMP(6) WHERE `user`.`user_id` = ?";
    private static final String USER_GROUP_INSERT_SQL = "INSERT INTO `user_tournament_group` (`user_id`, `tournament_group_id`, `level_when_joined`, `score`, `rewardsClaimed` ) VALUES ( ?, ?, (SELECT level from user where user_id = ?), '0', '0')";
    private static final String USER_GROUP_SELECT_SQL = "SELECT * FROM `user_tournament_group` WHERE utg_id=?";

    // SELECT * FROM `user` u join user_tournament_group utg on utg.user_id = u.user_id JOIN tournament_group tg on tg.tournament_group_id = utg.tournament_group_id join tournament t on t.tournament_id = tg.tournament_id;

    // SELECT * FROM `user` u join user_tournament_group utg on utg.user_id = u.user_id JOIN tournament_group tg on tg.tournament_group_id = utg.tournament_group_id join tournament t on t.tournament_id = tg.tournament_id where tg.tournament_group_id = 22 order by utg.score DESC;
    private static final String TOURNAMENT_GROUP_RANK = "SELECT u.user_id, u.name, u.countryISO2,  utg.score, utg.utg_id, t.tournament_id FROM `user` u join  user_tournament_group utg on  utg.user_id = u.user_id JOIN tournament_group tg on tg.tournament_group_id = utg.tournament_group_id join tournament t on t.tournament_id = tg.tournament_id where tg.tournament_group_id = ? order by utg.score DESC";
    
    private static final String TOURNAMENT_COUNTRY_RANK = "SELECT u.countryISO2, sum(utg.score) as score FROM `user` u join user_tournament_group utg on utg.user_id = u.user_id JOIN tournament_group tg on tg.tournament_group_id = utg.tournament_group_id join tournament t on t.tournament_id = tg.tournament_id where t.tournament_id = ? GROUP by u.countryISO2 ORDER by sum(utg.score) DESC";
    
    private static final String TOURNAMENT_GROUP_BY_USER_AND_TOURNAMENT = "SELECT u.user_id, u.countryISO2, u.name, utg.score, utg.utg_id, tg.tournament_group_id, t.tournament_id FROM `user` u join user_tournament_group utg on utg.user_id = u.user_id JOIN tournament_group tg on tg.tournament_group_id = utg.tournament_group_id join tournament t on t.tournament_id = tg.tournament_id where tg.tournament_id = ? and u.user_id = ? order by utg.score DESC";

    private static final String USER_LAST_TOURNAMENT = "SELECT u.user_id, u.name, u.countryISO2, utg.score, utg.utg_id, tg.tournament_group_id, t.tournament_id, utg.rewardsClaimed, t.end_time FROM `user` u join user_tournament_group utg on utg.user_id = u.user_id JOIN tournament_group tg on tg.tournament_group_id = utg.tournament_group_id join tournament t on t.tournament_id = tg.tournament_id where u.user_id = ? AND t.end_time <= UTC_TIMESTAMP(6) ORDER BY t.end_time DESC LIMIT ?";

    private static final String USER_CLAIM_REWARDS = "UPDATE `user_tournament_group` SET `rewardsClaimed` = '1' WHERE `user_tournament_group`.`utg_id` = ?; UPDATE `user` SET `coin` = `coin` + ? WHERE `user`.`user_id` = ?;";
    
    private static final long LEVEL_AWARD = 25;
    private static final long[] TOURNAMENT_AWARDS = { 10000, 5000, 0, 0, 0 };
    
    public static final long TOURNAMENT_ENTRANCE_FEE = 1000L;
    public static final long TOURNAMENT_MIN_LEVEL = 20L;

    private static class SingletonHolder {
        static DBService instance = new DBService();
    }

    private DBService() {

    }

    public static DBService getInstance() {
        return SingletonHolder.instance;
    }

    public void initialize() throws FileNotFoundException, IOException, SQLException {
        DBService instance = DBService.getInstance();
        if (instance.con != null && !instance.con.isClosed()) {
            log.info("DBService is already initialized");
            return;
        }
        log.info("DBService initializing");

        properties = new Properties();
        properties.load(new FileInputStream(DB_CONFIG));
        log.info("DBService properties loaded");

        // connect to DB
        String dbUser = properties.getProperty(DBUSER_KEY);
        String dbPass = properties.getProperty(DBPASS_KEY);
        String dbHost = properties.getProperty(DBHOST_KEY);
        String dbPort = properties.getProperty(DBPORT_KEY);
        String dbName = properties.getProperty(DBNAME_KEY);

        log.info("All properties are present, establishing connection to DB");
        con = DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName+"?allowMultiQueries=YES&connectionTimeZone=UTC", dbUser, dbPass);
        log.info("DB Connection to {} established", con.getMetaData().getURL());
    }

    public JSONObject insertUser(JSONObject user) throws SQLException, MyException{
        // insert user and get the id
        ResultSet rs;
        PreparedStatement stmt = con.prepareStatement(USER_INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, user.getString("countryISO2"));
        if(user.isNull("name")) {
            log.trace("name is null, setting null to DB");
            stmt.setNull(2, Types.VARCHAR);
        } else {
            stmt.setString(2, user.getString("name"));
        }
        log.trace("Executing SQL: {}", stmt.toString());

        if (stmt.executeUpdate() != 1) {
            log.error("Error while inserting user {}", user);
            throw new SQLException("Error while inserting user");
        }
        
        rs = stmt.getGeneratedKeys();
        if(!rs.next()){
            log.error("Error while inserting user {}", user);
        }
        long user_id = rs.getLong(1);
        log.trace("SQL executed, user_id: {}", user_id);

        return selectUser(user_id);
    }

    public JSONObject selectUser(Long user_id) throws SQLException, MyException{
        // retrieve the user
        log.debug("selectUser with user_id: {}", user_id);
        PreparedStatement stmt = con.prepareStatement(USER_SELECT_SQL);
        stmt.setLong(1, user_id);
        log.trace("retrieving user information SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);
        JSONArray js = resultSetToJSON(rs);
        if(js == null || js.length() == 0){
            throw new MyException("User with user_id: "+user_id+" does not exist");
        }
       return js.getJSONObject(0);
    }


    /**
     * increment user level by 1 and coins by 25
     * @param user
     * @return
     * @throws SQLException
     * @throws JSONException
     * @throws MyException
     */
    public JSONObject incrementUserLevel(JSONObject user) throws SQLException, MyException, JSONException {
        PreparedStatement stmt = con.prepareStatement(USER_UPDATE_SQL);
        stmt.setLong(1, 1); // increment level by 1
        stmt.setLong(2, LEVEL_AWARD); // increment coins by 25
        stmt.setLong(3, user.getLong("user_id"));

        log.trace("Executing SQL: {}", stmt.toString());
        if (stmt.executeUpdate() != 1) {
            log.error("Error while incrementing user level {}", user);
            throw new MyException("Couldn't increment user level with user_id: "+user.getLong("user_id")  );
        }
        log.trace("SQL executed");
        return selectUser(user.getLong("user_id"));
    }

    /**
     * increment user score by 1
     * @return
     * @throws SQLException
     * @throws JSONException
     */
    public void incrementTournamentScore(long tournament_group_id) throws SQLException {
        updateTournamentScore(tournament_group_id, 1);
    }

    public void updateTournamentScore(long tournament_group_id, int score) throws SQLException {
        PreparedStatement stmt = con.prepareStatement("UPDATE `user_tournament_group` SET `score`=`score`+? WHERE utg_id = ?");
        stmt.setLong(1, score);
        stmt.setLong(2, tournament_group_id);

        log.trace("Executing SQL: {}", stmt.toString());
        if (stmt.executeUpdate() != 1) {
            log.error("Error while incrementing tournament score {}", tournament_group_id);
        }
        log.trace("SQL executed {}", stmt.toString());
    }


    /**
     * Insert tournament to DB
     * @return
     * @throws SQLException
     * @throws JSONException
     */
    public JSONArray insertTournament() throws SQLException, JSONException {
        // get the current time in UTC
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        log.info("Current time in UTC: {}", c.getTime());
        String code = String.format("%d%02d%02d", c.get(Calendar.YEAR), 1+c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));
        log.info("Generated code: {}", code);
        String start_time = String.format("%d-%02d-%02d 00:00:00", c.get(Calendar.YEAR), 1+c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));
        log.info("Generated start_time: {}", start_time);
        String end_time = String.format("%d-%02d-%02d 20:00:00", c.get(Calendar.YEAR), 1+c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));

        PreparedStatement stmt = con.prepareStatement(TOURNAMENT_INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, code);
        stmt.setString(2, start_time);
        stmt.setString(3, end_time);

        log.trace("Executing SQL: {}", stmt.toString());

        if (stmt.executeUpdate() != 1) {
            log.error("Error while inserting tournament");
        }

        ResultSet rs = stmt.getGeneratedKeys();
        if (!rs.next()) {
            log.error("Error while inserting tournament");
        }
        long tournament_id = rs.getLong(1);
        log.trace("SQL executed, tournament_id: {}", tournament_id);

        // retrieve the inserted tournament
        stmt = con.prepareStatement(TOURNAMENT_SELECT_SQL);
        stmt.setLong(1, tournament_id);
        log.trace("retrieving created tournament data SQL: {}", stmt.toString());
        rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

        return resultSetToJSON(rs);
    }

    /**
     * Select current tournaments from DB
     * @param
     * @return
    */
    public JSONArray getCurrentTournaments() throws SQLException, JSONException {
        PreparedStatement stmt = con.prepareStatement(TOURNAMENT_SELECT_ALL_CURRENT_TOURNAMENTS);
        log.trace("retrieving current tournaments SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

        return resultSetToJSON(rs);
    }

    /**
     * Select current tournaments from DB
     * @param
     * @return
     * @throws MyException
    */
    public JSONArray getTodaysTournaments() throws SQLException, JSONException, MyException {
        PreparedStatement stmt = con.prepareStatement(TOURNAMENT_SELECT_ALL_TODAYS_TOURNAMENTS);
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        String start_time = String.format("%d-%02d-%02d 00:00:00", c.get(Calendar.YEAR), 1+c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH));
        String end_time = String.format("%d-%02d-%02d 00:00:00", c.get(Calendar.YEAR), 1+c.get(Calendar.MONTH), 1+c.get(Calendar.DAY_OF_MONTH));
        stmt.setString(1, end_time);
        stmt.setString(2, start_time);
        log.trace("retrieving current tournaments SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);
        return resultSetToJSON(rs);
    }
    /**
     * Select tournament from DB
     * @param tournamentId
     * @return
    */
    public JSONObject getTournament(long tournamentId) throws SQLException, JSONException {
        PreparedStatement stmt = con.prepareStatement(TOURNAMENT_SELECT_SQL);
        stmt.setLong(1, tournamentId);
        log.trace("retrieving tournament SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

        return resultSetToJSON(rs).getJSONObject(0);
    }

    /**
     * 
     * @param rs
     * @return
     */
    private static JSONArray resultSetToJSON(ResultSet rs) {
        JSONArray jsonArray = new JSONArray();
        try {
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                int columnCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    obj.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
                jsonArray.put(obj);
            }
        } catch (SQLException e) {
            log.error("Error while converting ResultSet to JSON", e.getMessage());
            log.debug("stack trace {}", String.valueOf(e.getStackTrace()));
        }
        return jsonArray;
    }

    public static void close() {
        DBService instance = DBService.getInstance();
        try {
            if (instance.con != null && !instance.con.isClosed())
            { 
                instance.con.close();
            }
        } catch (SQLException e) {
            log.error("Error while closing DB connection {}", instance.con);
        }
        
    }

    public JSONArray insertGroup(JSONObject tournament) throws SQLException {
        String code = tournament.optString("code") + "-" + Math.round(Math.random() * 10000);
        Long tournament_id = tournament.getLong("tournament_id");
        PreparedStatement stmt = con.prepareStatement(GROUP_INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
        stmt.setString(1, code);
        stmt.setLong(2, tournament_id);

        log.trace("Executing SQL: {}", stmt.toString());

        if (stmt.executeUpdate() != 1) {
            log.error("Error while inserting tournament");
        }

        ResultSet rs = stmt.getGeneratedKeys();
        if (!rs.next()) {
            log.error("Error while inserting tournament");
        }
        long tournament_group_id = rs.getLong(1);
        log.trace("SQL executed, tournament_group_id: {}", tournament_group_id);

        return new JSONArray(1).put(new JSONObject().put("tournament_group_id", tournament_group_id).put("code", code)
                .put("tournament_id", tournament_id));
    }

    public JSONObject insertUserGroup(Long user_id, Long tournament_group_id) throws SQLException {

        PreparedStatement stmt = con.prepareStatement(USER_DEDUCT_COIN);

        stmt.setLong(1, TOURNAMENT_ENTRANCE_FEE);
        stmt.setLong(2, user_id);
        
        if(stmt.executeUpdate() != 1) {
            log.warn("Error while deducting coin from user {}, he is lucky will join for free hehehe such a user may not exist though", user_id);
        }
        
        stmt = con.prepareStatement(USER_GROUP_INSERT_SQL, Statement.RETURN_GENERATED_KEYS);
        stmt.setLong(1, user_id);
        stmt.setLong(2, tournament_group_id);
        stmt.setLong(3, user_id);

        log.trace("Executing SQL: {}", stmt.toString());

        if (stmt.executeUpdate() != 1) {
            log.error("Error while inserting user to tournament group");
        }

        ResultSet rs = stmt.getGeneratedKeys();
        if (!rs.next()) {
            log.error("Error while inserting user to tournament group");
        }
        long user_tournament_group_id = rs.getLong(1);
        log.trace("SQL executed, user_tournament_group_id: {}", user_tournament_group_id);

        try {
            return getTournamentGroup(user_tournament_group_id);
        } catch (Exception e) {
            log.error("Error while retrieving user_tournament_group", e);
            return new JSONObject().put("utg_id" , user_tournament_group_id);
        }
    }

    public JSONObject getTournamentGroup(long user_tournament_group_id) throws SQLException {

        PreparedStatement stmt = con.prepareStatement(USER_GROUP_SELECT_SQL);
        stmt.setLong(1, user_tournament_group_id);
        log.trace("retrieving user_tournament_group SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

        return resultSetToJSON(rs).getJSONObject(0);
    }

    /**
     * 
     * @param tournament_id
     * @param user_id
     * @return null if user is not in any tournament group
     * @throws SQLException
     */
    public JSONObject getTournamentGroupIdByTournamentUserId(long tournament_id, long user_id) throws SQLException {
        PreparedStatement stmt = con.prepareStatement(TOURNAMENT_GROUP_BY_USER_AND_TOURNAMENT);
        stmt.setLong(1, tournament_id);
        stmt.setLong(2, user_id);
        log.trace("retrieving user_tournament_group SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

        JSONArray  js = resultSetToJSON(rs);
        if(js.length() == 0){
            return null;
        }
        return js.getJSONObject(0);
    }



    public JSONArray getTournamentGroupLeaderboard(long tournament_group_id) throws SQLException {
        PreparedStatement stmt = con.prepareStatement(TOURNAMENT_GROUP_RANK);
        stmt.setLong(1, tournament_group_id);
        log.trace("retrieving tournament group leaderboard SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

        return resultSetToJSON(rs);
    }

    public JSONArray getTournamentCountryLeaderBoard(long tournament_id) throws SQLException {
        PreparedStatement stmt = con.prepareStatement(TOURNAMENT_COUNTRY_RANK);
        stmt.setLong(1, tournament_id);
        log.trace("retrieving tournament group leaderboard SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

        return resultSetToJSON(rs);
    }

    /**
     * return the information about the last completed tournament
     * u.user_id, u.name, u.countryISO2, utg.score, utg.utg_id, tg.tournament_group_id, t.tournament_id, utg.rewardsClaimed, t.end_time
     *  returns null if there is no completed tournament
     */
    public JSONObject getLastCompletedTournament(long user_id) throws SQLException, JSONException {
        return getLastCompletedTournaments(user_id, 1).optJSONObject(0);
    }

    
    /**
     * return the information about the last maxCount completed tournaments
     * u.user_id, u.name, u.countryISO2, utg.score, utg.utg_id, tg.tournament_group_id, t.tournament_id, utg.rewardsClaimed, t.end_time
     *  @return JSONArray of tournaments, returns empty array if there is no completed tournament
     */
    public JSONArray getLastCompletedTournaments(long user_id, long maxCount ) throws SQLException, JSONException {
        PreparedStatement stmt = con.prepareStatement(USER_LAST_TOURNAMENT);
        stmt.setLong(1, user_id);
        stmt.setLong(2, maxCount);
        log.trace("retrieving last tournament SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

        return resultSetToJSON(rs);
    }

    public JSONObject claimRewards(long user_id, long user_tournament_group_id, Long rank) throws SQLException, MyException{
        PreparedStatement stmt = con.prepareStatement(USER_CLAIM_REWARDS);
        stmt.setLong(1, user_tournament_group_id);
        stmt.setLong(2, TOURNAMENT_AWARDS[rank.intValue()-1]);
        stmt.setLong(3, user_id);

        log.trace("claimRewards SQL: {}", stmt.toString());
        if (stmt.executeUpdate() != 1) {
            log.error("Error while claiming rewards for user {}", user_id);
        }

        return selectUser(user_id);

    }
}
