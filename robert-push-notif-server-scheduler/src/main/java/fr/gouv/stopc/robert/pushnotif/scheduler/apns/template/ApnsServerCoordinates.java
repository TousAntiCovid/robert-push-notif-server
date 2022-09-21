package fr.gouv.stopc.robert.pushnotif.scheduler.apns.template;

import lombok.Value;

@Value
public class ApnsServerCoordinates {

    String host;

    int port;

    @Override
    public String toString() {
        return String.format("%s:%d", host, port);
    }
}
