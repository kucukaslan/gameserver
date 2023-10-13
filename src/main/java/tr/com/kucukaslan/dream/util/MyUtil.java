package tr.com.kucukaslan.dream.util;

import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.MDC;

import jakarta.servlet.http.HttpServletRequest;

public class MyUtil {
    public static final String TRACE_ID = "traceId";
    
    public static String setTraceId(HttpServletRequest httpRequest) {
        String traceId = MDC.get(TRACE_ID);
        if(traceId == null) {
            traceId = httpRequest.getHeader(TRACE_ID);
        }
        return setOrGenerateTraceId(traceId);
    }

    public static String setTraceId(JSONObject jsonObject) {
        String traceId = MDC.get(TRACE_ID);
        if(traceId == null) {
            traceId = jsonObject.optString(TRACE_ID);
        }
        
        return setOrGenerateTraceId(traceId);
    }

    public static String setOrGenerateTraceId(String traceId) {
        if(traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID, traceId);
        return traceId;
    }

    public static boolean isJSONFormatted(String test) {
        if (test == null || test.isBlank()) {
            return false;
        }

        try {
            new JSONObject(test);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static boolean isJSONFormatted(String test, boolean isNullable) {
        if (test == null) {
            return isNullable;
        }

        try {
            new JSONObject(test.toString());
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    public static String getRandomCountry() {
        String[] countries = { "TR", "US", "DE", "FR", "GB" };
        return countries[(int) (Math.random() * countries.length)];
    }
}
