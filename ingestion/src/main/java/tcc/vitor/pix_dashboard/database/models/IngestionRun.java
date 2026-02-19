package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ingestion_run")
public class IngestionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "source")
    private IngestionRunSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IngestionRunStatus status;

    @Column(name = "params", columnDefinition = "TEXT")
    private String params;

    @Column(name = "records_fetched")
    private Integer recordsFetched;

    @Column(name = "records_upserted")
    private Integer recordsUpserted;

    @Column(name = "records_failed")
    private Integer recordsFailed;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public IngestionRunSource getSource() {
        return source;
    }

    public void setSource(IngestionRunSource source) {
        this.source = source;
    }

    public IngestionRunStatus getStatus() {
        return status;
    }

    public void setStatus(IngestionRunStatus status) {
        this.status = status;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Integer getRecordsFetched() {
        return recordsFetched;
    }

    public void setRecordsFetched(Integer recordsFetched) {
        this.recordsFetched = recordsFetched;
    }

    public Integer getRecordsUpserted() {
        return recordsUpserted;
    }

    public void setRecordsUpserted(Integer recordsUpserted) {
        this.recordsUpserted = recordsUpserted;
    }

    public Integer getRecordsFailed() {
        return recordsFailed;
    }

    public void setRecordsFailed(Integer recordsFailed) {
        this.recordsFailed = recordsFailed;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
