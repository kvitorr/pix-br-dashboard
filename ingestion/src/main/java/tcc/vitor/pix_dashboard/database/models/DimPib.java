package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "dim_pib")
public class DimPib {

    @EmbeddedId
    private MunicipioAnoId id;

    @Column(name = "pib")
    private BigDecimal pib;

    @Column(name = "pib_per_capita")
    private BigDecimal pibPerCapita;

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
