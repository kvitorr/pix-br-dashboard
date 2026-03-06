package tcc.vitor.pix_dashboard.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.database.repositories.IngestionRunRepository;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.enums.IngestionRunStatus;

import java.time.LocalDateTime;

@Service
public class IngestionRunManager {

    private final IngestionRunRepository ingestionRunRepository;

    public IngestionRunManager(IngestionRunRepository ingestionRunRepository) {
        this.ingestionRunRepository = ingestionRunRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IngestionRun createRunningRecord(IngestionRunSource source, String params) {
        IngestionRun run = new IngestionRun();
        run.setSource(source);
        run.setStatus(IngestionRunStatus.RUNNING);
        run.setParams(params);
        run.setStartedAt(LocalDateTime.now());
        return ingestionRunRepository.save(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsSuccess(IngestionRun run, int recordsFetched, int recordsUpserted) {
        run.setStatus(IngestionRunStatus.SUCCESS);
        run.setRecordsFetched(recordsFetched);
        run.setRecordsUpserted(recordsUpserted);
        run.setRecordsFailed(0);
        run.setEndedAt(LocalDateTime.now());
        ingestionRunRepository.save(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsFailed(IngestionRun run, String errorCode, String errorMessage) {
        run.setStatus(IngestionRunStatus.FAILED);
        run.setEndedAt(LocalDateTime.now());
        run.setErrorCode(errorCode);
        run.setErrorMessage(errorMessage);
        ingestionRunRepository.save(run);
    }
}
