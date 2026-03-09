package tcc.vitor.pix_dashboard.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tcc.vitor.pix_dashboard.database.repositories.DashboardQueryRepository;
import tcc.vitor.pix_dashboard.services.dto.dashboard.*;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceFatoresTest {

    @Mock
    private DashboardQueryRepository repository;

    @InjectMocks
    private DashboardService dashboardService;

    private static final LocalDate ANO_MES = LocalDate.of(2024, 12, 1);
    private static final String ANO_MES_STR = "2024-12";

    // Helper: cria um ScatterMunicipioDTO com os campos mais relevantes
    private ScatterMunicipioDTO scatter(String ibge, String municipio, String regiao,
                                        Double pib, Double idhm, Double urb,
                                        Double pen, Double ticket) {
        return new ScatterMunicipioDTO(ibge, municipio, "UF", regiao,
                pib, idhm, urb, pen, ticket, null, null);
    }

    @BeforeEach
    void setUp() {
        when(repository.findMunicipiosAtipicos(any(), any())).thenReturn(List.of());
    }

    // =========================================================================
    // Estrutura da resposta
    // =========================================================================

    @Test
    void getFatoresSocioeconomicos_happyPath_returnsResponseWithExpectedStructure() {
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Sul", 10.0, 0.5, 70.0, 30.0, 50.0),
                scatter("2", "B", "Sul", 20.0, 0.6, 75.0, 40.0, 60.0),
                scatter("3", "C", "Sul", 30.0, 0.7, 80.0, 50.0, 70.0),
                scatter("4", "D", "Sul", 40.0, 0.8, 85.0, 60.0, 80.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "penetracaoPf");

        assertThat(response.scatterData()).hasSize(4);
        assertThat(response.correlacoes()).hasSize(3);
        assertThat(response.top10()).isNotNull();
        assertThat(response.bottom10()).isNotNull();
        assertThat(response.municipiosAtipicos()).isEmpty();
    }

    @Test
    void getFatoresSocioeconomicos_correlacoes_contemFatoresEsperados() {
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Norte", 10.0, 0.5, 60.0, 10.0, 100.0),
                scatter("2", "B", "Norte", 20.0, 0.6, 70.0, 20.0, 200.0),
                scatter("3", "C", "Norte", 30.0, 0.7, 80.0, 30.0, 300.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "penetracaoPf");

        List<String> fatores = response.correlacoes().stream()
                .map(CorrelacaoSpearmanDTO::fator)
                .toList();
        assertThat(fatores).containsExactlyInAnyOrder("pibPerCapita", "idhm", "taxaUrbanizacao");
    }

    // =========================================================================
    // Cálculo de Spearman
    // =========================================================================

    @Test
    void getFatoresSocioeconomicos_comCorrelacaoPositivaPerfeita_retornaRhoProximoDeUm() {
        // PIB e Penetração PF têm exatamente os mesmos ranks → ρ = 1.0
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Norte", 10.0, 0.5, 60.0, 10.0, 100.0),
                scatter("2", "B", "Norte", 20.0, 0.6, 70.0, 20.0, 200.0),
                scatter("3", "C", "Norte", 30.0, 0.7, 80.0, 30.0, 300.0),
                scatter("4", "D", "Norte", 40.0, 0.8, 90.0, 40.0, 400.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "penetracaoPf");

        CorrelacaoSpearmanDTO pibCorr = response.correlacoes().stream()
                .filter(c -> c.fator().equals("pibPerCapita"))
                .findFirst().orElseThrow();

        assertThat(pibCorr.rho()).isCloseTo(1.0, within(0.01));
        assertThat(pibCorr.forca()).isEqualTo("Forte");
        assertThat(pibCorr.n()).isEqualTo(4);
        assertThat(pibCorr.pValor()).isLessThan(0.05);
    }

    @Test
    void getFatoresSocioeconomicos_comCorrelacaoNegativaPerfeita_retornaRhoProximoDeMinusUm() {
        // PIB cresce, Penetração decresce → ρ = -1.0
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Sul", 10.0, 0.5, 60.0, 40.0, 100.0),
                scatter("2", "B", "Sul", 20.0, 0.6, 70.0, 30.0, 200.0),
                scatter("3", "C", "Sul", 30.0, 0.7, 80.0, 20.0, 300.0),
                scatter("4", "D", "Sul", 40.0, 0.8, 90.0, 10.0, 400.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "penetracaoPf");

        CorrelacaoSpearmanDTO pibCorr = response.correlacoes().stream()
                .filter(c -> c.fator().equals("pibPerCapita"))
                .findFirst().orElseThrow();

        assertThat(pibCorr.rho()).isCloseTo(-1.0, within(0.01));
        assertThat(pibCorr.forca()).isEqualTo("Forte");
    }

    @Test
    void getFatoresSocioeconomicos_comPoucosParesValidos_retornaFracaEPValorUm() {
        // Apenas 1 município com idhm não-nulo → n < 3 → Fraca, rho = 0.0
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Norte", 10.0, null, null, 20.0, null)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "penetracaoPf");

        CorrelacaoSpearmanDTO idhmCorr = response.correlacoes().stream()
                .filter(c -> c.fator().equals("idhm"))
                .findFirst().orElseThrow();

        assertThat(idhmCorr.forca()).isEqualTo("Fraca");
        assertThat(idhmCorr.rho()).isZero();
        assertThat(idhmCorr.pValor()).isEqualTo(1.0);
    }

    @Test
    void getFatoresSocioeconomicos_forcaLabel_classificaCorretamente() {
        // |ρ| >= 0.5 → Forte; |ρ| >= 0.3 → Moderada; < 0.3 → Fraca
        // Geramos correlação perfeita para PIB (Forte) e verificamos o label
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Norte", 10.0, 0.5, 60.0, 10.0, 100.0),
                scatter("2", "B", "Norte", 20.0, 0.6, 70.0, 20.0, 200.0),
                scatter("3", "C", "Norte", 30.0, 0.7, 80.0, 30.0, 300.0),
                scatter("4", "D", "Norte", 40.0, 0.8, 90.0, 40.0, 400.0),
                scatter("5", "E", "Norte", 50.0, 0.9, 95.0, 50.0, 500.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "penetracaoPf");

        response.correlacoes().forEach(c ->
                assertThat(c.forca()).isIn("Forte", "Moderada", "Fraca")
        );

        CorrelacaoSpearmanDTO pibCorr = response.correlacoes().stream()
                .filter(c -> c.fator().equals("pibPerCapita"))
                .findFirst().orElseThrow();
        assertThat(pibCorr.forca()).isEqualTo("Forte");
    }

    // =========================================================================
    // Variável Y dinâmica
    // =========================================================================

    @Test
    void getFatoresSocioeconomicos_comTicketMedioPfComoY_usaTicketNaCorrelacao() {
        // PIB e Ticket têm correlação perfeita; Penetração é inversa (não deve influenciar)
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Norte", 10.0, 0.5, 60.0, 90.0, 100.0),
                scatter("2", "B", "Norte", 20.0, 0.6, 70.0, 80.0, 200.0),
                scatter("3", "C", "Norte", 30.0, 0.7, 80.0, 70.0, 300.0),
                scatter("4", "D", "Norte", 40.0, 0.8, 90.0, 60.0, 400.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "ticketMedioPf");

        CorrelacaoSpearmanDTO pibCorr = response.correlacoes().stream()
                .filter(c -> c.fator().equals("pibPerCapita"))
                .findFirst().orElseThrow();

        // PIB e Ticket têm mesmos ranks → ρ ≈ 1.0
        assertThat(pibCorr.rho()).isCloseTo(1.0, within(0.01));
    }

    @Test
    void getFatoresSocioeconomicos_comVariavelYNula_usaPenetracaoPfComoDefault() {
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Norte", 10.0, 0.5, 60.0, 10.0, 100.0),
                scatter("2", "B", "Norte", 20.0, 0.6, 70.0, 20.0, 200.0),
                scatter("3", "C", "Norte", 30.0, 0.7, 80.0, 30.0, 300.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        // variavelY null → não deve lançar exceção e retorna 3 correlações
        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, null);

        assertThat(response.correlacoes()).hasSize(3);
    }

    // =========================================================================
    // Ranking
    // =========================================================================

    @Test
    void getFatoresSocioeconomicos_top10_ordenadoPorPenetracaoDecrescente() {
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Norte", 10.0, 0.5, 60.0, 50.0, 100.0),
                scatter("2", "B", "Norte", 20.0, 0.6, 70.0, 90.0, 200.0), // maior pen
                scatter("3", "C", "Norte", 30.0, 0.7, 80.0, 10.0, 300.0), // menor pen
                scatter("4", "D", "Norte", 40.0, 0.8, 90.0, 70.0, 400.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "penetracaoPf");

        List<MunicipioRankingDTO> top10 = response.top10();
        assertThat(top10).isNotEmpty();
        assertThat(top10.get(0).penetracaoPf()).isEqualTo(90.0); // B tem maior penetração
    }

    @Test
    void getFatoresSocioeconomicos_bottom10_ordenadoPorPenetracaoCrescente() {
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Norte", 10.0, 0.5, 60.0, 50.0, 100.0),
                scatter("2", "B", "Norte", 20.0, 0.6, 70.0, 90.0, 200.0),
                scatter("3", "C", "Norte", 30.0, 0.7, 80.0, 10.0, 300.0), // menor pen
                scatter("4", "D", "Norte", 40.0, 0.8, 90.0, 70.0, 400.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "penetracaoPf");

        List<MunicipioRankingDTO> bottom10 = response.bottom10();
        assertThat(bottom10).isNotEmpty();
        assertThat(bottom10.get(0).penetracaoPf()).isEqualTo(10.0); // C tem menor penetração
    }

    @Test
    void getFatoresSocioeconomicos_rankingExcluiMunicipiosComYNulo() {
        // A tem ticketMedioPf nulo → deve ser excluído do ranking quando Y = ticketMedioPf
        List<ScatterMunicipioDTO> data = List.of(
                scatter("1", "A", "Norte", 10.0, 0.5, 60.0, 50.0, null),   // ticket nulo
                scatter("2", "B", "Norte", 20.0, 0.6, 70.0, 80.0, 100.0),
                scatter("3", "C", "Norte", 30.0, 0.7, 80.0, 30.0, 200.0)
        );
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "ticketMedioPf");

        assertThat(response.top10()).noneMatch(m -> m.municipio().equals("A"));
        assertThat(response.bottom10()).noneMatch(m -> m.municipio().equals("A"));
    }

    @Test
    void getFatoresSocioeconomicos_top10_limitadoA10Itens() {
        // 15 municípios → top10 deve ter no máximo 10
        List<ScatterMunicipioDTO> data = new java.util.ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            data.add(scatter(String.valueOf(i), "Mun" + i, "Sul",
                    (double) i * 10, 0.5 + i * 0.01, 60.0 + i,
                    (double) i * 5, (double) i * 100));
        }
        when(repository.findScatterData(ANO_MES, null)).thenReturn(data);

        FatoresSocioeconomicosResponse response =
                dashboardService.getFatoresSocioeconomicos(null, ANO_MES_STR, "penetracaoPf");

        assertThat(response.top10()).hasSize(10);
        assertThat(response.bottom10()).hasSize(10);
    }
}
