package tr.com.kucukaslan.dream.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

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

    private static DBService instance;
    Properties properties;
    Connection con;

    private static final String USER_INSERT_SQL = "INSERT INTO user (countryISO2, name) VALUES (?, ?)";
    // UPDATE `user` SET `level` = '1', `coin` = '5025', `name` = 'Wow updated', `date_modified` = CURRENT_TIMESTAMP WHERE `user`.`user_id` = 10
    private static final String USER_UPDATE_SQL = "UPDATE  `user` SET `level` = `level` + ?, `coin` = `coin` + ?, `date_modified` = CURRENT_TIMESTAMP WHERE `user`.`user_id` = ?";
    private static final String USER_SELECT_SQL = "SELECT * FROM `user` WHERE `user_id` = ?";

    private static final long LEVEL_AWARD = 25;
    private static final long[] TOURNAMENT_AWARDS = { 10000, 5000 };

    private DBService() {

    }

    public static DBService getInstance() {
        if (instance == null) {
            instance = new DBService();
        }
        return instance;
    }

    public void initialize() throws FileNotFoundException, IOException, SQLException {
        if (instance == null) {
            instance = new DBService();
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
        con = DriverManager.getConnection("jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName, dbUser, dbPass);
        log.info("DB Connection to {} established", con.getMetaData().getURL());
    }

    public JSONObject insertUser(JSONObject user) throws SQLException{
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

        // retrieve the inserted user
        stmt = con.prepareStatement(USER_SELECT_SQL);
        stmt.setLong(1, user_id);
        log.trace("retrieving created user data SQL: {}", stmt.toString());
        rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

       return resultSetToJSON(rs).getJSONObject(0);
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

        // retrieve the updated user
        stmt = con.prepareStatement(USER_SELECT_SQL);
        stmt.setLong(1, user.getLong("user_id"));
        log.trace("retrieving updated user data SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

        return resultSetToJSON(rs).getJSONObject(0);
    }

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
            log.debug("stack trace {}", e.getStackTrace());
        }
        return jsonArray;
    }
}
