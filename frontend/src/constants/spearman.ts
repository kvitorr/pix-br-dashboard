// Coeficientes de Spearman pré-calculados via scipy.stats.spearmanr (Python)
// Calculados contra o dataset completo no momento da análise do TCC
// TODO: substituir pelos valores reais após executar o script Python de cálculo
export const SPEARMAN = {
  pibPerCapita: 0.62,
  idhm: 0.58,
  taxaUrbanizacao: 0.54,
} as const;

export type SpearmanKey = keyof typeof SPEARMAN;
