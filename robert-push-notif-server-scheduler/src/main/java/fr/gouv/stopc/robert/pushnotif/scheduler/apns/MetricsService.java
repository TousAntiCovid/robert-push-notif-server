package fr.gouv.stopc.robert.pushnotif.scheduler.apns;

import fr.gouv.stopc.robert.pushnotif.scheduler.configuration.RobertPushServerProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRejectionCode.NONE;
import static fr.gouv.stopc.robert.pushnotif.scheduler.apns.ApnsRequestOutcome.*;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;

@Service
public class MetricsService {

    /**
     * This map contains timers with their varying tags as keys. Using tags as keys
     * allows us to have a variable tags structure: for example, Success outcome
     * timers do not possess any rejectionCode. the current varying tags are:
     * outcome host port rejectionCode
     */
    private final Map<Tags, Timer> tagsToTimerMap;

    public MetricsService(final MeterRegistry meterRegistry,
            final RobertPushServerProperties robertPushServerProperties) {

        final var clients = robertPushServerProperties.getApns().getClients();

        final var successTags = clients.stream()
                .map(
                        apnsClient -> Tags.of(
                                "host", apnsClient.getHost(),
                                "port", String.valueOf(apnsClient.getPort()),
                                "outcome", ACCEPTED.name(),
                                "rejectionCode", NONE.name()
                        )
                );

        final var rejectedTags = clients.stream()
                .flatMap(
                        apnsClient -> Arrays.stream(ApnsRejectionCode.values())
                                .map(
                                        rejectionCode -> Tags.of(
                                                "host", apnsClient.getHost(),
                                                "port", String.valueOf(apnsClient.getPort()),
                                                "outcome", REJECTED.name(),
                                                "rejectionCode", rejectionCode.name()
                                        )
                                )
                );

        final var errorTags = clients.stream()
                .map(
                        apnsClient -> Tags.of(
                                "host", apnsClient.getHost(),
                                "port", String.valueOf(apnsClient.getPort()),
                                "outcome", ERROR.name(),
                                "rejectionCode", NONE.name()
                        )
                );

        tagsToTimerMap = concat(concat(rejectedTags, successTags), errorTags).distinct().collect(
                toMap(
                        identity(),
                        tags -> Timer.builder("pushy.notifications.sent.timer")
                                .tags(tags)
                                .register(meterRegistry)
                )
        );
    }

    /**
     * returns the Timer matching various tags matching parameters.
     *
     * @param host          server host
     * @param port          server port
     * @param outcome       request outcome. Either Accepted, Rejected, or Error.
     * @param rejectionCode rejection code when outcome is Rejected. Otherwise, use
     *                      the value NONE
     * @return
     * @see fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsTemplate.host
     * @see fr.gouv.stopc.robert.pushnotif.scheduler.apns.template.ApnsTemplate.port
     * @see ApnsRequestOutcome
     * @see NONE
     */
    public Timer getTimer(final String host,
            final String port,
            final ApnsRequestOutcome outcome,
            final ApnsRejectionCode rejectionCode) {
        return tagsToTimerMap.get(
                Tags.of(
                        "host", host,
                        "port", port,
                        "outcome", outcome.name(),
                        "rejectionCode", rejectionCode.name()
                )
        );
    }
}
