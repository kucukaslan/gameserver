package tr.com.kucukaslan.dream.service;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.Types;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

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
        PreparedStatement stmt = con.prepareStatement("INSERT INTO user (countryISO2, name) VALUES (?, ?)", new String[]{"id", "coin", "level"});
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
        
        long id = stmt.getGeneratedKeys().getLong(1);
        log.trace("SQL executed, id: {}", id);

        // retrieve the inserted user
        stmt = con.prepareStatement("SELECT * FROM user WHERE id = ?");
        stmt.setLong(1, id);
        log.trace("retrieving created user data SQL: {}", stmt.toString());
        ResultSet rs = stmt.executeQuery();
        log.trace("SQL executed, result set: {}", rs);

       return resultSetToJSON(rs).getJSONObject(0);
    }

    private static JSONArray resultSetToJSON(ResultSet rs) {
        JSONArray jsonArray = new JSONArray();
        try {
            rs.beforeFirst();
            while (rs.next()) {
                JSONObject obj = new JSONObject();
                int columnCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    obj.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
                jsonArray.put(obj);
            }
        } catch (SQLException e) {
            log.error("Error while converting ResultSet to JSON", e);
        }
        return jsonArray;
    }
}
