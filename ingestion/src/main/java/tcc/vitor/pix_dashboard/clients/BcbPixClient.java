package tcc.vitor.pix_dashboard.clients;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tcc.vitor.pix_dashboard.exceptions.BcbApiException;
import tcc.vitor.pix_dashboard.exceptions.BcbRetryableException;
import tcc.vitor.pix_dashboard.services.dto.BcbOdataResponse;
import tcc.vitor.pix_dashboard.services.dto.PixTransacaoMunicipioDTO;

import java.util.List;

@Component
public class BcbPixClient {

    private static final Logger log = LoggerFactory.getLogger(BcbPixClient.class);

    private static final String BASE_URL = "https://olinda.bcb.gov.br/olinda/servico/Pix_DadosAbertos/versao/v1/odata";
    private static final int PAGE_SIZE = 10_000;
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_BACKOFF_MS = 1_000;
    private static final String SELECT_FIELDS = String.join(",",
            "AnoMes", "Municipio_Ibge", "Municipio", "Estado_Ibge", "Estado",
            "Sigla_Regiao", "Regiao",
            "VL_PagadorPF", "QT_PagadorPF", "VL_PagadorPJ", "QT_PagadorPJ",
            "VL_RecebedorPF", "QT_RecebedorPF", "VL_RecebedorPJ", "QT_RecebedorPJ",
            "QT_PES_PagadorPF", "QT_PES_PagadorPJ", "QT_PES_RecebedorPF", "QT_PES_RecebedorPJ"
    );

    private final RestClient restClient;

    public BcbPixClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .build();
    }

    public List<PixTransacaoMunicipioDTO> fetchAll(String database) {
        log.atInfo()
                .addKeyValue("database", database)
                .addKeyValue("top", PAGE_SIZE)
                .log("Buscando dados da API do BCB");

        List<PixTransacaoMunicipioDTO> records = fetchWithRetry(database);

        log.atInfo()
                .addKeyValue("database", database)
                .addKeyValue("totalRecords", records.size())
                .log("Dados recebidos com sucesso");

        return records;
    }

    private List<PixTransacaoMunicipioDTO> fetchWithRetry(String database) {
        int attempt = 0;
        long backoffMs = INITIAL_BACKOFF_MS;

        while (true) {
            attempt++;
            try {
                return fetchPage(database);
            } catch (BcbRetryableException e) {
                if (attempt >= MAX_RETRIES) {
                    log.atError()
                            .addKeyValue("database", database)
                            .addKeyValue("attempts", attempt)
                            .addKeyValue("statusCode", e.getStatusCode())
                            .log("Numero maximo de tentativas atingido");
                    throw new BcbApiException("Numero maximo de tentativas atingido para a API do BCB", e);
                }

                log.atWarn()
                        .addKeyValue("database", database)
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

    private List<PixTransacaoMunicipioDTO> fetchPage(String database) {
        BcbOdataResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/TransacoesPixPorMunicipio(DataBase=@DataBase)")
                        .queryParam("@DataBase", "'" + database + "'")
                        .queryParam("$top", PAGE_SIZE)
                        .queryParam("$format", "json")
                        .queryParam("$select", SELECT_FIELDS)
                        .build())
                .retrieve()
                .onStatus(status -> status.value() == 429 || status.value() == 503, (req, res) -> {
                    throw new BcbRetryableException(res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new BcbApiException("Erro na API do BCB: HTTP " + res.getStatusCode().value());
                })
                .body(BcbOdataResponse.class);

        if (response == null || response.value() == null) {
            return List.of();
        }

        return response.value();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BcbApiException("Interrompido durante backoff", e);
        }
    }
}
