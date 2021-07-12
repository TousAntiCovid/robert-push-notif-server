package fr.gouv.stopc.robert.pushnotif.database.repository;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface PushInfoRepository extends JpaRepository<PushInfo, Long> {

    Optional<PushInfo> findByToken(String token);

    @Query("SELECT max(lastSuccessfulPush) FROM PushInfo")
    Optional<Date> findMaxLastSuccessfulPush();

    @Query("SELECT max(lastFailurePush) FROM PushInfo")
    Optional<Date> findMaxLastFailurePush();

}
