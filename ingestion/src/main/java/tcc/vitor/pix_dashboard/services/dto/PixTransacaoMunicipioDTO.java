package tcc.vitor.pix_dashboard.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PixTransacaoMunicipioDTO(
        @JsonProperty("AnoMes") String anoMes,
        @JsonProperty("Municipio_Ibge") String municipioIbge,
        @JsonProperty("Municipio") String municipio,
        @JsonProperty("Estado_Ibge") String estadoIbge,
        @JsonProperty("Estado") String estado,
        @JsonProperty("Sigla_Regiao") String siglaRegiao,
        @JsonProperty("Regiao") String regiao,
        @JsonProperty("VL_PagadorPF") BigDecimal vlPagadorPF,
        @JsonProperty("QT_PagadorPF") Long qtPagadorPF,
        @JsonProperty("VL_PagadorPJ") BigDecimal vlPagadorPJ,
        @JsonProperty("QT_PagadorPJ") Long qtPagadorPJ,
        @JsonProperty("VL_RecebedorPF") BigDecimal vlRecebedorPF,
        @JsonProperty("QT_RecebedorPF") Long qtRecebedorPF,
        @JsonProperty("VL_RecebedorPJ") BigDecimal vlRecebedorPJ,
        @JsonProperty("QT_RecebedorPJ") Long qtRecebedorPJ,
        @JsonProperty("QT_PES_PagadorPF") Long qtPesPagadorPF,
        @JsonProperty("QT_PES_PagadorPJ") Long qtPesPagadorPJ,
        @JsonProperty("QT_PES_RecebedorPF") Long qtPesRecebedorPF,
        @JsonProperty("QT_PES_RecebedorPJ") Long qtPesRecebedorPJ
) {
}
