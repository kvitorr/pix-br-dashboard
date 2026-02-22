package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tcc.vitor.pix_dashboard.clients.IbgeUrbanizacaoClient;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.services.dto.IbgeUrbanizacaoDTO;

import java.util.List;

@Service
public class IbgeUrbanizacaoIngestionService {

    private static final Logger log = LoggerFactory.getLogger(IbgeUrbanizacaoIngestionService.class);

    private final IbgeUrbanizacaoClient ibgeUrbanizacaoClient;
    private final IngestionService ingestionService;

    public IbgeUrbanizacaoIngestionService(IbgeUrbanizacaoClient ibgeUrbanizacaoClient,
                                            IngestionService ingestionService) {
        this.ibgeUrbanizacaoClient = ibgeUrbanizacaoClient;
        this.ingestionService = ingestionService;
    }

    public IngestionRun ingest() {
        log.atInfo().log("Iniciando ingestao de taxa de urbanizacao (Censo 2022)");

        IngestionRun run = ingestionService.createIbgeRunningRecord(IngestionRunSource.IBGE_URBANIZACAO, null);
        long startTime = System.currentTimeMillis();

        try {
            List<IbgeUrbanizacaoDTO> records = ibgeUrbanizacaoClient.fetchAll();

            int updated = ingestionService.persistUrbanizacao(records);

            long durationMs = System.currentTimeMillis() - startTime;
            ingestionService.markAsSuccess(run, records.size(), updated);

            log.atInfo()
                    .addKeyValue("totalMunicipios", records.size())
                    .addKeyValue("updated", updated)
                    .addKeyValue("durationMs", durationMs)
                    .log("Ingestao de urbanizacao concluida com sucesso");

            return run;

        } catch (IbgeApiException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            ingestionService.markAsFailed(run, "IBGE_API_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Falha na ingestao de urbanizacao");

            throw e;
        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            ingestionService.markAsFailed(run, "UNEXPECTED_ERROR", e.getMessage());

            log.atError()
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Erro inesperado na ingestao de urbanizacao");

            throw new IbgeApiException("Erro inesperado na ingestao de urbanizacao", e);
        }
    }
}
