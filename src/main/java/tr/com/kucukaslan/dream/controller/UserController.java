package tr.com.kucukaslan.dream.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.dream.service.DBService;
import tr.com.kucukaslan.dream.util.MyUtil;

@Slf4j
@RestController
public class UserController {

    /**
     * This request creates a new user, returning a unique user ID, level,
coins, and country ISO2 code.
     * @param body optional json object with countryISO2 and name
     * @param httpRequest
     * @return
     */
    @RequestMapping(value = "/CreateUserRequest", method = RequestMethod.POST)
    public ResponseEntity<String> createUserRequest(@RequestBody (required = false) String body, HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        log.debug("CreateUserRequest method is called");
        JSONObject user = new JSONObject();
        if(MyUtil.isJSONFormatted(body)) {
            user = new JSONObject(body);
        } else {
            user.put("name", JSONObject.NULL);
        }
        user.put("countryISO2", MyUtil.getRandomCountry());
        
        JSONObject response = new JSONObject();
        try {
            response = DBService.getInstance().insertUser(user);
        } catch (SQLException | JSONException e) {
            log.error("Error while inserting user {} due {} ", user, e.getMessage());
            log.debug("{}", e);
            log.debug(String.valueOf(e.getStackTrace()));
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject().put("message", "Internal Server Error, couldn't create user.").toString());
        }
    

        
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(response.toString());
    }

    @RequestMapping(value = "/UpdateLevelRequest", method = RequestMethod.POST)
    public ResponseEntity<String> updateLevelRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        //TODO: implement
        log.debug("UpdateLevelRequest method is called");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "Level is not updated either LoL").toString());
    }
}
