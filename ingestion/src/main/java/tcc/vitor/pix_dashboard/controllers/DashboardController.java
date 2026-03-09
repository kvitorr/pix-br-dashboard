package tcc.vitor.pix_dashboard.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tcc.vitor.pix_dashboard.services.DashboardService;
import tcc.vitor.pix_dashboard.services.dto.dashboard.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController implements DashboardApi {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    @GetMapping("/visao-geral")
    public ResponseEntity<VisaoGeralResponse> getVisaoGeral(
            @RequestParam(required = false) String regiao,
            @RequestParam(required = false) String anoMes) {
        return ResponseEntity.ok(dashboardService.getVisaoGeral(regiao, anoMes));
    }

    @Override
    @GetMapping("/disparidade-regional")
    public ResponseEntity<DisparidadeRegionalResponse> getDisparidadeRegional(
            @RequestParam(required = false) String regiao,
            @RequestParam(required = false) String anoMes) {
        return ResponseEntity.ok(dashboardService.getDisparidadeRegional(regiao, anoMes));
    }

    @Override
    @GetMapping("/evolucao-temporal")
    public ResponseEntity<EvolucaoTemporalResponse> getEvolucaoTemporal(
            @RequestParam(required = false) String regiao,
            @RequestParam(required = false) String dataInicio,
            @RequestParam(required = false) String dataFim) {
        return ResponseEntity.ok(dashboardService.getEvolucaoTemporal(regiao, dataInicio, dataFim));
    }

    @Override
    @GetMapping("/municipios")
    public ResponseEntity<List<MunicipioListItemDTO>> getMunicipios() {
        return ResponseEntity.ok(dashboardService.getMunicipios());
    }

    @Override
    @GetMapping("/municipio/{municipioIbge}")
    public ResponseEntity<MunicipioDetalhesDTO> getMunicipioDetalhes(
            @PathVariable String municipioIbge,
            @RequestParam(required = false) String anoMes) {
        return ResponseEntity.ok(dashboardService.getMunicipioDetalhes(municipioIbge, anoMes));
    }
}
