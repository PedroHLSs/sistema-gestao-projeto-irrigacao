package br.edu.iftm.irrigacao.dominio.execucao;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Apontamento de horas e de materiais consumidos na instalação (RF16), feito na ordem de serviço
 * aberta com o projeto em EXECUTANDO_INSTALACAO.
 */
public interface ApontamentoService {

    /**
     * Horas trabalhadas. O {@code valorHora} é o custo da hora ({@code irrigacao.mao-obra.custo-hora})
     * no momento do apontamento, para o custo realizado (RF22).
     */
    ApontamentoHoras apontarHoras(Long ordemId, BigDecimal horas);

    /**
     * Material consumido, só da lista técnica do projeto. A quantidade soma em
     * {@code ItemProjeto.quantidadeApontada}.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se o material não está na lista
     *                                                             técnica
     */
    ApontamentoMaterial apontarMaterial(Long ordemId, Long materialId, BigDecimal quantidade);

    /**
     * Aviso, sem bloquear, quando o apontado do material passou do que foi separado para o projeto.
     *
     * @return o texto do aviso, ou vazio quando o apontado não passa do separado
     */
    Optional<String> avisoDeExcesso(Long projetoId, Long materialId);

    /**
     * Exclui o último lançamento da ordem, de horas ou de material. Se for de material, a quantidade
     * sai de {@code ItemProjeto.quantidadeApontada}.
     *
     * @return o lançamento excluído
     */
    Apontamento excluirUltimo(Long ordemId);

    List<ApontamentoHoras> horasDaOrdem(Long ordemId);

    List<ApontamentoMaterial> materiaisDaOrdem(Long ordemId);

    List<ApontamentoHoras> horasDoProjeto(Long projetoId);

    List<ApontamentoMaterial> materiaisDoProjeto(Long projetoId);
}
