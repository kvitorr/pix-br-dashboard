package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tcc.vitor.pix_dashboard.clients.IbgePibClient;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.IbgeApiException;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.services.abstracts.AbstractIngestionService;
import tcc.vitor.pix_dashboard.services.dto.IbgePibDTO;
import tcc.vitor.pix_dashboard.services.persistence.PibPersistenceService;

import java.util.List;

@Service
public class IbgePibIngestionService extends AbstractIngestionService<IbgePibDTO> {

    private static final Logger log = LoggerFactory.getLogger(IbgePibIngestionService.class);

    private final IbgePibClient ibgePibClient;
    private final PibPersistenceService pibPersistenceService;

    public IbgePibIngestionService(IngestionRunManager runManager,
                                    IbgePibClient ibgePibClient,
                                    PibPersistenceService pibPersistenceService) {
        super(runManager);
        this.ibgePibClient = ibgePibClient;
        this.pibPersistenceService = pibPersistenceService;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected IngestionRunSource source() {
        return IngestionRunSource.IBGE_PIB;
    }

    @Override
    protected String sourceName() {
        return "PIB IBGE/SIDRA";
    }

    @Override
    protected String buildParams(String param) {
        return param != null ? "{\"ano\":\"" + param + "\"}" : "{}";
    }

    @Override
    protected List<IbgePibDTO> fetch(String param) {
        return ibgePibClient.fetchAll(param);
    }

    @Override
    protected int persist(List<IbgePibDTO> records, IngestionRun run, String param) {
        return pibPersistenceService.persist(records, Integer.parseInt(param));
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
