package tr.com.kucukaslan.dream.controller;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
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
import tr.com.kucukaslan.dream.service.DBService;
import tr.com.kucukaslan.dream.util.MyException;
import tr.com.kucukaslan.dream.util.MyUtil;
import tr.com.kucukaslan.dream.util.TournamentGroup;
import tr.com.kucukaslan.dream.util.TournamentManager;

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
        // TODO check if already joined
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

        try {
            user = DBService.getInstance().selectUser(user.getLong("user_id"));
        } catch (SQLException | MyException | JSONException e) {
            return MyUtil.getResponseEntity(e, "User cannot be found " + user.getLong("user_id"));
        }
        long user_id = user.getLong("user_id");
        String country = user.getString("countryISO2");

        log.debug("{}: joining country: {} ", user_id, country);
        TournamentGroup group;
        try {
            group = TournamentManager.getInstance().join(country, user);
        } catch (SQLException | JSONException | MyException e) {
            // TODO a possible exception is that user is already joined to the tournament
            // It might be sensible to discard that exception make this operation `idempotent`
            return MyUtil.getResponseEntity(e,  "Exception while joining " + user_id+ " from "+ country+" to tournament");
        }
        while (true) {
            long count = 0l;
            long sleep = 500l;
            try {
                // TODO What a shame LOL

                if (count * sleep % 10000L == 0)
                    log.debug("{}:{} waiting group {}", user_id, country, group);
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            if (group.size() == 5) {
                break;
            }
        }
        log.debug("{} joined country {} to group: {} ",user_id, country, group);
        // try {
        //     group.wait();
        // } catch (InterruptedException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }
        log.debug("{}: woke up {}", user_id, String.valueOf(group));
        JSONObject relation = null;
        try {
            relation = TournamentManager.getInstance().join(user_id, group.getGroupJson().getLong("tournament_group_id"));
        } catch (SQLException | JSONException e) {
            return MyUtil.getResponseEntity(e,  "Exception while joining " + user_id+ " from "+ country+" to tournament group");
        }
        log.debug("{}: joined to tournament group relation: {} ", user_id, relation);

        // todo return leaderboard

        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(relation.toString());
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
        JSONObject input;
        if (MyUtil.isJSONFormatted(body)) {
            input = new JSONObject(body);
            if (!input.has("user_id")) {
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                        .body(new JSONObject().put("traceId", MDC.get(MyUtil.TRACE_ID))
                                .put("message", "`user_id` field is required").toString());
            }
        } else {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject().put("traceId", MDC.get(MyUtil.TRACE_ID))
                            .put("message", "body is not a valid json").toString());
        }
        Long user_id =input.getLong("user_id");
        Long tournament_group_id = input.optLong("tournament_group_id");
        Long tournament_id = input.optLong("tournament_id");
        try {
            if(!input.has("tournament_id")) {
                log.debug("request does not contain tournament_id, retrieving tournament group of the user by user_id");
                tournament_id= TournamentManager.getInstance().getTodaysTournament().getLong("tournament_id");
            }

            JSONObject tournamentGroup = DBService.getInstance().getTournamentGroupIdByTournamentUserId(tournament_id,
                    user_id);
            if (tournamentGroup == null) {
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                        .body(new JSONObject().put("traceId", MDC.get(MyUtil.TRACE_ID))
                                .put("message", "user is not registered to tournament " + tournament_id).toString());
            }
            tournament_group_id = tournamentGroup.getLong("tournament_group_id");
            
            JSONArray group = DBService.getInstance().getTournamentGroupLeaderboard(tournament_group_id);
            int rank = 1;
            long score = 0;
            for(int i = 0; i < group.length(); i++){
                if(group.getJSONObject(i).getLong("user_id") == user_id){
                    rank = i+1;
                    score = group.getJSONObject(i).optLong("score");
                    break;
                }
            }
            log.debug("rank of {} in tournament {} in group {} is {} with score", user_id, tournament_id, tournament_group_id, rank, score);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject().put("rank", rank).put("user_id", "user_id").put("score", score).put("tournament_id", tournament_id).toString());
        } catch (SQLException | JSONException | MyException e) {
            return MyUtil.getResponseEntity(e,  "Exception while retrieving tournament group " + tournament_group_id);
        } 
    }
