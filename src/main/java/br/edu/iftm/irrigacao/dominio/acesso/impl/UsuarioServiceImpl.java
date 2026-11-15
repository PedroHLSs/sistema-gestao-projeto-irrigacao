package br.edu.iftm.irrigacao.dominio.acesso.impl;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioAutenticado;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Gestão de usuários (RF23) e carga do usuário para o login do Spring Security (RNF04).
 */
@Service
public class UsuarioServiceImpl implements UsuarioService, UserDetailsService {

    /** O BCrypt só considera os primeiros 72 bytes da senha, e o encoder recusa o que passa disso. */
    private static final int SENHA_BYTES_MAXIMO = 72;

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UsuarioLogado usuarioLogado;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, BCryptPasswordEncoder passwordEncoder,
            UsuarioLogado usuarioLogado) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioLogado = usuarioLogado;
    }

    /**
     * Usado pelo Spring Security no login (RNF04). Só carrega usuário ativo: para o inativo a
     * resposta é a mesma de um login que não existe.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue(login)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado ou inativo: " + login));
        return new UsuarioAutenticado(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAllByOrderByNome();
    }

    @Override
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Usuário não encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean loginEmUso(String login, Long idIgnorado) {
        if (idIgnorado == null) {
            return usuarioRepository.existsByLogin(login);
        }
        return usuarioRepository.existsByLoginAndIdNot(login, idIgnorado);
    }

    @Override
    @Transactional
    public Usuario criar(String nome, String login, String senha, Perfil perfil) {
        exigirLoginLivre(login, null);
        Usuario usuario = new Usuario(null, nome.strip(), login, codificar(senha), perfil, true);
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public Usuario atualizar(Long id, String nome, String login, Perfil perfil) {
        Usuario usuario = buscarPorId(id);
        exigirLoginLivre(login, id);
        if (ehOUsuarioLogado(id) && usuario.getPerfil() == Perfil.ADMINISTRADOR && perfil != Perfil.ADMINISTRADOR) {
            throw new RegraNegocioException("Você não pode tirar o seu próprio perfil de Administrador.");
        }
        usuario.setNome(nome.strip());
        usuario.setLogin(login);
        usuario.setPerfil(perfil);
        return usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void ativar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuario.setAtivo(true);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void desativar(Long id) {
        Usuario usuario = buscarPorId(id);
        if (ehOUsuarioLogado(id)) {
            throw new RegraNegocioException("Você não pode desativar o seu próprio usuário.");
        }
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
    }

    @Override
    @Transactional
    public void redefinirSenha(Long id, String novaSenha) {
        Usuario usuario = buscarPorId(id);
        usuario.setSenhaHash(codificar(novaSenha));
        usuarioRepository.save(usuario);
    }

    private boolean ehOUsuarioLogado(Long id) {
        return usuarioLogado.idAtual().filter(id::equals).isPresent();
    }

    private void exigirLoginLivre(String login, Long idIgnorado) {
        if (loginEmUso(login, idIgnorado)) {
            throw new RegraNegocioException("Já existe um usuário com o login \"" + login + "\".");
        }
    }

    private String codificar(String senha) {
        if (senha == null || senha.length() < SENHA_TAMANHO_MINIMO) {
            throw new RegraNegocioException("A senha deve ter no mínimo " + SENHA_TAMANHO_MINIMO + " caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_BYTES_MAXIMO) {
            throw new RegraNegocioException("A senha é longa demais. Use até " + SENHA_TAMANHO_MAXIMO
                    + " caracteres (letra acentuada conta como dois).");
        }
        return passwordEncoder.encode(senha);
    }
}
