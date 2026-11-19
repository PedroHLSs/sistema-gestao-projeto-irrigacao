package br.edu.iftm.irrigacao.dominio.projeto;

import java.util.List;

/**
 * Números da página inicial de cada perfil. Só vêm preenchidas as partes do perfil logado; as demais
 * ficam {@code null}.
 */
public record PainelInicial(Vendedor vendedor, Projetista projetista, Estoque estoque, Tecnico tecnico,
        Financeiro financeiro, Diretoria diretoria) {

    /** Quantos projetos estão numa situação. */
    public record ProjetosNaSituacao(SituacaoProjeto situacao, long total) {
    }

    /** Projetos do vendedor por situação, propostas perto do vencimento e pendências abertas. */
    public record Vendedor(List<ProjetosNaSituacao> projetosPorSituacao, long propostasPertoDoVencimento,
            long pendencias) {
    }

    public record Projetista(long levantamentosCompletos, long projetosEmProgramacao) {
    }

    public record Estoque(long requisicoesAbertas, long separacoesPendentes, long sobrasAReceber) {
    }

    public record Tecnico(long ordensDoDia) {
    }

    /** Projetos prontos para liberar e os que ainda esperam as sobras. */
    public record Financeiro(long projetosParaLiberar, long aguardandoSobras) {
    }

    public record Diretoria(long propostasAguardando, ResumoOrcadoRealizado orcadoRealizado) {
    }
}
