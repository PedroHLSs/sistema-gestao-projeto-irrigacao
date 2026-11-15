package br.edu.iftm.irrigacao.dominio.acesso;

import java.util.List;

/**
 * Gestão de usuários e perfis de acesso (RF23). Só o administrador cria usuários: não existe
 * auto-registro.
 */
public interface UsuarioService {

    /** Tamanho mínimo da senha, em caracteres. */
    int SENHA_TAMANHO_MINIMO = 8;

    /** Tamanho máximo da senha, em caracteres. O BCrypt não aceita senha com mais de 72 bytes. */
    int SENHA_TAMANHO_MAXIMO = 72;

    /** Todos os usuários, ativos e inativos, em ordem de nome. */
    List<Usuario> listar();

    /** @throws br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException se o id não existir */
    Usuario buscarPorId(Long id);

    /**
     * @param idIgnorado id do usuário que está sendo editado, para o login dele mesmo não contar;
     *                   {@code null} na inclusão
     */
    boolean loginEmUso(String login, Long idIgnorado);

    /** Cria um usuário ativo, com a senha gravada em BCrypt. */
    Usuario criar(String nome, String login, String senha, Perfil perfil);

    /** Altera nome, login e perfil. O administrador não pode tirar o próprio perfil de ADMINISTRADOR. */
    Usuario atualizar(Long id, String nome, String login, Perfil perfil);

    void ativar(Long id);

    /** O usuário inativo não entra mais no sistema. O administrador não pode desativar a si mesmo. */
    void desativar(Long id);

    void redefinirSenha(Long id, String novaSenha);
}
