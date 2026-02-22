package tcc.vitor.pix_dashboard.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Tag(name = "BACEN PIX", description = "Ingestão de dados de transações PIX do Banco Central")
public interface BacenPixIngestionApi {

    @Operation(
            summary = "Ingerir dados PIX do BACEN",
            description = "Dispara o processo de ingestão de dados de transações PIX por município a partir da API OData do Banco Central do Brasil.",
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
                                                      "ingestionRunId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
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
    ResponseEntity<Map<String, Object>> ingestBacenPix(
            @Parameter(
                    description = "Período da base de dados no formato AAAAMM (ex: 202401 para janeiro de 2024)",
                    required = true,
                    example = "202401"
            )
            @RequestParam String database);
}
