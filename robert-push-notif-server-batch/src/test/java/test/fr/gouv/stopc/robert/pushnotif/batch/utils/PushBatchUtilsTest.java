package test.fr.gouv.stopc.robert.pushnotif.batch.utils;

import fr.gouv.stopc.robert.pushnotif.batch.utils.PushBatchUtils;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PushBatchUtilsTest {

    @Test
    public void testConstructorShouldThrowAssertionError() {

        // Then
        Assertions.assertThrows(AssertionError.class, () -> {
            Constructor<PushBatchUtils> constructor;
            try {
                // Given
                constructor = PushBatchUtils.class.getDeclaredConstructor(null);
                assertNotNull(constructor);
                constructor.setAccessible(true);
                // When
                constructor.newInstance(null);
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException e) {
                Assertions.fail("Should not throw these exceptions");
            }
        });
    }

    @Test
    public void testSetNextPlannedPushDateWhenPush() {

        // Given
        PushInfo push = PushInfo.builder()
                .token("token")
                .locale("fr-FR")
                .timezone("Europe/Paris")
                .build();

        assertNull(push.getNextPlannedPush());

        // When
        PushBatchUtils.setNextPlannedPushDate(push, 8, 20);

        // Then
        assertNotNull(push.getNextPlannedPush());
    }
}
