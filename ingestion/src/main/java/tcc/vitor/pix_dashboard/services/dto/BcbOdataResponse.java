package tcc.vitor.pix_dashboard.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record BcbOdataResponse(
        @JsonProperty("@odata.context") String odataContext,
        List<PixTransacaoMunicipioDTO> value
) {
}
