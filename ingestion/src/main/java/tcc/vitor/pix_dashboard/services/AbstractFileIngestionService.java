package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.springframework.web.multipart.MultipartFile;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.IngestionException;

import java.io.IOException;
import java.util.List;

/**
 * Template Method para serviços de ingestão baseados em upload de arquivo.
 * Encapsula o ciclo de vida completo: criação do run, parse, persistência e
 * atualização de status (sucesso/falha), análogo a {@link AbstractIngestionService}
 * para ingestões via API.
 *
 * @param <T> tipo do DTO produzido pelo parser
 */
public abstract class AbstractFileIngestionService<T> {

    protected final IngestionRunManager runManager;

    protected AbstractFileIngestionService(IngestionRunManager runManager) {
        this.runManager = runManager;
    }

    protected abstract Logger log();

    protected abstract IngestionRunSource source();

    protected abstract String sourceName();

    protected abstract String buildParams(String param);

    protected abstract List<T> parse(MultipartFile file) throws IOException;

    protected abstract int persist(List<T> records, String param);

    protected abstract boolean isKnownException(RuntimeException e);

    protected abstract String knownErrorCode();

    public IngestionRun ingest(MultipartFile file, String param) {
        log().atInfo()
                .addKeyValue("nomeArquivo", file.getOriginalFilename())
                .addKeyValue("tamanhoBytes", file.getSize())
                .addKeyValue("param", param)
                .log("Iniciando ingestao de {}", sourceName());

        IngestionRun run = runManager.createRunningRecord(source(), buildParams(param));
        long startTime = System.currentTimeMillis();

        try {
            List<T> records = parse(file);
            int upserted = persist(records, param);
            long durationMs = System.currentTimeMillis() - startTime;
            runManager.markAsSuccess(run, records.size(), upserted);

            log().atInfo()
                    .addKeyValue("param", param)
                    .addKeyValue("totalRecords", records.size())
                    .addKeyValue("upserted", upserted)
                    .addKeyValue("durationMs", durationMs)
                    .log("Ingestao de {} concluida com sucesso", sourceName());

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

        } catch (IOException e) {
            long durationMs = System.currentTimeMillis() - startTime;
            runManager.markAsFailed(run, "IO_ERROR", e.getMessage());

            log().atError()
                    .addKeyValue("param", param)
                    .addKeyValue("error", e.getMessage())
                    .addKeyValue("durationMs", durationMs)
                    .log("Erro de leitura do arquivo na ingestao de {}", sourceName());

            throw new IngestionException("Erro ao ler arquivo na ingestao de " + sourceName(), e);
        }
    }
}
