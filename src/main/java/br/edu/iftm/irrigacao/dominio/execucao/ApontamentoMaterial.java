package br.edu.iftm.irrigacao.dominio.execucao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

/**
 * Apontamento de material consumido na instalação (RF16).
 */
@Entity
@DiscriminatorValue("MATERIAL")
public class ApontamentoMaterial extends Apontamento {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id")
    private Material material;

    @NotNull
    @Column(name = "quantidade", precision = 14, scale = 3)
    private BigDecimal quantidade;

    public ApontamentoMaterial() {
    }

    public ApontamentoMaterial(Long id, OrdemServico ordemServico, LocalDateTime dataHora, Usuario usuario,
            Material material, BigDecimal quantidade) {
        super(id, ordemServico, dataHora, usuario);
        this.material = material;
        this.quantidade = quantidade;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public BigDecimal getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(BigDecimal quantidade) {
        this.quantidade = quantidade;
    }
}
