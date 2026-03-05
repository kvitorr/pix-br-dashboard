package tcc.vitor.pix_dashboard.services.dto;

import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;

import java.util.UUID;

public record IngestionRunResponse(
        UUID ingestionRunId,
        IngestionRunStatus status,
        int recordsFetched,
        int recordsUpserted
) {
    public static IngestionRunResponse from(IngestionRun run) {
        return new IngestionRunResponse(
                run.getId(),
                run.getStatus(),
                run.getRecordsFetched() != null ? run.getRecordsFetched() : 0,
                run.getRecordsUpserted() != null ? run.getRecordsUpserted() : 0
        );
    }
}
