package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import br.edu.iftm.irrigacao.dominio.comercial.ParametrosComerciais;
import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.PropostaService;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
import br.edu.iftm.irrigacao.dominio.comercial.VerificadorDePropostas;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import jakarta.validation.Valid;

/**
 * Orçamento e proposta pelo comercial (RF09 a RF12): prévia e geração, alteração de horas e
 * desconto, pedido de aprovação à diretoria (G2), envio, impressão, resposta do cliente (G3) e a
 * lista de propostas, que antes de abrir vence as que passaram da validade. Tudo sob
 * {@code /comercial} (VENDEDOR, DIRETORIA e ADMINISTRADOR).
 */
@Controller
@RequestMapping("/comercial")
public class PropostaController {

    private final PropostaService propostaService;
    private final ProjetoService projetoService;
    private final VerificadorDePropostas verificador;
    private final ParametrosComerciais parametros;
    private final Clock clock;

    public PropostaController(PropostaService propostaService, ProjetoService projetoService,
            VerificadorDePropostas verificador, ParametrosComerciais parametros, Clock clock) {
        this.propostaService = propostaService;
        this.projetoService = projetoService;
        this.verificador = verificador;
        this.parametros = parametros;
        this.clock = clock;
    }

    // ---------- Prévia e geração (RF09) ----------

    /** Prévia do orçamento com os preços de hoje, item por item, antes de gerar a proposta. */
    @GetMapping("/projetos/{id:\\d+}/orcamento")
    public String previa(@PathVariable Long id, @Valid @ModelAttribute("orcamento") OrcamentoForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        Projeto projeto = projetoService.buscarPorId(id);
        Optional<Proposta> aberta = propostaService.abertaDoProjeto(id);
        if (aberta.isPresent()) {
            redirect.addFlashAttribute("erro", "O projeto já tem a proposta " + aberta.get().getNumero()
                    + " em andamento. Altere essa proposta.");
            return paginaDaProposta(aberta.get().getId());
        }
        if (projeto.getSituacao() != SituacaoProjeto.ORCANDO) {
            redirect.addFlashAttribute("erro", "O orçamento é gerado com o projeto em \"Orçando\".");
            return ProjetoController.paginaDoProjeto(id);
        }
        model.addAttribute("projeto", projeto);
        model.addAttribute("parametros", parametros);
        if (!resultado.hasErrors()) {
            model.addAttribute("previa",
                    propostaService.simular(id, form.getHorasPrevistas(), form.getPercentualDesconto()));
            model.addAttribute("dentroDaAlcada", propostaService.dentroDaAlcada(form.getPercentualDesconto()));
        }
        return "comercial/orcamento";
    }

