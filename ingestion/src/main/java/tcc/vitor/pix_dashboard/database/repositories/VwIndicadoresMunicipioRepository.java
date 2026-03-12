package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tcc.vitor.pix_dashboard.database.models.VwIndicadoresMunicipio;
import tcc.vitor.pix_dashboard.database.models.VwIndicadoresMunicipioId;
import tcc.vitor.pix_dashboard.database.repositories.projections.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VwIndicadoresMunicipioRepository
        extends JpaRepository<VwIndicadoresMunicipio, VwIndicadoresMunicipioId> {

    @Query("SELECT MAX(v.id.anoMes) FROM VwIndicadoresMunicipio v")
    LocalDate findMaxAnoMes();

    @Query("""
            SELECT CASE WHEN SUM(v.populacao) = 0 THEN NULL
                        ELSE (SUM(v.qtPesPagadorPf) * 100.0 / SUM(v.populacao)) END AS penetracaoMedia,
                   CASE WHEN SUM(v.qtPagadorPf) = 0 THEN NULL
                        ELSE (SUM(v.vlPagadorPf) / SUM(v.qtPagadorPf)) END          AS ticketMedio,
                   CASE WHEN SUM(v.qtPagadorPf) = 0 THEN NULL
                        ELSE (SUM(v.qtPagadorPj) * 1.0 / SUM(v.qtPagadorPf)) END    AS razaoPjPf,
                   CASE WHEN SUM(v.populacao) = 0 THEN NULL
                        ELSE (SUM(v.vlPagadorPf + v.vlRecebedorPf) / SUM(v.populacao)) END AS vlPerCapita
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
            """)
    KpisVisaoGeralProjection findKpisVisaoGeral(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v.id.municipioIbge AS municipioIbge,
                   v.municipio        AS municipioNome,
                   v.penetracaoPf     AS penetracaoPf,
                   v.ticketMedioPf    AS ticketMedioPf,
                   v.razaoPjPf        AS razaoPjPf,
                   v.vlPerCapitaPf    AS vlPerCapitaPf
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
            """)
    List<MapaMunicipioProjection> findMapaMunicipios(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v.regiao              AS regiao,
                   v.siglaRegiao         AS siglaRegiao,
                   CASE WHEN SUM(v.populacao) = 0 THEN NULL
                        ELSE (SUM(v.qtPesPagadorPf) * 100.0 / SUM(v.populacao)) END AS penetracaoMedia,
                   CASE WHEN SUM(v.qtPagadorPf) = 0 THEN NULL
                        ELSE (SUM(v.vlPagadorPf) / SUM(v.qtPagadorPf)) END          AS ticketMedioMedia,
                   CASE WHEN SUM(v.qtPagadorPf) = 0 THEN NULL
                        ELSE (SUM(v.qtPagadorPj) * 1.0 / SUM(v.qtPagadorPf)) END    AS razaoMedia,
                   CASE WHEN SUM(v.populacao) = 0 THEN NULL
                        ELSE (SUM(v.vlPagadorPf + v.vlRecebedorPf) / SUM(v.populacao)) END AS perCapitaMedia
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
            GROUP BY v.regiao, v.siglaRegiao
            ORDER BY v.regiao
            """)
    List<RegiaoWeightedAverageMetricsProjection> findWeightedAverageMetricsPorRegiao(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v.regiao           AS regiao,
                   v.penetracaoPf     AS penetracaoPf,
                   v.ticketMedioPf    AS ticketMedioPf,
                   v.razaoPjPf        AS razaoPjPf,
                   v.vlPerCapitaPf    AS vlPerCapitaPf
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
            ORDER BY v.regiao
            """)
    List<MetricasBrutaProjection> findMetricasBruta(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
              AND v.pibPerCapita IS NOT NULL
            """)
    List<VwIndicadoresMunicipio> findAllWithPib(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v.id.municipioIbge AS municipioIbge,
                   v.municipio        AS municipio,
                   v.estado           AS estado,
                   v.regiao           AS regiao,
                   v.siglaRegiao      AS siglaRegiao,
                   v.penetracaoPf     AS penetracaoPf
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
              AND v.penetracaoPf IS NOT NULL
            ORDER BY v.penetracaoPf DESC
            """)
    List<MunicipioRankingProjection> findTopMunicipios(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao,
            Pageable pageable);

    @Query("""
            SELECT v.id.municipioIbge AS municipioIbge,
                   v.municipio        AS municipio,
                   v.estado           AS estado,
                   v.regiao           AS regiao,
                   v.siglaRegiao      AS siglaRegiao,
                   v.penetracaoPf     AS penetracaoPf
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
              AND v.penetracaoPf IS NOT NULL
            ORDER BY v.penetracaoPf ASC
            """)
    List<MunicipioRankingProjection> findBottomMunicipios(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao,
            Pageable pageable);

    @Query("""
            SELECT v.id.municipioIbge AS municipioIbge,
                   v.municipio        AS municipio,
                   v.estado           AS estado,
                   v.regiao           AS regiao,
                   v.siglaRegiao      AS siglaRegiao
            FROM VwIndicadoresMunicipio v
            GROUP BY v.id.municipioIbge, v.municipio, v.estado, v.regiao, v.siglaRegiao
            ORDER BY v.municipio
            """)
    List<MunicipioListProjection> findAllMunicipios();

    @Query("""
            SELECT v.id.municipioIbge AS municipioIbge,
                   v.municipio        AS municipio,
                   v.estado           AS estado,
                   v.regiao           AS regiao,
                   v.siglaRegiao      AS siglaRegiao
            FROM VwIndicadoresMunicipio v
            WHERE LOWER(v.municipio) LIKE LOWER(CONCAT('%', :nome, '%'))
            GROUP BY v.id.municipioIbge, v.municipio, v.estado, v.regiao, v.siglaRegiao
            ORDER BY v.municipio
            """)
    List<MunicipioListProjection> searchByName(
            @Param("nome") String nome,
            Pageable pageable);

    @Query("""
            SELECT v FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
            """)
    List<VwIndicadoresMunicipio> findAllForScatter(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
              AND v.penetracaoPf IS NOT NULL
              AND v.pibPerCapita IS NOT NULL
            """)
    List<VwIndicadoresMunicipio> findAllWithPibAndPenetracao(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v.id.municipioIbge AS municipioIbge,
                   v.municipio        AS municipio,
                   v.estado           AS estado,
                   v.regiao           AS regiao,
                   v.siglaRegiao      AS siglaRegiao,
                   v.penetracaoPf     AS penetracaoPf,
                   v.ticketMedioPf    AS ticketMedioPf,
                   v.razaoPjPf        AS razaoPjPf,
                   v.vlPerCapitaPf    AS vlPerCapitaPf,
                   v.pibPerCapita     AS pibPerCapita,
                   v.idhm             AS idhm,
                   v.taxaUrbanizacao  AS taxaUrbanizacao
            FROM VwIndicadoresMunicipio v
            WHERE v.id.municipioIbge = :municipioIbge
              AND v.id.anoMes = :anoMes
            """)
    Optional<MunicipioDetalhesProjection> findMunicipioDetalhes(
            @Param("municipioIbge") String municipioIbge,
            @Param("anoMes") LocalDate anoMes);

    @Query("""
            SELECT v FROM VwIndicadoresMunicipio v
            WHERE v.id.municipioIbge = :municipioIbge
              AND v.id.anoMes >= :dataInicio
              AND v.id.anoMes <= :dataFim
            ORDER BY v.id.anoMes
            """)
    List<VwIndicadoresMunicipio> findSerieMunicipio(
            @Param("municipioIbge") String municipioIbge,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    @Query("""
            SELECT v.id.anoMes          AS anoMes,
                   CASE WHEN SUM(v.populacao) = 0 THEN NULL
                        ELSE (SUM(v.qtPesPagadorPf) * 100.0 / SUM(v.populacao)) END AS penetracaoMedia,
                   CASE WHEN SUM(v.qtPagadorPf) = 0 THEN NULL
                        ELSE (SUM(v.vlPagadorPf) / SUM(v.qtPagadorPf)) END          AS ticketMedioMedia,
                   CASE WHEN SUM(v.populacao) = 0 THEN NULL
                        ELSE (SUM(v.vlPagadorPf + v.vlRecebedorPf) / SUM(v.populacao)) END AS vlPerCapitaMedia,
                   CASE WHEN SUM(v.qtPagadorPf) = 0 THEN NULL
                        ELSE (SUM(v.qtPagadorPj) * 1.0 / SUM(v.qtPagadorPf)) END    AS razaoPjPfMedia
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes >= :dataInicio
              AND v.id.anoMes <= :dataFim
              AND v.regiao = :regiao
            GROUP BY v.id.anoMes
            ORDER BY v.id.anoMes
            """)
    List<MediaTemporalProjection> findMediasRegionais(
            @Param("regiao") String regiao,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);

    @Query("""
            SELECT v.id.anoMes          AS anoMes,
                   CASE WHEN SUM(v.populacao) = 0 THEN NULL
                        ELSE (SUM(v.qtPesPagadorPf) * 100.0 / SUM(v.populacao)) END AS penetracaoMedia,
                   CASE WHEN SUM(v.qtPagadorPf) = 0 THEN NULL
                        ELSE (SUM(v.vlPagadorPf) / SUM(v.qtPagadorPf)) END          AS ticketMedioMedia,
                   CASE WHEN SUM(v.populacao) = 0 THEN NULL
                        ELSE (SUM(v.vlPagadorPf + v.vlRecebedorPf) / SUM(v.populacao)) END AS vlPerCapitaMedia,
                   CASE WHEN SUM(v.qtPagadorPf) = 0 THEN NULL
                        ELSE (SUM(v.qtPagadorPj) * 1.0 / SUM(v.qtPagadorPf)) END    AS razaoPjPfMedia
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes >= :dataInicio
              AND v.id.anoMes <= :dataFim
            GROUP BY v.id.anoMes
            ORDER BY v.id.anoMes
            """)
    List<MediaTemporalProjection> findMediasNacionais(
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);
}
