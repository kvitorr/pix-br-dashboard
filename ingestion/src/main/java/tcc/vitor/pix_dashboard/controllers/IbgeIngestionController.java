package tcc.vitor.pix_dashboard.controllers;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.services.IbgePibIngestionService;
import tcc.vitor.pix_dashboard.services.IbgePopulacaoIngestionService;
import tcc.vitor.pix_dashboard.services.IbgeUrbanizacaoIngestionService;
import tcc.vitor.pix_dashboard.services.IidhmIngestionService;

import java.util.Map;

@RestController
@RequestMapping("/api/ingestion")
public class IbgeIngestionController implements IbgeIngestionApi {

    private final IbgePopulacaoIngestionService ibgePopulacaoIngestionService;
    private final IbgePibIngestionService ibgePibIngestionService;
    private final IbgeUrbanizacaoIngestionService ibgeUrbanizacaoIngestionService;
    private final IidhmIngestionService iidhmIngestionService;

    public IbgeIngestionController(IbgePopulacaoIngestionService ibgePopulacaoIngestionService,
                                    IbgePibIngestionService ibgePibIngestionService,
                                    IbgeUrbanizacaoIngestionService ibgeUrbanizacaoIngestionService,
                                    IidhmIngestionService iidhmIngestionService) {
        this.ibgePopulacaoIngestionService = ibgePopulacaoIngestionService;
        this.ibgePibIngestionService = ibgePibIngestionService;
        this.ibgeUrbanizacaoIngestionService = ibgeUrbanizacaoIngestionService;
        this.iidhmIngestionService = iidhmIngestionService;
    }

    @Override
    @PostMapping("/ibge-populacao")
    public ResponseEntity<Map<String, Object>> ingestPopulacao(@RequestParam String ano) {
        IngestionRun run = ibgePopulacaoIngestionService.ingest(ano);
        return ResponseEntity.ok(buildResponseMap(run));
    }

    @Override
    @PostMapping("/ibge-pib")
    public ResponseEntity<Map<String, Object>> ingestPib() {
        IngestionRun run = ibgePibIngestionService.ingest();
        return ResponseEntity.ok(buildResponseMap(run));
    }

    @Override
    @PostMapping("/ibge-urbanizacao")
    public ResponseEntity<Map<String, Object>> ingestUrbanizacao() {
        IngestionRun run = ibgeUrbanizacaoIngestionService.ingest();
        return ResponseEntity.ok(buildResponseMap(run));
    }

    @Override
    @PostMapping(value = "/idhm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> ingestIdhm(@RequestParam("file") MultipartFile file) {
        IngestionRun run = iidhmIngestionService.ingest(file);
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
