package tcc.vitor.pix_dashboard.services;

import org.junit.jupiter.api.Test;
import tcc.vitor.pix_dashboard.services.dto.IidhmDTO;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IidhmCsvParserTest {

    private final IidhmCsvParser parser = new IidhmCsvParser();

    private InputStream toStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parse_validTsv_returnsCorrectDtos() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\tNOME\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n" +
                     "2021\tEstado\t35\tSão Paulo\t0.783\t0.845\t0.736\t0.769\n" +
                     "2021\tEstado\t33\tRio de Janeiro\t0.761\t0.831\t0.710\t0.744\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).hasSize(2);

        IidhmDTO sp = result.get(0);
        assertThat(sp.nomeEstado()).isEqualTo("São Paulo");
        assertThat(sp.idhm()).isEqualByComparingTo(new BigDecimal("0.783"));
        assertThat(sp.idhmLongevidade()).isEqualByComparingTo(new BigDecimal("0.845"));
        assertThat(sp.idhmEducacao()).isEqualByComparingTo(new BigDecimal("0.736"));
        assertThat(sp.idhmRenda()).isEqualByComparingTo(new BigDecimal("0.769"));

        IidhmDTO rj = result.get(1);
        assertThat(rj.nomeEstado()).isEqualTo("Rio de Janeiro");
        assertThat(rj.idhm()).isEqualByComparingTo(new BigDecimal("0.761"));
    }

    @Test
    void parse_skipsHeaderLine() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\tNOME\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n" +
                     "2021\tEstado\t35\tSão Paulo\t0.783\t0.845\t0.736\t0.769\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nomeEstado()).isEqualTo("São Paulo");
    }

    @Test
    void parse_skipsBlankLines() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\tNOME\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n" +
                     "\n" +
                     "2021\tEstado\t35\tSão Paulo\t0.783\t0.845\t0.736\t0.769\n" +
                     "\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).hasSize(1);
    }

    @Test
    void parse_skipLineWithInsufficientColumns() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\tNOME\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n" +
                     "2021\tEstado\t35\tSão Paulo\t0.783\n" +
                     "2021\tEstado\t33\tRio de Janeiro\t0.761\t0.831\t0.710\t0.744\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nomeEstado()).isEqualTo("Rio de Janeiro");
    }

    @Test
    void parse_skipLineWithBlankName() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\t\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n" +
                     "2021\tEstado\t35\t\t0.783\t0.845\t0.736\t0.769\n" +
                     "2021\tEstado\t33\tRio de Janeiro\t0.761\t0.831\t0.710\t0.744\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nomeEstado()).isEqualTo("Rio de Janeiro");
    }

    @Test
    void parse_skipLineWithBlankIdhm() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\tNOME\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n" +
                     "2021\tEstado\t35\tSão Paulo\t\t0.845\t0.736\t0.769\n" +
                     "2021\tEstado\t33\tRio de Janeiro\t0.761\t0.831\t0.710\t0.744\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).nomeEstado()).isEqualTo("Rio de Janeiro");
    }

    @Test
    void parse_commaDecimalSeparator_normalizedToDot() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\tNOME\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n" +
                     "2021\tEstado\t35\tSão Paulo\t0,783\t0,845\t0,736\t0,769\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).idhm()).isEqualByComparingTo(new BigDecimal("0.783"));
        assertThat(result.get(0).idhmLongevidade()).isEqualByComparingTo(new BigDecimal("0.845"));
    }

    @Test
    void parse_dashValueForSubDimension_returnsNullBigDecimal() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\tNOME\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n" +
                     "2021\tEstado\t35\tSão Paulo\t0.783\t-\t-\t-\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).idhm()).isEqualByComparingTo(new BigDecimal("0.783"));
        assertThat(result.get(0).idhmLongevidade()).isNull();
        assertThat(result.get(0).idhmEducacao()).isNull();
        assertThat(result.get(0).idhmRenda()).isNull();
    }

    @Test
    void parse_emptyInputStream_returnsEmptyList() throws IOException {
        List<IidhmDTO> result = parser.parse(toStream(""));

        assertThat(result).isEmpty();
    }

    @Test
    void parse_onlyHeader_returnsEmptyList() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\tNOME\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).isEmpty();
    }

    @Test
    void parse_mixedValidAndInvalidLines_returnsOnlyValidOnes() throws IOException {
        String tsv = "ANO\tAGREGACAO\tCODIGO\tNOME\tIDHM\tIDHM_L\tIDHM_E\tIDHM_R\n" +
                     "2021\tEstado\t35\tSão Paulo\t0.783\t0.845\t0.736\t0.769\n" +
                     "2021\tEstado\t35\t\t0.783\t0.845\t0.736\t0.769\n" +     // blank name — skip
                     "2021\tEstado\t33\tRio de Janeiro\t0.761\t0.831\t0.710\t0.744\n";

        List<IidhmDTO> result = parser.parse(toStream(tsv));

        assertThat(result).hasSize(2);
        assertThat(result).extracting(IidhmDTO::nomeEstado)
                .containsExactly("São Paulo", "Rio de Janeiro");
    }
}
