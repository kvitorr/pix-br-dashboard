package tcc.vitor.pix_dashboard.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.services.BacenPixIngestionService;
import tcc.vitor.pix_dashboard.services.dto.IngestionRunResponse;

@RestController
@RequestMapping("/api/ingestion")
public class BacenPixIngestionController implements BacenPixIngestionApi {

    private final BacenPixIngestionService bacenPixIngestionService;

    public BacenPixIngestionController(BacenPixIngestionService bacenPixIngestionService) {
        this.bacenPixIngestionService = bacenPixIngestionService;
    }

    @Override
    @PostMapping("/bacen-pix")
    public ResponseEntity<IngestionRunResponse> ingestBacenPix(@RequestParam String database) {
        IngestionRun run = bacenPixIngestionService.ingest(database);
        return ResponseEntity.ok(IngestionRunResponse.from(run));
    }
}
