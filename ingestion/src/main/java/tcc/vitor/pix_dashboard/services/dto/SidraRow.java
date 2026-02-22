package tcc.vitor.pix_dashboard.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SidraRow(
        @JsonProperty("NC") String nc,
        @JsonProperty("NN") String nn,
        @JsonProperty("MC") String mc,
        @JsonProperty("MN") String mn,
        @JsonProperty("V") String v,
        @JsonProperty("D1C") String d1c,
        @JsonProperty("D1N") String d1n,
        @JsonProperty("D2C") String d2c,
        @JsonProperty("D2N") String d2n,
        @JsonProperty("D3C") String d3c,
        @JsonProperty("D3N") String d3n
) {}
