package fr.gouv.stopc.robert.pushnotif.batch.configuration;

import java.util.Date;
import java.util.Objects;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.batch.listener.PushJobExecutionListener;
import fr.gouv.stopc.robert.pushnotif.batch.partitioner.PushPartitioner;
import fr.gouv.stopc.robert.pushnotif.batch.processor.PushProcessor;
import fr.gouv.stopc.robert.pushnotif.batch.reader.PushPagingItemReader;
import fr.gouv.stopc.robert.pushnotif.batch.rest.service.IRestApiService;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PropertyLoader;
import fr.gouv.stopc.robert.pushnotif.batch.utils.PushBatchConstants;
import fr.gouv.stopc.robert.pushnotif.batch.writer.PushItemWriter;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;

@Configuration
@EnableBatchProcessing
public class PushNotificationBatchConfiguration {

    private final JobBuilderFactory jobBuilderFactory;

    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource;
    private final IPushInfoService pushInfoService;
    private final PropertyLoader propertyLoader;
    private final IApnsPushNotificationService apnsPushNotifcationService;

    @Inject
    public PushNotificationBatchConfiguration(JobBuilderFactory jobBuilderFactory,
            StepBuilderFactory stepBuilderFactory,
            DataSource dataSource,
            IPushInfoService pushInfoService,
            PropertyLoader propertyLoader,
            IRestApiService restApiService,
            IApnsPushNotificationService apnsPushNotifcationService) {

        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.dataSource = dataSource;
        this.pushInfoService = pushInfoService;
        this.propertyLoader = propertyLoader;
        this.apnsPushNotifcationService = apnsPushNotifcationService;

    }

    @Bean
    public PushPartitioner partitioner() {
        PushPartitioner partitioner = new PushPartitioner(this.dataSource);
        return partitioner;
    }

    @Bean
    @StepScope
    public PushPagingItemReader pushPagingReader(PagingQueryProvider pagingQueryProvider,
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId,
            @Value("#{stepExecutionContext['pushDate']}") Date pushDate) {

        return new PushPagingItemReader(this.dataSource, pagingQueryProvider, minId, maxId, pushDate, this.propertyLoader.getPageSize());
    }

    @Bean
    @StepScope
    public SqlPagingQueryProviderFactoryBean pagingQueryProvider(
            @Value("#{stepExecutionContext['minId']}") Long minId,
            @Value("#{stepExecutionContext['maxId']}") Long maxId,
            @Value("#{stepExecutionContext['pushDate']}") Date pushDate) { 


        StringBuilder dateWhereClause = new StringBuilder(" and next_planned_push <= :");
        dateWhereClause.append(PushBatchConstants.PUSH_DATE);

        StringBuilder whereClause = new StringBuilder("where id >= :");
        whereClause.append(PushBatchConstants.MIN_ID);

        if(minId != maxId) {
            whereClause.append(" and id <= :");
            whereClause.append(PushBatchConstants.MAX_ID);
        }

        if(Objects.nonNull(pushDate) && this.propertyLoader.isEnablePushDate()) {
            whereClause.append(dateWhereClause.toString());
        }

        whereClause.append(" and deleted = 'f' and active = 't' ");
        SqlPagingQueryProviderFactoryBean factoryBean = new SqlPagingQueryProviderFactoryBean(); 
        factoryBean.setDataSource(dataSource);
        factoryBean.setSelectClause("select *");
        factoryBean.setFromClause("from push");
        factoryBean.setWhereClause(whereClause.toString());
        factoryBean.setSortKey("token"); 
        return factoryBean; 
    }

    @Bean
    public PushProcessor pushProcessor() {

        return new PushProcessor(this.apnsPushNotifcationService);
    }

    @Bean
    public PushItemWriter pushItemWriter() {
        return new PushItemWriter(this.pushInfoService);
    }

    @Bean
    public TaskExecutorPartitionHandler partitionHandler() {
        TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
        partitionHandler.setGridSize(this.propertyLoader.getGridSize());
        partitionHandler.setStep(step1());
        partitionHandler.setTaskExecutor(new SimpleAsyncTaskExecutor());
        return partitionHandler; 
    } 

    @Bean
    public Step partitionedMaster() {
        return this.stepBuilderFactory.get("Step1")
                .partitioner("partitioner", partitioner())
                .partitionHandler(partitionHandler())
                .build();
    }

    @Bean
    public Step step1() {
        return this.stepBuilderFactory.get("step1")
                .<PushInfo, PushInfo>chunk(this.propertyLoader.getChunkSize())
                .reader(pushPagingReader(null, null, null, null))
                .processor(pushProcessor())
                .writer(pushItemWriter())
                .build();
    }

    @Bean
    public Job pushPartitionedJob() {
        return this.jobBuilderFactory.get("pushPartitionedJob")
                .listener(new PushJobExecutionListener(this.apnsPushNotifcationService))
                .incrementer(new RunIdIncrementer())
                .start(partitionedMaster())
                .build();
    }
}
