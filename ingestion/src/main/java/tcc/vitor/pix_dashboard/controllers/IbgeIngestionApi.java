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
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "IBGE", description = "Ingestão de dados populacionais, PIB, urbanização e IDHM do IBGE")
public interface IbgeIngestionApi {

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
    ResponseEntity<Map<String, Object>> ingestPopulacao(
            @Parameter(
                    description = "Ano de referência para os dados populacionais (ex: 2022)",
                    required = true,
                    example = "2022"
            )
            @RequestParam String ano);

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
    ResponseEntity<Map<String, Object>> ingestPib();

    @Operation(
            summary = "Ingerir dados de urbanização do IBGE (Censo 2022)",
            description = """
                    Dispara o processo de ingestão de dados de população urbana e rural por município
                    a partir da tabela 9923 do SIDRA (Censo Demográfico 2022).
                    Calcula e persiste a taxa de urbanização (%) além dos valores absolutos de população urbana e rural.
                    """,
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
                                                      "ingestionRunId": "d4e5f6a7-b8c9-0123-defa-234567890123",
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
    ResponseEntity<Map<String, Object>> ingestUrbanizacao();

    @Operation(
            summary = "Ingerir dados de IDHM a partir de CSV estadual",
            description = """
                    Recebe um arquivo CSV com dados de IDHM estadual (formato TSV — separado por tabulação)
                    com as colunas: ANO, AGREGACAO, CODIGO, NOME, IDHM, IDHM_L, IDHM_E, IDHM_R.
                    O IDHM é imputado em todos os municípios do respectivo estado (JOIN por nome do estado).
                    Fonte: Atlas do Desenvolvimento Humano no Brasil (PNUD/IPEA/FJP).
                    Limitação: dado estadual, não municipal — documentado como limitação do estudo.
                    """,
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
                                                      "ingestionRunId": "e5f6a7b8-c9d0-1234-efab-345678901234",
                                                      "status": "SUCCESS",
                                                      "recordsFetched": 27,
                                                      "recordsUpserted": 5570
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<Map<String, Object>> ingestIdhm(
            @Parameter(
                    description = "Arquivo CSV com dados de IDHM estadual (separado por tabulação)",
                    required = true
            )
            @RequestParam("file") MultipartFile file);
}
