package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dim_urbanizacao")
public class DimUrbanizacao {

    @EmbeddedId
    private MunicipioAnoId id;

    @Column(name = "populacao_urbana")
    private Integer populacaoUrbana;

    @Column(name = "populacao_rural")
    private Integer populacaoRural;

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

    public MunicipioAnoId getId() {
        return id;
    }

    public void setId(MunicipioAnoId id) {
        this.id = id;
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
