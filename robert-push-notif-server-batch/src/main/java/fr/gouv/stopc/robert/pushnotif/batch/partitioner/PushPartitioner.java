package fr.gouv.stopc.robert.pushnotif.batch.partitioner;

import fr.gouv.stopc.robert.pushnotif.batch.utils.PushBatchConstants;
import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import lombok.Data;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.inject.Inject;
import javax.sql.DataSource;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Data
public class PushPartitioner implements Partitioner {

    private JdbcOperations jdbcTemplate;

    @Inject
    public PushPartitioner(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {

        long min = jdbcTemplate.queryForObject("SELECT MIN(id) FROM PUSH", Long.class);

        Long max = jdbcTemplate.queryForObject("SELECT MAX(id) FROM PUSH", Long.class);

        Long targetSize = (max - min) / gridSize + 1;

        Map<String, ExecutionContext> result = new HashMap<>();

        Date pushDate = TimeUtils.getNowZoneUTC();

        int number = 0;
        long start = min;
        long end = start + targetSize - 1;

        while (start <= max) {
            ExecutionContext value = new ExecutionContext();
            result.put("partition" + number, value);

            if (end >= max) {
                end = max;
            }

            value.putLong(PushBatchConstants.MIN_ID, start);
            value.putLong(PushBatchConstants.MAX_ID, end);
            value.put(PushBatchConstants.PUSH_DATE, pushDate);

            start += targetSize;
            end += targetSize;

            number++;
        }
        return result;
    }

}
