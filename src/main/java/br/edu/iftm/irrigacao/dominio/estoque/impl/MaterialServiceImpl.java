package br.edu.iftm.irrigacao.dominio.estoque.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.estoque.MaterialService;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;

/**
 * Cadastro de materiais e preços (RF06).
 */
@Service
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;

    public MaterialServiceImpl(MaterialRepository materialRepository) {
        this.materialRepository = materialRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Material> pesquisar(String busca) {
        return materialRepository.pesquisar(padrao(busca));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Material> buscarParaListaTecnica(String busca) {
        if (busca == null || busca.isBlank()) {
            return List.of();
        }
        return materialRepository.pesquisar(padrao(busca), PageRequest.of(0, LIMITE_BUSCA));
    }

    @Override
    @Transactional(readOnly = true)
    public Material buscarPorId(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Material não encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean codigoEmUso(String codigo, Long idIgnorado) {
        String normalizado = normalizarCodigo(codigo);
        if (idIgnorado == null) {
            return materialRepository.existsByCodigo(normalizado);
        }
        return materialRepository.existsByCodigoAndIdNot(normalizado, idIgnorado);
    }

    @Override
    @Transactional
    public Material criar(String codigo, String descricao, String unidade, BigDecimal precoVenda,
            BigDecimal custoUnitario) {
        String normalizado = exigirCodigoLivre(codigo, null);
        Material material = new Material(null, normalizado, obrigatorio(descricao, "Informe a descrição."),
                obrigatorio(unidade, "Informe a unidade."), preco(precoVenda, "preço de venda"),
                preco(custoUnitario, "custo unitário"), BigDecimal.ZERO, BigDecimal.ZERO);
        return materialRepository.save(material);
    }

    @Override
    @Transactional
    public Material atualizar(Long id, String codigo, String descricao, String unidade, BigDecimal precoVenda,
            BigDecimal custoUnitario) {
        Material material = buscarPorId(id);
        material.setCodigo(exigirCodigoLivre(codigo, id));
        material.setDescricao(obrigatorio(descricao, "Informe a descrição."));
        material.setUnidade(obrigatorio(unidade, "Informe a unidade."));
        material.setPrecoVenda(preco(precoVenda, "preço de venda"));
        material.setCustoUnitario(preco(custoUnitario, "custo unitário"));
        // saldo e saldo_reservado têm updatable = false: este save não os inclui no UPDATE
        return materialRepository.save(material);
    }

    private String exigirCodigoLivre(String codigo, Long idIgnorado) {
        String normalizado = normalizarCodigo(obrigatorio(codigo, "Informe o código."));
        if (codigoEmUso(normalizado, idIgnorado)) {
            throw new RegraNegocioException("Já existe um material com o código " + normalizado + ".");
        }
        return normalizado;
    }

    private static String normalizarCodigo(String codigo) {
        return codigo == null ? "" : codigo.strip().toUpperCase(Locale.ROOT);
    }

    private static String padrao(String busca) {
        String termo = busca == null ? "" : busca.strip().toLowerCase(Locale.ROOT);
        return "%" + termo + "%";
    }

    private static String obrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException(mensagem);
        }
        return valor.strip();
    }

    private static BigDecimal preco(BigDecimal valor, String nome) {
        if (valor == null || valor.signum() < 0) {
            throw new RegraNegocioException("Informe o " + nome + ", zero ou mais.");
        }
        return valor;
    }
}
