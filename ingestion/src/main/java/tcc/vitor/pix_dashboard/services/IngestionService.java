package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.exceptions.BcbApiException;
import tcc.vitor.pix_dashboard.services.dto.PixTransacaoMunicipioDTO;

import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final BcbPixClient bcbPixClient;
    private final IngestionPersistenceService ingestionPersistenceService;

    public IngestionService(BcbPixClient bcbPixClient,
                            IngestionPersistenceService ingestionPersistenceService) {
        this.bcbPixClient = bcbPixClient;
        this.ingestionPersistenceService = ingestionPersistenceService;
    }

    public IngestionRun ingest(String database) {
        log.atInfo()
                .addKeyValue("database", database)
                .log("Iniciando ingestao para o periodo");

        IngestionRun run = ingestionPersistenceService.createRunningRecord(database);
        long startTime = System.currentTimeMillis();

        try {
            List<PixTransacaoMunicipioDTO> records = bcbPixClient.fetchAll(database);

            int upserted = ingestionPersistenceService.persistRecords(records, run);

            long durationMs = System.currentTimeMillis() - startTime;
            ingestionPersistenceService.markAsSuccess(run, records.size(), upserted);

            log.atInfo()
                    .addKeyValue("database", database)
                    .addKeyValue("totalRecords", records.size())
                    .addKeyValue("upserted", upserted)
                    .addKeyValue("durationMs", durationMs)
                    .log("Ingestao concluida com sucesso");

            return run;

        } catch (BcbApiException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            ingestionPersistenceService.markAsFailed(run, "BCB_API_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("database", database)
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Falha na ingestao");

            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            ingestionPersistenceService.markAsFailed(run, "UNEXPECTED_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("database", database)
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Erro inesperado na ingestao");

            throw new BcbApiException("Erro inesperado na ingestao", e);
        }
    }
}
