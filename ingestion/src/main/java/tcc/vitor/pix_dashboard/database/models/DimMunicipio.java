package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dim_municipio")
public class DimMunicipio {

    @Id
    @Column(name = "municipio_ibge", nullable = false)
    private String municipioIbge;

    @Column(name = "municipio")
    private String municipio;

    @Column(name = "estado_ibge")
    private String estadoIbge;

    @Column(name = "estado")
    private String estado;

    @Column(name = "sigla_regiao")
    private String siglaRegiao;

    @Column(name = "regiao")
    private String regiao;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getMunicipioIbge() {
        return municipioIbge;
    }

    public void setMunicipioIbge(String municipioIbge) {
        this.municipioIbge = municipioIbge;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getEstadoIbge() {
        return estadoIbge;
    }

    public void setEstadoIbge(String estadoIbge) {
        this.estadoIbge = estadoIbge;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getSiglaRegiao() {
        return siglaRegiao;
    }

    public void setSiglaRegiao(String siglaRegiao) {
        this.siglaRegiao = siglaRegiao;
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
