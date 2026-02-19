package tcc.vitor.pix_dashboard.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.services.IngestionService;

import java.util.Map;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/bacen-pix")
    public ResponseEntity<Map<String, Object>> ingestBacenPix(@RequestParam String database) {
        IngestionRun run = ingestionService.ingest(database);

        Map<String, Object> response = Map.of(
                "ingestionRunId", run.getId(),
                "status", run.getStatus(),
                "recordsFetched", run.getRecordsFetched() != null ? run.getRecordsFetched() : 0,
                "recordsUpserted", run.getRecordsUpserted() != null ? run.getRecordsUpserted() : 0
        );

        return ResponseEntity.ok(response);
    }
}
