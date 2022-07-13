package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static java.util.Arrays.stream;

@RequiredArgsConstructor
public enum ApnsRejectionCode {

    BAD_COLLAPSE_ID("BadCollapseId"),
    BAD_DEVICE_TOKEN("BadDeviceToken"),
    BAD_EXPIRATION_DATE("BadExpirationDate"),
    BAD_MESSAGE_ID("BadMessageId"),
    BAD_PRIORITY("BadPriority"),
    BAD_TOPIC("BadTopic"),
    DEVICE_TOKEN_NOT_FOR_TOPIC("DeviceTokenNotForTopic"),
    DUPLICATE_HEADERS("DuplicateHeaders"),
    IDLE_TIMEOUT("IdleTimeout"),
    INVALID_PUSH_TYPE("InvalidPushType"),
    MISSING_DEVICE_TOKEN("MissingDeviceToken"),
    MISSING_TOPIC("MissingTopic"),
    PAYLOAD_EMPTY("PayloadEmpty"),
    TOPIC_DISALLOWED("TopicDisallowed"),
    BAD_CERTIFICATE("BadCertificate"),
    BAD_CERTIFICATE_ENVIRONMENT("BadCertificateEnvironment"),
    EXPIRED_PROVIDER_TOKEN("ExpiredProviderToken"),
    FORBIDDEN("Forbidden"),
    INVALID_PROVIDER_TOKEN("InvalidProviderToken"),
    MISSING_PROVIDER_TOKEN("MissingProviderToken"),
    BAD_PATH("BadPath"),
    METHOD_NOT_ALLOWED("MethodNotAllowed"),
    UNREGISTERED("Unregistered"),
    PAYLOAD_TOO_LARGE("PayloadTooLarge"),
    TOO_MANY_PROVIDER_TOKEN_UPDATES("TooManyProviderTokenUpdates"),
    TOO_MANY_REQUESTS("TooManyRequests"),
    INTERNAL_SERVER_ERROR("InternalServerError"),
    SERVER_UNAVAILABLE("ServiceUnavailable"),
    SHUTDOWN("Shutdown"),
    UNKNOWN("UnknownError"),
    NONE("None");

    @Getter
    private final String value;

    public static ApnsRejectionCode getRejectionCodeOrUnknown(final String value) {
        return stream(ApnsRejectionCode.values()).filter(code -> code.value.equals(value)).findFirst().orElse(UNKNOWN);
    }
}
