package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dim_idhm")
public class DimIdhm {

    @EmbeddedId
    private MunicipioAnoId id;

    @Column(name = "idhm")
    private BigDecimal idhm;

    @Column(name = "idhm_longevidade")
    private BigDecimal idhmLongevidade;

    @Column(name = "idhm_educacao")
    private BigDecimal idhmEducacao;

    @Column(name = "idhm_renda")
    private BigDecimal idhmRenda;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
