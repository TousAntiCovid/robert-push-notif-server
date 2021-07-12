package fr.gouv.stopc.robert.pushnotif.batch.writer;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class PushItemWriter implements ItemWriter<PushInfo> {

    @Override
    public void write(List<? extends PushInfo> items) {
        // This writer is not useful because the database updates are done by the
        // asynchronous tasks launched by processor
    }

}
