package tcc.vitor.pix_dashboard.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tcc.vitor.pix_dashboard.clients.BcbPixClient;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;
import tcc.vitor.pix_dashboard.exceptions.BcbApiException;
import tcc.vitor.pix_dashboard.services.dto.PixTransacaoMunicipioDTO;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BacenPixIngestionServiceTest {

    @Mock
    private BcbPixClient bcbPixClient;

    @Mock
    private IngestionService ingestionService;

    @InjectMocks
    private BacenPixIngestionService bacenPixIngestionService;

    private PixTransacaoMunicipioDTO sampleDto;
    private IngestionRun runningRun;

    @BeforeEach
    void setUp() {
        sampleDto = new PixTransacaoMunicipioDTO(
                "202401", "3550308", "Sao Paulo", "35", "SP", "SE", "Sudeste",
                new BigDecimal("1000000.00"), 100L,
                new BigDecimal("500000.00"), 50L,
                new BigDecimal("800000.00"), 80L,
                new BigDecimal("400000.00"), 40L,
                90L, 45L, 70L, 35L
        );

        runningRun = new IngestionRun();
        runningRun.setId(UUID.randomUUID());
        runningRun.setStatus(IngestionRunStatus.RUNNING);
        runningRun.setSource(IngestionRunSource.BACEN_PIX);
    }

    @Test
    void ingest_happyPath_delegatesToPersistenceServiceAndReturnsRun() {
        when(ingestionService.createRunningRecord("202401")).thenReturn(runningRun);
        when(bcbPixClient.fetchAll("202401")).thenReturn(List.of(sampleDto));
        when(ingestionService.persistRecords(any(), eq(runningRun))).thenReturn(1);

        IngestionRun result = bacenPixIngestionService.ingest("202401");

        assertThat(result).isEqualTo(runningRun);
        verify(ingestionService).createRunningRecord("202401");
        verify(bcbPixClient).fetchAll("202401");
        verify(ingestionService).persistRecords(List.of(sampleDto), runningRun);
        verify(ingestionService).markAsSuccess(runningRun, 1, 1);
        verify(ingestionService, never()).markAsFailed(any(), anyString(), anyString());
    }

    @Test
    void ingest_whenBcbApiFails_marksRunAsFailedAndRethrows() {
        when(ingestionService.createRunningRecord("202401")).thenReturn(runningRun);
        when(bcbPixClient.fetchAll("202401")).thenThrow(new BcbApiException("API timeout"));

        assertThatThrownBy(() -> bacenPixIngestionService.ingest("202401"))
                .isInstanceOf(BcbApiException.class)
                .hasMessageContaining("API timeout");

        verify(ingestionService).markAsFailed(runningRun, "BCB_API_ERROR", "API timeout");
        verify(ingestionService, never()).markAsSuccess(any(), anyInt(), anyInt());
    }

    @Test
    void ingest_withEmptyRecords_callsPersistWithEmptyListAndSucceeds() {
        when(ingestionService.createRunningRecord("202401")).thenReturn(runningRun);
        when(bcbPixClient.fetchAll("202401")).thenReturn(List.of());
        when(ingestionService.persistRecords(any(), eq(runningRun))).thenReturn(0);

        bacenPixIngestionService.ingest("202401");

        verify(ingestionService).markAsSuccess(runningRun, 0, 0);
    }

    @Test
    void ingest_whenUnexpectedExceptionOccurs_wrapsInBcbApiException() {
        when(ingestionService.createRunningRecord("202401")).thenReturn(runningRun);
        when(bcbPixClient.fetchAll("202401")).thenReturn(List.of(sampleDto));
        when(ingestionService.persistRecords(any(), any()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> bacenPixIngestionService.ingest("202401"))
                .isInstanceOf(BcbApiException.class)
                .hasMessageContaining("Erro inesperado na ingestao");

        verify(ingestionService).markAsFailed(runningRun, "UNEXPECTED_ERROR", "DB connection lost");
    }
}
