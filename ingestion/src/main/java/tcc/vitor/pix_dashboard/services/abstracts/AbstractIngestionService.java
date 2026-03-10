package tcc.vitor.pix_dashboard.services.abstracts;

import org.slf4j.Logger;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.IngestionException;
import tcc.vitor.pix_dashboard.services.IngestionRunManager;

import java.util.List;

public abstract class AbstractIngestionService<T> {

    protected final IngestionRunManager runManager;

    protected AbstractIngestionService(IngestionRunManager runManager) {
        this.runManager = runManager;
    }

    protected abstract Logger log();

    protected abstract IngestionRunSource source();

    protected abstract String sourceName();

    protected abstract String buildParams(String param);

    protected abstract List<T> fetch(String param);

    protected abstract int persist(List<T> records, IngestionRun run, String param);

    protected abstract boolean isKnownException(RuntimeException e);

    protected abstract String knownErrorCode();

    /** Hook chamado após ingestão bem-sucedida. Subclasses podem sobrescrever para pós-processamento. */
    protected void onSuccess(List<T> records, IngestionRun run, String param) {
        // no-op por padrão
    }

    public IngestionRun ingest(String param) {
        log().atInfo()
                .addKeyValue("param", param)
                .log("Iniciando ingestao de {}", sourceName());

        IngestionRun run = runManager.createRunningRecord(source(), buildParams(param));
        long startTime = System.currentTimeMillis();

        try {
            List<T> records = fetch(param);
            int upserted = persist(records, run, param);
            long durationMs = System.currentTimeMillis() - startTime;
            runManager.markAsSuccess(run, records.size(), upserted);

            log().atInfo()
                    .addKeyValue("param", param)
                    .addKeyValue("totalRecords", records.size())
                    .addKeyValue("upserted", upserted)
                    .addKeyValue("durationMs", durationMs)
                    .log("Ingestao de {} concluida com sucesso", sourceName());

            try {
                onSuccess(records, run, param);
            } catch (Exception e) {
                log().atWarn()
                        .addKeyValue("error", e.getMessage())
                        .log("Pos-processamento falhou apos ingestao de {}", sourceName());
            }

            return run;

        } catch (RuntimeException e) {
            long durationMs = System.currentTimeMillis() - startTime;

            if (isKnownException(e)) {
                runManager.markAsFailed(run, knownErrorCode(), e.getMessage());

                log().atError()
                        .addKeyValue("param", param)
                        .addKeyValue("error", e.getMessage())
                        .addKeyValue("durationMs", durationMs)
                        .log("Falha na ingestao de {}", sourceName());

                throw e;
            }

            runManager.markAsFailed(run, "UNEXPECTED_ERROR", e.getMessage());

            log().atError()
                    .addKeyValue("param", param)
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Erro inesperado na ingestao de {}", sourceName());

            throw new IngestionException("Erro inesperado na ingestao de " + sourceName(), e);
        }
    }
}
