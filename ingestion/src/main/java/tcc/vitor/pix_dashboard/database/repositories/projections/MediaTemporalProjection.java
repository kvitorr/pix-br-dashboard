package tcc.vitor.pix_dashboard.database.repositories.projections;

import java.time.LocalDate;

public interface MediaTemporalProjection {
    LocalDate getAnoMes();
    Double getPenetracaoMedia();
    Double getTicketMedioMedia();
    Double getVlPerCapitaMedia();
    Double getRazaoPjPfMedia();
}
