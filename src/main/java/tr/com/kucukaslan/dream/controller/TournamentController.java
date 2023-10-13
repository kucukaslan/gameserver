package tr.com.kucukaslan.dream.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import tr.com.kucukaslan.dream.util.TournamentGroup;

@Slf4j
@RestController
public class TournamentController {
    Map<String, ConcurrentLinkedQueue<Long>> queueMap = new HashMap<>() {
        {
            put("TR", new ConcurrentLinkedQueue<Long>());
            put("US", new ConcurrentLinkedQueue<Long>());
            put("DE", new ConcurrentLinkedQueue<Long>());
            put("FR", new ConcurrentLinkedQueue<Long>());
            put("GB", new ConcurrentLinkedQueue<Long>());
        }
    };

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

    // TODO would it be  nice to make it a get request? Is it public?
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
     * GetCountryLeaderboardRequest: This request retrieves the leaderboard data of the countries for a tournament.
    */
    @RequestMapping(value = "/GetCountryLeaderboardRequest", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<String> getCountryLeaderboardRequest(@RequestBody(required = false) String body, HttpServletRequest httpRequest){
        MyUtil.setTraceId(httpRequest);
        log.debug("GetCountryLeaderboardRequest method is called");
        //TODO: implement
        return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "leaderboard is not retrieved LoL").toString());
    }

    @RequestMapping("/test")
    public ResponseEntity<String> test() {
        String[] countries = new String[] { "TR", "US", "DE", "FR", "GB" };
        log.info("request is received");
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        for (Long i = 0L; i < 5; i++) {
            Long j = i.longValue()*1000 + Math.round(Math.random()*1000d);
            executorService.submit(() -> {
                log.debug("{}: joining country: {} ", j.intValue(), countries[j.intValue()]);
                TournamentGroup group = TournamentGroup.join(countries[j.intValue()], j, this);
                log.debug("{} joined country {} to group: {} ", j.intValue(), countries[j.intValue()], group);
                // try {
                //     group.wait();
                // } catch (InterruptedException e) {
                //     // TODO Auto-generated catch block
                //     e.printStackTrace();
                // }
                log.debug("{}: woke up", j);

            });
        }

        try {
            executorService.awaitTermination(15000l, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "test is ok").toString());
    }
}