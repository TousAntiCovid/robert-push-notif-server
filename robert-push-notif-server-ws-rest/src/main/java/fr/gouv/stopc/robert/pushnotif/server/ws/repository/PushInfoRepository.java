package fr.gouv.stopc.robert.pushnotif.server.ws.repository;

import fr.gouv.stopc.robert.pushnotif.server.ws.model.PushInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PushInfoRepository extends JpaRepository<PushInfo, Long> {

    Optional<PushInfo> findByToken(String token);

}
