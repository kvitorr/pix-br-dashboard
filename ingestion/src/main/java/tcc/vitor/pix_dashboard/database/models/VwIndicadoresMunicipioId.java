package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class VwIndicadoresMunicipioId implements Serializable {

    @Column(name = "municipio_ibge")
    private String municipioIbge;

    @Column(name = "ano_mes")
    private LocalDate anoMes;

    public VwIndicadoresMunicipioId() {}

    public VwIndicadoresMunicipioId(String municipioIbge, LocalDate anoMes) {
        this.municipioIbge = municipioIbge;
        this.anoMes = anoMes;
    }

    public String getMunicipioIbge() { return municipioIbge; }
    public LocalDate getAnoMes() { return anoMes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VwIndicadoresMunicipioId that)) return false;
        return Objects.equals(municipioIbge, that.municipioIbge) && Objects.equals(anoMes, that.anoMes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(municipioIbge, anoMes);
    }
}
