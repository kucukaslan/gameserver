package tr.com.kucukaslan3.dream.util;

import java.util.UUID;

import org.json.HTTP;
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
            traceId = jsonObject.getString(TRACE_ID);
        }
        
        return setOrGenerateTraceId(traceId);
    }

    public static String setOrGenerateTraceId(String traceId) {
        if(traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(TRACE_ID, traceId);
        return traceId;
    }
}
