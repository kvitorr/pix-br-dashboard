package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.exceptions.IbgeRetryableException;
import tcc.vitor.pix_dashboard.services.dto.IbgePibDTO;
import tcc.vitor.pix_dashboard.services.dto.SidraRow;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

@Component
public class IbgePibClient {

    private static final Logger log = LoggerFactory.getLogger(IbgePibClient.class);

    private static final String BASE_URL = "https://apisidra.ibge.gov.br";
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MS = 1_000;

    private final RestClient restClient;

    public IbgePibClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .build();
    }

    public List<IbgePibDTO> fetchAll() {
        log.atInfo().log("Buscando dados de PIB do IBGE/SIDRA");

        SidraRow[] response = fetchWithRetry();

        if (response == null || response.length <= 1) {
            return List.of();
        }

        List<IbgePibDTO> records = Arrays.stream(response)
                .skip(1) // primeira linha e header
                .filter(row -> row.v() != null
                        && !row.v().isBlank()
                        && !row.v().equals("-")
                        && !row.v().equals("..."))
                .map(row -> new IbgePibDTO(
                        row.d1c(),
                        new BigDecimal(row.v())
                ))
                .toList();

        log.atInfo()
                .addKeyValue("totalRecords", records.size())
                .log("Dados de PIB recebidos com sucesso");

        return records;
    }

    private SidraRow[] fetchWithRetry() {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            attempt++;
            try {
                return fetchPage();
            } catch (IbgeRetryableException e) {
                if (attempt >= MAX_RETRIES) {
                    log.atError()
                            .addKeyValue("attempts", attempt)
                            .addKeyValue("statusCode", e.getStatusCode())
                            .log("Numero maximo de tentativas atingido");
                    throw new IbgeApiException("Numero maximo de tentativas atingido para a API SIDRA", e);
                }

                log.atWarn()
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("maxRetries", MAX_RETRIES)
                        .addKeyValue("statusCode", e.getStatusCode())
                        .addKeyValue("backoffMs", backoffMs)
                        .log("Erro retentavel, aguardando antes de tentar novamente");

                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
    }

    private SidraRow[] fetchPage() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/values/t/5938/n6/all/v/37/p/last")
                        .build())
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503, (req, res) -> {
                    throw new IbgeRetryableException(res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IbgeApiException("Erro na API SIDRA: HTTP " + res.getStatusCode().value());
                })
                .body(SidraRow[].class);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IbgeApiException("Interrompido durante backoff", e);
        }
    }
}
