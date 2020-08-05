package fr.gouv.stopc.robert.pushnotif.database.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;

@Repository
public interface PushInfoRepository extends JpaRepository<PushInfo, String> {

}
