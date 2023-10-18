package tr.com.kucukaslan.dream.util;

import org.json.JSONObject;

public class MyException extends Exception {

    private static final long serialVersionUID = 1L;

    public MyException(String message) {
        super(message);
    }

    public MyException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", getMessage());
        jsonObject.put("cause", getCause());
        return jsonObject;
    }
}
