package tcc.vitor.pix_dashboard.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tcc.vitor.pix_dashboard.clients.IbgePibClient;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.services.dto.IbgePibDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IbgePibIngestionServiceTest {

    @Mock
    private IbgePibClient ibgePibClient;

    @Mock
    private IngestionService ingestionService;

    @InjectMocks
    private IbgePibIngestionService ibgePibIngestionService;

    private IbgePibDTO sampleDto;
    private IngestionRun runningRun;

    @BeforeEach
    void setUp() {
        sampleDto = new IbgePibDTO("3550308", new BigDecimal("1046343"));

        runningRun = new IngestionRun();
        runningRun.setId(UUID.randomUUID());
        runningRun.setStatus(IngestionRunStatus.RUNNING);
        runningRun.setSource(IngestionRunSource.IBGE_PIB);
    }

    @Test
    void ingest_happyPath_delegatesToPersistenceAndReturnsRun() {
        when(ingestionService.createRunningRecord(eq(IngestionRunSource.IBGE_PIB), anyString()))
                .thenReturn(runningRun);
        when(ibgePibClient.fetchAll("2023")).thenReturn(List.of(sampleDto));
        when(ingestionService.persistPib(any(), eq(2023))).thenReturn(1);

        IngestionRun result = ibgePibIngestionService.ingest("2023");

        assertThat(result).isEqualTo(runningRun);
        verify(ingestionService).createRunningRecord(eq(IngestionRunSource.IBGE_PIB), anyString());
        verify(ibgePibClient).fetchAll("2023");
        verify(ingestionService).persistPib(List.of(sampleDto), 2023);
        verify(ingestionService).markAsSuccess(runningRun, 1, 1);
        verify(ingestionService, never()).markAsFailed(any(), anyString(), anyString());
    }

    @Test
    void ingest_whenIbgeApiFails_marksRunAsFailedAndRethrows() {
        when(ingestionService.createRunningRecord(eq(IngestionRunSource.IBGE_PIB), anyString()))
                .thenReturn(runningRun);
        when(ibgePibClient.fetchAll("2023")).thenThrow(new IbgeApiException("SIDRA timeout"));

        assertThatThrownBy(() -> ibgePibIngestionService.ingest("2023"))
                .isInstanceOf(IbgeApiException.class)
                .hasMessageContaining("SIDRA timeout");

        verify(ingestionService).markAsFailed(runningRun, "IBGE_API_ERROR", "SIDRA timeout");
        verify(ingestionService, never()).markAsSuccess(any(), anyInt(), anyInt());
    }

    @Test
    void ingest_withEmptyRecords_callsPersistWithEmptyListAndSucceeds() {
        when(ingestionService.createRunningRecord(eq(IngestionRunSource.IBGE_PIB), anyString()))
                .thenReturn(runningRun);
        when(ibgePibClient.fetchAll("2023")).thenReturn(List.of());
        when(ingestionService.persistPib(any(), eq(2023))).thenReturn(0);

        ibgePibIngestionService.ingest("2023");

        verify(ingestionService).markAsSuccess(runningRun, 0, 0);
    }

    @Test
    void ingest_whenUnexpectedExceptionOccurs_wrapsInIbgeApiException() {
        when(ingestionService.createRunningRecord(eq(IngestionRunSource.IBGE_PIB), anyString()))
                .thenReturn(runningRun);
        when(ibgePibClient.fetchAll("2023")).thenReturn(List.of(sampleDto));
        when(ingestionService.persistPib(any(), anyInt()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> ibgePibIngestionService.ingest("2023"))
                .isInstanceOf(IbgeApiException.class)
                .hasMessageContaining("Erro inesperado na ingestao de PIB");

        verify(ingestionService).markAsFailed(runningRun, "UNEXPECTED_ERROR", "DB connection lost");
    }
}
