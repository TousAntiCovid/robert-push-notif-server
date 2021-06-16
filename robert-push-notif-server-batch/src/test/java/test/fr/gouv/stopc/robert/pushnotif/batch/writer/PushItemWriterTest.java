package test.fr.gouv.stopc.robert.pushnotif.batch.writer;

import fr.gouv.stopc.robert.pushnotif.batch.writer.PushItemWriter;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
public class PushItemWriterTest {

    @InjectMocks
    private PushItemWriter writer;

    @Mock
    private IPushInfoService pushInfoService;

    @Test
    public void testWrite() {

        try {
            // Given
            List<PushInfo> items = new ArrayList<>();

            // When
            this.writer.write(items);

            // Then
            verify(this.pushInfoService).saveAll(items);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}
