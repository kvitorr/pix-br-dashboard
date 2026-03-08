package tcc.vitor.pix_dashboard.database.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import tcc.vitor.pix_dashboard.services.dto.dashboard.*;

import java.time.LocalDate;
import java.util.List;

@Repository
public class DashboardQueryRepository {

    private final EntityManager em;

    public DashboardQueryRepository(EntityManager em) {
        this.em = em;
    }

    // =========================================================================
    // Helper
    // =========================================================================

    public LocalDate findLatestAnoMes() {
        Object result = em.createNativeQuery(
                "SELECT MAX(ano_mes) FROM vw_indicadores_municipio"
        ).getSingleResult();
        if (result instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.now().withDayOfMonth(1);
    }

    // =========================================================================
    // Visão Geral
    // =========================================================================

    public KpisVisaoGeralDTO findKpisVisaoGeral(LocalDate anoMes, String regiao) {
        Object[] row = (Object[]) em.createNativeQuery("""
                SELECT
                    ROUND(AVG(penetracao_pf)::numeric, 2),
                    ROUND(AVG(ticket_medio_pf)::numeric, 2),
                    ROUND(AVG(razao_pj_pf)::numeric, 4),
                    ROUND(AVG(vl_per_capita_pf)::numeric, 2)
                FROM vw_indicadores_municipio
                WHERE ano_mes = :anoMes
                  AND (:regiao IS NULL OR regiao = :regiao)
                """)
                .setParameter("anoMes", anoMes)
                .setParameter("regiao", regiao)
                .getSingleResult();
        return new KpisVisaoGeralDTO(
                toDouble(row[0]), toDouble(row[1]), toDouble(row[2]), toDouble(row[3])
        );
    }

    @SuppressWarnings("unchecked")
    public List<MapaMunicipioDTO> findMapaMunicipios(LocalDate anoMes, String regiao) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT municipio_ibge, penetracao_pf
                FROM vw_indicadores_municipio
                WHERE ano_mes = :anoMes
                  AND (:regiao IS NULL OR regiao = :regiao)
                """)
                .setParameter("anoMes", anoMes)
                .setParameter("regiao", regiao)
                .getResultList();
        return rows.stream()
                .map(r -> new MapaMunicipioDTO((String) r[0], toDouble(r[1])))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<PenetracaoRegiaoDTO> findPenetracaoPorRegiao(LocalDate anoMes, String regiao) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT regiao, sigla_regiao, ROUND(AVG(penetracao_pf)::numeric, 2)
                FROM vw_indicadores_municipio
                WHERE ano_mes = :anoMes
                  AND (:regiao IS NULL OR regiao = :regiao)
                GROUP BY regiao, sigla_regiao
                ORDER BY regiao
                """)
                .setParameter("anoMes", anoMes)
                .setParameter("regiao", regiao)
                .getResultList();
        return rows.stream()
                .map(r -> new PenetracaoRegiaoDTO((String) r[0], (String) r[1], toDouble(r[2])))
                .toList();
    }

    public DonutCoberturaNacionalDTO findCoberturaNacional(LocalDate anoMes, String regiao) {
        Object[] row = (Object[]) em.createNativeQuery("""
                SELECT
                    COUNT(*) FILTER (WHERE penetracao_pf > 50),
                    COUNT(*) FILTER (WHERE penetracao_pf <= 50)
                FROM vw_indicadores_municipio
                WHERE ano_mes = :anoMes
                  AND (:regiao IS NULL OR regiao = :regiao)
                """)
                .setParameter("anoMes", anoMes)
                .setParameter("regiao", regiao)
                .getSingleResult();
        return new DonutCoberturaNacionalDTO(toLong(row[0]), toLong(row[1]));
    }

    // =========================================================================
    // Disparidade Regional
    // =========================================================================

    @SuppressWarnings("unchecked")
    public List<IqrRegiaoDTO> findIqrPorRegiao(LocalDate anoMes, String regiao) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT
                    regiao,
                    ROUND(PERCENTILE_CONT(0.25) WITHIN GROUP (ORDER BY penetracao_pf)::numeric, 2) AS q1,
                    ROUND(PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY penetracao_pf)::numeric, 2) AS mediana,
                    ROUND(PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY penetracao_pf)::numeric, 2) AS q3,
                    ROUND(STDDEV(penetracao_pf)::numeric, 2) AS stddev
                FROM vw_indicadores_municipio
                WHERE ano_mes = :anoMes
                  AND (:regiao IS NULL OR regiao = :regiao)
                  AND penetracao_pf IS NOT NULL
                GROUP BY regiao
                ORDER BY regiao
                """)
                .setParameter("anoMes", anoMes)
                .setParameter("regiao", regiao)
                .getResultList();
        return rows.stream()
                .map(r -> new IqrRegiaoDTO((String) r[0], toDouble(r[1]), toDouble(r[2]), toDouble(r[3]), toDouble(r[4])))
                .toList();
    }

    @SuppressWarnings("unchecked")
    public List<MunicipioRankingDTO> findTop10(LocalDate anoMes, String regiao) {
        return rankingQuery(anoMes, regiao, "DESC");
    }

    @SuppressWarnings("unchecked")
    public List<MunicipioRankingDTO> findBottom10(LocalDate anoMes, String regiao) {
        return rankingQuery(anoMes, regiao, "ASC");
    }

    private List<MunicipioRankingDTO> rankingQuery(LocalDate anoMes, String regiao, String order) {
        String sql = """
                SELECT municipio_ibge, municipio, estado, regiao, sigla_regiao, penetracao_pf
                FROM vw_indicadores_municipio
                WHERE ano_mes = :anoMes
                  AND (:regiao IS NULL OR regiao = :regiao)
                  AND penetracao_pf IS NOT NULL
                ORDER BY penetracao_pf %s NULLS LAST
                LIMIT 10
                """.formatted(order);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
                .setParameter("anoMes", anoMes)
                .setParameter("regiao", regiao)
                .getResultList();
        return rows.stream()
                .map(r -> new MunicipioRankingDTO(
                        (String) r[0], (String) r[1], (String) r[2],
                        (String) r[3], (String) r[4], toDouble(r[5])
                ))
                .toList();
    }

    // =========================================================================
    // Fatores Socioeconômicos
    // =========================================================================

    @SuppressWarnings("unchecked")
    public List<ScatterMunicipioDTO> findScatterData(LocalDate anoMes, String regiao) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT municipio_ibge, municipio, regiao, pib_per_capita, idhm, taxa_urbanizacao, penetracao_pf
                FROM vw_indicadores_municipio
                WHERE ano_mes = :anoMes
                  AND (:regiao IS NULL OR regiao = :regiao)
                  AND pib_per_capita IS NOT NULL
                  AND idhm IS NOT NULL
                  AND taxa_urbanizacao IS NOT NULL
                  AND penetracao_pf IS NOT NULL
                """)
                .setParameter("anoMes", anoMes)
                .setParameter("regiao", regiao)
                .getResultList();
        return rows.stream()
                .map(r -> new ScatterMunicipioDTO(
                        (String) r[0], (String) r[1], (String) r[2],
                        toDouble(r[3]), toDouble(r[4]), toDouble(r[5]), toDouble(r[6])
                ))
                .toList();
    }

    // =========================================================================
    // Evolução Temporal
    // =========================================================================

    @SuppressWarnings("unchecked")
    public List<Object[]> findSerieTemporalRegional(String regiao, LocalDate dataInicio, LocalDate dataFim) {
        Query query = em.createNativeQuery("""
                SELECT TO_CHAR(ano_mes, 'YYYY-MM') AS ano_mes_str, regiao, penetracao_media, ticket_medio
                FROM vw_evolucao_regional
                WHERE (:dataInicio IS NULL OR ano_mes >= :dataInicio)
                  AND (:dataFim IS NULL OR ano_mes <= :dataFim)
                  AND (:regiao IS NULL OR regiao = :regiao)
                ORDER BY ano_mes, regiao
                """);
        query.setParameter("dataInicio", dataInicio);
        query.setParameter("dataFim", dataFim);
        query.setParameter("regiao", regiao);
        return query.getResultList();
    }

    // =========================================================================
    // Helpers de conversão
    // =========================================================================

    private Double toDouble(Object o) {
        if (o == null) return null;
        return ((Number) o).doubleValue();
    }

    private long toLong(Object o) {
        if (o == null) return 0L;
        return ((Number) o).longValue();
    }
}
