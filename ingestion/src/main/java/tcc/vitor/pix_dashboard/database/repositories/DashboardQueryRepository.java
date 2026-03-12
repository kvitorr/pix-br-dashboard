package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import tcc.vitor.pix_dashboard.database.models.VwIndicadoresMunicipio;
import tcc.vitor.pix_dashboard.database.repositories.projections.*;
import tcc.vitor.pix_dashboard.services.dto.dashboard.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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
                .map(p -> new MapaMunicipioDTO(
                        p.getMunicipioIbge(),
                        p.getMunicipioNome(),
                        p.getPenetracaoPf(),
                        round2(p.getTicketMedioPf()),
                        round4(p.getRazaoPjPf()),
                        round2(p.getVlPerCapitaPf())))
                .toList();
    }

    public List<PenetracaoRegiaoDTO> findWeightedAverageMetricsPorRegiao(LocalDate anoMes, String regiao) {
        return indicadoresRepo.findWeightedAverageMetricsPorRegiao(anoMes, regiao).stream()
                .map(p -> new PenetracaoRegiaoDTO(
                        p.getRegiao(),
                        p.getSiglaRegiao(),
                        round2(p.getPenetracaoMedia()),
                        round2(p.getTicketMedioMedia()),
                        round4(p.getRazaoMedia()),
                        round2(p.getPerCapitaMedia())))
                .toList();
    }

    // =========================================================================
    // Disparidade Regional
    // =========================================================================

    public List<IqrRegiaoDTO> findIqrPorRegiao(LocalDate anoMes, String regiao, String metrica) {
        List<MetricasBrutaProjection> bruta = indicadoresRepo.findMetricasBruta(anoMes, regiao);

        Map<String, List<Double>> porRegiao = bruta.stream()
                .filter(p -> getMetricaValueFromBruta(p, metrica) != null)
                .collect(Collectors.groupingBy(
                        MetricasBrutaProjection::getRegiao,
                        Collectors.mapping(p -> getMetricaValueFromBruta(p, metrica), Collectors.toList())
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

    public List<MunicipioRankingDTO> findTop10(LocalDate anoMes, String regiao, String metrica) {
        return indicadoresRepo.findAllForScatter(anoMes, regiao).stream()
                .filter(m -> getMetricaValueFromEntity(m, metrica) != null)
                .sorted(Comparator.comparingDouble((VwIndicadoresMunicipio m) ->
                        getMetricaValueFromEntity(m, metrica)).reversed())
                .limit(10)
                .map(this::toRankingDTO)
                .toList();
    }

    public List<MunicipioRankingDTO> findBottom10(LocalDate anoMes, String regiao, String metrica) {
        return indicadoresRepo.findAllForScatter(anoMes, regiao).stream()
                .filter(m -> getMetricaValueFromEntity(m, metrica) != null)
                .sorted(Comparator.comparingDouble((VwIndicadoresMunicipio m) ->
                        getMetricaValueFromEntity(m, metrica)))
                .limit(10)
                .map(this::toRankingDTO)
                .toList();
    }

    @Cacheable(
            value = "municipiosAtipicos",
            key = "#anoMes.toString() + '_' + (#regiao != null ? #regiao : 'ALL') + '_' + (#metrica != null ? #metrica : 'penetracaoPf')",
            condition = "#anoMes.isBefore(T(java.time.LocalDate).now().withDayOfMonth(1))"
    )
    public List<MunicipioAtipicoDTO> findMunicipiosAtipicos(LocalDate anoMes, String regiao, String metrica) {
        List<VwIndicadoresMunicipio> municipios = indicadoresRepo.findAllWithPib(anoMes, regiao).stream()
                .filter(m -> getMetricaValueFromEntity(m, metrica) != null)
                .toList();

        if (municipios.isEmpty()) return List.of();

        List<Double> sortedMetrica = municipios.stream()
                .map(m -> getMetricaValueFromEntity(m, metrica))
                .sorted()
                .toList();
        List<Double> sortedPib = municipios.stream()
                .map(VwIndicadoresMunicipio::getPibPerCapita)
                .sorted()
                .toList();

        double medianaMetrica = percentileCont(sortedMetrica, 0.50);
        double medianaPib = percentileCont(sortedPib, 0.50);

        String tagAcima = getTagAcima(metrica);
        String tagAbaixo = getTagAbaixo(metrica);

        List<MunicipioAtipicoDTO> altaAdocaoBaixoPib = municipios.stream()
                .filter(m -> getMetricaValueFromEntity(m, metrica) > medianaMetrica
                        && m.getPibPerCapita() < medianaPib)
                .sorted(Comparator.comparingDouble((VwIndicadoresMunicipio m) ->
                        getMetricaValueFromEntity(m, metrica)).reversed())
                .limit(6)
                .map(m -> new MunicipioAtipicoDTO(
                        m.getId().getMunicipioIbge(), m.getMunicipio(), m.getEstado(),
                        m.getRegiao(), m.getSiglaRegiao(),
                        round2(m.getPenetracaoPf()),
                        round2(m.getTicketMedioPf()),
                        round4(m.getRazaoPjPf()),
                        round2(m.getVlPerCapitaPf()),
                        round2(m.getPibPerCapita()),
                        "alta-adocao-baixo-pib",
                        List.of("PIB baixo", tagAcima)
                ))
                .toList();

        List<MunicipioAtipicoDTO> baixaAdocaoAltoPib = municipios.stream()
                .filter(m -> getMetricaValueFromEntity(m, metrica) < medianaMetrica
                        && m.getPibPerCapita() > medianaPib)
                .sorted(Comparator.comparingDouble((VwIndicadoresMunicipio m) ->
                        getMetricaValueFromEntity(m, metrica)))
                .limit(6)
                .map(m -> new MunicipioAtipicoDTO(
                        m.getId().getMunicipioIbge(), m.getMunicipio(), m.getEstado(),
                        m.getRegiao(), m.getSiglaRegiao(),
                        round2(m.getPenetracaoPf()),
                        round2(m.getTicketMedioPf()),
                        round4(m.getRazaoPjPf()),
                        round2(m.getVlPerCapitaPf()),
                        round2(m.getPibPerCapita()),
                        "baixa-adocao-alto-pib",
                        List.of("PIB alto", tagAbaixo)
                ))
                .toList();

        List<MunicipioAtipicoDTO> result = new ArrayList<>(altaAdocaoBaixoPib);
        result.addAll(baixaAdocaoAltoPib);
        return result;
    }

    // =========================================================================
    // Fatores Socioeconômicos
    // =========================================================================

    public List<ScatterMunicipioDTO> findScatterData(LocalDate anoMes, String regiao) {
        return indicadoresRepo.findAllForScatter(anoMes, regiao).stream()
                .map(m -> new ScatterMunicipioDTO(
                        m.getId().getMunicipioIbge(),
                        m.getMunicipio(),
                        m.getEstado(),
                        m.getRegiao(),
                        round2(m.getPibPerCapita()),
                        round4(m.getIdhm()),
                        round2(m.getTaxaUrbanizacao()),
                        round2(m.getPenetracaoPf()),
                        round2(m.getTicketMedioPf()),
                        round4(m.getRazaoPjPf()),
                        round2(m.getVlPerCapitaPf())
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

    public List<MunicipioListItemDTO> searchMunicipios(String nome, int limit) {
        return indicadoresRepo.searchByName(nome, org.springframework.data.domain.PageRequest.of(0, limit)).stream()
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

    public List<VwIndicadoresMunicipio> findSerieMunicipio(
            String municipioIbge, LocalDate dataInicio, LocalDate dataFim) {
        return indicadoresRepo.findSerieMunicipio(municipioIbge, dataInicio, dataFim);
    }

    public List<MediaTemporalProjection> findMediasRegionais(
            String regiao, LocalDate dataInicio, LocalDate dataFim) {
        return indicadoresRepo.findMediasRegionais(regiao, dataInicio, dataFim);
    }

    public List<MediaTemporalProjection> findMediasNacionais(
            LocalDate dataInicio, LocalDate dataFim) {
        return indicadoresRepo.findMediasNacionais(dataInicio, dataFim);
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

    private Double getMetricaValueFromBruta(MetricasBrutaProjection p, String metrica) {
        return switch (metrica != null ? metrica : "penetracaoPf") {
            case "ticketMedioPf" -> p.getTicketMedioPf();
            case "razaoPjPf"     -> p.getRazaoPjPf();
            case "vlPerCapitaPf" -> p.getVlPerCapitaPf();
            default              -> p.getPenetracaoPf();
        };
    }

    private Double getMetricaValueFromEntity(VwIndicadoresMunicipio m, String metrica) {
        return switch (metrica != null ? metrica : "penetracaoPf") {
            case "ticketMedioPf" -> m.getTicketMedioPf();
            case "razaoPjPf"     -> m.getRazaoPjPf();
            case "vlPerCapitaPf" -> m.getVlPerCapitaPf();
            default              -> m.getPenetracaoPf();
        };
    }

    private String getTagAcima(String metrica) {
        return switch (metrica != null ? metrica : "penetracaoPf") {
            case "ticketMedioPf" -> "Ticket Médio acima da média";
            case "razaoPjPf"     -> "Razão PJ/PF acima da média";
            case "vlPerCapitaPf" -> "Per Capita acima da média";
            default              -> "Penetração acima da média";
        };
    }

    private String getTagAbaixo(String metrica) {
        return switch (metrica != null ? metrica : "penetracaoPf") {
            case "ticketMedioPf" -> "Ticket Médio abaixo do esperado";
            case "razaoPjPf"     -> "Razão PJ/PF abaixo do esperado";
            case "vlPerCapitaPf" -> "Per Capita abaixo do esperado";
            default              -> "Penetração abaixo do esperado";
        };
    }

    private MunicipioRankingDTO toRankingDTO(VwIndicadoresMunicipio m) {
        return new MunicipioRankingDTO(
                m.getId().getMunicipioIbge(), m.getMunicipio(), m.getEstado(),
                m.getRegiao(), m.getSiglaRegiao(),
                round2(m.getPenetracaoPf()),
                round2(m.getTicketMedioPf()),
                round4(m.getRazaoPjPf()),
                round2(m.getVlPerCapitaPf())
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
