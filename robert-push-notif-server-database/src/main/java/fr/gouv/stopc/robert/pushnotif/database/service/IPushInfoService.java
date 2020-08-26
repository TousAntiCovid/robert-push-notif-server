package fr.gouv.stopc.robert.pushnotif.database.service;

import java.util.List;
import java.util.Optional;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

public interface IPushInfoService {

    Optional<PushInfo> findByPushToken(String pushToken);

    Optional<PushInfo> createOrUpdate(PushInfo push);
    
    void saveAll(List<PushInfo> pushInfos);
}
