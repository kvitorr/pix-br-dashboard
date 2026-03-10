package tcc.vitor.pix_dashboard.database.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tcc.vitor.pix_dashboard.database.models.VwEvolucaoRegional;
import tcc.vitor.pix_dashboard.database.models.VwEvolucaoRegionalId;
import tcc.vitor.pix_dashboard.database.repositories.projections.SerieTemporalRegionalProjection;

import java.time.LocalDate;
import java.util.List;

public interface VwEvolucaoRegionalRepository
        extends JpaRepository<VwEvolucaoRegional, VwEvolucaoRegionalId> {

    @Query("""
            SELECT v.id.anoMes       AS anoMes,
                   v.id.regiao       AS regiao,
                   v.penetracaoMedia AS penetracaoMedia,
                   v.ticketMedio     AS ticketMedio,
                   v.valorPerCapita  AS vlPerCapitaMedia,
                   v.razaoPjPfMedia  AS razaoPjPfMedia
            FROM VwEvolucaoRegional v
            WHERE v.id.anoMes >= :dataInicio
              AND v.id.anoMes <= :dataFim
              AND (:regiao IS NULL OR v.id.regiao = :regiao)
            ORDER BY v.id.anoMes, v.id.regiao
            """)
    List<SerieTemporalRegionalProjection> findSerieTemporalRegional(
            @Param("regiao") String regiao,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim);
}
