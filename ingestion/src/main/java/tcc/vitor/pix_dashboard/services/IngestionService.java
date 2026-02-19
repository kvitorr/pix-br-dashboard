package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tcc.vitor.pix_dashboard.exceptions.BcbApiException;
import tcc.vitor.pix_dashboard.services.dto.PixTransacaoMunicipioDTO;

import java.util.List;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final BcbPixClient bcbPixClient;

    public IngestionService(BcbPixClient bcbPixClient) {
        this.bcbPixClient = bcbPixClient;
    }

    public List<PixTransacaoMunicipioDTO> fetchAll(String database) {
        log.atInfo()
                .addKeyValue("database", database)
                .log("Iniciando ingestao para o periodo");

        long startTime = System.currentTimeMillis();

        try {
            List<PixTransacaoMunicipioDTO> records = bcbPixClient.fetchAll(database);

            long durationMs = System.currentTimeMillis() - startTime;

            log.atInfo()
                    .addKeyValue("database", database)
                    .addKeyValue("totalRecords", records.size())
                    .addKeyValue("durationMs", durationMs)
                    .log("Ingestao concluida com sucesso");

            return records;
        } catch (BcbApiException e) {
            long durationMs = System.currentTimeMillis() - startTime;

            log.atError()
                    .addKeyValue("database", database)
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Falha na ingestao");

            throw e;
        }
    }
}
