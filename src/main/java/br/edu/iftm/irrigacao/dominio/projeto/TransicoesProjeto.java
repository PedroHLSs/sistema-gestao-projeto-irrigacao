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

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Mapa das transições permitidas de {@link SituacaoProjeto}, igual à tabela da seção 4 do
 * docs/modelo.md (transições 1 a 17), que vem da Figura 5, da Figura 9 e do BPMN TO-BE (RF21).
 *
 * <p>O mapa só diz o que pode seguir o quê. As condições de negócio de cada transição
 * (levantamento completo, proposta enviada, medição dentro do parâmetro...) são conferidas pelo
 * service que pede a mudança, antes de chamar {@code ProjetoService.transitar(...)}.
 *
 * <p>A transição 1 é a abertura: o projeto nasce em {@link #SITUACAO_INICIAL}, sem situação
 * anterior, o que aqui aparece como {@code atual == null}. ENCERRADO e CANCELADO são finais: não
 * têm transição de saída.
 */
public final class TransicoesProjeto {

    /** Situação em que todo projeto nasce (transição 1, RF01). */
    public static final SituacaoProjeto SITUACAO_INICIAL = EM_LEVANTAMENTO;

    private static final Map<SituacaoProjeto, Set<SituacaoProjeto>> SEGUINTES = montar();

    private TransicoesProjeto() {
    }

    private static Map<SituacaoProjeto, Set<SituacaoProjeto>> montar() {
        Map<SituacaoProjeto, Set<SituacaoProjeto>> mapa = new EnumMap<>(SituacaoProjeto.class);
        for (SituacaoProjeto situacao : SituacaoProjeto.values()) {
            mapa.put(situacao, EnumSet.noneOf(SituacaoProjeto.class));
        }
        // 1 (abertura -> EM_LEVANTAMENTO) fica em SITUACAO_INICIAL
        permitir(mapa, EM_LEVANTAMENTO, ORCANDO);                      // 2  levantamento completo (G1), dimensionamento e lista técnica
        permitir(mapa, ORCANDO, AGUARDANDO_RESPOSTA);                  // 3  proposta enviada
        permitir(mapa, AGUARDANDO_RESPOSTA, ORCANDO);                  // 4  proposta vencida
        permitir(mapa, AGUARDANDO_RESPOSTA, PREPARANDO_MATERIAL);      // 5  proposta aprovada
        permitir(mapa, AGUARDANDO_RESPOSTA, CANCELADO);                // 6  proposta recusada (G3)
        permitir(mapa, PREPARANDO_MATERIAL, PROGRAMANDO_EXECUCAO);     // 7  separação conferida (G5)
        permitir(mapa, PROGRAMANDO_EXECUCAO, EXECUTANDO_INSTALACAO);   // 8  ordem de serviço emitida
        permitir(mapa, EXECUTANDO_INSTALACAO, VALIDANDO_MEDICAO);      // 9  instalação concluída
        permitir(mapa, VALIDANDO_MEDICAO, COLHENDO_ACEITE);            // 10 medição dentro do parâmetro
        permitir(mapa, VALIDANDO_MEDICAO, CORRIGINDO_INSTALACAO);      // 11 medição fora do parâmetro
        permitir(mapa, CORRIGINDO_INSTALACAO, EXECUTANDO_INSTALACAO);  // 12 inconformidade registrada
        permitir(mapa, COLHENDO_ACEITE, FINALIZANDO_EXECUCAO);         // 13 termo de aceite assinado
        permitir(mapa, COLHENDO_ACEITE, TRATANDO_PENDENCIA);           // 14 aceite recusado
        permitir(mapa, TRATANDO_PENDENCIA, COLHENDO_ACEITE);           // 15 visita reprogramada, pendência ADMINISTRATIVA
        permitir(mapa, TRATANDO_PENDENCIA, VALIDANDO_MEDICAO);         // 16 correção solicitada, pendência TECNICA
        permitir(mapa, FINALIZANDO_EXECUCAO, ENCERRADO);               // 17 sobras devolvidas (G8) e faturamento liberado
        // ENCERRADO e CANCELADO ficam sem saída: são finais
        mapa.replaceAll((situacao, seguintes) -> Collections.unmodifiableSet(seguintes));
        return Collections.unmodifiableMap(mapa);
    }

    private static void permitir(Map<SituacaoProjeto, Set<SituacaoProjeto>> mapa, SituacaoProjeto de,
            SituacaoProjeto para) {
        mapa.get(de).add(para);
    }

    /**
     * @param atual situação de agora; {@code null} na abertura do projeto
     * @param nova  situação pedida
     * @return se o mapa permite passar de {@code atual} para {@code nova}
     */
    public static boolean permite(SituacaoProjeto atual, SituacaoProjeto nova) {
        if (nova == null) {
            return false;
        }
        if (atual == null) {
            return nova == SITUACAO_INICIAL;
        }
        return SEGUINTES.get(atual).contains(nova);
    }

    /** Situações para onde o projeto pode ir a partir de {@code atual}. Vazio nas situações finais. */
    public static Set<SituacaoProjeto> seguintes(SituacaoProjeto atual) {
        return SEGUINTES.get(atual);
    }

    /** ENCERRADO e CANCELADO: o projeto não muda mais de situação. */
    public static boolean ehFinal(SituacaoProjeto situacao) {
        return SEGUINTES.get(situacao).isEmpty();
    }
}
