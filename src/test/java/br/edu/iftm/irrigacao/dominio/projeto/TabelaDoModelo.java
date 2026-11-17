package br.edu.iftm.irrigacao.dominio.projeto;

import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.AGUARDANDO_RESPOSTA;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.CANCELADO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.COLHENDO_ACEITE;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.CORRIGINDO_INSTALACAO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.EM_LEVANTAMENTO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.ENCERRADO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.EXECUTANDO_INSTALACAO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.FINALIZANDO_EXECUCAO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.ORCANDO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.PREPARANDO_MATERIAL;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.PROGRAMANDO_EXECUCAO;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.TRATANDO_PENDENCIA;
import static br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto.VALIDANDO_MEDICAO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;

/**
 * A tabela "Transições permitidas" da seção 4 do docs/modelo.md, copiada linha a linha. Os testes
 * conferem o código contra ela, e não contra o próprio mapa de {@link TransicoesProjeto}.
 */
public final class TabelaDoModelo {

    /** Nº, de, para. A transição 1 é a abertura: não tem situação de origem. */
    private static final Object[][] TRANSICOES = {
            { 1, null, EM_LEVANTAMENTO },
            { 2, EM_LEVANTAMENTO, ORCANDO },
            { 3, ORCANDO, AGUARDANDO_RESPOSTA },
            { 4, AGUARDANDO_RESPOSTA, ORCANDO },
            { 5, AGUARDANDO_RESPOSTA, PREPARANDO_MATERIAL },
            { 6, AGUARDANDO_RESPOSTA, CANCELADO },
            { 7, PREPARANDO_MATERIAL, PROGRAMANDO_EXECUCAO },
            { 8, PROGRAMANDO_EXECUCAO, EXECUTANDO_INSTALACAO },
            { 9, EXECUTANDO_INSTALACAO, VALIDANDO_MEDICAO },
            { 10, VALIDANDO_MEDICAO, COLHENDO_ACEITE },
            { 11, VALIDANDO_MEDICAO, CORRIGINDO_INSTALACAO },
            { 12, CORRIGINDO_INSTALACAO, EXECUTANDO_INSTALACAO },
            { 13, COLHENDO_ACEITE, FINALIZANDO_EXECUCAO },
            { 14, COLHENDO_ACEITE, TRATANDO_PENDENCIA },
            { 15, TRATANDO_PENDENCIA, COLHENDO_ACEITE },
            { 16, TRATANDO_PENDENCIA, VALIDANDO_MEDICAO },
            { 17, FINALIZANDO_EXECUCAO, ENCERRADO },
    };

    private TabelaDoModelo() {
    }

    /** As 17 transições: (número, de, para). */
    public static Stream<Arguments> todas() {
        return Arrays.stream(TRANSICOES).map(Arguments::of);
    }

    /** As transições 2 a 17, que mudam a situação de um projeto que já existe. */
    public static Stream<Arguments> mudancas() {
        return Arrays.stream(TRANSICOES).filter(linha -> linha[1] != null).map(Arguments::of);
    }

    public static boolean permitida(SituacaoProjeto de, SituacaoProjeto para) {
        return Arrays.stream(TRANSICOES).anyMatch(linha -> linha[1] == de && linha[2] == para);
    }

    /** Todos os pares (de, para) entre as 13 situações que não estão na tabela: 169 - 16 = 153. */
    public static Stream<Arguments> proibidas() {
        List<Arguments> pares = new ArrayList<>();
        for (SituacaoProjeto de : SituacaoProjeto.values()) {
            for (SituacaoProjeto para : SituacaoProjeto.values()) {
                if (!permitida(de, para)) {
                    pares.add(Arguments.of(de, para));
                }
            }
        }
        return pares.stream();
    }

    /** Toda saída de ENCERRADO e de CANCELADO, inclusive para a mesma situação. */
    public static Stream<Arguments> saidasDasFinais() {
        return Stream.of(ENCERRADO, CANCELADO)
                .flatMap(fim -> Arrays.stream(SituacaoProjeto.values()).map(para -> Arguments.of(fim, para)));
    }
}
