package fr.gouv.stopc.robert.pushnotif.batch.utils;

import java.io.File;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class PropertyLoader {

    @Value("${robert.push.notif.server.minPushHour}")
    private Integer minPushHour;

    @Value("${robert.push.notif.server.maxPushHour}")
    private Integer maxPushHour;

    @Value("${robert.push.notif.server.notification.available-languages}")
    private String[] availableNotificationLanguages;

    @Value("${robert.push.notif.server.notification.url.version}")
    private String notificationContentUrlVersion;

    @Value("${robert.push.notif.server.notification.url}")
    private String notificationContentUrl;

    @Value("${robert.push.notif.server.apns.auth.token.file}")
    private String apnsAuthTokenFile;

    @Value("${robert.push.notif.server.apns.auth.key-id}")
    private String apnsAuthKeyId;

    @Value("${robert.push.notif.server.apns.team-id}")
    private String apnsTeamId;

    @Value("${robert.push.notif.server.apns.host}")
    private String apnsHost;

    @Value("#{'${robert.push.notif.server.apns.inactive-rejection-reason}'.split(',')}")
    private List<String> apnsInactiveRejectionReason;

    @Value("${robert.push.notif.server.apns.topic}")
    private String apnsTopic;

    @Value("${robert.push.notif.server.push.date.enable}")
    private boolean enablePushDate;

    @Value("${robert.push.notif.server.apns.secondary.enable}")
    private boolean enableSecondaryPush;

    @Value("${robert.push.notif.server.batch.grid-size}")
    private int gridSize;

    @Value("${robert.push.notif.server.batch.chunk-size}")
    private int chunkSize;

    @Value("${robert.push.notif.server.batch.page-size}")
    private int pageSize;
    
    private File apnsAuthFile;

    @PostConstruct
    private void initFile() {
        this.apnsAuthFile = new File(this.apnsAuthTokenFile);
    }

}
