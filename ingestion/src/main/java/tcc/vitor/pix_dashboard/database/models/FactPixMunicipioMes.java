package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fact_pix_municipio_mes")
public class FactPixMunicipioMes {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "ano_mes", nullable = false)
    private LocalDate anoMes;

    @Column(name = "municipio_ibge", nullable = false)
    private String municipioIbge;

    @Column(name = "estado_ibge")
    private String estadoIbge;

    @Column(name = "estado")
    private String estado;

    @Column(name = "regiao")
    private String regiao;

    @Column(name = "sigla_regiao")
    private String siglaRegiao;

    @Column(name = "municipio")
    private String municipio;

    @Column(name = "vl_pagador_pf")
    private BigDecimal vlPagadorPf;

    @Column(name = "qt_pagador_pf")
    private Long qtPagadorPf;

    @Column(name = "qt_pes_pagador_pf")
    private Long qtPesPagadorPf;

    @Column(name = "vl_recebedor_pf")
    private BigDecimal vlRecebedorPf;

    @Column(name = "qt_recebedor_pf")
    private Long qtRecebedorPf;

    @Column(name = "qt_pes_recebedor_pf")
    private Long qtPesRecebedorPf;

    @Column(name = "vl_pagador_pj")
    private BigDecimal vlPagadorPj;

    @Column(name = "qt_pagador_pj")
    private Long qtPagadorPj;

    @Column(name = "qt_pes_pagador_pj")
    private Long qtPesPagadorPj;

    @Column(name = "vl_recebedor_pj")
    private BigDecimal vlRecebedorPj;

    @Column(name = "qt_recebedor_pj")
    private Long qtRecebedorPj;

    @Column(name = "qt_pes_recebedor_pj")
    private Long qtPesRecebedorPj;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingestion_run_id", nullable = false)
    private IngestionRun ingestionRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "municipio_ibge", referencedColumnName = "municipio_ibge", insertable = false, updatable = false)
    private DimMunicipio dimMunicipio;

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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getAnoMes() {
        return anoMes;
    }

    public void setAnoMes(LocalDate anoMes) {
        this.anoMes = anoMes;
    }

    public String getMunicipioIbge() {
        return municipioIbge;
    }

    public void setMunicipioIbge(String municipioIbge) {
        this.municipioIbge = municipioIbge;
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

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        this.regiao = regiao;
    }

    public String getSiglaRegiao() {
        return siglaRegiao;
    }

    public void setSiglaRegiao(String siglaRegiao) {
        this.siglaRegiao = siglaRegiao;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public BigDecimal getVlPagadorPf() {
        return vlPagadorPf;
    }

    public void setVlPagadorPf(BigDecimal vlPagadorPf) {
        this.vlPagadorPf = vlPagadorPf;
    }

    public Long getQtPagadorPf() {
        return qtPagadorPf;
    }

    public void setQtPagadorPf(Long qtPagadorPf) {
        this.qtPagadorPf = qtPagadorPf;
    }

    public Long getQtPesPagadorPf() {
        return qtPesPagadorPf;
    }

    public void setQtPesPagadorPf(Long qtPesPagadorPf) {
        this.qtPesPagadorPf = qtPesPagadorPf;
    }

    public BigDecimal getVlRecebedorPf() {
        return vlRecebedorPf;
    }

    public void setVlRecebedorPf(BigDecimal vlRecebedorPf) {
        this.vlRecebedorPf = vlRecebedorPf;
    }

    public Long getQtRecebedorPf() {
        return qtRecebedorPf;
    }

    public void setQtRecebedorPf(Long qtRecebedorPf) {
        this.qtRecebedorPf = qtRecebedorPf;
    }

    public Long getQtPesRecebedorPf() {
        return qtPesRecebedorPf;
    }

    public void setQtPesRecebedorPf(Long qtPesRecebedorPf) {
        this.qtPesRecebedorPf = qtPesRecebedorPf;
    }

    public BigDecimal getVlPagadorPj() {
        return vlPagadorPj;
    }

    public void setVlPagadorPj(BigDecimal vlPagadorPj) {
        this.vlPagadorPj = vlPagadorPj;
    }

    public Long getQtPagadorPj() {
        return qtPagadorPj;
    }

    public void setQtPagadorPj(Long qtPagadorPj) {
        this.qtPagadorPj = qtPagadorPj;
    }

    public Long getQtPesPagadorPj() {
        return qtPesPagadorPj;
    }

    public void setQtPesPagadorPj(Long qtPesPagadorPj) {
        this.qtPesPagadorPj = qtPesPagadorPj;
    }

    public BigDecimal getVlRecebedorPj() {
        return vlRecebedorPj;
    }

    public void setVlRecebedorPj(BigDecimal vlRecebedorPj) {
        this.vlRecebedorPj = vlRecebedorPj;
    }

    public Long getQtRecebedorPj() {
        return qtRecebedorPj;
    }

    public void setQtRecebedorPj(Long qtRecebedorPj) {
        this.qtRecebedorPj = qtRecebedorPj;
    }

    public Long getQtPesRecebedorPj() {
        return qtPesRecebedorPj;
    }

    public void setQtPesRecebedorPj(Long qtPesRecebedorPj) {
        this.qtPesRecebedorPj = qtPesRecebedorPj;
    }

    public IngestionRun getIngestionRun() {
        return ingestionRun;
    }

    public void setIngestionRun(IngestionRun ingestionRun) {
        this.ingestionRun = ingestionRun;
    }

    public DimMunicipio getDimMunicipio() {
        return dimMunicipio;
    }

    public void setDimMunicipio(DimMunicipio dimMunicipio) {
        this.dimMunicipio = dimMunicipio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
