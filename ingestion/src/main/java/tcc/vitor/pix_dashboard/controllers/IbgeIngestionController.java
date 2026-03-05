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
import tcc.vitor.pix_dashboard.services.dto.IngestionRunResponse;

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
    public ResponseEntity<IngestionRunResponse> ingestPopulacao(@RequestParam String ano) {
        IngestionRun run = ibgePopulacaoIngestionService.ingest(ano);
        return ResponseEntity.ok(IngestionRunResponse.from(run));
    }

    @Override
    @PostMapping("/ibge-pib")
    public ResponseEntity<IngestionRunResponse> ingestPib(@RequestParam String ano) {
        IngestionRun run = ibgePibIngestionService.ingest(ano);
        return ResponseEntity.ok(IngestionRunResponse.from(run));
    }

    @Override
    @PostMapping("/ibge-urbanizacao")
    public ResponseEntity<IngestionRunResponse> ingestUrbanizacao(@RequestParam String ano) {
        IngestionRun run = ibgeUrbanizacaoIngestionService.ingest(ano);
        return ResponseEntity.ok(IngestionRunResponse.from(run));
    }

    @Override
    @PostMapping(value = "/idhm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IngestionRunResponse> ingestIdhm(@RequestParam("file") MultipartFile file,
                                                            @RequestParam String ano) {
        IngestionRun run = iidhmIngestionService.ingest(file, ano);
        return ResponseEntity.ok(IngestionRunResponse.from(run));
    }
}
