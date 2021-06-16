package test.fr.gouv.stopc.robert.pushnotif.batch.utils;

import fr.gouv.stopc.robert.pushnotif.batch.utils.PushBatchConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PushBatchConstantsTest {

    @Test
    public void testConstructorShouldThrowAssertionError() {

        // Then
        Assertions.assertThrows(AssertionError.class, () -> {
            Constructor<PushBatchConstants> constructor;
            try {
                // Given
                constructor = PushBatchConstants.class.getDeclaredConstructor(null);
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
}
