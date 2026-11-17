package br.edu.iftm.irrigacao.dominio.comercial.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.comercial.CpfCnpj;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.ProdutorService;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.persistencia.ProdutorRepository;
import br.edu.iftm.irrigacao.persistencia.PropriedadeRepository;

/**
 * Cadastro de produtores e propriedades (RF01).
 */
@Service
public class ProdutorServiceImpl implements ProdutorService {

    private final ProdutorRepository produtorRepository;
    private final PropriedadeRepository propriedadeRepository;

    public ProdutorServiceImpl(ProdutorRepository produtorRepository, PropriedadeRepository propriedadeRepository) {
        this.produtorRepository = produtorRepository;
        this.propriedadeRepository = propriedadeRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produtor> pesquisar(String busca) {
        String termo = busca == null ? "" : busca.strip();
        if (termo.isEmpty()) {
            return produtorRepository.findAllByOrderByNome();
        }
        // Só procura no CPF/CNPJ quando a busca tem número: "João" não deve casar com documento nenhum
        String documento = CpfCnpj.normalizar(termo);
        boolean temNumero = documento.chars().anyMatch(Character::isDigit);
        return produtorRepository.pesquisar("%" + termo.toLowerCase(Locale.ROOT) + "%",
                temNumero ? "%" + documento + "%" : "");
    }

    @Override
    @Transactional(readOnly = true)
    public Produtor buscarPorId(Long id) {
        return produtorRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Produtor não encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean cpfCnpjEmUso(String cpfCnpj, Long idIgnorado) {
        String documento = CpfCnpj.normalizar(cpfCnpj);
        if (idIgnorado == null) {
            return produtorRepository.existsByCpfCnpj(documento);
        }
        return produtorRepository.existsByCpfCnpjAndIdNot(documento, idIgnorado);
    }

    @Override
    @Transactional
    public Produtor criar(String nome, String cpfCnpj, String telefone, String email) {
        String documento = exigirCpfCnpjValidoELivre(cpfCnpj, null);
        Produtor produtor = new Produtor(null, obrigatorio(nome, "Informe o nome do produtor."), documento,
                opcional(telefone), opcional(email));
        return produtorRepository.save(produtor);
    }

    @Override
    @Transactional
    public Produtor atualizar(Long id, String nome, String cpfCnpj, String telefone, String email) {
        Produtor produtor = buscarPorId(id);
        String documento = exigirCpfCnpjValidoELivre(cpfCnpj, id);
        produtor.setNome(obrigatorio(nome, "Informe o nome do produtor."));
        produtor.setCpfCnpj(documento);
        produtor.setTelefone(opcional(telefone));
        produtor.setEmail(opcional(email));
        return produtorRepository.save(produtor);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Propriedade> listarPropriedades(Long produtorId) {
        return propriedadeRepository.findByProdutorIdOrderByNome(produtorId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Propriedade> listarPropriedadesComProdutor() {
        return propriedadeRepository.listarComProdutor();
    }

    @Override
    @Transactional(readOnly = true)
    public Propriedade buscarPropriedade(Long produtorId, Long propriedadeId) {
        return propriedadeRepository.findByIdAndProdutorId(propriedadeId, produtorId)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Propriedade não encontrada."));
    }

    @Override
    @Transactional
    public Propriedade criarPropriedade(Long produtorId, String nome, String municipio, Double latitude,
            Double longitude, Double areaTotalHa) {
        Produtor produtor = buscarPorId(produtorId);
        Propriedade propriedade = new Propriedade(null, produtor, obrigatorio(nome, "Informe o nome da propriedade."),
                obrigatorio(municipio, "Informe o município."), latitude, longitude, areaTotalHa);
        return propriedadeRepository.save(propriedade);
    }

    @Override
    @Transactional
    public Propriedade atualizarPropriedade(Long produtorId, Long propriedadeId, String nome, String municipio,
            Double latitude, Double longitude, Double areaTotalHa) {
        Propriedade propriedade = buscarPropriedade(produtorId, propriedadeId);
        propriedade.setNome(obrigatorio(nome, "Informe o nome da propriedade."));
        propriedade.setMunicipio(obrigatorio(municipio, "Informe o município."));
        propriedade.setLatitude(latitude);
        propriedade.setLongitude(longitude);
        propriedade.setAreaTotalHa(areaTotalHa);
        return propriedadeRepository.save(propriedade);
    }

    /** @return o documento normalizado, pronto para gravar */
    private String exigirCpfCnpjValidoELivre(String cpfCnpj, Long idIgnorado) {
        if (!CpfCnpj.valido(cpfCnpj)) {
            throw new RegraNegocioException("CPF ou CNPJ inválido. Confira os números digitados.");
        }
        if (cpfCnpjEmUso(cpfCnpj, idIgnorado)) {
            throw new RegraNegocioException("Já existe um produtor com o CPF/CNPJ " + CpfCnpj.formatar(cpfCnpj) + ".");
        }
        return CpfCnpj.normalizar(cpfCnpj);
    }

    private static String obrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new RegraNegocioException(mensagem);
        }
        return valor.strip();
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }
}
