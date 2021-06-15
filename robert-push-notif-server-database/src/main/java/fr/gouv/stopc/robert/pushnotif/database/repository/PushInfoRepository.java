package fr.gouv.stopc.robert.pushnotif.database.repository;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PushInfoRepository extends JpaRepository<PushInfo, Long> {

    Optional<PushInfo> findByToken(String token);
}
