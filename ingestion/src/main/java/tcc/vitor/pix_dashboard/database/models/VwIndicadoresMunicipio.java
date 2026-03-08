package tcc.vitor.pix_dashboard.database.models;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "vw_indicadores_municipio")
public class VwIndicadoresMunicipio {

    @EmbeddedId
    private VwIndicadoresMunicipioId id;

    @Column(name = "municipio")
    private String municipio;

    @Column(name = "estado")
    private String estado;

    @Column(name = "regiao")
    private String regiao;

    @Column(name = "sigla_regiao")
    private String siglaRegiao;

    @Column(name = "penetracao_pf")
    private Double penetracaoPf;

    @Column(name = "ticket_medio_pf")
    private Double ticketMedioPf;

    @Column(name = "razao_pj_pf")
    private Double razaoPjPf;

    @Column(name = "vl_per_capita_pf")
    private Double vlPerCapitaPf;

    @Column(name = "pib_per_capita")
    private Double pibPerCapita;

    @Column(name = "idhm")
    private Double idhm;

    @Column(name = "taxa_urbanizacao")
    private Double taxaUrbanizacao;

    public VwIndicadoresMunicipioId getId() { return id; }
    public String getMunicipio() { return municipio; }
    public String getEstado() { return estado; }
    public String getRegiao() { return regiao; }
    public String getSiglaRegiao() { return siglaRegiao; }
    public Double getPenetracaoPf() { return penetracaoPf; }
    public Double getTicketMedioPf() { return ticketMedioPf; }
    public Double getRazaoPjPf() { return razaoPjPf; }
    public Double getVlPerCapitaPf() { return vlPerCapitaPf; }
    public Double getPibPerCapita() { return pibPerCapita; }
    public Double getIdhm() { return idhm; }
    public Double getTaxaUrbanizacao() { return taxaUrbanizacao; }
}
