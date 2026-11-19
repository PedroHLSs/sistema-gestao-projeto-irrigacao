package br.edu.iftm.irrigacao.dominio.projeto;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Exportação do orçado x realizado para o Excel em português (RF22): UTF-8 com BOM, para os acentos
 * saírem certos, ponto e vírgula entre as colunas e vírgula decimal.
 *
 * <p>Texto que começa com {@code =}, {@code +}, {@code -} ou {@code @} recebe um apóstrofo na frente,
 * porque o Excel executaria o conteúdo como fórmula.
 */
public final class OrcadoRealizadoCsv {

    private static final byte[] BOM = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
    private static final String SEPARADOR = ";";
    private static final String FIM_DE_LINHA = "\r\n";
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String CABECALHO = String.join(SEPARADOR, "Projeto", "Produtor", "Liberação do faturamento",
            "Valor total (R$)", "Custo orçado (R$)", "Custo realizado (R$)", "Margem prevista (R$)", "Margem prevista (%)",
            "Margem real (R$)", "Margem real (%)");

    private OrcadoRealizadoCsv() {
    }

    /** @return o arquivo pronto, com o BOM nos três primeiros bytes */
    public static byte[] gerar(List<OrcadoRealizadoLinha> linhas) {
        StringBuilder csv = new StringBuilder(CABECALHO).append(FIM_DE_LINHA);
        for (OrcadoRealizadoLinha linha : linhas) {
            Apuracao apuracao = linha.apuracao();
            csv.append(String.join(SEPARADOR, texto(linha.codigo()), texto(linha.produtor()),
                    linha.dataLiberacao() == null ? "" : DATA.format(linha.dataLiberacao()),
                    numero(apuracao.valorTotal()), numero(apuracao.custoOrcado()), numero(apuracao.custoRealizado()),
                    numero(apuracao.getMargemPrevista()), numero(apuracao.getMargemPrevistaPercentual()),
                    numero(apuracao.getMargemReal()), numero(apuracao.getMargemRealPercentual())))
                    .append(FIM_DE_LINHA);
        }
        ByteArrayOutputStream arquivo = new ByteArrayOutputStream();
        arquivo.writeBytes(BOM);
        arquivo.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return arquivo.toByteArray();
    }

    private static String numero(BigDecimal valor) {
        return valor == null ? "" : valor.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private static String texto(String valor) {
        if (valor == null) {
            return "";
        }
        String seguro = !valor.isEmpty() && "=+-@\t\r".indexOf(valor.charAt(0)) >= 0 ? "'" + valor : valor;
        boolean precisaDeAspas = seguro.contains(SEPARADOR) || seguro.contains("\"") || seguro.contains("\n")
                || seguro.contains("\r");
        return precisaDeAspas ? "\"" + seguro.replace("\"", "\"\"") + "\"" : seguro;
    }
}
