package br.edu.iftm.irrigacao.apresentacao.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioService;
import jakarta.validation.Valid;

/**
 * Gestão de usuários pelo administrador (RF23): listar, criar, editar, ativar, desativar e
 * redefinir a senha. O prefixo {@code /admin} é liberado só para o perfil ADMINISTRADOR no
 * {@code SecurityConfig} (RNF04). Tudo o que altera dado é POST.
 */
@Controller
@RequestMapping("/admin/usuarios")
public class UsuarioController {

    private static final String LISTA = "redirect:/admin/usuarios";

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /** Opções do campo de perfil dos formulários. */
    @ModelAttribute("perfis")
    public Perfil[] perfis() {
        return Perfil.values();
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return "acesso/listarUsuarios";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("usuario", new UsuarioNovoForm());
        return "acesso/cadastrarUsuario";
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("usuario") UsuarioNovoForm form, BindingResult resultado,
            RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return "acesso/cadastrarUsuario";
        }
        Usuario usuario = usuarioService.criar(form.getNome(), form.getLogin(), form.getSenha(), form.getPerfil());
        redirect.addFlashAttribute("sucesso", "Usuário " + usuario.getLogin() + " criado.");
        return LISTA;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", new UsuarioForm(usuarioService.buscarPorId(id)));
        model.addAttribute("senha", new SenhaForm());
        return "acesso/editarUsuario";
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id, @Valid @ModelAttribute("usuario") UsuarioForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("senha", new SenhaForm());
            return "acesso/editarUsuario";
        }
        Usuario usuario = usuarioService.atualizar(id, form.getNome(), form.getLogin(), form.getPerfil());
        redirect.addFlashAttribute("sucesso", "Usuário " + usuario.getLogin() + " alterado.");
        return LISTA;
    }

    @PostMapping("/{id}/senha")
    public String redefinirSenha(@PathVariable Long id, @Valid @ModelAttribute("senha") SenhaForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            model.addAttribute("usuario", new UsuarioForm(usuarioService.buscarPorId(id)));
            return "acesso/editarUsuario";
        }
        usuarioService.redefinirSenha(id, form.getNovaSenha());
        redirect.addFlashAttribute("sucesso", "Senha redefinida.");
        return LISTA;
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, RedirectAttributes redirect) {
        usuarioService.ativar(id);
        redirect.addFlashAttribute("sucesso", "Usuário ativado.");
        return LISTA;
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, RedirectAttributes redirect) {
        usuarioService.desativar(id);
        redirect.addFlashAttribute("sucesso", "Usuário desativado. Esse login não entra mais no sistema.");
        return LISTA;
    }
}
