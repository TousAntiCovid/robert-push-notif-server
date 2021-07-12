package fr.gouv.stopc.robert.pushnotif.database.service;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface IPushInfoService {

    Optional<PushInfo> findByPushToken(String pushToken);

    Optional<PushInfo> createOrUpdate(PushInfo push);

    void saveAll(List<PushInfo> pushInfos);

    Optional<Date> findMaxLastSuccessfulPush();

    Optional<Date> findMaxLastFailurePush();
}
