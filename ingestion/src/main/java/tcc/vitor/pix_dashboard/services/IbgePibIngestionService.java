package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tcc.vitor.pix_dashboard.clients.IbgePibClient;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.services.dto.IbgePibDTO;

import java.util.List;

@Service
public class IbgePibIngestionService {

    private static final Logger log = LoggerFactory.getLogger(IbgePibIngestionService.class);

    private final IbgePibClient ibgePibClient;
    private final IngestionService ingestionService;

    public IbgePibIngestionService(IbgePibClient ibgePibClient,
                                    IngestionService ingestionService) {
        this.ibgePibClient = ibgePibClient;
        this.ingestionService = ingestionService;
    }

    public IngestionRun ingest() {
        log.atInfo().log("Iniciando ingestao de PIB IBGE/SIDRA");

        IngestionRun run = ingestionService.createIbgeRunningRecord(IngestionRunSource.IBGE_PIB, null);
        long startTime = System.currentTimeMillis();

        try {
            List<IbgePibDTO> records = ibgePibClient.fetchAll();

            int upserted = ingestionService.persistPib(records);

            long durationMs = System.currentTimeMillis() - startTime;
            ingestionService.markAsSuccess(run, records.size(), upserted);

            log.atInfo()
                    .addKeyValue("totalRecords", records.size())
                    .addKeyValue("upserted", upserted)
                    .addKeyValue("durationMs", durationMs)
                    .log("Ingestao de PIB concluida com sucesso");

            return run;

        } catch (IbgeApiException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            ingestionService.markAsFailed(run, "IBGE_API_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Falha na ingestao de PIB");

            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            ingestionService.markAsFailed(run, "UNEXPECTED_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Erro inesperado na ingestao de PIB");

            throw new IbgeApiException("Erro inesperado na ingestao de PIB", e);
        }
    }
}
