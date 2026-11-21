package br.edu.iftm.irrigacao.dominio.estoque;

import java.util.List;

/**
 * Andamento da separação do material do projeto (RF14). A separação é feita aos poucos, em quantas
 * gravações forem precisas; aqui fica quantos itens da lista técnica já chegaram na quantidade
 * prevista e quantos já começaram. A conferência (G5) só passa com todos completos.
 */
public class ProgressoSeparacao {

    private final int total;
    private final int completos;
    private final int emAndamento;
    private final int itensAlterados;

    /** Andamento atual, sem gravação. */
    public ProgressoSeparacao(List<ItemProjeto> itens) {
        this(itens, 0);
    }

    /**
     * @param itens          todos os itens da lista técnica do projeto, já com a separada gravada
     * @param itensAlterados quantos itens mudaram na gravação que acabou de ser feita
     */
    public ProgressoSeparacao(List<ItemProjeto> itens, int itensAlterados) {
        this.total = itens.size();
        this.completos = (int) itens.stream().filter(ProgressoSeparacao::completo).count();
        this.emAndamento = (int) itens.stream()
                .filter(item -> item.getQuantidadeSeparada().signum() > 0 && !completo(item))
                .count();
        this.itensAlterados = itensAlterados;
    }

    private static boolean completo(ItemProjeto item) {
        return item.getQuantidadeSeparada().compareTo(item.getQuantidadePrevista()) == 0;
    }

    public int getTotal() {
        return total;
    }

    /** Itens com a separada igual à prevista. */
    public int getCompletos() {
        return completos;
    }

    /** Itens com alguma quantidade separada que ainda não bate com a prevista. */
    public int getEmAndamento() {
        return emAndamento;
    }

    /** Itens que ainda não estão completos. */
    public int getFaltam() {
        return total - completos;
    }

    public int getItensAlterados() {
        return itensAlterados;
    }

    /** Todos os itens separados na quantidade prevista: pronto para conferir. */
    public boolean isCompleta() {
        return total > 0 && completos == total;
    }

    /** Itens completos sobre o total, de 0 a 100, para a barra de andamento. */
    public int getPercentual() {
        return total == 0 ? 0 : completos * 100 / total;
    }
}
