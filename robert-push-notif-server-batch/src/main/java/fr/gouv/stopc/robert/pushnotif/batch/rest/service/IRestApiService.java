package fr.gouv.stopc.robert.pushnotif.batch.rest.service;

import java.util.Optional;

import fr.gouv.stopc.robert.pushnotif.batch.rest.dto.NotificationDetailsDto;

public interface IRestApiService {

    Optional<NotificationDetailsDto> getNotificationDetails(String locale);
}
