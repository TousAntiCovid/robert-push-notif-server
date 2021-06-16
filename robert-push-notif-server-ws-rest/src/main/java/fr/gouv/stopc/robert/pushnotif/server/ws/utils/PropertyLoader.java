package fr.gouv.stopc.robert.pushnotif.server.ws.utils;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class PropertyLoader {

    @Value("${robert.push.notif.server.minPushHour}")
    private Integer minPushHour;

    @Value("${robert.push.notif.server.maxPushHour}")
    private Integer maxPushHour;

}
