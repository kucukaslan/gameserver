package tr.com.kucukaslan.dream.controller;

import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.dream.util.MyUtil;

@Slf4j
@RestController
public class UserController {

    @RequestMapping(value = "/CreateUserRequest", method = RequestMethod.POST)
    public ResponseEntity<String> createUserRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        log.info("CreateUserRequest method is called");
        //TODO: implement
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "USER Is not created LoL").toString());
    }

    @RequestMapping(value = "/UpdateLevelRequest", method = RequestMethod.POST)
    public ResponseEntity<String> updateLevelRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        //TODO: implement
        log.info("UpdateLevelRequest method is called");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "Level is not updated either LoL").toString());
    }
}
