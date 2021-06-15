package test.fr.gouv.stopc.robert.pushnotif.batch.configuration;

import fr.gouv.stopc.robert.pushnotif.batch.apns.service.IApnsPushNotificationService;
import fr.gouv.stopc.robert.pushnotif.batch.configuration.PushNotificationBatchConfiguration;
import fr.gouv.stopc.robert.pushnotif.batch.rest.dto.NotificationDetailsDto;
import fr.gouv.stopc.robert.pushnotif.common.utils.TimeUtils;
import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.service.IPushInfoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

import java.net.URI;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { PushNotificationBatchConfigurationTest.BatchTestConfig.class })
@TestPropertySource("classpath:application.properties")
public class PushNotificationBatchConfigurationTest {

    @Inject
    private JobLauncherTestUtils jobLauncherTestUtils;

    @MockBean
    private RestTemplate restTemplate;

    @MockBean
    private IApnsPushNotificationService apnsPushNotifcationService;

    @Autowired
    private IPushInfoService pushInfoService;

    private Random random = new Random();

    @Test
    public void testPushPartitionedJobShouldFailWhenRetrievingNotificationContentFails() {

        // Given
        when(this.restTemplate.getForEntity(any(URI.class), any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        try {
            // When
            JobExecution jobExecution = this.jobLauncherTestUtils.launchJob();

            // Then
            assertTrue(jobExecution.getExitStatus().getExitCode().equals("FAILED"));
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void testPushPartitionedJobShouldSucceeds() {

        // Given
        when(this.restTemplate.getForEntity(any(URI.class), any()))
                .thenReturn(
                        ResponseEntity.ok(
                                NotificationDetailsDto.builder().title("Hello").message("Merci").build()
                        )
                );

        when(this.apnsPushNotifcationService.sendPushNotification(any(PushInfo.class)))
                .thenAnswer(new Answer<PushInfo>() {

                    @Override
                    public PushInfo answer(InvocationOnMock invocation) throws Throwable {
                        Object[] args = invocation.getArguments();
                        PushInfo push = (PushInfo) args[0];
                        push.setSuccessfulPushSent(1);
                        push.setLastSuccessfulPush(TimeUtils.getNowZoneUTC());
                        return push;
                    }
                });

        this.loadData();

        try {
            // When
            JobExecution jobExecution = this.jobLauncherTestUtils.launchJob();

            // Then
            assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
            assertThat(jobExecution.getStepExecutions())
                    .filteredOn(s -> s.getStatus().equals(BatchStatus.COMPLETED))
                    .hasSize(11);
            assertThat(jobExecution.getStepExecutions())
                    .filteredOn(s -> s.getStepName().equalsIgnoreCase("Step1"))
                    .extracting("writeCount").containsExactly(1000);
        } catch (Exception e) {
            fail(e);
        }
    }

    private void loadData() {
        LongStream.rangeClosed(1, 1000L).forEach(i -> {
            PushInfo push = PushInfo.builder()
                    .token(UUID.randomUUID().toString())
                    .locale("fr_FR")
                    .timezone("Europe/Paris")
                    .active(true)
                    .deleted(false)
                    .nextPlannedPush(
                            Date.from(
                                    LocalDate.now().atStartOfDay().plusHours(getRandomNumberInRange(0, 23))
                                            .plusMinutes(getRandomNumberInRange(0, 59)).minusDays(1)
                                            .toInstant(ZoneOffset.UTC)
                            )
                    )
                    .build();

            this.pushInfoService.createOrUpdate(push);
        });
    }

    private int getRandomNumberInRange(int min, int max) {

        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }

        return random.nextInt((max - min) + 1) + min;
    }

    @ComponentScan(basePackages = "fr.gouv.stopc")
    @EnableJpaRepositories("fr.gouv.stopc")
    @EntityScan("fr.gouv.stopc")
    @Configuration
    @Import({ PushNotificationBatchConfiguration.class })
    public static class BatchTestConfig {

        private Job pushPartitionedJob;

        @Bean
        @Inject
        JobLauncherTestUtils jobLauncherTestUtils(Job pushPartitionedJob) {

            this.pushPartitionedJob = pushPartitionedJob;

            JobLauncherTestUtils jobLauncherTestUtils = new JobLauncherTestUtils();
            jobLauncherTestUtils.setJob(this.pushPartitionedJob);

            return jobLauncherTestUtils;
        }
    }

}
