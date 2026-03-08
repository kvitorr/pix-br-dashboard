package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import tcc.vitor.pix_dashboard.database.repositories.projections.*;
import tcc.vitor.pix_dashboard.services.dto.dashboard.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DashboardQueryRepository {

    private final VwIndicadoresMunicipioRepository indicadoresRepo;
    private final VwEvolucaoRegionalRepository evolucaoRepo;

    public DashboardQueryRepository(VwIndicadoresMunicipioRepository indicadoresRepo,
                                    VwEvolucaoRegionalRepository evolucaoRepo) {
        this.indicadoresRepo = indicadoresRepo;
        this.evolucaoRepo = evolucaoRepo;
    }

    // =========================================================================
    // Helper
    // =========================================================================

    public LocalDate findLatestAnoMes() {
        LocalDate result = indicadoresRepo.findMaxAnoMes();
        return result != null ? result : LocalDate.now().withDayOfMonth(1);
    }

    // =========================================================================
    // Visão Geral
    // =========================================================================

    public KpisVisaoGeralDTO findKpisVisaoGeral(LocalDate anoMes, String regiao) {
        KpisVisaoGeralProjection p = indicadoresRepo.findKpisVisaoGeral(anoMes, regiao);
        return new KpisVisaoGeralDTO(
                round2(p.getPenetracaoMedia()),
                round2(p.getTicketMedio()),
                round4(p.getRazaoPjPf()),
                round2(p.getVlPerCapita())
        );
    }

    public List<MapaMunicipioDTO> findMapaMunicipios(LocalDate anoMes, String regiao) {
        return indicadoresRepo.findMapaMunicipios(anoMes, regiao).stream()
                .map(p -> new MapaMunicipioDTO(p.getMunicipioIbge(), p.getMunicipioNome(), p.getPenetracaoPf()))
                .toList();
    }

    public List<PenetracaoRegiaoDTO> findPenetracaoPorRegiao(LocalDate anoMes, String regiao) {
        return indicadoresRepo.findPenetracaoPorRegiao(anoMes, regiao).stream()
                .map(p -> new PenetracaoRegiaoDTO(p.getRegiao(), p.getSiglaRegiao(), round2(p.getPenetracaoMedia())))
                .toList();
    }

    public DonutCoberturaNacionalDTO findCoberturaNacional(LocalDate anoMes, String regiao) {
        CoberturaNacionalProjection p = indicadoresRepo.findCoberturaNacional(anoMes, regiao);
        return new DonutCoberturaNacionalDTO(
                p.getAcima50() != null ? p.getAcima50() : 0L,
                p.getAbaixo50() != null ? p.getAbaixo50() : 0L
        );
    }

    // =========================================================================
    // Disparidade Regional
    // =========================================================================

    public List<IqrRegiaoDTO> findIqrPorRegiao(LocalDate anoMes, String regiao) {
        List<PenetracaoBrutaProjection> bruta = indicadoresRepo.findPenetracaoBruta(anoMes, regiao);

        Map<String, List<Double>> porRegiao = bruta.stream()
                .collect(Collectors.groupingBy(
                        PenetracaoBrutaProjection::getRegiao,
                        Collectors.mapping(PenetracaoBrutaProjection::getPenetracaoPf, Collectors.toList())
                ));

        return porRegiao.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    List<Double> vals = e.getValue();
                    return new IqrRegiaoDTO(
                            e.getKey(),
                            round2(percentileCont(vals, 0.25)),
                            round2(percentileCont(vals, 0.50)),
                            round2(percentileCont(vals, 0.75)),
                            round2(stddev(vals))
                    );
                })
                .toList();
    }

    public List<MunicipioRankingDTO> findTop10(LocalDate anoMes, String regiao) {
        return indicadoresRepo.findTopMunicipios(anoMes, regiao, PageRequest.of(0, 10))
                .stream()
                .map(this::toRankingDTO)
                .toList();
    }

    public List<MunicipioRankingDTO> findBottom10(LocalDate anoMes, String regiao) {
        return indicadoresRepo.findBottomMunicipios(anoMes, regiao, PageRequest.of(0, 10))
                .stream()
                .map(this::toRankingDTO)
                .toList();
    }

    // =========================================================================
    // Fatores Socioeconômicos
    // =========================================================================

    public List<ScatterMunicipioDTO> findScatterData(LocalDate anoMes, String regiao) {
        return indicadoresRepo.findScatterData(anoMes, regiao).stream()
                .map(p -> new ScatterMunicipioDTO(
                        p.getMunicipioIbge(), p.getMunicipio(), p.getRegiao(),
                        p.getPibPerCapita(), p.getIdhm(), p.getTaxaUrbanizacao(), p.getPenetracaoPf()
                ))
                .toList();
    }

    // =========================================================================
    // Análise Municipal
    // =========================================================================

    public List<MunicipioListItemDTO> findAllMunicipios() {
        return indicadoresRepo.findAllMunicipios().stream()
                .map(p -> new MunicipioListItemDTO(
                        p.getMunicipioIbge(), p.getMunicipio(), p.getEstado(),
                        p.getRegiao(), p.getSiglaRegiao()
                ))
                .toList();
    }

    public Optional<MunicipioDetalhesDTO> findMunicipioDetalhes(String municipioIbge, LocalDate anoMes) {
        return indicadoresRepo.findMunicipioDetalhes(municipioIbge, anoMes)
                .map(p -> new MunicipioDetalhesDTO(
                        p.getMunicipioIbge(), p.getMunicipio(), p.getEstado(),
                        p.getRegiao(), p.getSiglaRegiao(),
                        round2(p.getPenetracaoPf()),
                        round2(p.getTicketMedioPf()),
                        round4(p.getRazaoPjPf()),
                        round2(p.getVlPerCapitaPf()),
                        round2(p.getPibPerCapita()),
                        round4(p.getIdhm()),
                        round2(p.getTaxaUrbanizacao())
                ));
    }

    // =========================================================================
    // Evolução Temporal
    // =========================================================================

    public List<SerieTemporalRegionalProjection> findSerieTemporalRegional(
            String regiao, LocalDate dataInicio, LocalDate dataFim) {
        return evolucaoRepo.findSerieTemporalRegional(regiao, dataInicio, dataFim);
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private MunicipioRankingDTO toRankingDTO(MunicipioRankingProjection p) {
        return new MunicipioRankingDTO(
                p.getMunicipioIbge(), p.getMunicipio(), p.getEstado(),
                p.getRegiao(), p.getSiglaRegiao(), p.getPenetracaoPf()
        );
    }

    private double percentileCont(List<Double> sorted, double p) {
        int n = sorted.size();
        if (n == 0) return 0.0;
        double idx = p * (n - 1);
        int lo = (int) idx;
        int hi = lo + 1;
        double frac = idx - lo;
        return hi >= n ? sorted.get(lo) : sorted.get(lo) + frac * (sorted.get(hi) - sorted.get(lo));
    }

    private double stddev(List<Double> values) {
        if (values.isEmpty()) return 0.0;
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private Double round2(Double v) {
        return v == null ? null : Math.round(v * 100.0) / 100.0;
    }

    private Double round4(Double v) {
        return v == null ? null : Math.round(v * 10000.0) / 10000.0;
    }
}
