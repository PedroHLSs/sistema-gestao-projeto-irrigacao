package br.edu.iftm.irrigacao.dominio.estoque;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lista técnica do projeto: os materiais e as quantidades previstas pelo projetista (RF05).
 *
 * <p>Os itens só mudam enquanto o dimensionamento está liberado (projeto em EM_LEVANTAMENTO e
 * levantamento completo). O {@code precoUnitario} do item fica vazio aqui: ele é congelado no
 * orçamento (RF09).
 */
public interface ListaTecnicaService {

    /** Itens do projeto com o material carregado, em ordem de descrição. */
    List<ItemProjeto> listar(Long projetoId);

    /**
     * Total estimado pelo preço de venda atual de cada material: soma de quantidade prevista vezes
     * preço de venda, arredondada em centavos. Material sem preço conta como zero.
     */
    BigDecimal totalEstimado(List<ItemProjeto> itens);

    /**
     * Quanto da quantidade prevista o disponível em estoque do material (saldo menos reservado) não
     * cobre; zero quando cobre (RF05, RF07). Serve só de aviso ao projetista: o que faltar vira
     * faltante para cotação no orçamento (RF08, G4) e não impede a lista técnica.
     */
    BigDecimal faltaEmEstoque(ItemProjeto item);

    /**
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se o material já está na lista,
     *                                                             a quantidade é inválida ou a
     *                                                             engenharia não está liberada
     */
    ItemProjeto adicionar(Long projetoId, Long materialId, BigDecimal quantidadePrevista);

    ItemProjeto alterarQuantidade(Long projetoId, Long itemId, BigDecimal quantidadePrevista);

    void remover(Long projetoId, Long itemId);
}
