package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado da conferência da separação contra a lista técnica (RF14, G5): conferida, ou a lista dos
 * itens em que a quantidade separada não bate com a prevista.
 */
public class ResultadoConferencia {

    private final List<Divergencia> divergencias;

    public ResultadoConferencia(List<Divergencia> divergencias) {
        this.divergencias = List.copyOf(divergencias);
    }

    /** A separação bateu com a lista técnica e o material saiu do estoque. */
    public boolean isConferida() {
        return divergencias.isEmpty();
    }

    public List<Divergencia> getDivergencias() {
        return divergencias;
    }

    /** Um item em que a separada difere da prevista. */
    public static class Divergencia {

        private final ItemProjeto item;
        private final BigDecimal prevista;
        private final BigDecimal separada;

        public Divergencia(ItemProjeto item) {
            this.item = item;
            this.prevista = item.getQuantidadePrevista();
            this.separada = item.getQuantidadeSeparada();
        }

        public ItemProjeto getItem() {
            return item;
        }

        public BigDecimal getPrevista() {
            return prevista;
        }

        public BigDecimal getSeparada() {
            return separada;
        }

        /** Separada menos prevista: negativa quando falta, positiva quando sobra. */
        public BigDecimal getDiferenca() {
            return separada.subtract(prevista);
        }

        /** Como a divergência aparece para o almoxarife, com a diferença com sinal. */
        public String getDescricao() {
            String unidade = " " + item.getMaterial().getUnidade();
            return item.getMaterial().getCodigo() + ": prevista " + Quantidade.formatar(prevista) + unidade
                    + ", separada " + Quantidade.formatar(separada) + unidade + " ("
                    + Quantidade.comSinal(getDiferenca()) + ")";
        }
    }
}
