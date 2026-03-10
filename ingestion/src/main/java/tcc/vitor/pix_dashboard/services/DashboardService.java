package tcc.vitor.pix_dashboard.services;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tcc.vitor.pix_dashboard.database.models.VwIndicadoresMunicipio;
import tcc.vitor.pix_dashboard.database.repositories.DashboardQueryRepository;
import tcc.vitor.pix_dashboard.database.repositories.projections.MediaTemporalProjection;
import tcc.vitor.pix_dashboard.database.repositories.projections.SerieTemporalRegionalProjection;
import tcc.vitor.pix_dashboard.services.dto.dashboard.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DashboardService {

    private static final DateTimeFormatter ANO_MES_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private final DashboardQueryRepository repository;

    public DashboardService(DashboardQueryRepository repository) {
        this.repository = repository;
    }

    // =========================================================================
    // Página 1 — Visão Geral Nacional
    // =========================================================================

    public VisaoGeralResponse getVisaoGeral(String regiao, String anoMes) {
        LocalDate data = resolveAnoMes(anoMes);
        String regiaoParam = emptyToNull(regiao);
        return new VisaoGeralResponse(
                repository.findKpisVisaoGeral(data, regiaoParam),
                repository.findMapaMunicipios(data, regiaoParam),
                repository.findPenetracaoPorRegiao(data, regiaoParam),
                repository.findCoberturaNacional(data, regiaoParam)
        );
    }

    // =========================================================================
    // Página 2 — Disparidade Regional
    // =========================================================================

    public DisparidadeRegionalResponse getDisparidadeRegional(String regiao, String anoMes, String metrica) {
        LocalDate data = resolveAnoMes(anoMes);
        String regiaoParam = emptyToNull(regiao);
        String metricaParam = (metrica == null || metrica.isBlank()) ? "penetracaoPf" : metrica;
        return new DisparidadeRegionalResponse(
                repository.findIqrPorRegiao(data, regiaoParam, metricaParam),
                repository.findTop10(data, regiaoParam, metricaParam),
                repository.findBottom10(data, regiaoParam, metricaParam),
                repository.findMunicipiosAtipicos(data, regiaoParam, metricaParam)
        );
    }

    // =========================================================================
    // Página 3 — Evolução Temporal
    // =========================================================================

    public EvolucaoTemporalResponse getEvolucaoTemporal(String regiao, String dataInicio, String dataFim) {
        String regiaoParam = emptyToNull(regiao);
        LocalDate inicio = parseMesOpcional(dataInicio);
        LocalDate fim = parseMesOpcional(dataFim);

        if (inicio == null) inicio = LocalDate.of(2020, 11, 1); // lançamento do Pix
        if (fim == null) fim = LocalDate.now().withDayOfMonth(1);

        List<SerieTemporalRegionalProjection> rows =
                repository.findSerieTemporalRegional(regiaoParam, inicio, fim);

        // Agrupar por ano_mes → { anoMes: string, porRegiao: [{regiao, penetracaoMedia}] }
        LinkedHashMap<String, List<RegiaoPenetracaoDTO>> porMes = new LinkedHashMap<>();
        Map<String, Double> primeiroMesPorRegiao = new LinkedHashMap<>();
        Map<String, Double> ultimoMesPorRegiao = new LinkedHashMap<>();
        Map<String, Double> ticketNacionalPorMes = new LinkedHashMap<>();

        for (SerieTemporalRegionalProjection row : rows) {
            String mes = ANO_MES_FORMATTER.format(row.getAnoMes());
            String reg = row.getRegiao();
            Double penetracao = row.getPenetracaoMedia();
            Double ticket = row.getTicketMedio();

            porMes.computeIfAbsent(mes, k -> new ArrayList<>())
                    .add(new RegiaoPenetracaoDTO(reg, penetracao));

            primeiroMesPorRegiao.putIfAbsent(reg, penetracao);
            if (penetracao != null) {
                ultimoMesPorRegiao.put(reg, penetracao);
            }

            // Ticket médio nacional: média simples dos valores regionais por mês
            ticketNacionalPorMes.merge(mes, ticket != null ? ticket : 0.0, Double::sum);
        }

        List<SerieTemporalPontoDTO> serieTemporal = porMes.entrySet().stream()
                .map(e -> new SerieTemporalPontoDTO(e.getKey(), e.getValue()))
                .toList();

        // Crescimento acumulado por região
        List<CrescimentoAcumuladoDTO> crescimento = ultimoMesPorRegiao.entrySet().stream()
                .map(e -> {
                    Double primeiro = primeiroMesPorRegiao.getOrDefault(e.getKey(), e.getValue());
                    double variacao = e.getValue() - (primeiro != null ? primeiro : 0.0);
                    return new CrescimentoAcumuladoDTO(e.getKey(), Math.round(variacao * 100.0) / 100.0);
                })
                .toList();

        // Ticket médio nacional por mês (média das regiões)
        int totalRegioes = porMes.isEmpty() ? 1 : (int) porMes.values().stream()
                .mapToLong(List::size).average().orElse(1);
        List<TicketNacionalDTO> ticketEvolucao = ticketNacionalPorMes.entrySet().stream()
                .map(e -> new TicketNacionalDTO(e.getKey(),
                        Math.round(e.getValue() / totalRegioes * 100.0) / 100.0))
                .toList();

        // KPIs
        KpisEvolucaoDTO kpis = buildKpisEvolucao(ultimoMesPorRegiao, crescimento, serieTemporal.size());

        return new EvolucaoTemporalResponse(kpis, serieTemporal, crescimento, ticketEvolucao);
    }

    // =========================================================================
    // Análise Municipal
    // =========================================================================

    public List<MunicipioListItemDTO> getMunicipios() {
        return repository.findAllMunicipios();
    }

    public List<MunicipioListItemDTO> searchMunicipios(String nome, int limit) {
        return repository.searchMunicipios(nome, limit);
    }

    public MunicipioDetalhesDTO getMunicipioDetalhes(String municipioIbge, String anoMes) {
        LocalDate data = resolveAnoMes(anoMes);
        return repository.findMunicipioDetalhes(municipioIbge, data)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Município não encontrado: " + municipioIbge + " para " + data
                ));
    }

    public MunicipioSerieResponse getMunicipioSerie(
            String municipioIbge, String dataInicio, String dataFim) {

        LocalDate fim = parseMesOpcional(dataFim);
        if (fim == null) fim = repository.findLatestAnoMes();

        LocalDate inicio = parseMesOpcional(dataInicio);
        if (inicio == null) inicio = fim.minusMonths(11);

        List<VwIndicadoresMunicipio> serieMunicipio =
                repository.findSerieMunicipio(municipioIbge, inicio, fim);

        if (serieMunicipio.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Sem dados para o município: " + municipioIbge
            );
        }

        String regiao = serieMunicipio.getFirst().getRegiao();
        String siglaRegiao = serieMunicipio.getFirst().getSiglaRegiao();

        Map<String, MediaTemporalProjection> mediasRegiao =
                toMapPorMes(repository.findMediasRegionais(regiao, inicio, fim));
        Map<String, MediaTemporalProjection> mediasNacional =
                toMapPorMes(repository.findMediasNacionais(inicio, fim));

        List<MunicipioSeriePontoDTO> serie = serieMunicipio.stream()
                .map(m -> {
                    String mes = ANO_MES_FORMATTER.format(m.getId().getAnoMes());
                    MediaTemporalProjection mRegiao = mediasRegiao.get(mes);
                    MediaTemporalProjection mNacional = mediasNacional.get(mes);
                    return new MunicipioSeriePontoDTO(
                            mes,
                            round2(m.getPenetracaoPf()),
                            round2(m.getTicketMedioPf()),
                            round2(m.getVlPerCapitaPf()),
                            round4(m.getRazaoPjPf()),
                            mRegiao != null ? round2(mRegiao.getPenetracaoMedia()) : null,
                            mRegiao != null ? round2(mRegiao.getTicketMedioMedia()) : null,
                            mRegiao != null ? round2(mRegiao.getVlPerCapitaMedia()) : null,
                            mRegiao != null ? round4(mRegiao.getRazaoPjPfMedia()) : null,
                            mNacional != null ? round2(mNacional.getPenetracaoMedia()) : null,
                            mNacional != null ? round2(mNacional.getTicketMedioMedia()) : null,
                            mNacional != null ? round2(mNacional.getVlPerCapitaMedia()) : null,
                            mNacional != null ? round4(mNacional.getRazaoPjPfMedia()) : null
                    );
                })
                .toList();

        return new MunicipioSerieResponse(regiao, siglaRegiao, serie);
    }

    private Map<String, MediaTemporalProjection> toMapPorMes(List<MediaTemporalProjection> list) {
        return list.stream().collect(Collectors.toMap(
                p -> ANO_MES_FORMATTER.format(p.getAnoMes()),
                p -> p,
                (a, b) -> a
        ));
    }

    // =========================================================================
    // Página 5 — Fatores Socioeconômicos
    // =========================================================================

    public FatoresSocioeconomicosResponse getFatoresSocioeconomicos(
            String regiao, String anoMes, String variavelY) {
        LocalDate data = resolveAnoMes(anoMes);
        String regiaoParam = emptyToNull(regiao);
        String y = (variavelY == null || variavelY.isBlank()) ? "penetracaoPf" : variavelY;

        List<ScatterMunicipioDTO> scatter = repository.findScatterData(data, regiaoParam);
        List<CorrelacaoSpearmanDTO> correlacoes = calcCorrelacoes(scatter, y);
        List<MunicipioRankingDTO> top10 = extractRanking(scatter, y, true);
        List<MunicipioRankingDTO> bottom10 = extractRanking(scatter, y, false);
        List<MunicipioAtipicoDTO> atipicos = repository.findMunicipiosAtipicos(data, regiaoParam, null);

        return new FatoresSocioeconomicosResponse(scatter, correlacoes, top10, bottom10, atipicos);
    }

    private List<CorrelacaoSpearmanDTO> calcCorrelacoes(List<ScatterMunicipioDTO> data, String variavelY) {
        return List.of("pibPerCapita", "idhm", "taxaUrbanizacao").stream()
                .map(fator -> {
                    List<double[]> pairs = data.stream()
                            .filter(m -> getXValue(m, fator) != null && getYValue(m, variavelY) != null)
                            .map(m -> new double[]{getXValue(m, fator), getYValue(m, variavelY)})
                            .toList();

                    if (pairs.size() < 3)
                        return new CorrelacaoSpearmanDTO(fator, 0.0, 1.0, pairs.size(), "Fraca");

                    double[] xArr = pairs.stream().mapToDouble(p -> p[0]).toArray();
                    double[] yArr = pairs.stream().mapToDouble(p -> p[1]).toArray();
                    int n = pairs.size();

                    double rho = new SpearmansCorrelation().correlation(xArr, yArr);
                    double t = rho * Math.sqrt((n - 2.0) / (1.0 - rho * rho));
                    double pValor = 2.0 * (1.0 - new TDistribution(n - 2).cumulativeProbability(Math.abs(t)));

                    String forca = Math.abs(rho) >= 0.5 ? "Forte"
                            : Math.abs(rho) >= 0.3 ? "Moderada" : "Fraca";

                    return new CorrelacaoSpearmanDTO(fator, roundD2(rho), roundD4(pValor), n, forca);
                })
                .toList();
    }

    private List<MunicipioRankingDTO> extractRanking(
            List<ScatterMunicipioDTO> data, String variavelY, boolean top) {
        Comparator<ScatterMunicipioDTO> cmp = Comparator.comparingDouble(
                m -> m.penetracaoPf() != null ? m.penetracaoPf() : -999.0);
        Stream<ScatterMunicipioDTO> filtered = data.stream()
                .filter(m -> getYValue(m, variavelY) != null && m.penetracaoPf() != null);
        Stream<ScatterMunicipioDTO> sorted = top ? filtered.sorted(cmp.reversed()) : filtered.sorted(cmp);
        return sorted.limit(10)
                .map(m -> new MunicipioRankingDTO(
                        m.municipioIbge(), m.municipio(), m.estado(), m.regiao(), "",
                        m.penetracaoPf(), m.ticketMedioPf(), m.razaoPjPf(), m.vlPerCapitaPf()))
                .toList();
    }

    private Double getXValue(ScatterMunicipioDTO m, String fator) {
        return switch (fator) {
            case "idhm" -> m.idhm();
            case "taxaUrbanizacao" -> m.taxaUrbanizacao();
            default -> m.pibPerCapita();
        };
    }

    private Double getYValue(ScatterMunicipioDTO m, String y) {
        return switch (y) {
            case "ticketMedioPf" -> m.ticketMedioPf();
            case "razaoPjPf" -> m.razaoPjPf();
            case "vlPerCapitaPf" -> m.vlPerCapitaPf();
            default -> m.penetracaoPf();
        };
    }

    private double roundD2(double v) { return Math.round(v * 100.0) / 100.0; }
    private double roundD4(double v) { return Math.round(v * 10000.0) / 10000.0; }
    private Double round2(Double v) { return v == null ? null : Math.round(v * 100.0) / 100.0; }
    private Double round4(Double v) { return v == null ? null : Math.round(v * 10000.0) / 10000.0; }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private KpisEvolucaoDTO buildKpisEvolucao(
            Map<String, Double> ultimoMesPorRegiao,
            List<CrescimentoAcumuladoDTO> crescimento,
            int totalMeses
    ) {
        double penetracaoAtual = ultimoMesPorRegiao.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        String regiaoMaiorCrescimento = crescimento.stream()
                .max(Comparator.comparingDouble(c -> c.variacaoPp() != null ? c.variacaoPp() : Double.MIN_VALUE))
                .map(CrescimentoAcumuladoDTO::regiao)
                .orElse("N/D");

        // Convergência: redução do gap entre Sul e Norte no período
        double valSul = ultimoMesPorRegiao.getOrDefault("Sul", 0.0);
        double valNorte = ultimoMesPorRegiao.getOrDefault("Norte", 0.0);
        double convergencia = Math.round(Math.abs(valSul - valNorte) * 100.0) / 100.0;

        return new KpisEvolucaoDTO(
                Math.round(penetracaoAtual * 100.0) / 100.0,
                regiaoMaiorCrescimento,
                convergencia,
                totalMeses
        );
    }

    private LocalDate resolveAnoMes(String anoMes) {
        if (anoMes == null || anoMes.isBlank()) {
            return repository.findLatestAnoMes();
        }
        return LocalDate.parse(anoMes + "-01");
    }

    private LocalDate parseMesOpcional(String mes) {
        if (mes == null || mes.isBlank()) return null;
        return LocalDate.parse(mes + "-01");
    }

    private String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.toUpperCase();
    }
}
