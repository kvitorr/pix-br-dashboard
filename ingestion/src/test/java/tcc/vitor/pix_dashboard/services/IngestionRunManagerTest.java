package tcc.vitor.pix_dashboard.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.database.repositories.IngestionRunRepository;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionRunManagerTest {

    @Mock
    private IngestionRunRepository ingestionRunRepository;

    @InjectMocks
    private IngestionRunManager ingestionRunManager;

    private IngestionRun savedRun;

    @BeforeEach
    void setUp() {
        savedRun = new IngestionRun();
        savedRun.setId(UUID.randomUUID());
        savedRun.setStatus(IngestionRunStatus.RUNNING);
        savedRun.setSource(IngestionRunSource.BACEN_PIX);
        when(ingestionRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createRunningRecord_savesRunWithCorrectFieldsAndReturnsIt() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);

        IngestionRun result = ingestionRunManager.createRunningRecord(
                IngestionRunSource.BACEN_PIX, "{\"periodo\":\"202401\"}");

        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(result.getSource()).isEqualTo(IngestionRunSource.BACEN_PIX);
        assertThat(result.getStatus()).isEqualTo(IngestionRunStatus.RUNNING);
        assertThat(result.getParams()).isEqualTo("{\"periodo\":\"202401\"}");
        assertThat(result.getStartedAt()).isBetween(before, after);

        verify(ingestionRunRepository).save(any(IngestionRun.class));
    }

    @Test
    void markAsSuccess_updatesStatusCountsAndEndTime() {
        IngestionRun run = new IngestionRun();
        run.setStatus(IngestionRunStatus.RUNNING);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ingestionRunManager.markAsSuccess(run, 100, 95);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(run.getStatus()).isEqualTo(IngestionRunStatus.SUCCESS);
        assertThat(run.getRecordsFetched()).isEqualTo(100);
        assertThat(run.getRecordsUpserted()).isEqualTo(95);
        assertThat(run.getRecordsFailed()).isEqualTo(0);
        assertThat(run.getEndedAt()).isBetween(before, after);

        verify(ingestionRunRepository).save(run);
    }

    @Test
    void markAsSuccess_withZeroRecords_setsCountsToZero() {
        IngestionRun run = new IngestionRun();

        ingestionRunManager.markAsSuccess(run, 0, 0);

        assertThat(run.getStatus()).isEqualTo(IngestionRunStatus.SUCCESS);
        assertThat(run.getRecordsFetched()).isEqualTo(0);
        assertThat(run.getRecordsUpserted()).isEqualTo(0);
        assertThat(run.getRecordsFailed()).isEqualTo(0);
    }

    @Test
    void markAsFailed_updatesStatusErrorCodeMessageAndEndTime() {
        IngestionRun run = new IngestionRun();
        run.setStatus(IngestionRunStatus.RUNNING);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        ingestionRunManager.markAsFailed(run, "BCB_API_ERROR", "Connection refused");
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertThat(run.getStatus()).isEqualTo(IngestionRunStatus.FAILED);
        assertThat(run.getErrorCode()).isEqualTo("BCB_API_ERROR");
        assertThat(run.getErrorMessage()).isEqualTo("Connection refused");
        assertThat(run.getEndedAt()).isBetween(before, after);

        verify(ingestionRunRepository).save(run);
    }

    @Test
    void markAsFailed_withUnexpectedErrorCode_setsFieldsCorrectly() {
        IngestionRun run = new IngestionRun();

        ingestionRunManager.markAsFailed(run, "UNEXPECTED_ERROR", "NullPointerException");

        assertThat(run.getStatus()).isEqualTo(IngestionRunStatus.FAILED);
        assertThat(run.getErrorCode()).isEqualTo("UNEXPECTED_ERROR");
        assertThat(run.getErrorMessage()).isEqualTo("NullPointerException");
    }

    @Test
    void createRunningRecord_callsSaveOnce() {
        ingestionRunManager.createRunningRecord(IngestionRunSource.IBGE_PIB, "{}");

        verify(ingestionRunRepository, times(1)).save(any());
    }

    @Test
    void markAsSuccess_callsSaveOnce() {
        ingestionRunManager.markAsSuccess(new IngestionRun(), 10, 10);

        verify(ingestionRunRepository, times(1)).save(any());
    }

    @Test
    void markAsFailed_callsSaveOnce() {
        ingestionRunManager.markAsFailed(new IngestionRun(), "ERR", "msg");

        verify(ingestionRunRepository, times(1)).save(any());
    }
}
