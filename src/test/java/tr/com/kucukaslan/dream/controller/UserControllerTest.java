package tr.com.kucukaslan.dream.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import tr.com.kucukaslan.dream.service.DBService;

@TestExecutionListeners(listeners = {
        EventPublishingTestExecutionListener.class }, inheritListeners = false, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UserController.class)
class UserControllerTest {

    @BeforeAll
    static void setUp() {
        try {
            DBService.getInstance().initialize();
        } catch (IOException | SQLException e) {
            // log.error("Error while initializing DBService due to {}", e.getMessage());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    private static List<JSONObject> users = new ArrayList<>();

    //   @MockBean
    //   private RegisterUseCase registerUseCase;

    @Test
    // TODO COULD NOT FIND A WAY TO USE THIS ANNOTATION
    // @BeforeTestMethod("event.testContext.testMethod.name matches '.*checkUpdateRequest.*'")
    void createUser() throws Exception {
        // send a request to the endpoint /CreateUserRequest"  
        ResultActions res = mockMvc.perform(post("/CreateUserRequest")
                .contentType("application/json")
                .content(new JSONObject().put("name", "John").toString()));

        res.andExpect(status().isOk());
        res.andExpect(content().contentType(MediaType.APPLICATION_JSON));

        JSONObject user = new JSONObject(
                "{\"date_modified\": \"2023-10-13T15:25:30\",\"level\": 0,\"date_created\": \"2023-10-13T15:25:30\",\"name\": \"1140464 Diye isim mi olur?\",\"id\": 14,\"countryISO2\": \"GB\",\"coin\": 5000}");
        res.andExpect(jsonFieldPresence(user));

        res.andExpect(jsonExactValues(
                new JSONObject("{\"level\": 1,\"coin\": 5000}")));
        user = new JSONObject(res.andReturn().getResponse().getContentAsString());
        users.add(user);
    }

    /**
     * This method test update level request its prerequisite is createUser
     * it uses the output of createUser method to get user_id
     * "/UpdateLevelRequest"
     * @param jsonObject
     * @return
     */
    @Test
    public void checkUpdateRequest() throws Exception {
        // TODO IF beforeMethod does not work then we will call lol 
        createUser();
        for (int i = 0; i < users.size(); i++) {
            ResultActions res = mockMvc.perform(post("/UpdateLevelRequest")
                    .contentType("application/json")
                    .content(new JSONObject().put("user_id", users.get(0).get("user_id")).toString()));

            res.andExpect(status().isOk());
            res.andExpect(content().contentType(MediaType.APPLICATION_JSON));

            res.andExpect(jsonFieldPresence(new JSONObject().put("level", i + 2).put("coin", 5000 + (i + 1) * 25)));
        }
    }

    public ResultMatcher jsonFieldPresence(JSONObject jsonObject) {
        return result -> {
            String content = result.getResponse().getContentAsString();

            Iterator it = jsonObject.keys();
            while (it.hasNext()) {
                if (!content.contains(it.next().toString())) {
                    throw new AssertionError("JSON does not contain key: " + it.next().toString());
                }
            }
        };
    }

    public ResultMatcher jsonExactValues(JSONObject jsonObject) {
        return result -> {
            String content = result.getResponse().getContentAsString();
            JSONObject response = new JSONObject(content);
            Iterator<String> it = (Iterator<String>) jsonObject.keys();
            while (it.hasNext()) {
                String key = it.next();
                if (!content.contains(key.toString())) {
                    throw new AssertionError("JSON does not contain key: " + key.toString());
                } else if (!response.get(key.toString()).equals(jsonObject.get(key.toString()))) {
                    throw new AssertionError("Values are not equal for key: " + key + " expected: "
                            + jsonObject.get(key.toString()) + " actual: " + response.get(key.toString()));
                }
            }
        };
    }
}
