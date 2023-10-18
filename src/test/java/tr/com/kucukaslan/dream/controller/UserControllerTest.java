package tr.com.kucukaslan.dream.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

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
import org.springframework.test.context.event.annotation.BeforeTestMethod;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import tr.com.kucukaslan.dream.service.DBService;
import tr.com.kucukaslan.dream.util.TestUtil;

@TestExecutionListeners(listeners = {
        EventPublishingTestExecutionListener.class }, inheritListeners = false, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = UserController.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserControllerTest {
    @BeforeAll
    public static void init() {
        try {
            DBService.getInstance().initialize();
        } catch (IOException | SQLException e) {
            // log.error("Error while initializing DBService due to {}", e.getMessage());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    // private static Map<String, JSONObject> users = new HashMap<>();
    private static JSONObject user;

    // TODO COULD NOT FIND A WAY TO USE THIS ANNOTATION
    // @BeforeTestMethod("event.testContext.testMethod.name matches '.*checkUpdateRequest.*'")
    @Test
    JSONObject createUser() throws Exception {
        // send a request to the endpoint /CreateUserRequest"  
        ResultActions res = mockMvc.perform(post("/CreateUserRequest")
                .contentType("application/json")
                .content(new JSONObject().put("name", "John").toString()));

        res.andExpect(status().isOk());
        res.andExpect(content().contentType(MediaType.APPLICATION_JSON));

        JSONObject usertmp = new JSONObject(
                "{\"date_modified\": \"2023-10-13T15:25:30\",\"level\": 0,\"date_created\": \"2023-10-13T15:25:30\",\"name\": \"1140464 Diye isim mi olur?\",\"id\": 14,\"countryISO2\": \"GB\",\"coin\": 5000}");
        res.andExpect(TestUtil.jsonFieldPresence(usertmp));

        res.andExpect(TestUtil.jsonExactValues(
                new JSONObject("{\"level\": 1,\"coin\": 5000}")));
        user = new JSONObject(res.andReturn().getResponse().getContentAsString());
        return user;
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
        // assert user != null;
        // while(users.size() < 5 ){
        JSONObject user = createUser();
        // }

        // for (String country : MyUtil.countries) {
        for (int i = 0; i < 5; i++) {
            ResultActions res = mockMvc.perform(post("/UpdateLevelRequest")
                    .contentType("application/json")
                    .content(new JSONObject().put("user_id", user.get("user_id")).toString()));

            res.andExpect(status().isOk());
            res.andExpect(content().contentType(MediaType.APPLICATION_JSON));

            res.andExpect(
                    TestUtil.jsonFieldPresence(new JSONObject().put("level", i + 2).put("coin", 5000 + (i + 1) * 25)));
        }
    }

}
