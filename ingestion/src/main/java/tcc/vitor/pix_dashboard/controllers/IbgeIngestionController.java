package tcc.vitor.pix_dashboard.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.services.IbgePibIngestionService;
import tcc.vitor.pix_dashboard.services.IbgePopulacaoIngestionService;

import java.util.Map;

@RestController
@RequestMapping("/api/ingestion")
public class IbgeIngestionController {

    private final IbgePopulacaoIngestionService ibgePopulacaoIngestionService;
    private final IbgePibIngestionService ibgePibIngestionService;

    public IbgeIngestionController(IbgePopulacaoIngestionService ibgePopulacaoIngestionService,
                                    IbgePibIngestionService ibgePibIngestionService) {
        this.ibgePopulacaoIngestionService = ibgePopulacaoIngestionService;
        this.ibgePibIngestionService = ibgePibIngestionService;
    }

    @PostMapping("/ibge-populacao")
    public ResponseEntity<Map<String, Object>> ingestPopulacao(@RequestParam String ano) {
        IngestionRun run = ibgePopulacaoIngestionService.ingest(ano);
        return ResponseEntity.ok(buildResponseMap(run));
    }

    @PostMapping("/ibge-pib")
    public ResponseEntity<Map<String, Object>> ingestPib() {
        IngestionRun run = ibgePibIngestionService.ingest();
        return ResponseEntity.ok(buildResponseMap(run));
    }

    private Map<String, Object> buildResponseMap(IngestionRun run) {
        return Map.of(
                "ingestionRunId", run.getId(),
                "status", run.getStatus(),
                "recordsFetched", run.getRecordsFetched() != null ? run.getRecordsFetched() : 0,
                "recordsUpserted", run.getRecordsUpserted() != null ? run.getRecordsUpserted() : 0
        );
    }
}
