package tr.com.kucukaslan.gameserver.controller;

import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.gameserver.service.DBService;
import tr.com.kucukaslan.gameserver.util.MyException;
import tr.com.kucukaslan.gameserver.util.MyUtil;
import tr.com.kucukaslan.gameserver.util.TournamentManager;

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
    @RequestMapping(value = "/CreateUserRequest", method = { RequestMethod.POST, RequestMethod.GET })
    public ResponseEntity<String> createUserRequest(@RequestBody(required = false) String body,
            HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        log.debug("CreateUserRequest method is called");
        JSONObject user = new JSONObject();
        if (MyUtil.isJSONFormatted(body)) {
            user = new JSONObject(body);
        } else {
            user.put("name", JSONObject.NULL);
        }
        user.put("countryISO2", MyUtil.getRandomCountry());

        JSONObject response = new JSONObject();
        try {
            response = DBService.getInstance().insertUser(user);
        } catch (SQLException | JSONException | MyException e) {
            MyUtil.getResponseEntity(e);
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(response.toString());
    }

    /**
     * This request is sent by the client after each level completion. It
    updates the user's level and coins. Returns updated progress data.
     * @param body json object with `user_id`
     * @param httpRequest
     * @return
     */
    @RequestMapping(value = "/UpdateLevelRequest", method = RequestMethod.POST)
    public ResponseEntity<String> updateLevelRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        log.debug("UpdateLevelRequest method is called");
        MyUtil.setTraceId(httpRequest);
        JSONObject user = new JSONObject();
        if (MyUtil.isJSONFormatted(body)) {
            user = new JSONObject(body);
            if (!user.has("user_id")) {
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                        .body(new JSONObject().put("traceId", MDC.get(MyUtil.TRACE_ID))
                                .put("message", "user_id is missing").toString());
            }
        } else {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject().put("traceId", MDC.get(MyUtil.TRACE_ID))
                            .put("message", "body is not a valid json").toString());
        }

        JSONObject response;
        try {
            response = DBService.getInstance().incrementUserLevel(user);
        } catch (SQLException e) {
            log.error("Error while incrementing user level {} due {} ", user, e.getMessage());
            log.debug("{}", e);
            log.debug(String.valueOf(e.getStackTrace()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON)
                    .body(
                            new JSONObject()
                                    .put("traceId", MDC.get(MyUtil.TRACE_ID))
                                    .put("message", "Internal Server Error, couldn't update user level.").toString());
        } catch (MyException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON).body(
                    e.toJSON().put(MyUtil.TRACE_ID, MDC.get(MyUtil.TRACE_ID)).toString());
        }

        // TODO maybe we can write it to redis and update it on another thread instead of blocking the request
        try {
            TournamentManager.getInstance().updateTournamentScore(user.getLong("user_id"));
        } catch (SQLException | JSONException | MyException e) {
            // TODO should we return error instead?
            log.debug("Error while updating tournament score {} due {} ", user, e.getMessage());
            log.debug("{}", e);
            log.debug(String.valueOf(e.getStackTrace()));
        }

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(response.toString());
    }
}
