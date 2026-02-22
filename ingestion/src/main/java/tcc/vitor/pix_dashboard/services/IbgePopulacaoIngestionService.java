package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.services.dto.IbgePopulacaoDTO;

import java.util.List;

@Service
public class IbgePopulacaoIngestionService {

    private static final Logger log = LoggerFactory.getLogger(IbgePopulacaoIngestionService.class);

    private final IbgePopulacaoClient ibgePopulacaoClient;
    private final IngestionRunService ingestionRunService;

    public IbgePopulacaoIngestionService(IbgePopulacaoClient ibgePopulacaoClient,
                                         IngestionRunService ingestionRunService) {
        this.ibgePopulacaoClient = ibgePopulacaoClient;
        this.ingestionRunService = ingestionRunService;
    }

    public IngestionRun ingest(String ano) {
        log.atInfo()
                .addKeyValue("ano", ano)
                .log("Iniciando ingestao de populacao IBGE");

        IngestionRun run = ingestionRunService.createIbgeRunningRecord(IngestionRunSource.IBGE_POP, ano);
        long startTime = System.currentTimeMillis();

        try {
            List<IbgePopulacaoDTO> records = ibgePopulacaoClient.fetchAll(ano);

            int upserted = ingestionRunService.persistPopulacao(records);

            long durationMs = System.currentTimeMillis() - startTime;
            ingestionRunService.markAsSuccess(run, records.size(), upserted);

            log.atInfo()
                    .addKeyValue("ano", ano)
                    .addKeyValue("totalRecords", records.size())
                    .addKeyValue("upserted", upserted)
                    .addKeyValue("durationMs", durationMs)
                    .log("Ingestao de populacao concluida com sucesso");

            return run;

        } catch (IbgeApiException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            ingestionRunService.markAsFailed(run, "IBGE_API_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("ano", ano)
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Falha na ingestao de populacao");

            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            ingestionRunService.markAsFailed(run, "UNEXPECTED_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("ano", ano)
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Erro inesperado na ingestao de populacao");

            throw new IbgeApiException("Erro inesperado na ingestao de populacao", e);
        }
    }
}
