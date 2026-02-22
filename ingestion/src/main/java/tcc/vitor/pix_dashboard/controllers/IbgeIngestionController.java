package tcc.vitor.pix_dashboard.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "IBGE", description = "Ingestão de dados populacionais e de PIB do IBGE")
public class IbgeIngestionController {

    private final IbgePopulacaoIngestionService ibgePopulacaoIngestionService;
    private final IbgePibIngestionService ibgePibIngestionService;

    public IbgeIngestionController(IbgePopulacaoIngestionService ibgePopulacaoIngestionService,
                                    IbgePibIngestionService ibgePibIngestionService) {
        this.ibgePopulacaoIngestionService = ibgePopulacaoIngestionService;
        this.ibgePibIngestionService = ibgePibIngestionService;
    }

    @Operation(
            summary = "Ingerir dados de população do IBGE",
            description = "Dispara o processo de ingestão de dados populacionais por município a partir da API do IBGE para o ano especificado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Ingestão realizada com sucesso",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "ingestionRunId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
                                                      "status": "SUCCESS",
                                                      "recordsFetched": 5570,
                                                      "recordsUpserted": 5570
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    @PostMapping("/ibge-populacao")
    public ResponseEntity<Map<String, Object>> ingestPopulacao(
            @Parameter(
                    description = "Ano de referência para os dados populacionais (ex: 2022)",
                    required = true,
                    example = "2022"
            )
            @RequestParam String ano) {
        IngestionRun run = ibgePopulacaoIngestionService.ingest(ano);
        return ResponseEntity.ok(buildResponseMap(run));
    }

    @Operation(
            summary = "Ingerir dados de PIB do IBGE",
            description = "Dispara o processo de ingestão de dados de PIB por município a partir da API SIDRA do IBGE.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Ingestão realizada com sucesso",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "ingestionRunId": "c3d4e5f6-a7b8-9012-cdef-123456789012",
                                                      "status": "SUCCESS",
                                                      "recordsFetched": 5570,
                                                      "recordsUpserted": 5570
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
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
