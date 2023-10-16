package tr.com.kucukaslan.dream.util;

import java.util.UUID;

import org.json.JSONObject;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MyUtil {
    public static final String[] countries = { "TR", "US", "DE", "FR", "GB" };

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
            log.warn("Error while parsing json string {} due to {}", test, ex.getMessage());
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
        return countries[(int) (Math.random() * countries.length)];
    }

    public static ResponseEntity<String> getResponseEntity(Exception ex, String... args) {
        log.error("Error {} due to {}", args, ex.getMessage());
        log.debug("{}", ex);
        log.trace("{}", String.valueOf(ex.getStackTrace()));
        // join args with comma and space to create a message
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.APPLICATION_JSON)
                    .body(
                            new JSONObject()
                                    .put("traceId", MDC.get(MyUtil.TRACE_ID))
                                    .put("message", String.join(", ", args))
                                    .put("exception", ex.getMessage()).toString());
    }
}
