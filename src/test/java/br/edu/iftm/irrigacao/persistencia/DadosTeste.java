package br.edu.iftm.irrigacao.persistencia;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * Objetos prontos para os testes de persistência.
 */
final class DadosTeste {

    private DadosTeste() {
    }

    static Usuario usuario(String login, Perfil perfil) {
        return new Usuario(null, "Usuário " + login, login, "hash-de-teste", perfil, true);
    }

    static Produtor produtor(String cpfCnpj) {
        return new Produtor(null, "João Batista Ferreira", cpfCnpj, "(34) 99999-0000", "joao@exemplo.com");
    }

    static Propriedade propriedade(Produtor produtor) {
        return new Propriedade(null, produtor, "Fazenda Boa Vista", "Uberlândia", -18.9113, -48.2622, 42.5);
    }

    /** Projeto montado pelo construtor vazio, para os valores padrão da entidade valerem. */
    static Projeto projeto(String codigo, Propriedade propriedade, Usuario responsavel) {
        Projeto projeto = new Projeto();
        projeto.setCodigo(codigo);
        projeto.setPropriedade(propriedade);
        projeto.setResponsavel(responsavel);
        projeto.setDataAbertura(LocalDate.of(2026, 3, 2));
        return projeto;
    }

    static Material material(String codigo, String descricao, String saldo, String saldoReservado) {
        return new Material(null, codigo, descricao, "un", new BigDecimal("10.00"), new BigDecimal("6.00"),
                new BigDecimal(saldo), new BigDecimal(saldoReservado));
    }

    /** Grava vendedor, produtor, propriedade e projeto, e devolve o projeto. */
    static Projeto projetoGravado(TestEntityManager em, String codigo) {
        Usuario vendedor = em.persist(usuario("vendedor-" + codigo, Perfil.VENDEDOR));
        Produtor produtor = em.persist(produtor("cpf-" + codigo));
        Propriedade propriedade = em.persist(propriedade(produtor));
        return em.persist(projeto(codigo, propriedade, vendedor));
    }
}
