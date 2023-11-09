package tr.com.kucukaslan.gameserver.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import tr.com.kucukaslan.gameserver.service.DBService;
import tr.com.kucukaslan.gameserver.util.MyUtil;
import tr.com.kucukaslan.gameserver.util.TestUtil;

@TestExecutionListeners(listeners = {
        EventPublishingTestExecutionListener.class }, inheritListeners = false, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = { TournamentController.class, UserController.class })
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TournamentTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    public static void init() {
        try {
            DBService.getInstance().initialize();
        } catch (IOException | SQLException e) {
            // log.error("Error while initializing DBService due to {}", e.getMessage());
        }
    }

    @Test
    void testTournamentLevelCheck() throws Exception {
        JSONObject user = createUser();
        // test that under level 20 it cannot join tournaments
        ResultActions res = mockMvc.perform(post("/EnterTournamentRequest")
                .contentType("application/json")
                .content(user.toString()));

        res.andExpect(status().is5xxServerError());
        res.andExpect(content().contentType(MediaType.APPLICATION_JSON));

        res.andExpect(TestUtil.jsonFieldPresence(new JSONObject().put("message", "")));

    }

    Map<String, JSONObject> users = new HashMap<>();
    Map<String, JSONObject> group = new HashMap<>();

    @Test
    void testJoinTournament() throws Exception {
        Map<String, Long> scores = new HashMap<>();
        scores.put(MyUtil.countries[0], 7l);
        scores.put(MyUtil.countries[1], 5l);
        scores.put(MyUtil.countries[2], 3l);
        scores.put(MyUtil.countries[3], 2l);
        scores.put(MyUtil.countries[4], 1l);

        synchronized (users) {

            JSONObject user;
            while (users.size() < 5) {
                user = createUser();
                users.put(user.getString("countryISO2"), user);
            }

            // ensure that every one of them is over level 20
            for (String country : MyUtil.countries) {
                user = users.get(country);
                for (int i = 0; i < 23; i++) {
                    updateLevel(user);
                }
            }

            // enroll them to tournament
            ExecutorService es = Executors.newFixedThreadPool(10);

            for (String country : MyUtil.countries) {
                JSONObject u = users.get(country);
                es.submit(() -> {
                    try {
                        ResultActions res = mockMvc.perform(post("/EnterTournamentRequest")
                                .contentType("application/json")
                                .content(u.toString()));
                        res.andExpect(status().isOk());
                        res.andExpect(content().contentType(MediaType.APPLICATION_JSON));

                        res.andExpect(TestUtil.jsonFieldPresence(new JSONObject("tournament_group_id")));

                        for (int i = 0; i < scores.get(country); i++) {
                            updateLevel(u);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            es.shutdown();
            es.awaitTermination(120, TimeUnit.SECONDS);
            users.notifyAll();
        }

    }

    @Test
    public void claimRewardsNoTournament() throws Exception {
        JSONObject user = createUser();
        ResultActions res = mockMvc.perform(post("/ClaimRewardRequest")
                .contentType("application/json")
                .content(user.toString()));
        res.andExpect(status().is4xxClientError());
    }

    @Test
    public void getGroupRankRequestUserNotRegisteredToTournament() throws Exception {
        JSONObject user = createUser();
        ResultActions res = mockMvc.perform(post("/GetGroupRankRequest")
                .contentType("application/json")
                .content(user.toString()));
        res.andExpect(status().is4xxClientError());
    }

    @Test
    public void getGroupLeaderboardRequestUserNotRegisteredToTournament() throws Exception {
        JSONObject user = createUser();
        ResultActions res = mockMvc.perform(post("/GetGroupLeaderboardRequest")
                .contentType("application/json")
                .content(user.toString()));
        res.andExpect(status().is4xxClientError());
    }

    @Test
    // DependsOn testJoinTournament
    public void getGroupRankRequest() throws Exception {
        // synchronized (users) {
        if (users.isEmpty()) {
            try {
                users.wait(50000);
            } catch (InterruptedException e) {

            }
            if (users.isEmpty()) {
                // methods are in principal independent
                // but this one depends on the testJoinTournament
                // for the users to be created. But @Before* annotations didn't help
                // so just return :(
                return;
            }
            // }
            // so we have actual users created at testJoinTournament
            for (String country : MyUtil.countries) {
                ResultActions res = mockMvc.perform(post("/GetGroupLeaderboardRequest")
                        .contentType("application/json")
                        .content(users.get(country).toString()));
                res.andExpect(status().isOk());

            }

        }
    }

    @Test
    // DependsOn testJoinTournament
    public void getCountryLeaderBoardRequest() throws Exception {
        ResultActions res = mockMvc.perform(get("/GetCountryLeaderboardRequest")
                .contentType("application/json"));
        res.andExpect(status().isOk());
    }

    private JSONObject createUser() throws Exception {
        // send a request to the endpoint /CreateUserRequest"  
        ResultActions res = mockMvc.perform(post("/CreateUserRequest")
                .contentType("application/json")
                .content(new JSONObject().put("name", "John").toString()));

        res.andExpect(status().isOk());
        res.andExpect(content().contentType(MediaType.APPLICATION_JSON));

        return new JSONObject(res.andReturn().getResponse().getContentAsString());
    }

    private JSONObject updateLevel(JSONObject user) throws Exception {
        ResultActions res = mockMvc.perform(post("/UpdateLevelRequest")
                .contentType("application/json")
                .content(new JSONObject().put("user_id", user.get("user_id")).toString()));

        res.andExpect(status().isOk());
        res.andExpect(content().contentType(MediaType.APPLICATION_JSON));
        return new JSONObject(res.andReturn().getResponse().getContentAsString());

    }
}
