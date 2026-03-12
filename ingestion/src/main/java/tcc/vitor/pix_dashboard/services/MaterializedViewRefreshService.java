package tcc.vitor.pix_dashboard.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterializedViewRefreshService {

    private static final Logger log = LoggerFactory.getLogger(MaterializedViewRefreshService.class);

    @PersistenceContext
    private EntityManager em;

    @Transactional
    @CacheEvict(cacheNames = {"municipio-serie", "evolucao-temporal", "municipiosAtipicos"}, allEntries = true)
    public void refreshAll() {
        log.info("Iniciando refresh das materialized views");
        long start = System.currentTimeMillis();

        em.createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_indicadores_municipio")
                .executeUpdate();
        log.info("mv_indicadores_municipio atualizada em {}ms", System.currentTimeMillis() - start);

        long startRegional = System.currentTimeMillis();
        em.createNativeQuery("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_evolucao_regional")
                .executeUpdate();
        log.info("mv_evolucao_regional atualizada em {}ms", System.currentTimeMillis() - startRegional);

        log.info("Refresh concluído em {}ms total", System.currentTimeMillis() - start);
    }
}
