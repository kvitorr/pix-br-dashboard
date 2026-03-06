package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tcc.vitor.pix_dashboard.clients.IbgeUrbanizacaoClient;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.services.dto.IbgeUrbanizacaoDTO;
import tcc.vitor.pix_dashboard.services.persistence.UrbanizacaoPersistenceService;

import java.util.List;

@Service
public class IbgeUrbanizacaoIngestionService extends AbstractIngestionService<IbgeUrbanizacaoDTO> {

    private static final Logger log = LoggerFactory.getLogger(IbgeUrbanizacaoIngestionService.class);

    private final IbgeUrbanizacaoClient ibgeUrbanizacaoClient;
    private final UrbanizacaoPersistenceService urbanizacaoPersistenceService;

    public IbgeUrbanizacaoIngestionService(IngestionRunManager runManager,
                                            IbgeUrbanizacaoClient ibgeUrbanizacaoClient,
                                            UrbanizacaoPersistenceService urbanizacaoPersistenceService) {
        super(runManager);
        this.ibgeUrbanizacaoClient = ibgeUrbanizacaoClient;
        this.urbanizacaoPersistenceService = urbanizacaoPersistenceService;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected IngestionRunSource source() {
        return IngestionRunSource.IBGE_URBANIZACAO;
    }

    @Override
    protected String sourceName() {
        return "urbanizacao IBGE";
    }

    @Override
    protected String buildParams(String param) {
        return param != null ? "{\"ano\":\"" + param + "\"}" : "{}";
    }

    @Override
    protected List<IbgeUrbanizacaoDTO> fetch(String param) {
        return ibgeUrbanizacaoClient.fetchAll(param);
    }

    @Override
    protected int persist(List<IbgeUrbanizacaoDTO> records, IngestionRun run, String param) {
        return urbanizacaoPersistenceService.persist(records, Integer.parseInt(param));
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
