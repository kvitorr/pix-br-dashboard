package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class MunicipioAnoId implements Serializable {

    @Column(name = "municipio_ibge", nullable = false)
    private String municipioIbge;

    @Column(name = "ano", nullable = false)
    private Integer ano;

    public MunicipioAnoId() {
    }

    public MunicipioAnoId(String municipioIbge, Integer ano) {
        this.municipioIbge = municipioIbge;
        this.ano = ano;
    }

    public String getMunicipioIbge() {
        return municipioIbge;
    }

    public void setMunicipioIbge(String municipioIbge) {
        this.municipioIbge = municipioIbge;
    }

    public Integer getAno() {
        return ano;
    }

    public void setAno(Integer ano) {
        this.ano = ano;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MunicipioAnoId that = (MunicipioAnoId) o;
        return Objects.equals(municipioIbge, that.municipioIbge) && Objects.equals(ano, that.ano);
    }

    @Override
    public int hashCode() {
        return Objects.hash(municipioIbge, ano);
    }
}
