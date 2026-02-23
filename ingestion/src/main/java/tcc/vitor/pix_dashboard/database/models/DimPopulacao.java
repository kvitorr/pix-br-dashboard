package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dim_populacao")
public class DimPopulacao {

    @EmbeddedId
    private MunicipioAnoId id;

    @Column(name = "populacao")
    private Integer populacao;

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

    public Integer getPopulacao() {
        return populacao;
    }

    public void setPopulacao(Integer populacao) {
        this.populacao = populacao;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
