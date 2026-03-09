// ============================================================
// Tipos espelhando os Java records do backend
// ============================================================

// --- Visão Geral ---
export interface MapaMunicipio {
  municipioIbge: string;
  municipioNome: string;
  penetracaoPf: number | null;
}

export interface PenetracaoRegiao {
  regiao: string;
  siglaRegiao: string;
  penetracaoMedia: number;
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
}

export interface MunicipioAtipico {
  municipioIbge: string;
  municipio: string;
  estado: string;
  regiao: string;
  siglaRegiao: string;
  penetracaoPf: number | null;
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
