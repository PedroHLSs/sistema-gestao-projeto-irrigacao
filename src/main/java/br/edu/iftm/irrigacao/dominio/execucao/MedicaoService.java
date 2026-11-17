package br.edu.iftm.irrigacao.dominio.execucao;

import java.math.BigDecimal;
import java.util.List;

import br.edu.iftm.irrigacao.dominio.projeto.FotoArmazenada;
import br.edu.iftm.irrigacao.dominio.projeto.FotoEnviada;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * Medição de pressão e vazão e validação contra os parâmetros do projeto (RF17, G6), pela
 * Figura 6. A medição é aprovada quando pressão e vazão ficam dentro de mais ou menos
 * {@code toleranciaPercentual} da pressão e da vazão de projeto, com os limites contando como
 * dentro.
 */
public interface MedicaoService {

    /** A faixa aceita para pressão e vazão, para o técnico ver antes de medir. */
    FaixaMedicao faixa(Projeto projeto);

    /**
     * Registra a medição com o projeto em VALIDANDO_MEDICAO. Aprovada, o projeto vai para
     * COLHENDO_ACEITE (transição 10); reprovada, para CORRIGINDO_INSTALACAO (transição 11).
     *
     * @param foto opcional; mesmas regras da foto do levantamento (JPEG ou PNG até 5 MB)
     */
    Medicao registrar(Long projetoId, Double pressaoMedidaMca, Double vazaoMedidaM3h, FotoEnviada foto,
            Double latitude, Double longitude);

    /** Medições do projeto, da mais nova para a mais antiga, com o desvio de cada valor. */
    List<MedicaoAvaliada> doProjeto(Long projetoId);

    /** Foto da medição, se ela for do projeto informado. */
    FotoArmazenada foto(Long projetoId, Long medicaoId);

    /** |medida - referência| <= referência x tolerância / 100, com o limite contando como dentro. */
    boolean dentroDaTolerancia(double medida, double referencia, double toleranciaPercentual);

    /** (medida - referência) / referência x 100, com 2 casas. */
    BigDecimal desvioPercentual(double medida, double referencia);
}
