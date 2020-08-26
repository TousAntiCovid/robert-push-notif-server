package fr.gouv.stopc.robert.pushnotif.batch.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class NotificationDetailsDto {

    @JsonProperty("notification.error.title")
    private String title;

    @JsonProperty("notification.error.message")
    private String message;
}
