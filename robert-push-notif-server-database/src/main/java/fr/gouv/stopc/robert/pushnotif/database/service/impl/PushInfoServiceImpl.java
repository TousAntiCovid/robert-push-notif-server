package fr.gouv.stopc.robert.pushnotif.database.service.impl;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.repository.PushInfoRepository;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;

@Service
public class PushInfoServiceImpl implements IPushInfoService {

    private final PushInfoRepository pushInfoRepository;

    @Inject
    public PushInfoServiceImpl(PushInfoRepository pushInfoRepository) {
        this.pushInfoRepository = pushInfoRepository;

    }

    @Override
    public Optional<PushInfo> findByPushToken(String pushToken) {
        return Optional.ofNullable(pushToken).filter(StringUtils::isNotBlank).flatMap(this.pushInfoRepository::findById);
    }

    @Override
    public Optional<PushInfo> createOrUpdate(PushInfo push) {

        return Optional.ofNullable(push).map(this.pushInfoRepository::save);
    }

}
