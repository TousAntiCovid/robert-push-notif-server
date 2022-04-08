package fr.gouv.stopc.robert.pushnotif.server.ws.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PublishVersionOnStartupListener {

    private final MeterRegistry meterRegistry;

    private final BuildProperties buildProperties;

    private final GitProperties gitProperties;

    @EventListener
    public void publishVersion(final ApplicationStartedEvent event) {
        Counter.builder("app").description("Information about application")
                .tag("version", buildProperties.getVersion())
                .tag("commit", gitProperties.getShortCommitId())
                .register(meterRegistry)
                .increment();
    }
}
