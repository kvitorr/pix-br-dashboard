package tcc.vitor.pix_dashboard.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import tcc.vitor.pix_dashboard.services.dto.dashboard.*;

import java.util.List;

@Tag(name = "Dashboard", description = "Endpoints consumidos pelo frontend do dashboard de visualização")
public interface DashboardApi {

    @Operation(
            summary = "Visão Geral Nacional",
            description = "Retorna KPIs nacionais, dados do mapa coroplético, penetração por região e cobertura de municípios.",
            responses = @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso")
    )
    ResponseEntity<VisaoGeralResponse> getVisaoGeral(
            @Parameter(description = "Filtro por região (ex: Norte, Nordeste, Centro-Oeste, Sudeste, Sul)")
            @RequestParam(required = false) String regiao,
            @Parameter(description = "Mês de referência no formato YYYY-MM (ex: 2024-12). Padrão: mês mais recente disponível.")
            @RequestParam(required = false) String anoMes
    );

    @Operation(
            summary = "Disparidade Regional",
            description = "Retorna distribuição IQR por região, desvio padrão intra-regional e rankings Top/Bottom 10 municípios.",
            responses = @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso")
    )
    ResponseEntity<DisparidadeRegionalResponse> getDisparidadeRegional(
            @Parameter(description = "Filtro por região")
            @RequestParam(required = false) String regiao,
            @Parameter(description = "Mês de referência no formato YYYY-MM. Padrão: mês mais recente.")
            @RequestParam(required = false) String anoMes
    );

    @Operation(
            summary = "Evolução Temporal",
            description = "Retorna série temporal de penetração regional, crescimento acumulado e ticket médio nacional.",
            responses = @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso")
    )
    ResponseEntity<EvolucaoTemporalResponse> getEvolucaoTemporal(
            @Parameter(description = "Filtro por região")
            @RequestParam(required = false) String regiao,
            @Parameter(description = "Início do período no formato YYYY-MM (ex: 2021-01). Padrão: início da série.")
            @RequestParam(required = false) String dataInicio,
            @Parameter(description = "Fim do período no formato YYYY-MM (ex: 2024-12). Padrão: fim da série.")
            @RequestParam(required = false) String dataFim
    );

    @Operation(
            summary = "Lista de Municípios",
            description = "Retorna a lista de todos os municípios com dados Pix para uso no autocomplete.",
            responses = @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso")
    )
    ResponseEntity<List<MunicipioListItemDTO>> getMunicipios();

    @Operation(
            summary = "Detalhes de Município",
            description = "Retorna KPIs e indicadores socioeconômicos de um município específico.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dados retornados com sucesso"),
                    @ApiResponse(responseCode = "404", description = "Município não encontrado para o período")
            }
    )
    ResponseEntity<MunicipioDetalhesDTO> getMunicipioDetalhes(
            @Parameter(description = "Código IBGE do município (7 dígitos)")
            @PathVariable String municipioIbge,
            @Parameter(description = "Mês de referência no formato YYYY-MM. Padrão: mês mais recente.")
            @RequestParam(required = false) String anoMes
    );
}
