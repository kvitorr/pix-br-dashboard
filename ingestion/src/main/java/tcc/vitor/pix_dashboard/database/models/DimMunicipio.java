package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
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

    @Column(name = "populacao")
    private Integer populacao;

    @Column(name = "pib")
    private BigDecimal pib;

    @Column(name = "pib_per_capita")
    private BigDecimal pibPerCapita;

    @Column(name = "idhm")
    private BigDecimal idhm;

    @Column(name = "taxa_urbanizacao")
    private BigDecimal taxaUrbanizacao;

    @Column(name = "cobertura_internet")
    private BigDecimal coberturaInternet;

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

    public Integer getPopulacao() {
        return populacao;
    }

    public void setPopulacao(Integer populacao) {
        this.populacao = populacao;
    }

    public BigDecimal getPib() {
        return pib;
    }

    public void setPib(BigDecimal pib) {
        this.pib = pib;
    }

    public BigDecimal getPibPerCapita() {
        return pibPerCapita;
    }

    public void setPibPerCapita(BigDecimal pibPerCapita) {
        this.pibPerCapita = pibPerCapita;
    }

    public BigDecimal getIdhm() {
        return idhm;
    }

    public void setIdhm(BigDecimal idhm) {
        this.idhm = idhm;
    }

    public BigDecimal getTaxaUrbanizacao() {
        return taxaUrbanizacao;
    }

    public void setTaxaUrbanizacao(BigDecimal taxaUrbanizacao) {
        this.taxaUrbanizacao = taxaUrbanizacao;
    }

    public BigDecimal getCoberturaInternet() {
        return coberturaInternet;
    }

    public void setCoberturaInternet(BigDecimal coberturaInternet) {
        this.coberturaInternet = coberturaInternet;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
