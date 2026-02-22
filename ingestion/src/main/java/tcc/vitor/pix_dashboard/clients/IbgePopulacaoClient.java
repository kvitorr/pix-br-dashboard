package tcc.vitor.pix_dashboard.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.exceptions.IbgeRetryableException;
import tcc.vitor.pix_dashboard.services.dto.IbgeAgregadosResponse;
import tcc.vitor.pix_dashboard.services.dto.IbgePopulacaoDTO;

import java.net.URI;
import java.util.List;

@Component
public class IbgePopulacaoClient {

    private static final Logger log = LoggerFactory.getLogger(IbgePopulacaoClient.class);

    private static final String BASE_URL = "https://servicodados.ibge.gov.br/api/v3/agregados";
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MS = 1_000;

    private final RestClient restClient;

    public IbgePopulacaoClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .build();
    }

    public List<IbgePopulacaoDTO> fetchAll(String ano) {
        log.atInfo()
                .addKeyValue("ano", ano)
                .log("Buscando dados de populacao do IBGE");

        List<IbgeAgregadosResponse> response = fetchWithRetry(ano);

        if (response == null || response.isEmpty()) {
            return List.of();
        }

        List<IbgePopulacaoDTO> records = response.getFirst().resultados().stream()
                .flatMap(resultado -> resultado.series().stream())
                .filter(serie -> serie.serie().get(ano) != null
                        && !serie.serie().get(ano).isBlank()
                        && !serie.serie().get(ano).equals("-"))
                .map(serie -> new IbgePopulacaoDTO(
                        serie.localidade().id(),
                        Integer.parseInt(serie.serie().get(ano))
                ))
                .toList();

        log.atInfo()
                .addKeyValue("ano", ano)
                .addKeyValue("totalRecords", records.size())
                .log("Dados de populacao recebidos com sucesso");

        return records;
    }

    private List<IbgeAgregadosResponse> fetchWithRetry(String ano) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            attempt++;
            try {
                return fetchPage(ano);
            } catch (IbgeRetryableException e) {
                if (attempt >= MAX_RETRIES) {
                    log.atError()
                            .addKeyValue("ano", ano)
                            .addKeyValue("attempts", attempt)
                            .addKeyValue("statusCode", e.getStatusCode())
                            .log("Numero maximo de tentativas atingido");
                    throw new IbgeApiException("Numero maximo de tentativas atingido para a API do IBGE", e);
                }

                log.atWarn()
                        .addKeyValue("ano", ano)
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

    private List<IbgeAgregadosResponse> fetchPage(String ano) {
        String url = BASE_URL + "/6579/periodos/" + ano + "/variaveis/9324?localidades=N6[all]";

        return restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503, (req, res) -> {
                    throw new IbgeRetryableException(res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IbgeApiException("Erro na API do IBGE: HTTP " + res.getStatusCode().value());
                })
                .body(new ParameterizedTypeReference<>() {});
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
