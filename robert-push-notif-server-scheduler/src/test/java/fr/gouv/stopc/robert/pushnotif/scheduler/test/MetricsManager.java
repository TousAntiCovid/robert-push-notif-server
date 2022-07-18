package fr.gouv.stopc.robert.pushnotif.scheduler.test;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AbstractLongAssert;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class MetricsManager implements TestExecutionListener {

    private static MeterRegistry meterRegistry;

    private static Map<Meter.Id, Long> metersSnapshot;

    private static final Tags SERVER_INFORMATION_TAGS = Tags.of("host", "localhost", "port", "2198");

    @Override
    public void beforeTestMethod(final TestContext testContext) {
        meterRegistry = testContext.getApplicationContext().getBean(MeterRegistry.class);

        metersSnapshot = meterRegistry.getMeters()
                .stream()
                .filter(meter -> meter instanceof Timer)
                .map(meter -> (Timer) meter)
                .collect(Collectors.toMap(Timer::getId, Timer::count));
    }

    public static AbstractLongAssert<?> assertCounterIncremented(final String name, final long increment,
            final Tags tags) {
        final var timer = meterRegistry.timer(name, tags.and(SERVER_INFORMATION_TAGS));
        return assertThat(timer.count() - metersSnapshot.getOrDefault(timer.getId(), 0L)).isEqualTo(increment);
    }
}
