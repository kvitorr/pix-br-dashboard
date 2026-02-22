package tcc.vitor.pix_dashboard.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.services.dto.IbgePopulacaoDTO;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IbgePopulacaoIngestionServiceTest {

    @Mock
    private IbgePopulacaoClient ibgePopulacaoClient;

    @Mock
    private IngestionRunService ingestionRunService;

    @InjectMocks
    private IbgePopulacaoIngestionService ibgePopulacaoIngestionService;

    private IbgePopulacaoDTO sampleDto;
    private IngestionRun runningRun;

    @BeforeEach
    void setUp() {
        sampleDto = new IbgePopulacaoDTO("3550308", 11895578);

        runningRun = new IngestionRun();
        runningRun.setId(UUID.randomUUID());
        runningRun.setStatus(IngestionRunStatus.RUNNING);
        runningRun.setSource(IngestionRunSource.IBGE_POP);
    }

    @Test
    void ingest_happyPath_delegatesToPersistenceAndReturnsRun() {
        when(ingestionRunService.createIbgeRunningRecord(IngestionRunSource.IBGE_POP, "2024"))
                .thenReturn(runningRun);
        when(ibgePopulacaoClient.fetchAll("2024")).thenReturn(List.of(sampleDto));
        when(ingestionRunService.persistPopulacao(any())).thenReturn(1);

        IngestionRun result = ibgePopulacaoIngestionService.ingest("2024");

        assertThat(result).isEqualTo(runningRun);
        verify(ingestionRunService).createIbgeRunningRecord(IngestionRunSource.IBGE_POP, "2024");
        verify(ibgePopulacaoClient).fetchAll("2024");
        verify(ingestionRunService).persistPopulacao(List.of(sampleDto));
        verify(ingestionRunService).markAsSuccess(runningRun, 1, 1);
        verify(ingestionRunService, never()).markAsFailed(any(), anyString(), anyString());
    }

    @Test
    void ingest_whenIbgeApiFails_marksRunAsFailedAndRethrows() {
        when(ingestionRunService.createIbgeRunningRecord(IngestionRunSource.IBGE_POP, "2024"))
                .thenReturn(runningRun);
        when(ibgePopulacaoClient.fetchAll("2024")).thenThrow(new IbgeApiException("API timeout"));

        assertThatThrownBy(() -> ibgePopulacaoIngestionService.ingest("2024"))
                .isInstanceOf(IbgeApiException.class)
                .hasMessageContaining("API timeout");

        verify(ingestionRunService).markAsFailed(runningRun, "IBGE_API_ERROR", "API timeout");
        verify(ingestionRunService, never()).markAsSuccess(any(), anyInt(), anyInt());
    }

    @Test
    void ingest_withEmptyRecords_callsPersistWithEmptyListAndSucceeds() {
        when(ingestionRunService.createIbgeRunningRecord(IngestionRunSource.IBGE_POP, "2024"))
                .thenReturn(runningRun);
        when(ibgePopulacaoClient.fetchAll("2024")).thenReturn(List.of());
        when(ingestionRunService.persistPopulacao(any())).thenReturn(0);

        ibgePopulacaoIngestionService.ingest("2024");

        verify(ingestionRunService).markAsSuccess(runningRun, 0, 0);
    }

    @Test
    void ingest_whenUnexpectedExceptionOccurs_wrapsInIbgeApiException() {
        when(ingestionRunService.createIbgeRunningRecord(IngestionRunSource.IBGE_POP, "2024"))
                .thenReturn(runningRun);
        when(ibgePopulacaoClient.fetchAll("2024")).thenReturn(List.of(sampleDto));
        when(ingestionRunService.persistPopulacao(any()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> ibgePopulacaoIngestionService.ingest("2024"))
                .isInstanceOf(IbgeApiException.class)
                .hasMessageContaining("Erro inesperado na ingestao de populacao");

        verify(ingestionRunService).markAsFailed(runningRun, "UNEXPECTED_ERROR", "DB connection lost");
    }
}
