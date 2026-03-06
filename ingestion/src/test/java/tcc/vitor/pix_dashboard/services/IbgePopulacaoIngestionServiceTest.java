package tcc.vitor.pix_dashboard.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tcc.vitor.pix_dashboard.clients.IbgePopulacaoClient;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.exceptions.IngestionException;
import tcc.vitor.pix_dashboard.services.dto.IbgePopulacaoDTO;
import tcc.vitor.pix_dashboard.services.persistence.PopulacaoPersistenceService;

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
    private IngestionRunManager runManager;

    @Mock
    private PopulacaoPersistenceService populacaoPersistenceService;

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
        when(runManager.createRunningRecord(eq(IngestionRunSource.IBGE_POP), anyString()))
                .thenReturn(runningRun);
        when(ibgePopulacaoClient.fetchAll("2024")).thenReturn(List.of(sampleDto));
        when(populacaoPersistenceService.persist(any(), eq(2024))).thenReturn(1);

        IngestionRun result = ibgePopulacaoIngestionService.ingest("2024");

        assertThat(result).isEqualTo(runningRun);
        verify(runManager).createRunningRecord(eq(IngestionRunSource.IBGE_POP), anyString());
        verify(ibgePopulacaoClient).fetchAll("2024");
        verify(populacaoPersistenceService).persist(List.of(sampleDto), 2024);
        verify(runManager).markAsSuccess(runningRun, 1, 1);
        verify(runManager, never()).markAsFailed(any(), anyString(), anyString());
    }

    @Test
    void ingest_whenIbgeApiFails_marksRunAsFailedAndRethrows() {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IBGE_POP), anyString()))
                .thenReturn(runningRun);
        when(ibgePopulacaoClient.fetchAll("2024")).thenThrow(new IbgeApiException("API timeout"));

        assertThatThrownBy(() -> ibgePopulacaoIngestionService.ingest("2024"))
                .isInstanceOf(IbgeApiException.class)
                .hasMessageContaining("API timeout");

        verify(runManager).markAsFailed(runningRun, "IBGE_API_ERROR", "API timeout");
        verify(runManager, never()).markAsSuccess(any(), anyInt(), anyInt());
    }

    @Test
    void ingest_withEmptyRecords_callsPersistWithEmptyListAndSucceeds() {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IBGE_POP), anyString()))
                .thenReturn(runningRun);
        when(ibgePopulacaoClient.fetchAll("2024")).thenReturn(List.of());
        when(populacaoPersistenceService.persist(any(), eq(2024))).thenReturn(0);

        ibgePopulacaoIngestionService.ingest("2024");

        verify(runManager).markAsSuccess(runningRun, 0, 0);
    }

    @Test
    void ingest_whenUnexpectedExceptionOccurs_wrapsInIngestionException() {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IBGE_POP), anyString()))
                .thenReturn(runningRun);
        when(ibgePopulacaoClient.fetchAll("2024")).thenReturn(List.of(sampleDto));
        when(populacaoPersistenceService.persist(any(), anyInt()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> ibgePopulacaoIngestionService.ingest("2024"))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("Erro inesperado na ingestao de populacao IBGE");

        verify(runManager).markAsFailed(runningRun, "UNEXPECTED_ERROR", "DB connection lost");
    }
}
