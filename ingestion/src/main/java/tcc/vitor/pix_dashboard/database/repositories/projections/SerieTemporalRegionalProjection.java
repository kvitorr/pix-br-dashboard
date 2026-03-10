package tcc.vitor.pix_dashboard.database.repositories.projections;

import java.time.LocalDate;

public interface SerieTemporalRegionalProjection {
    LocalDate getAnoMes();
    String getRegiao();
    Double getPenetracaoMedia();
    Double getTicketMedio();
    Double getVlPerCapitaMedia();
    Double getRazaoPjPfMedia();
}
