package br.edu.iftm.irrigacao.apresentacao.web;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import br.edu.iftm.irrigacao.dominio.projeto.Apuracao;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoCsv;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoLinha;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoService;
import jakarta.validation.Valid;

/**
 * Orçado x realizado dos projetos encerrados (RF22), com filtro por período e exportação em CSV. Fica
 * sob {@code /diretoria} (DIRETORIA e ADMINISTRADOR).
 */
@Controller
@RequestMapping("/diretoria/orcado-realizado")
public class OrcadoRealizadoController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final OrcadoRealizadoService orcadoRealizadoService;

    public OrcadoRealizadoController(OrcadoRealizadoService orcadoRealizadoService) {
        this.orcadoRealizadoService = orcadoRealizadoService;
    }

    @GetMapping
    public String listar(@Valid @ModelAttribute("filtro") PeriodoFiltro filtro, BindingResult resultado, Model model) {
        List<OrcadoRealizadoLinha> linhas = resultado.hasErrors() ? List.of()
                : orcadoRealizadoService.encerrados(filtro.getDe(), filtro.getAte());
        model.addAttribute("linhas", linhas);
        model.addAttribute("total", new Apuracao(somar(linhas, Apuracao::valorTotal), somar(linhas, Apuracao::custoOrcado),
                somar(linhas, Apuracao::custoRealizado)));
        return "diretoria/orcadoRealizado";
    }

    /** O arquivo sai em bytes para o BOM e os acentos não dependerem da codificação da resposta. */
    @GetMapping("/exportar")
    public ResponseEntity<byte[]> exportar(@Valid @ModelAttribute("filtro") PeriodoFiltro filtro,
            BindingResult resultado) {
        if (resultado.hasErrors()) {
            return ResponseEntity.badRequest().build();
        }
        byte[] arquivo = OrcadoRealizadoCsv.gerar(orcadoRealizadoService.encerrados(filtro.getDe(), filtro.getAte()));
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("orcado-realizado.csv").build().toString())
                .body(arquivo);
    }

    private static BigDecimal somar(List<OrcadoRealizadoLinha> linhas, Function<Apuracao, BigDecimal> valor) {
        return linhas.stream().map(linha -> valor.apply(linha.apuracao())).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
