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

    @Column(name = "populacao_urbana")
    private Integer populacaoUrbana;

    @Column(name = "populacao_rural")
    private Integer populacaoRural;

    @Column(name = "idhm")
    private BigDecimal idhm;

    @Column(name = "idhm_longevidade")
    private BigDecimal idhmLongevidade;

    @Column(name = "idhm_educacao")
    private BigDecimal idhmEducacao;

    @Column(name = "idhm_renda")
    private BigDecimal idhmRenda;

    @Column(name = "taxa_urbanizacao")
    private BigDecimal taxaUrbanizacao;

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

    public Integer getPopulacaoUrbana() {
        return populacaoUrbana;
    }

    public void setPopulacaoUrbana(Integer populacaoUrbana) {
        this.populacaoUrbana = populacaoUrbana;
    }

    public Integer getPopulacaoRural() {
        return populacaoRural;
    }

    public void setPopulacaoRural(Integer populacaoRural) {
        this.populacaoRural = populacaoRural;
    }

    public BigDecimal getIdhm() {
        return idhm;
    }

    public void setIdhm(BigDecimal idhm) {
        this.idhm = idhm;
    }

    public BigDecimal getIdhmLongevidade() {
        return idhmLongevidade;
    }

    public void setIdhmLongevidade(BigDecimal idhmLongevidade) {
        this.idhmLongevidade = idhmLongevidade;
    }

    public BigDecimal getIdhmEducacao() {
        return idhmEducacao;
    }

    public void setIdhmEducacao(BigDecimal idhmEducacao) {
        this.idhmEducacao = idhmEducacao;
    }

    public BigDecimal getIdhmRenda() {
        return idhmRenda;
    }

    public void setIdhmRenda(BigDecimal idhmRenda) {
        this.idhmRenda = idhmRenda;
    }

    public BigDecimal getTaxaUrbanizacao() {
        return taxaUrbanizacao;
    }

    public void setTaxaUrbanizacao(BigDecimal taxaUrbanizacao) {
        this.taxaUrbanizacao = taxaUrbanizacao;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
