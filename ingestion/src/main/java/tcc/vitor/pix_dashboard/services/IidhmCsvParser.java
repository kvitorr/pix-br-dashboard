package tcc.vitor.pix_dashboard.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tcc.vitor.pix_dashboard.services.dto.IidhmDTO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser para o arquivo CSV de IDHM estadual do Atlas do Desenvolvimento Humano (PNUD/IPEA/FJP).
 * Formato esperado: separado por tabulação (TSV), com cabeçalho na primeira linha.
 * Colunas: ANO, AGREGACAO, CODIGO, NOME, IDHM, IDHM_L, IDHM_E, IDHM_R
 */
@Component
public class IidhmCsvParser {

    private static final Logger log = LoggerFactory.getLogger(IidhmCsvParser.class);

    private static final String SEPARADOR = "\t";
    private static final int IDX_ANO = 0;
    private static final int IDX_NOME = 3;
    private static final int IDX_IDHM = 4;
    private static final int IDX_IDHM_L = 5;
    private static final int IDX_IDHM_E = 6;
    private static final int IDX_IDHM_R = 7;

    public List<IidhmDTO> parse(InputStream inputStream) throws IOException {
        List<IidhmDTO> resultado = new ArrayList<>();
        int linhaAtual = 0;
        int erros = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                linhaAtual++;

                // Pula o cabeçalho
                if (linhaAtual == 1) {
                    log.atDebug()
                            .addKeyValue("cabecalho", linha)
                            .log("Cabeçalho do CSV de IDHM identificado");
                    continue;
                }

                // Pula linhas vazias
                if (linha.isBlank()) {
                    continue;
                }

                try {
                    String[] campos = linha.split(SEPARADOR, -1);

                    if (campos.length < 8) {
                        log.atWarn()
                                .addKeyValue("linha", linhaAtual)
                                .addKeyValue("totalCampos", campos.length)
                                .log("Linha com número insuficiente de campos, ignorando");
                        erros++;
                        continue;
                    }

                    String ano = campos[IDX_ANO].trim();
                    String nomeEstado = campos[IDX_NOME].trim();
                    String idhmStr = campos[IDX_IDHM].trim();
                    String idhmLStr = campos[IDX_IDHM_L].trim();
                    String idhmEStr = campos[IDX_IDHM_E].trim();
                    String idhmRStr = campos[IDX_IDHM_R].trim();

                    if (nomeEstado.isBlank() || idhmStr.isBlank()) {
                        log.atWarn()
                                .addKeyValue("linha", linhaAtual)
                                .log("Linha com nome ou IDHM vazio, ignorando");
                        erros++;
                        continue;
                    }

                    // Normaliza separador decimal (vírgula → ponto, caso o CSV use vírgula)
                    IidhmDTO dto = new IidhmDTO(
                            parseInteger(ano),
                            nomeEstado,
                            parseBigDecimal(idhmStr),
                            parseBigDecimal(idhmLStr),
                            parseBigDecimal(idhmEStr),
                            parseBigDecimal(idhmRStr)
                    );

                    resultado.add(dto);

                } catch (Exception e) {
                    log.atWarn()
                            .addKeyValue("linha", linhaAtual)
                            .addKeyValue("conteudo", linha)
                            .addKeyValue("erro", e.getMessage())
                            .log("Erro ao parsear linha do CSV de IDHM, ignorando");
                    erros++;
                }
            }
        }

        log.atInfo()
                .addKeyValue("totalLinhas", linhaAtual - 1)
                .addKeyValue("registrosParsed", resultado.size())
                .addKeyValue("erros", erros)
                .log("Parse do CSV de IDHM concluido");

        return resultado;
    }

    private BigDecimal parseBigDecimal(String valor) {
        if (valor == null || valor.isBlank() || valor.equals("-")) {
            return null;
        }
        // Normaliza separador decimal: vírgula → ponto
        return new BigDecimal(valor.replace(",", "."));
    }

    private Integer parseInteger(String valor) {
        if (valor == null || valor.isBlank() || valor.equals("-")) {
            return null;
        }
        return Integer.valueOf(valor);
    }
}
