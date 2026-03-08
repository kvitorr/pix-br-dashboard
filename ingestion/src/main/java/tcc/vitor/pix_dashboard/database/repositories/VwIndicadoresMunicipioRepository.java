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
            SELECT AVG(v.penetracaoPf)   AS penetracaoMedia,
                   AVG(v.ticketMedioPf)  AS ticketMedio,
                   AVG(v.razaoPjPf)      AS razaoPjPf,
                   AVG(v.vlPerCapitaPf)  AS vlPerCapita
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
            """)
    KpisVisaoGeralProjection findKpisVisaoGeral(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v.id.municipioIbge AS municipioIbge,
                   v.municipio AS municipioNome,
                   v.penetracaoPf AS penetracaoPf
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
            """)
    List<MapaMunicipioProjection> findMapaMunicipios(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v.regiao       AS regiao,
                   v.siglaRegiao  AS siglaRegiao,
                   AVG(v.penetracaoPf) AS penetracaoMedia
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
            GROUP BY v.regiao, v.siglaRegiao
            ORDER BY v.regiao
            """)
    List<PenetracaoRegiaoProjection> findPenetracaoPorRegiao(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT SUM(CASE WHEN v.penetracaoPf > 50 THEN 1 ELSE 0 END) AS acima50,
                   SUM(CASE WHEN v.penetracaoPf <= 50 THEN 1 ELSE 0 END) AS abaixo50
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
            """)
    CoberturaNacionalProjection findCoberturaNacional(
            @Param("anoMes") LocalDate anoMes,
            @Param("regiao") String regiao);

    @Query("""
            SELECT v.regiao        AS regiao,
                   v.penetracaoPf  AS penetracaoPf
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
              AND v.penetracaoPf IS NOT NULL
            ORDER BY v.regiao, v.penetracaoPf
            """)
    List<PenetracaoBrutaProjection> findPenetracaoBruta(
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
                   v.regiao           AS regiao,
                   v.pibPerCapita     AS pibPerCapita,
                   v.idhm             AS idhm,
                   v.taxaUrbanizacao  AS taxaUrbanizacao,
                   v.penetracaoPf     AS penetracaoPf
            FROM VwIndicadoresMunicipio v
            WHERE v.id.anoMes = :anoMes
              AND (:regiao IS NULL OR v.regiao = :regiao)
              AND v.pibPerCapita IS NOT NULL
              AND v.idhm IS NOT NULL
              AND v.taxaUrbanizacao IS NOT NULL
              AND v.penetracaoPf IS NOT NULL
            """)
    List<ScatterMunicipioProjection> findScatterData(
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
            GROUP BY v.id.municipioIbge, v.municipio, v.estado, v.regiao, v.siglaRegiao
            ORDER BY v.municipio
            """)
    List<MunicipioRankingProjection> findAllMunicipios();

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
}
