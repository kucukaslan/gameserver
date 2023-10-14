package tr.com.kucukaslan.dream.util;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TournamentGroup {
    private volatile Map<String, Long> groupMap;
    private volatile JSONObject groupJson;

    TournamentGroup() {
        groupMap = new HashMap<>();
    }

    public synchronized JSONObject getGroupJson() {
        if (groupJson == null) {
            groupJson = new JSONObject(groupMap);
        }
        return groupJson;
    }

    public synchronized void setGroupJson(JSONObject groupJson) {
        this.groupJson = groupJson;
    }


    public synchronized void put(String countryISO2, Long userId) {
        log.trace("{}: put country: {} to group {}" , userId, countryISO2, groupMap);
        groupMap.put(countryISO2, userId);
    }

    public synchronized int size() {
        return groupMap.size();
    }

    @Override
    public String toString() {
     return  toString(0)  ; 
    }
    
    public String toString(int i) {
        return new JSONObject().put("group", groupJson).put("members", groupMap).toString(i)   ; 
    }
}
