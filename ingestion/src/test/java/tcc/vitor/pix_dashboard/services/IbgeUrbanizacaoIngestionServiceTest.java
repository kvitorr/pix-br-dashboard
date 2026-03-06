package tcc.vitor.pix_dashboard.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tcc.vitor.pix_dashboard.clients.IbgeUrbanizacaoClient;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.exceptions.IngestionException;
import tcc.vitor.pix_dashboard.services.dto.IbgeUrbanizacaoDTO;
import tcc.vitor.pix_dashboard.services.persistence.UrbanizacaoPersistenceService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IbgeUrbanizacaoIngestionServiceTest {

    @Mock
    private IbgeUrbanizacaoClient ibgeUrbanizacaoClient;

    @Mock
    private IngestionRunManager runManager;

    @Mock
    private UrbanizacaoPersistenceService urbanizacaoPersistenceService;

    @InjectMocks
    private IbgeUrbanizacaoIngestionService ibgeUrbanizacaoIngestionService;

    private IbgeUrbanizacaoDTO sampleDto;
    private IngestionRun runningRun;

    @BeforeEach
    void setUp() {
        sampleDto = new IbgeUrbanizacaoDTO("3550308", 10_000_000L, 1_895_578L);

        runningRun = new IngestionRun();
        runningRun.setId(UUID.randomUUID());
        runningRun.setStatus(IngestionRunStatus.RUNNING);
        runningRun.setSource(IngestionRunSource.IBGE_URBANIZACAO);
    }

    @Test
    void ingest_happyPath_delegatesToPersistenceAndReturnsRun() {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IBGE_URBANIZACAO), anyString()))
                .thenReturn(runningRun);
        when(ibgeUrbanizacaoClient.fetchAll("2022")).thenReturn(List.of(sampleDto));
        when(urbanizacaoPersistenceService.persist(any(), eq(2022))).thenReturn(1);

        IngestionRun result = ibgeUrbanizacaoIngestionService.ingest("2022");

        assertThat(result).isEqualTo(runningRun);
        verify(runManager).createRunningRecord(eq(IngestionRunSource.IBGE_URBANIZACAO), anyString());
        verify(ibgeUrbanizacaoClient).fetchAll("2022");
        verify(urbanizacaoPersistenceService).persist(List.of(sampleDto), 2022);
        verify(runManager).markAsSuccess(runningRun, 1, 1);
        verify(runManager, never()).markAsFailed(any(), anyString(), anyString());
    }

    @Test
    void ingest_whenIbgeApiFails_marksRunAsFailedAndRethrows() {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IBGE_URBANIZACAO), anyString()))
                .thenReturn(runningRun);
        when(ibgeUrbanizacaoClient.fetchAll("2022")).thenThrow(new IbgeApiException("API timeout"));

        assertThatThrownBy(() -> ibgeUrbanizacaoIngestionService.ingest("2022"))
                .isInstanceOf(IbgeApiException.class)
                .hasMessageContaining("API timeout");

        verify(runManager).markAsFailed(runningRun, "IBGE_API_ERROR", "API timeout");
        verify(runManager, never()).markAsSuccess(any(), anyInt(), anyInt());
    }

    @Test
    void ingest_withEmptyRecords_callsPersistWithEmptyListAndSucceeds() {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IBGE_URBANIZACAO), anyString()))
                .thenReturn(runningRun);
        when(ibgeUrbanizacaoClient.fetchAll("2022")).thenReturn(List.of());
        when(urbanizacaoPersistenceService.persist(any(), eq(2022))).thenReturn(0);

        ibgeUrbanizacaoIngestionService.ingest("2022");

        verify(runManager).markAsSuccess(runningRun, 0, 0);
    }

    @Test
    void ingest_whenUnexpectedExceptionOccurs_wrapsInIngestionException() {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IBGE_URBANIZACAO), anyString()))
                .thenReturn(runningRun);
        when(ibgeUrbanizacaoClient.fetchAll("2022")).thenReturn(List.of(sampleDto));
        when(urbanizacaoPersistenceService.persist(any(), anyInt()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> ibgeUrbanizacaoIngestionService.ingest("2022"))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("Erro inesperado na ingestao de urbanizacao IBGE");

        verify(runManager).markAsFailed(runningRun, "UNEXPECTED_ERROR", "DB connection lost");
    }
}
