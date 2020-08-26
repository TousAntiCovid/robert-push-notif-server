package test.fr.gouv.stopc.robert.pushnotif.database.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import fr.gouv.stopc.robert.pushnotif.database.model.PushInfo;
import fr.gouv.stopc.robert.pushnotif.database.repository.PushInfoRepository;
import fr.gouv.stopc.robert.pushnotif.database.service.impl.PushInfoServiceImpl;

@ExtendWith(SpringExtension.class)
public class PushInfoServiceImplTest {

    @InjectMocks
    private PushInfoServiceImpl pushInfoService;

    @Mock
    private PushInfoRepository pushInfoRepository;

    @Test
    public void testFindByPushTokenWhenNull() {

        // Given
        String pushToken = null;

        // When
        Optional<PushInfo> pushInfo = this.pushInfoService.findByPushToken(pushToken);

        // Then
        assertFalse(pushInfo.isPresent());
        verify(this.pushInfoRepository, never()).findByToken(anyString());
    }

    @Test
    public void testFindByPushTokenWhenEmpty() {

        // Given
        String pushToken = "";

        // When
        Optional<PushInfo> pushInfo = this.pushInfoService.findByPushToken(pushToken);

        // Then
        assertFalse(pushInfo.isPresent());
        verify(this.pushInfoRepository, never()).findByToken(anyString());
    }

    @Test
    public void testFindByPushTokenSucceedsWhenNotEmpty() {

        // Given
        String pushToken = "pushToken";

        when(this.pushInfoRepository.findByToken(pushToken)).thenReturn(Optional.of(PushInfo.builder().build()));

        // When
        Optional<PushInfo> pushInfo = this.pushInfoService.findByPushToken(pushToken);

        // Then
        assertTrue(pushInfo.isPresent());
        verify(this.pushInfoRepository).findByToken(pushToken);
    }

    @Test
    public void testCreateOrUpdateShouldNeverCallSaveWhenNull() {

        // Given
        PushInfo push = null;

        // When
        this.pushInfoService.createOrUpdate(push);

        // Then
        verify(this.pushInfoRepository, never()).save(any(PushInfo.class));
    }

    @Test
    public void testCreateOrUpdateShouldCallSaveWhenNotNull() {

        // Given
        PushInfo push = PushInfo.builder().token("pushToken").build();

        // When
        this.pushInfoService.createOrUpdate(push);

        // Then
        verify(this.pushInfoRepository).save(push);
    }

    @Test
    public void testSaveAllWhenListIsNull() {
        
        // Given
        List<PushInfo> pushInfos = null;
        
        // When
        this.pushInfoService.saveAll(pushInfos);
        
        // Then
        verify(this.pushInfoRepository, never()).saveAll(anyIterable());
    }

    @Test
    public void testSaveAllWhenListIsAEmpyt() {

        // Given
        List<PushInfo> pushInfos = Collections.emptyList();

        // When
        this.pushInfoService.saveAll(pushInfos);

        // Then
        verify(this.pushInfoRepository, never()).saveAll(anyIterable());
    }

    @Test
    public void testSaveAllWhenListIsNotEmpyt() {

        // Given
        List<PushInfo> pushInfos = new ArrayList<>();
        pushInfos.add(PushInfo.builder().build());

        // When
        this.pushInfoService.saveAll(pushInfos);

        // Then
        verify(this.pushInfoRepository).saveAll(pushInfos);
    }
}
