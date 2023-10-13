package tr.com.kucukaslan.dream.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import lombok.extern.slf4j.Slf4j;
import tr.com.kucukaslan.dream.controller.TournamentController;

@Slf4j
public class TournamentGroup {
    private Map<String, Long> groupMap;
    private static Map<String, ConcurrentLinkedQueue<TournamentGroup>> queueMap = new HashMap<>() {
        {
            put("TR", new ConcurrentLinkedQueue<TournamentGroup>());
            put("US", new ConcurrentLinkedQueue<TournamentGroup>());
            put("DE", new ConcurrentLinkedQueue<TournamentGroup>());
            put("FR", new ConcurrentLinkedQueue<TournamentGroup>());
            put("GB", new ConcurrentLinkedQueue<TournamentGroup>());
        }
    };

    private TournamentGroup() {
        groupMap = new HashMap<>();
    }

    public static synchronized TournamentGroup join(String countryISO2, Long userId, TournamentController controller) {
        TournamentGroup group = queueMap.get(countryISO2).poll();
        log.trace("{}: requested to join country: {} group {}" , userId, countryISO2, group);
        if (group == null) {
            log.trace("{}: no group found for country: {}, creating one!", userId, countryISO2 );
            group  = new TournamentGroup();
            group.put(countryISO2, userId);
            // add to other queues to wait other players
            for (String country : queueMap.keySet()) {
                if (!country.equals(countryISO2)) {
                    queueMap.get(country).add(group);
                }
            }
        } else {
            group.put(countryISO2, userId);
        }
        return group;

    }

    public synchronized void put(String countryISO2, Long userId) {
        log.trace("{}: put country: {} to group {}" , userId, countryISO2, groupMap);
        groupMap.put(countryISO2, userId);
        if(groupMap.size() == 5) {
            log.trace("{}: notify all for group {}" , userId, groupMap);
            // todo I'm not sure if we can be sure that all threads are waiting at this point
            // notifyAll();
        }
    }

}
