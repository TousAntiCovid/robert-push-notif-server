package fr.gouv.stopc.robert.pushnotif.database.service.impl;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.repository.PushInfoRepository;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.inject.Inject;

import java.util.List;
import java.util.Optional;

@Service
public class PushInfoServiceImpl implements IPushInfoService {

    private final PushInfoRepository pushInfoRepository;

    @Inject
    public PushInfoServiceImpl(PushInfoRepository pushInfoRepository) {
        this.pushInfoRepository = pushInfoRepository;

    }

    @Override
    public Optional<PushInfo> findByPushToken(String pushToken) {
        return Optional.ofNullable(pushToken).filter(StringUtils::isNotBlank)
                .flatMap(this.pushInfoRepository::findByToken);
    }

    @Override
    public Optional<PushInfo> createOrUpdate(PushInfo push) {

        try {
            return Optional.ofNullable(push).map(this.pushInfoRepository::save);
        } catch (ConstraintViolationException exception) {
            final PushInfo pushInfo = this.pushInfoRepository.findByToken(push.getToken()).get();
            return Optional.of(pushInfoRepository.save(push.withId(pushInfo.getId())));
        }
    }

    @Override
    public void saveAll(List<PushInfo> pushInfos) {

        if (!CollectionUtils.isEmpty(pushInfos)) {
            this.pushInfoRepository.saveAll(pushInfos);
        }
    }

}
