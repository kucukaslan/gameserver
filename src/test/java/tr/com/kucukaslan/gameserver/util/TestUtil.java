package tr.com.kucukaslan.gameserver.util;

import java.util.Iterator;

import org.json.JSONObject;
import org.springframework.test.web.servlet.ResultMatcher;

public class TestUtil {

    public static ResultMatcher jsonFieldPresence(JSONObject jsonObject) {
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

    public static ResultMatcher jsonExactValues(JSONObject jsonObject) {
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
