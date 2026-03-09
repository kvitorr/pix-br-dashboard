package tcc.vitor.pix_dashboard.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;
import tcc.vitor.pix_dashboard.exceptions.IngestionException;
import tcc.vitor.pix_dashboard.services.dto.IidhmDTO;
import tcc.vitor.pix_dashboard.services.persistence.IdhmPersistenceService;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IidhmIngestionServiceTest {

    @Mock
    private IidhmCsvParser iidhmCsvParser;

    @Mock
    private IngestionRunManager runManager;

    @Mock
    private IdhmPersistenceService idhmPersistenceService;

    @InjectMocks
    private IidhmIngestionService iidhmIngestionService;

    private IidhmDTO sampleDto;
    private IngestionRun runningRun;
    private MockMultipartFile sampleFile;

    @BeforeEach
    void setUp() {
        sampleDto = new IidhmDTO(
                2021,
                "São Paulo",
                new BigDecimal("0.783"),
                new BigDecimal("0.845"),
                new BigDecimal("0.736"),
                new BigDecimal("0.769")
        );

        runningRun = new IngestionRun();
        runningRun.setId(UUID.randomUUID());
        runningRun.setStatus(IngestionRunStatus.RUNNING);
        runningRun.setSource(IngestionRunSource.IDHM_ESTADUAL);

        sampleFile = new MockMultipartFile("file", "idhm.tsv", "text/plain", "conteudo".getBytes());
    }

    @Test
    void ingest_happyPath_parsesFileAndPersistsAndReturnsRun() throws IOException {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IDHM_ESTADUAL), isNull()))
                .thenReturn(runningRun);
        when(iidhmCsvParser.parse(any())).thenReturn(List.of(sampleDto));
        when(idhmPersistenceService.persist(any())).thenReturn(1);

        IngestionRun result = iidhmIngestionService.ingest(sampleFile);

        assertThat(result).isEqualTo(runningRun);
        verify(runManager).createRunningRecord(eq(IngestionRunSource.IDHM_ESTADUAL), isNull());
        verify(iidhmCsvParser).parse(any());
        verify(idhmPersistenceService).persist(List.of(sampleDto));
        verify(runManager).markAsSuccess(runningRun, 1, 1);
        verify(runManager, never()).markAsFailed(any(), anyString(), anyString());
    }

    @Test
    void ingest_withEmptyFile_callsPersistWithEmptyListAndSucceeds() throws IOException {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IDHM_ESTADUAL), isNull()))
                .thenReturn(runningRun);
        when(iidhmCsvParser.parse(any())).thenReturn(List.of());
        when(idhmPersistenceService.persist(any())).thenReturn(0);

        iidhmIngestionService.ingest(sampleFile);

        verify(runManager).markAsSuccess(runningRun, 0, 0);
    }

    @Test
    void ingest_whenParserThrowsIOException_marksRunAsFailedAndWraps() throws IOException {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IDHM_ESTADUAL), isNull()))
                .thenReturn(runningRun);
        when(iidhmCsvParser.parse(any())).thenThrow(new IOException("Arquivo corrompido"));

        assertThatThrownBy(() -> iidhmIngestionService.ingest(sampleFile))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("Erro ao ler arquivo na ingestao de IDHM Atlas/PNUD");

        verify(runManager).markAsFailed(runningRun, "IO_ERROR", "Arquivo corrompido");
        verify(runManager, never()).markAsSuccess(any(), anyInt(), anyInt());
    }

    @Test
    void ingest_whenPersistenceThrowsUnexpectedException_marksRunAsFailedAndWraps() throws IOException {
        when(runManager.createRunningRecord(eq(IngestionRunSource.IDHM_ESTADUAL), isNull()))
                .thenReturn(runningRun);
        when(iidhmCsvParser.parse(any())).thenReturn(List.of(sampleDto));
        when(idhmPersistenceService.persist(any()))
                .thenThrow(new RuntimeException("DB connection lost"));

        assertThatThrownBy(() -> iidhmIngestionService.ingest(sampleFile))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("Erro inesperado na ingestao de IDHM Atlas/PNUD");

        verify(runManager).markAsFailed(runningRun, "UNEXPECTED_ERROR", "DB connection lost");
    }
}
