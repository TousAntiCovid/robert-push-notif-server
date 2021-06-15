package fr.gouv.stopc.robert.pushnotif.batch.writer;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class PushItemWriter implements ItemWriter<PushInfo> {

    private final IPushInfoService pushInfoService;

    public PushItemWriter(IPushInfoService pushInfoService) {
        this.pushInfoService = pushInfoService;
    }

    @Override
    public void write(List<? extends PushInfo> items) throws Exception {
        this.pushInfoService.saveAll((List<PushInfo>) items);
    }

}
