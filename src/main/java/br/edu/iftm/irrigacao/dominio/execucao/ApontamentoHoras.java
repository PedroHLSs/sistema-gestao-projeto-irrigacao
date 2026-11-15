package br.edu.iftm.irrigacao.dominio.execucao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;

/**
 * Apontamento de horas trabalhadas (RF16). {@code valorHora} é o custo da hora no momento do
 * apontamento, guardado para o custo realizado não mudar se o parâmetro mudar depois (RF22).
 */
@Entity
@DiscriminatorValue("HORAS")
public class ApontamentoHoras extends Apontamento {

    @NotNull
    @Column(name = "horas", precision = 8, scale = 2)
    private BigDecimal horas;

    @NotNull
    @Column(name = "valor_hora", precision = 14, scale = 2)
    private BigDecimal valorHora;

    public ApontamentoHoras() {
    }

    public ApontamentoHoras(Long id, OrdemServico ordemServico, LocalDateTime dataHora, Usuario usuario,
            BigDecimal horas, BigDecimal valorHora) {
        super(id, ordemServico, dataHora, usuario);
        this.horas = horas;
        this.valorHora = valorHora;
    }

    public BigDecimal getHoras() {
        return horas;
    }

    public void setHoras(BigDecimal horas) {
        this.horas = horas;
    }

    public BigDecimal getValorHora() {
        return valorHora;
    }

    public void setValorHora(BigDecimal valorHora) {
        this.valorHora = valorHora;
    }
}
