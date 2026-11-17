package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.execucao.Apontamento;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoHoras;
import br.edu.iftm.irrigacao.dominio.execucao.ApontamentoMaterial;

/**
 * Um apontamento da ordem (RF16) pronto para a lista da tela: horas e material juntos, em ordem de
 * lançamento, com o texto que o técnico lê.
 */
public class LancamentoDaOrdem {

    private static final Comparator<Apontamento> ORDEM_DE_LANCAMENTO = Comparator
            .comparing(Apontamento::getDataHora)
            .thenComparing(Apontamento::getId);

    private final LocalDateTime dataHora;
    private final boolean horas;
    private final String descricao;
    private final String autor;

    public LancamentoDaOrdem(LocalDateTime dataHora, boolean horas, String descricao, String autor) {
        this.dataHora = dataHora;
        this.horas = horas;
        this.descricao = descricao;
        this.autor = autor;
    }

    /** Horas e materiais da ordem numa lista só, do primeiro lançamento para o último. */
    public static List<LancamentoDaOrdem> juntar(List<ApontamentoHoras> horas, List<ApontamentoMaterial> materiais) {
        List<Apontamento> todos = new ArrayList<>(horas);
        todos.addAll(materiais);
        todos.sort(ORDEM_DE_LANCAMENTO);
        return todos.stream().map(LancamentoDaOrdem::de).toList();
    }

    private static LancamentoDaOrdem de(Apontamento apontamento) {
        String autor = apontamento.getUsuario() == null ? null : apontamento.getUsuario().getNome();
        if (apontamento instanceof ApontamentoHoras horas) {
            return new LancamentoDaOrdem(horas.getDataHora(), true, Quantidade.formatar(horas.getHoras()) + " h", autor);
        }
        ApontamentoMaterial material = (ApontamentoMaterial) apontamento;
        BigDecimal quantidade = material.getQuantidade();
        return new LancamentoDaOrdem(material.getDataHora(), false, Quantidade.formatar(quantidade) + " "
                + material.getMaterial().getUnidade() + " de " + material.getMaterial().getCodigo() + " · "
                + material.getMaterial().getDescricao(), autor);
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public boolean isHoras() {
        return horas;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getAutor() {
        return autor;
    }
}
