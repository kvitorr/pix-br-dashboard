package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tcc.vitor.pix_dashboard.clients.BcbPixClient;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.exceptions.BcbApiException;
import tcc.vitor.pix_dashboard.database.models.IngestionRun;
import tcc.vitor.pix_dashboard.services.dto.PixTransacaoMunicipioDTO;
import tcc.vitor.pix_dashboard.services.persistence.PixPersistenceService;

import java.util.List;

@Service
public class BacenPixIngestionService extends AbstractIngestionService<PixTransacaoMunicipioDTO> {

    private static final Logger log = LoggerFactory.getLogger(BacenPixIngestionService.class);

    private final BcbPixClient bcbPixClient;
    private final PixPersistenceService pixPersistenceService;

    public BacenPixIngestionService(IngestionRunManager runManager,
                                    BcbPixClient bcbPixClient,
                                    PixPersistenceService pixPersistenceService) {
        super(runManager);
        this.bcbPixClient = bcbPixClient;
        this.pixPersistenceService = pixPersistenceService;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected IngestionRunSource source() {
        return IngestionRunSource.BACEN_PIX;
    }

    @Override
    protected String sourceName() {
        return "PIX BACEN";
    }

    @Override
    protected String buildParams(String param) {
        return "{\"database\":\"" + param + "\"}";
    }

    @Override
    protected List<PixTransacaoMunicipioDTO> fetch(String param) {
        return bcbPixClient.fetchAll(param);
    }

    @Override
    protected int persist(List<PixTransacaoMunicipioDTO> records, IngestionRun run, String param) {
        return pixPersistenceService.persistRecords(records, run.getId());
    }

    @Override
    protected boolean isKnownException(RuntimeException e) {
        return e instanceof BcbApiException;
    }

    @Override
    protected String knownErrorCode() {
        return "BCB_API_ERROR";
    }
}
