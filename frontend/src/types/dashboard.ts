// ============================================================
// Tipos espelhando os Java records do backend
// ============================================================

// --- Visão Geral ---
export interface MapaMunicipio {
  municipioIbge: string;
  municipioNome: string;
  penetracaoPf: number | null;
  ticketMedioPf: number | null;
  razaoPjPf: number | null;
  vlPerCapitaPf: number | null;
}

export interface PenetracaoRegiao {
  regiao: string;
  siglaRegiao: string;
  penetracaoMedia: number;
  ticketMedioMedia: number | null;
  razaoMedia: number | null;
  perCapitaMedia: number | null;
}

export interface KpisVisaoGeral {
  penetracaoMediaNacional: number;
  ticketMedioPf: number;
  razaoPjPf: number;
  vlPerCapitaPf: number;
}

export interface DonutCobertura {
  municipiosAcima50: number;
  municipiosAbaixo50: number;
}

export interface VisaoGeralResponse {
  kpis: KpisVisaoGeral;
  mapaMunicipios: MapaMunicipio[];
  penetracaoPorRegiao: PenetracaoRegiao[];
  coberturaNacional: DonutCobertura;
}

// --- Disparidade Regional ---
export interface IqrRegiao {
  regiao: string;
  q1: number;
  mediana: number;
  q3: number;
  stddev: number;
}

export interface MunicipioRanking {
  municipioIbge: string;
  municipio: string;
  estado: string;
  regiao: string;
  siglaRegiao: string;
  penetracaoPf: number | null;
  ticketMedioPf: number | null;
  razaoPjPf: number | null;
  vlPerCapitaPf: number | null;
}

export interface MunicipioAtipico {
  municipioIbge: string;
  municipio: string;
  estado: string;
  regiao: string;
  siglaRegiao: string;
  penetracaoPf: number | null;
  ticketMedioPf: number | null;
  razaoPjPf: number | null;
  vlPerCapitaPf: number | null;
  pibPerCapita: number | null;
  tipo: 'alta-adocao-baixo-pib' | 'baixa-adocao-alto-pib';
  tags: string[];
}

export interface DisparidadeRegionalResponse {
  distribuicaoIqr: IqrRegiao[];
  top10: MunicipioRanking[];
  bottom10: MunicipioRanking[];
  municipiosAtipicos: MunicipioAtipico[];
}

// --- Análise Municipal ---
export interface MunicipioListItem {
  municipioIbge: string;
  municipioNome: string;
  estado: string;
  regiao: string;
  siglaRegiao: string;
}

export interface MunicipioDetalhes {
  municipioIbge: string;
  municipioNome: string;
  estado: string;
  regiao: string;
  siglaRegiao: string;
  penetracaoPf: number | null;
  ticketMedioPf: number | null;
  razaoPjPf: number | null;
  vlPerCapitaPf: number | null;
  pibPerCapita: number | null;
  idhm: number | null;
  taxaUrbanizacao: number | null;
}

// --- Evolução Temporal ---
export interface RegiaoPenetracao {
  regiao: string;
  penetracaoMedia: number;
  ticketMedio: number | null;
}

export interface SerieTemporalPonto {
  anoMes: string;
  porRegiao: RegiaoPenetracao[];
}

export interface CrescimentoAcumulado {
  regiao: string;
  variacaoPp: number;
}

export interface TicketNacional {
  anoMes: string;
  ticketMedio: number;
}

export interface KpisEvolucao {
  penetracaoAtual: number;
  regiaoMaiorCrescimento: string;
  convergenciaNorteSul: number;
  totalMeses: number;
}

export interface EvolucaoTemporalResponse {
  kpis: KpisEvolucao;
  serieTemporal: SerieTemporalPonto[];
  crescimentoAcumulado: CrescimentoAcumulado[];
  ticketNacionalEvolucao: TicketNacional[];
}

// --- Série Temporal Municipal ---
export interface MunicipioSeriePonto {
  anoMes: string;
  municipioPenetracaoPf: number | null;
  municipioTicketMedioPf: number | null;
  municipioVlPerCapitaPf: number | null;
  municipioRazaoPjPf: number | null;
  regiaoPenetracaoPf: number | null;
  regiaoTicketMedioPf: number | null;
  regiaoVlPerCapitaPf: number | null;
  regiaoRazaoPjPf: number | null;
  nacionalPenetracaoPf: number | null;
  nacionalTicketMedioPf: number | null;
  nacionalVlPerCapitaPf: number | null;
  nacionalRazaoPjPf: number | null;
}

export interface MunicipioSerieResponse {
  regiao: string;
  siglaRegiao: string;
  serie: MunicipioSeriePonto[];
}

// --- Fatores Socioeconômicos ---
export interface ScatterMunicipio {
  municipioIbge: string;
  municipio: string;
  estado: string;
  regiao: string;
  pibPerCapita: number | null;
  idhm: number | null;
  taxaUrbanizacao: number | null;
  penetracaoPf: number | null;
  ticketMedioPf: number | null;
  razaoPjPf: number | null;
  vlPerCapitaPf: number | null;
}

export interface CorrelacaoSpearman {
  fator: 'pibPerCapita' | 'idhm' | 'taxaUrbanizacao';
  rho: number;
  pValor: number;
  n: number;
  forca: 'Forte' | 'Moderada' | 'Fraca';
}

export interface FatoresSocioeconomicosResponse {
  scatterData: ScatterMunicipio[];
  correlacoes: CorrelacaoSpearman[];
  top10: MunicipioRanking[];
  bottom10: MunicipioRanking[];
  municipiosAtipicos: MunicipioAtipico[];
}
