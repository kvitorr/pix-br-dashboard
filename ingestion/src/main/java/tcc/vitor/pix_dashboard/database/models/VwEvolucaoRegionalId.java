package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

@Embeddable
public class VwEvolucaoRegionalId implements Serializable {

    @Column(name = "ano_mes")
    private LocalDate anoMes;

    @Column(name = "regiao")
    private String regiao;

    public VwEvolucaoRegionalId() {}

    public VwEvolucaoRegionalId(LocalDate anoMes, String regiao) {
        this.anoMes = anoMes;
        this.regiao = regiao;
    }

    public LocalDate getAnoMes() { return anoMes; }
    public String getRegiao() { return regiao; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VwEvolucaoRegionalId that)) return false;
        return Objects.equals(anoMes, that.anoMes) && Objects.equals(regiao, that.regiao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(anoMes, regiao);
    }
}