/**
     * GetCountryLeaderboardRequest: This request retrieves the leaderboard data of the countries for a tournament.
    */
    @RequestMapping(value = "/GetCountryLeaderboardRequest", method = { RequestMethod.GET, RequestMethod.POST })
    public ResponseEntity<String> getCountryLeaderboardRequest(@RequestBody(required = false) String body, HttpServletRequest httpRequest){
        MyUtil.setTraceId(httpRequest);
        log.debug("GetCountryLeaderboardRequest method is called");
        JSONObject input = new JSONObject();
        if (MyUtil.isJSONFormatted(body)) {
            input = new JSONObject(body);
        }

        Long tournament_id = input.optLong("tournament_id");
        try {
            if(!input.has("tournament_id")) {
                log.debug("request does not contain tournament_id, retrieving today's tournament");
                JSONObject tourn = TournamentManager.getInstance().getTodaysTournament();
                
                tournament_id = tourn.getLong("tournament_id");
            }
            
            JSONArray leaderborad = DBService.getInstance().getTournamentCountryLeaderBoard(tournament_id);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(leaderborad.toString());
        } catch (SQLException | JSONException | MyException e) {
            return MyUtil.getResponseEntity(e,  "Exception while retrieving tournament group " + tournament_id);
        } 
    }

    /**
     *
     * Every tournament group has its leaderboard.
     * It is sorted by the highest to lowest scores, displaying user ID, username, country, and tournament score.
     * Provided leaderboard data should be real-time.
     * The first-place user wins 10,000 coins, while the second-place user wins 5,000 coins.
     * GetGroupLeaderboardRequest: This request fetches the leaderboard data of a tournament group.
     * @param body if it contains tournament_group_id it will be used to retrieve the leaderboard of the group
     * otherwise it will check for user_id and retrieve the leaderboard of the group that user is in. 
     * 
    */
    @RequestMapping(value = "/GetGroupLeaderboardRequest", method = {  RequestMethod.POST })
    public ResponseEntity<String> getGroupLeaderboardRequest(@RequestBody String body, HttpServletRequest httpRequest) {
        MyUtil.setTraceId(httpRequest);
        log.debug("GetGroupLeaderboardRequest method is called");
        JSONObject input;
        if (MyUtil.isJSONFormatted(body)) {
            input = new JSONObject(body);
            if (!input.has("tournament_group_id") && !input.has("user_id")) {
                return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                        .body(new JSONObject().put("traceId", MDC.get(MyUtil.TRACE_ID))
                                .put("message", "tournament_group_id is missing, there is no `user_id` to fallback either").toString());
            }
        } else {
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                    .body(new JSONObject().put("traceId", MDC.get(MyUtil.TRACE_ID))
                            .put("message", "body is not a valid json").toString());
        }

        Long tournament_group_id = input.optLong("tournament_group_id");
        try {
            if(!input.has("tournament_group_id")) {
                log.debug(
                        "request does not contain tournament_group_id, retrieving tournament group of the user by user_id");
                JSONObject tourn = TournamentManager.getInstance().getTodaysTournament();
                
                JSONObject tournamentGroup = DBService.getInstance()
                        .getTournamentGroupIdByTournamentUserId(tourn.getLong("tournament_id"),
                                input.getLong("user_id"));
                if (tournamentGroup == null) {
                    return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON)
                            .body(new JSONObject().put("traceId", MDC.get(MyUtil.TRACE_ID))
                                    .put("message",
                                            "user is not registered to tournament " + tourn.getLong("tournament_id"))
                                    .toString());
                }
                tournament_group_id = tournamentGroup.getLong("tournament_group_id");
            }
            
            JSONArray group = DBService.getInstance().getTournamentGroupLeaderboard(tournament_group_id);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                    .body(group.toString());
        } catch (SQLException | JSONException | MyException e) {
            return MyUtil.getResponseEntity(e,  "Exception while retrieving tournament group " + tournament_group_id);
        } 
    }

    @RequestMapping("/test")
    public ResponseEntity<String> test() {
        String[] countries = new String[] { "TR", "US", "DE", "FR", "GB" };
        log.info("request is received");
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (Long i = 0L; i < 10000; i++) {
            Long id = i.longValue() * 100000 + Math.round(Math.random() * 100000d);
            Long j = i.longValue() % 5;
            executorService.execute(() -> {
                log.debug("{}: joining country: {} ", id, countries[j.intValue()]);
                TournamentGroup group;
                try {
                    group = TournamentManager.getInstance().join(countries[j.intValue()],
                            new JSONObject().put("user_id", id).put("coin", 1000L));
                } catch (SQLException | JSONException | MyException e) {
                    log.error("error while joining country: {} ", countries[j.intValue()], e);
                    return ;
                }
                while (true) {
                    long count = 0l;
                    long sleep = 10l;
                    try {
                        // TODO What a shame LOL

                        if (count * sleep % 10000L == 0)
                        log.debug("{}:{} waiting group {}", id,  countries[j.intValue()], group);
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if(group.size() == 5) {
                        break;
                    }
                }
                log.debug("{} joined country {} to group: {} ", j.intValue(), countries[j.intValue()], group);
                // try {
                //     group.wait();
                // } catch (InterruptedException e) {
                //     // TODO Auto-generated catch block
                //     e.printStackTrace();
                // }
                log.debug("{}: woke up {}", j, String.valueOf(group));
                JSONObject relation = null;
                try {
                    relation = TournamentManager.getInstance().join(id, group.getGroupJson().getLong("tournament_group_id"));
                } catch (SQLException | JSONException e) {
                    log.error("error while joining country: {} ", countries[j.intValue()], e);    
                }
                log.debug("{}: joined to tournament group relation: {} ", id, relation);

            });
        }

        try {
            executorService.shutdown();
            executorService.awaitTermination(30l, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
                .body(new JSONObject().put("message", "test is ok").toString());
    }
}