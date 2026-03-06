package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tcc.vitor.pix_dashboard.clients.IbgePopulacaoClient;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.services.dto.IbgePopulacaoDTO;
import tcc.vitor.pix_dashboard.services.persistence.PopulacaoPersistenceService;

import java.util.List;

@Service
public class IbgePopulacaoIngestionService extends AbstractIngestionService<IbgePopulacaoDTO> {

    private static final Logger log = LoggerFactory.getLogger(IbgePopulacaoIngestionService.class);

    private final IbgePopulacaoClient ibgePopulacaoClient;
    private final PopulacaoPersistenceService populacaoPersistenceService;

    public IbgePopulacaoIngestionService(IngestionRunManager runManager,
                                         IbgePopulacaoClient ibgePopulacaoClient,
                                         PopulacaoPersistenceService populacaoPersistenceService) {
        super(runManager);
        this.ibgePopulacaoClient = ibgePopulacaoClient;
        this.populacaoPersistenceService = populacaoPersistenceService;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected IngestionRunSource source() {
        return IngestionRunSource.IBGE_POP;
    }

    @Override
    protected String sourceName() {
        return "populacao IBGE";
    }

    @Override
    protected String buildParams(String param) {
        return param != null ? "{\"ano\":\"" + param + "\"}" : "{}";
    }

    @Override
    protected List<IbgePopulacaoDTO> fetch(String param) {
        return ibgePopulacaoClient.fetchAll(param);
    }

    @Override
    protected int persist(List<IbgePopulacaoDTO> records, IngestionRun run, String param) {
        return populacaoPersistenceService.persist(records, Integer.parseInt(param));
    }

    @Override
    protected boolean isKnownException(RuntimeException e) {
        return e instanceof IbgeApiException;
    }

    @Override
    protected String knownErrorCode() {
        return "IBGE_API_ERROR";
    }
}
