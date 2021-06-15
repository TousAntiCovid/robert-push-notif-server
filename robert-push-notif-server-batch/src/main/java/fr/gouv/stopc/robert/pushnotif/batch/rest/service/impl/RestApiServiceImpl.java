package fr.gouv.stopc.robert.pushnotif.batch.rest.service.impl;

import fr.gouv.stopc.robert.pushnotif.batch.rest.dto.NotificationDetailsDto;
import fr.gouv.stopc.robert.pushnotif.batch.rest.service.IRestApiService;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.inject.Inject;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class RestApiServiceImpl implements IRestApiService {

    private final RestTemplate restTemplate;

    private final PropertyLoader propertyLoader;

    @Inject
    public RestApiServiceImpl(RestTemplate restTemplate, PropertyLoader propertyLoader) {

        this.restTemplate = restTemplate;
        this.propertyLoader = propertyLoader;
    }

    @Override
    public Optional<NotificationDetailsDto> getNotificationDetails(String locale) {

        if (StringUtils.isBlank(locale)) {
            log.warn("The locale({}) cannot be empty()", locale);
            return Optional.empty();
        }

        if (locale.length() < 2) {
            log.warn("The locale({}) length() < 2", locale, locale.length());
            return Optional.empty();
        }

        List<String> availableNotificationLanguages = Arrays
                .asList(this.propertyLoader.getAvailableNotificationLanguages());
        String lang = locale.substring(0, 2);

        if (!availableNotificationLanguages.contains(lang)) {
            log.warn(
                    "The language ({}) of locale({}) is not wihin {}", lang,
                    locale, availableNotificationLanguages
            );
            return Optional.empty();
        }

        try {
            ResponseEntity<NotificationDetailsDto> notifContent = this.restTemplate
                    .getForEntity(this.buildURI(lang), NotificationDetailsDto.class);
            return Optional.ofNullable(notifContent.getBody());

        } catch (RestClientException e) {
            log.error("Retrieve notification descritption failed due to {}", e.getMessage());
        }

        return Optional.empty();
    }

    private URI buildURI(String lang) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("lang", lang);
        parameters.put("version", this.propertyLoader.getNotificationContentUrlVersion());

        return UriComponentsBuilder.fromUriString(this.propertyLoader.getNotificationContentUrl())
                .build(parameters);
    }

}
