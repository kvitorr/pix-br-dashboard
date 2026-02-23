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
import tcc.vitor.pix_dashboard.services.dto.IbgeUrbanizacaoDTO;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class IbgeUrbanizacaoClient {

    private static final Logger log = LoggerFactory.getLogger(IbgeUrbanizacaoClient.class);

    private static final String BASE_URL = "https://servicodados.ibge.gov.br/api/v3/agregados";
    private static final String ANO_CENSO = "2022";
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MS = 1_000;

    private final RestClient restClient;

    public IbgeUrbanizacaoClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .build();
    }

    /**
     * Busca a população urbana e rural de todos os municípios do Censo 2022 (tabela 9923)
     * e retorna a lista consolidada com o cálculo de taxa de urbanização pronto para persistência.
     */
    public List<IbgeUrbanizacaoDTO> fetchAll() {
        log.atInfo().log("Buscando dados de populacao urbana e rural do IBGE (Censo 2022, tabela 9923)");

        List<IbgeAgregadosResponse> responseUrbana = fetchWithRetry("1[1]");
        List<IbgeAgregadosResponse> responseRural = fetchWithRetry("1[2]");

        Map<String, Long> populacaoUrbana = extrairPopulacaoPorMunicipio(responseUrbana);
        Map<String, Long> populacaoRural = extrairPopulacaoPorMunicipio(responseRural);

        List<IbgeUrbanizacaoDTO> resultado = new ArrayList<>();
        for (Map.Entry<String, Long> entry : populacaoUrbana.entrySet()) {
            String municipioIbge = entry.getKey();
            Long urbana = entry.getValue();
            Long rural = populacaoRural.getOrDefault(municipioIbge, 0L);
            resultado.add(new IbgeUrbanizacaoDTO(municipioIbge, urbana, rural));
        }

        // Municípios que só aparecem como rurais (edge case improvável, mas defensivo)
        for (Map.Entry<String, Long> entry : populacaoRural.entrySet()) {
            if (!populacaoUrbana.containsKey(entry.getKey())) {
                resultado.add(new IbgeUrbanizacaoDTO(entry.getKey(), 0L, entry.getValue()));
            }
        }

        log.atInfo()
                .addKeyValue("totalMunicipios", resultado.size())
                .log("Dados de urbanizacao consolidados com sucesso");

        return resultado;
    }

    private Map<String, Long> extrairPopulacaoPorMunicipio(List<IbgeAgregadosResponse> response) {
        Map<String, Long> mapa = new HashMap<>();

        if (response == null || response.isEmpty()) {
            return mapa;
        }

        response.getFirst().resultados().stream()
                .flatMap(resultado -> resultado.series().stream())
                .filter(serie -> {
                    String valor = serie.serie().get(ANO_CENSO);
                    return valor != null && !valor.isBlank() && !valor.equals("-");
                })
                .forEach(serie -> {
                    String municipioIbge = serie.localidade().id();
                    Long populacao = Long.parseLong(serie.serie().get(ANO_CENSO));
                    mapa.put(municipioIbge, populacao);
                });

        return mapa;
    }

    private List<IbgeAgregadosResponse> fetchWithRetry(String classificacao) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            attempt++;
            try {
                return fetchPage(classificacao);
            } catch (IbgeRetryableException e) {
                if (attempt >= MAX_RETRIES) {
                    log.atError()
                            .addKeyValue("classificacao", classificacao)
                            .addKeyValue("attempts", attempt)
                            .addKeyValue("statusCode", e.getStatusCode())
                            .log("Numero maximo de tentativas atingido para urbanizacao");
                    throw new IbgeApiException("Numero maximo de tentativas atingido para a API do IBGE (urbanizacao)", e);
                }

                log.atWarn()
                        .addKeyValue("classificacao", classificacao)
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("maxRetries", MAX_RETRIES)
                        .addKeyValue("statusCode", e.getStatusCode())
                        .addKeyValue("backoffMs", backoffMs)
                        .log("Erro retentavel ao buscar urbanizacao, aguardando antes de tentar novamente");

                sleep(backoffMs);
                backoffMs *= 2;
            }
        }
    }

    private List<IbgeAgregadosResponse> fetchPage(String classificacao) {
        // Tabela 9923 — "Populacao residente, por situacao do domicilio" (Censo 2022)
        // classificacao=1[1] = Urbana | classificacao=1[2] = Rural
        String url = BASE_URL + "/9923/periodos/" + ANO_CENSO + "/variaveis?classificacao=" + classificacao + "&localidades=N6[all]";

        return restClient.get()
                .uri(URI.create(url))
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503, (req, res) -> {
                    throw new IbgeRetryableException(res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IbgeApiException("Erro na API do IBGE (urbanizacao): HTTP " + res.getStatusCode().value());
                })
                .body(new ParameterizedTypeReference<>() {});
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IbgeApiException("Interrompido durante backoff de urbanizacao", e);
        }
    }
}
