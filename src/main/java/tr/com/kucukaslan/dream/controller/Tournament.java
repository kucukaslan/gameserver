package tr.com.kucukaslan.dream.controller;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
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
public class Tournament {
    /**
     * EnterTournamentRequest: This request allows a user to join the current tournament and returns the current group leaderboard.
    */
    @RequestMapping(value = "/EnterTournamentRequest", method = RequestMethod.POST)
    public ResponseEntity<String> enterTournamentRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        log.debug("EnterTournamentRequest method is called");
        //TODO: implement
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "not entered to tournament LoL").toString());
    }

    /**
     * ClaimRewardRequest: This request allows users to claim tournament rewards and returns updated progress data.
    */
    @RequestMapping(value = "/ClaimRewardRequest", method = RequestMethod.POST)
    public ResponseEntity<String> claimRewardRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        log.debug("ClaimRewardRequest method is called");
        //TODO: implement
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "reward is not claimed LoL").toString());
    }

    // TODO would it be nice to make it a get request? Is it public?
    /**
     * GetGroupRankRequest: This request retrieves the player's rank for any tournament.
    */
    @RequestMapping(value = "/GetGroupRankRequest", method = RequestMethod.POST)
    public ResponseEntity<String> getGroupRankRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        log.debug("GetGroupRankRequest method is called");
        //TODO: implement
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "rank is not retrieved LoL").toString());
    }

    /**
     * GetGroupLeaderboardRequest: This request fetches the leaderboard data of a tournament group.
    */
    @RequestMapping(value = "/GetGroupLeaderboardRequest", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<String> getGroupLeaderboardRequest(@RequestBody(required = false) String body, HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        log.debug("GetGroupLeaderboardRequest method is called");
        //TODO: implement
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "leaderboard is not retrieved LoL").toString());
    }

    /**
     * GetCountryLeaderboardRequest: This request retrieves the leaderboard data of thecountries for a tournament.
    */
    @RequestMapping(value = "/GetCountryLeaderboardRequest", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<String> getCountryLeaderboardRequest(@RequestBody(required = false) String body, HttpServletRequest httpRequest){
        MyUtil.setTraceId(httpRequest);
        log.debug("GetCountryLeaderboardRequest method is called");
        //TODO: implement
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "leaderboard is not retrieved LoL").toString());
    }
}