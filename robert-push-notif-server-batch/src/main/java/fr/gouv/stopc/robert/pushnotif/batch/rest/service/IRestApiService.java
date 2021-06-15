package fr.gouv.stopc.robert.pushnotif.batch.rest.service;

import fr.gouv.stopc.robert.pushnotif.batch.rest.dto.NotificationDetailsDto;

import java.util.Optional;

public interface IRestApiService {

    Optional<NotificationDetailsDto> getNotificationDetails(String locale);
}
