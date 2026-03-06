package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.IngestionException;
import tcc.vitor.pix_dashboard.services.dto.IidhmDTO;
import tcc.vitor.pix_dashboard.services.persistence.IdhmPersistenceService;

import java.util.List;

@Service
public class IidhmIngestionService {

    private static final Logger log = LoggerFactory.getLogger(IidhmIngestionService.class);

    private final IidhmCsvParser iidhmCsvParser;
    private final IngestionRunManager runManager;
    private final IdhmPersistenceService idhmPersistenceService;

    public IidhmIngestionService(IidhmCsvParser iidhmCsvParser,
                                  IngestionRunManager runManager,
                                  IdhmPersistenceService idhmPersistenceService) {
        this.iidhmCsvParser = iidhmCsvParser;
        this.runManager = runManager;
        this.idhmPersistenceService = idhmPersistenceService;
    }

    public IngestionRun ingest(MultipartFile file, String ano) {
        log.atInfo()
                .addKeyValue("nomeArquivo", file.getOriginalFilename())
                .addKeyValue("tamanhoBytes", file.getSize())
                .addKeyValue("ano", ano)
                .log("Iniciando ingestao de IDHM a partir de CSV estadual");

        IngestionRun run = runManager.createRunningRecord(
                IngestionRunSource.IDHM_ESTADUAL,
                ano != null ? "{\"ano\":\"" + ano + "\"}" : "{}"
        );
        long startTime = System.currentTimeMillis();

        try {
            List<IidhmDTO> records = iidhmCsvParser.parse(file.getInputStream());
            int updated = idhmPersistenceService.persist(records, Integer.parseInt(ano));
            long durationMs = System.currentTimeMillis() - startTime;
            runManager.markAsSuccess(run, records.size(), updated);

            log.atInfo()
                    .addKeyValue("estadosParsed", records.size())
                    .addKeyValue("municipiosAtualizados", updated)
                    .addKeyValue("durationMs", durationMs)
                    .log("Ingestao de IDHM concluida com sucesso");

            return run;

        } catch (RuntimeException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            runManager.markAsFailed(run, "IDHM_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Falha na ingestao de IDHM");

            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            runManager.markAsFailed(run, "UNEXPECTED_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Erro inesperado na ingestao de IDHM");

            throw new IngestionException("Erro inesperado na ingestao de IDHM", e);
        }
    }
}
