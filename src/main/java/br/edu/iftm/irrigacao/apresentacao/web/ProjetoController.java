package br.edu.iftm.irrigacao.apresentacao.web;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.ListaTecnicaService;
import br.edu.iftm.irrigacao.dominio.projeto.FotoArmazenada;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.ProjetoService;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Consulta dos projetos, aberta a todos os perfis (RNF04): a lista com filtro por situação e busca,
 * a página central do projeto com a linha do tempo (RF21), a agenda semanal das visitas (RF02) e
 * as fotos do levantamento (RF03).
 *
 * <p>A página do projeto mostra o levantamento e a lista técnica em modo leitura e só oferece as
 * ações que a situação atual permite: agendar a visita (comercial), preencher o levantamento
 * (campo) e dimensionar (engenharia), cada uma na tela do seu perfil.
 */
@Controller
@RequestMapping("/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;
    private final LevantamentoService levantamentoService;
    private final ListaTecnicaService listaTecnicaService;

    public ProjetoController(ProjetoService projetoService, LevantamentoService levantamentoService,
            ListaTecnicaService listaTecnicaService) {
        this.projetoService = projetoService;
        this.levantamentoService = levantamentoService;
        this.listaTecnicaService = listaTecnicaService;
    }

    /**
     * @param situacao só os projetos desta situação; vazio traz todas
     * @param busca    trecho do código ou do nome do produtor
     */
    @GetMapping
    public String listar(@RequestParam(required = false) SituacaoProjeto situacao,
            @RequestParam(required = false) String busca, Model model) {
        model.addAttribute("projetos", projetoService.pesquisar(situacao, busca));
        model.addAttribute("situacoes", SituacaoProjeto.values());
        model.addAttribute("situacao", situacao);
        model.addAttribute("busca", busca);
        return "projetos/listarProjetos";
    }

    @GetMapping("/{id:\\d+}")
    public String detalhe(@PathVariable Long id, Model model) {
        Projeto projeto = projetoService.buscarPorId(id);
        Levantamento levantamento = levantamentoService.buscarPorProjeto(id).orElse(null);
        List<String> faltantes = levantamentoService.camposFaltantes(id);
        List<ItemProjeto> itens = listaTecnicaService.listar(id);
        boolean podePreencher = levantamentoService.podePreencher(projeto);
        model.addAttribute("projeto", projeto);
        model.addAttribute("historico", projetoService.historico(id));
        model.addAttribute("levantamento", levantamento);
        model.addAttribute("faltantes", faltantes);
        model.addAttribute("itens", itens);
        model.addAttribute("total", listaTecnicaService.totalEstimado(itens));
        model.addAttribute("podeAgendarVisita", levantamentoService.podeAgendarVisita(projeto, levantamento));
        model.addAttribute("podePreencherLevantamento", podePreencher);
        model.addAttribute("podeDimensionar", podePreencher && faltantes.isEmpty());
        return "projetos/detalheProjeto";
    }

    /**
     * Foto do levantamento pela posição na lista (RF03). Qualquer usuário logado vê o projeto, e por
     * isso a foto; o arquivo é procurado pelo caminho gravado no levantamento daquele projeto, nunca
     * por um nome vindo da URL.
     */
    @GetMapping("/{id:\\d+}/levantamento/fotos/{indice:\\d+}")
    public ResponseEntity<Resource> foto(@PathVariable Long id, @PathVariable int indice) {
        FotoArmazenada foto = levantamentoService.foto(id, indice);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.tipo().getTipoConteudo()))
                .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
                .body(new FileSystemResource(foto.arquivo()));
    }

    /**
     * @param semana qualquer dia da semana desejada, no formato 2026-11-16; vazio é a semana de hoje
     */
    @GetMapping("/agenda")
    public String agenda(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
            Model model) {
        model.addAttribute("agenda", levantamentoService.agendaDaSemana(semana));
        return "projetos/agenda";
    }

    static String paginaDoProjeto(Long projetoId) {
        return "redirect:/projetos/" + projetoId;
    }
}
