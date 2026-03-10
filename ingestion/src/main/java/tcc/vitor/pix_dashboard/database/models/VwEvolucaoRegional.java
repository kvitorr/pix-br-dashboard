package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "vw_evolucao_regional")
public class VwEvolucaoRegional {

    @EmbeddedId
    private VwEvolucaoRegionalId id;

    @Column(name = "sigla_regiao")
    private String siglaRegiao;

    @Column(name = "penetracao_media", columnDefinition = "numeric")
    private Double penetracaoMedia;

    @Column(name = "ticket_medio", columnDefinition = "numeric")
    private Double ticketMedio;

    @Column(name = "valor_per_capita", columnDefinition = "numeric")
    private Double valorPerCapita;

    @Column(name = "razao_pj_pf_media", columnDefinition = "numeric")
    private Double razaoPjPfMedia;

    @Column(name = "populacao_total")
    private Long populacaoTotal;

    public VwEvolucaoRegionalId getId() { return id; }
    public String getSiglaRegiao() { return siglaRegiao; }
    public Double getPenetracaoMedia() { return penetracaoMedia; }
    public Double getTicketMedio() { return ticketMedio; }
    public Double getValorPerCapita() { return valorPerCapita; }
    public Double getRazaoPjPfMedia() { return razaoPjPfMedia; }
    public Long getPopulacaoTotal() { return populacaoTotal; }
}
