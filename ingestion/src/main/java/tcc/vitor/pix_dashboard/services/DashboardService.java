package tcc.vitor.pix_dashboard.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tcc.vitor.pix_dashboard.database.repositories.DashboardQueryRepository;
import tcc.vitor.pix_dashboard.database.repositories.projections.SerieTemporalRegionalProjection;
import tcc.vitor.pix_dashboard.services.dto.dashboard.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

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

    public DisparidadeRegionalResponse getDisparidadeRegional(String regiao, String anoMes) {
        LocalDate data = resolveAnoMes(anoMes);
        String regiaoParam = emptyToNull(regiao);
        return new DisparidadeRegionalResponse(
                repository.findIqrPorRegiao(data, regiaoParam),
                repository.findTop10(data, regiaoParam),
                repository.findBottom10(data, regiaoParam)
        );
    }

    // =========================================================================
    // Página 3 — Fatores Socioeconômicos
    // =========================================================================

    public FatoresSocioeconomicosResponse getFatoresSocioeconomicos(String regiao, String anoMes) {
        LocalDate data = resolveAnoMes(anoMes);
        String regiaoParam = emptyToNull(regiao);
        return new FatoresSocioeconomicosResponse(
                repository.findScatterData(data, regiaoParam)
        );
    }

    // =========================================================================
    // Página 4 — Evolução Temporal
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

    public MunicipioDetalhesDTO getMunicipioDetalhes(String municipioIbge, String anoMes) {
        LocalDate data = resolveAnoMes(anoMes);
        return repository.findMunicipioDetalhes(municipioIbge, data)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Município não encontrado: " + municipioIbge + " para " + data
                ));
    }

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
