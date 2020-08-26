package fr.gouv.stopc.robert.pushnotif.batch.utils;

import java.util.Optional;

import fr.gouv.stopc.robert.pushnotif.common.PushDate;
import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

public final class PushBatchUtils {

    private PushBatchUtils() {

        throw new AssertionError();
    }

    public static void setNextPlannedPushDate(PushInfo push, int minHour, int maxHour) {
        Optional.ofNullable(push).ifPresent(pushInfo -> {
            PushDate pushDate = PushDate.builder()
                    .lastPushDate(TimeUtils.getNowAtTimeZoneUTC())
                    .timezone(push.getTimezone())
                    .minPushHour(minHour)
                    .maxPushHour(maxHour)
                    .build();

            TimeUtils.getNextPushDate(pushDate).ifPresent(push::setNextPlannedPush);

        });

    }
}
