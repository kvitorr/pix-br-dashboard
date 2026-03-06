package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tcc.vitor.pix_dashboard.enums.IngestionRunSource;
import tcc.vitor.pix_dashboard.services.abstracts.AbstractFileIngestionService;
import tcc.vitor.pix_dashboard.services.dto.IidhmDTO;
import tcc.vitor.pix_dashboard.services.persistence.IdhmPersistenceService;

import java.io.IOException;
import java.util.List;

@Service
public class IidhmIngestionService extends AbstractFileIngestionService<IidhmDTO> {

    private static final Logger log = LoggerFactory.getLogger(IidhmIngestionService.class);

    private final IidhmCsvParser iidhmCsvParser;
    private final IdhmPersistenceService idhmPersistenceService;

    public IidhmIngestionService(IngestionRunManager runManager,
                                  IidhmCsvParser iidhmCsvParser,
                                  IdhmPersistenceService idhmPersistenceService) {
        super(runManager);
        this.iidhmCsvParser = iidhmCsvParser;
        this.idhmPersistenceService = idhmPersistenceService;
    }

    @Override
    protected Logger log() {
        return log;
    }

    @Override
    protected IngestionRunSource source() {
        return IngestionRunSource.IDHM_ESTADUAL;
    }

    @Override
    protected String sourceName() {
        return "IDHM Atlas/PNUD";
    }

    @Override
    protected String buildParams(String param) {
        return param != null ? "{\"ano\":\"" + param + "\"}" : "{}";
    }

    @Override
    protected List<IidhmDTO> parse(MultipartFile file) throws IOException {
        return iidhmCsvParser.parse(file.getInputStream());
    }

    @Override
    protected int persist(List<IidhmDTO> records, String param) {
        return idhmPersistenceService.persist(records, Integer.parseInt(param));
    }

    @Override
    protected boolean isKnownException(RuntimeException e) {
        return false;
    }

    @Override
    protected String knownErrorCode() {
        return "IDHM_ERROR";
    }
}
