package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;

import br.edu.iftm.irrigacao.dominio.estoque.Material;

/**
 * Material como sai no JSON da busca {@code /estoque/materiais/busca}, usada pela lista técnica
 * (RF05, RF06). Só os campos que a tela precisa; a entidade nunca vai para o JSON.
 *
 * @param disponivel saldo menos reservado
 */
public record MaterialResumo(Long id, String codigo, String descricao, String unidade, BigDecimal precoVenda,
        BigDecimal disponivel) {

    public static MaterialResumo de(Material material) {
        return new MaterialResumo(material.getId(), material.getCodigo(), material.getDescricao(),
                material.getUnidade(), material.getPrecoVenda(),
                material.getSaldo().subtract(material.getSaldoReservado()));
    }
}