    @PostMapping("/projetos/{id:\\d+}/propostas")
    public String gerar(@PathVariable Long id, @Valid @ModelAttribute("orcamento") OrcamentoForm form,
            BindingResult resultado, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            redirect.addFlashAttribute("erro", "Confira as horas e o desconto: " + resultado.getFieldError().getDefaultMessage());
            return "redirect:/comercial/projetos/" + id + "/orcamento";
        }
        Proposta proposta = propostaService.gerar(id, form.getHorasPrevistas(), form.getPercentualDesconto());
        redirect.addFlashAttribute("sucesso", "Proposta " + proposta.getNumero() + " gerada, em elaboração.");
        return paginaDaProposta(proposta.getId());
    }

    // ---------- Lista e página da proposta ----------

    /** Lista das propostas em andamento. Antes de abrir, vence as que passaram da validade (RF11). */
    @GetMapping("/propostas")
    public String listar(Model model) {
        int vencidas = verificador.verificar();
        if (vencidas > 0) {
            model.addAttribute("sucesso", vencidas + (vencidas == 1 ? " proposta venceu e foi refeita"
                    : " propostas venceram e foram refeitas") + " com os preços atuais.");
        }
        LocalDate hoje = LocalDate.now(clock);
        model.addAttribute("propostas", propostaService.listarEmAndamento());
        model.addAttribute("hoje", hoje);
        model.addAttribute("limiteAlerta", hoje.plusDays(parametros.getDiasAlerta()));
        return "comercial/listarPropostas";
    }

    @GetMapping("/propostas/{id:\\d+}")
    public String proposta(@PathVariable Long id, Model model) {
        Proposta proposta = propostaService.buscarPorId(id);
        model.addAttribute("orcamento", new OrcamentoForm(proposta));
        return proposta(proposta, model);
    }

    @PostMapping("/propostas/{id:\\d+}")
    public String alterar(@PathVariable Long id, @Valid @ModelAttribute("orcamento") OrcamentoForm form,
            BindingResult resultado, Model model, RedirectAttributes redirect) {
        if (resultado.hasErrors()) {
            return proposta(propostaService.buscarPorId(id), model);
        }
        Proposta proposta = propostaService.alterar(id, form.getHorasPrevistas(), form.getPercentualDesconto());
        redirect.addFlashAttribute("sucesso", "Proposta " + proposta.getNumero() + " alterada.");
        return paginaDaProposta(id);
    }

    @PostMapping("/propostas/{id:\\d+}/pedir-aprovacao")
    public String pedirAprovacao(@PathVariable Long id, RedirectAttributes redirect) {
        Proposta proposta = propostaService.pedirAprovacao(id);
        redirect.addFlashAttribute("sucesso", "Proposta " + proposta.getNumero()
                + " enviada para a aprovação da diretoria.");
        return paginaDaProposta(id);
    }

    @PostMapping("/propostas/{id:\\d+}/enviar")
    public String enviar(@PathVariable Long id, RedirectAttributes redirect) {
        Proposta proposta = propostaService.enviar(id);
        redirect.addFlashAttribute("sucesso", "Proposta " + proposta.getNumero()
                + " enviada ao cliente. Imprima ou gere o PDF pela página de impressão.");
        return paginaDaProposta(id);
    }

    @PostMapping("/propostas/{id:\\d+}/aprovacao-cliente")
    public String registrarAprovacao(@PathVariable Long id, RedirectAttributes redirect) {
        propostaService.registrarAprovacao(id);
        // O registro da aprovação limpa o contexto ao reservar o estoque: o projeto é lido de novo
        Proposta proposta = propostaService.buscarPorId(id);
        redirect.addFlashAttribute("sucesso", "Aprovação do cliente registrada. O projeto "
                + proposta.getProjeto().getCodigo() + " está em \"Preparando material\", com o disponível reservado.");
        return ProjetoController.paginaDoProjeto(proposta.getProjeto().getId());
    }

    @PostMapping("/propostas/{id:\\d+}/recusa")
    public String registrarRecusa(@PathVariable Long id, @RequestParam(required = false) String motivo,
            RedirectAttributes redirect) {
        Proposta proposta = propostaService.registrarRecusa(id, motivo);
        redirect.addFlashAttribute("sucesso", "Recusa do cliente registrada. O projeto foi cancelado.");
        return ProjetoController.paginaDoProjeto(proposta.getProjeto().getId());
    }

    /** Página para imprimir ou salvar em PDF. Só a versão atual, que é a dona dos preços congelados. */
    @GetMapping("/propostas/{id:\\d+}/imprimir")
    public String imprimir(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Proposta proposta = propostaService.buscarPorId(id);
        if (!versaoAtual(proposta)) {
            redirect.addFlashAttribute("erro", "Só a versão atual da proposta pode ser impressa.");
            return paginaDaProposta(id);
        }
        model.addAttribute("proposta", proposta);
        model.addAttribute("detalhe", propostaService.detalhar(id));
        model.addAttribute("validadeDias", proposta.getValidadeDias() == null
                ? parametros.getValidadeDias() : proposta.getValidadeDias());
        return "comercial/imprimirProposta";
    }

    private String proposta(Proposta proposta, Model model) {
        boolean atual = versaoAtual(proposta);
        boolean emElaboracao = proposta.getSituacao() == SituacaoProposta.EM_ELABORACAO;
        boolean dentroDaAlcada = propostaService.dentroDaAlcada(proposta.getPercentualDesconto());
        model.addAttribute("proposta", proposta);
        model.addAttribute("versaoAtual", atual);
        model.addAttribute("detalhe", atual ? propostaService.detalhar(proposta.getId()) : null);
        model.addAttribute("parametros", parametros);
        model.addAttribute("emElaboracao", emElaboracao);
        model.addAttribute("dentroDaAlcada", dentroDaAlcada);
        model.addAttribute("podeEnviar", propostaService.podeEnviar(proposta));
        model.addAttribute("precisaDiretoria", emElaboracao && !dentroDaAlcada && proposta.getAprovadoPor() == null);
        model.addAttribute("negada", emElaboracao && proposta.getAprovadoPor() == null
                && proposta.getObservacaoDiretoria() != null);
        model.addAttribute("vencida", proposta.getSituacao() == SituacaoProposta.ENVIADA
                && LocalDate.now(clock).isAfter(proposta.getDataValidade()));
        return "comercial/proposta";
    }

    private boolean versaoAtual(Proposta proposta) {
        List<Proposta> versoes = propostaService.listarDoProjeto(proposta.getProjeto().getId());
        return !versoes.isEmpty() && versoes.get(0).getId().equals(proposta.getId());
    }

    private static String paginaDaProposta(Long id) {
        return "redirect:/comercial/propostas/" + id;
    }
}
