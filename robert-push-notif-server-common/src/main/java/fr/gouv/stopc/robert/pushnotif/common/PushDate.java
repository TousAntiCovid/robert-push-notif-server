package fr.gouv.stopc.robert.pushnotif.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Builder
@Data
@AllArgsConstructor
public class PushDate {

    private int minPushHour;

    private int maxPushHour;

    private String timezone;

    private Date lastPushDate;

}
