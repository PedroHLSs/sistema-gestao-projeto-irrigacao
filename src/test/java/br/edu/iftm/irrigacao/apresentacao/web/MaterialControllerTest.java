package br.edu.iftm.irrigacao.apresentacao.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.edu.iftm.irrigacao.config.SecurityConfig;
import br.edu.iftm.irrigacao.config.TestConfig;
import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.estoque.MaterialService;
import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;
import br.edu.iftm.irrigacao.dominio.estoque.SaldoInsuficienteException;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * Telas de estoque (RF06, RF07) com os services mockados.
 */
@WebMvcTest(MaterialController.class)
@Import({ TestConfig.class, SecurityConfig.class })
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MaterialService materialService;

    @MockitoBean
    private EstoqueService estoqueService;

    private static Material tubo() {
        return new Material(1L, "TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("62.90"),
                new BigDecimal("44.80"), new BigDecimal("120.000"), new BigDecimal("20.000"));
    }

    private static Projeto projeto() {
        Projeto projeto = new Projeto();
        projeto.setId(7L);
        projeto.setCodigo("PRJ-2026-0001");
        return projeto;
    }

    private static MovimentacaoEstoque movimentacao(TipoMovimentacao tipo, String quantidade, Projeto projeto) {
        Usuario almir = new Usuario(3L, "Almir Almoxarife", "almir", "hash", Perfil.ALMOXARIFE, true);
        return new MovimentacaoEstoque(1L, tubo(), projeto, tipo, new BigDecimal(quantidade),
                LocalDateTime.of(2026, 11, 16, 9, 30), almir);
    }

    // ---------- Lista e cadastro ----------

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("GET /estoque/materiais - Lista com preço, saldo, reservado e disponível")
    void testListar() throws Exception {
        when(materialService.pesquisar("pvc")).thenReturn(List.of(tubo()));

        mockMvc.perform(get("/estoque/materiais").param("busca", "pvc"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque/listarMateriais"))
                .andExpect(content().string(containsString("TB-PVC-050")))
                .andExpect(content().string(containsString("R$ 62,90")))
                .andExpect(content().string(containsString("R$ 44,80")))
                .andExpect(content().string(containsString(">120<")))
                .andExpect(content().string(containsString(">20<")))
                .andExpect(content().string(containsString(">100<")))
                .andExpect(content().string(containsString("/estoque/materiais/1/extrato")));
        verify(materialService).pesquisar("pvc");
    }

    @Test
    @WithMockUser(authorities = "COMPRADOR")
    @DisplayName("GET /estoque/materiais - Busca sem resultado mostra a mensagem")
    void testListarVazio() throws Exception {
        when(materialService.pesquisar("xyz")).thenReturn(List.of());

        mockMvc.perform(get("/estoque/materiais").param("busca", "xyz"))
                .andExpect(content().string(containsString("Nenhum material encontrado para essa busca.")));
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("GET /estoque/materiais/novo - O formulário não tem campo de saldo")
    void testFormularioNovo() throws Exception {
        mockMvc.perform(get("/estoque/materiais/novo"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque/formMaterial"))
                .andExpect(content().string(containsString("name=\"codigo\"")))
                .andExpect(content().string(containsString("name=\"precoVenda\"")))
                .andExpect(content().string(containsString("name=\"custoUnitario\"")))
                .andExpect(content().string(not(containsString("name=\"saldo\""))))
                .andExpect(content().string(not(containsString("name=\"saldoReservado\""))));
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("POST /estoque/materiais - Cria o material e volta para a lista")
    void testCriar() throws Exception {
        when(materialService.criar("TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("62.90"),
                new BigDecimal("44.80"))).thenReturn(tubo());

        mockMvc.perform(post("/estoque/materiais").with(csrf())
                        .param("codigo", "TB-PVC-050")
                        .param("descricao", "Tubo PVC 50 mm")
                        .param("unidade", "barra")
                        .param("precoVenda", "62.90")
                        .param("custoUnitario", "44.80"))
                .andExpect(redirectedUrl("/estoque/materiais"))
                .andExpect(flash().attribute("sucesso",
                        "Material TB-PVC-050 cadastrado com saldo zero. O saldo entra pela entrada de compra."));

        verify(materialService).criar("TB-PVC-050", "Tubo PVC 50 mm", "barra", new BigDecimal("62.90"),
                new BigDecimal("44.80"));
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("POST /estoque/materiais - Campos vazios, preço negativo e casas demais voltam ao formulário")
    void testCriarInvalido() throws Exception {
        mockMvc.perform(post("/estoque/materiais").with(csrf())
                        .param("codigo", "")
                        .param("descricao", "")
                        .param("unidade", "unidade-longa")
                        .param("precoVenda", "-1")
                        .param("custoUnitario", "1.234"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque/formMaterial"))
                .andExpect(model().attributeHasFieldErrors("material", "codigo", "descricao", "unidade",
                        "precoVenda", "custoUnitario"))
                .andExpect(content().string(containsString("O preço de venda não pode ser negativo.")))
                .andExpect(content().string(containsString("Use até 12 dígitos e 2 casas decimais.")));

        verify(materialService, never()).criar(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("POST /estoque/materiais - Preço com vírgula aparece como erro de número no campo")
    void testCriarComVirgula() throws Exception {
        mockMvc.perform(post("/estoque/materiais").with(csrf())
                        .param("codigo", "TB-PVC-050")
                        .param("descricao", "Tubo")
                        .param("unidade", "barra")
                        .param("precoVenda", "62,90")
                        .param("custoUnitario", "44.80"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("material", "precoVenda"))
                .andExpect(content().string(containsString("Informe um número, com ponto antes das casas decimais")));
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("POST /estoque/materiais - Código de outro material aparece como erro no campo")
    void testCriarCodigoRepetido() throws Exception {
        when(materialService.codigoEmUso("TB-PVC-050", null)).thenReturn(true);

        mockMvc.perform(post("/estoque/materiais").with(csrf())
                        .param("codigo", "TB-PVC-050")
                        .param("descricao", "Outro tubo")
                        .param("unidade", "barra")
                        .param("precoVenda", "1")
                        .param("custoUnitario", "1"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("material", "codigo"))
                .andExpect(content().string(containsString("Já existe um material com este código.")));

        verify(materialService, never()).criar(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("GET e POST de edição - Formulário preenchido e alteração pelo id da URL")
    void testEditar() throws Exception {
        when(materialService.buscarPorId(1L)).thenReturn(tubo());
        when(materialService.atualizar(1L, "TB-PVC-050", "Tubo PVC 50 mm, barra 6 m", "barra",
                new BigDecimal("65.00"), new BigDecimal("46.00"))).thenReturn(tubo());

        mockMvc.perform(get("/estoque/materiais/1/editar"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"TB-PVC-050\"")))
                .andExpect(content().string(containsString("value=\"62.90\"")))
                .andExpect(content().string(containsString("action=\"/estoque/materiais/1\"")));

        mockMvc.perform(post("/estoque/materiais/1").with(csrf())
                        .param("codigo", "TB-PVC-050")
                        .param("descricao", "Tubo PVC 50 mm, barra 6 m")
                        .param("unidade", "barra")
                        .param("precoVenda", "65.00")
                        .param("custoUnitario", "46.00"))
                .andExpect(redirectedUrl("/estoque/materiais"))
                .andExpect(flash().attribute("sucesso", "Material TB-PVC-050 alterado."));

        // o código do próprio material não conta como repetido
        verify(materialService).codigoEmUso("TB-PVC-050", 1L);
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("Material que não existe responde 404")
    void testMaterialInexistente() throws Exception {
        when(materialService.buscarPorId(99L)).thenThrow(new RegistroNaoEncontradoException("Material não encontrado."));

        mockMvc.perform(get("/estoque/materiais/99/editar")).andExpect(status().isNotFound());
        mockMvc.perform(get("/estoque/materiais/99/extrato")).andExpect(status().isNotFound());
    }

    // ---------- Entrada e ajuste ----------

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("Entrada de compra - Formulário com o saldo atual e registro da quantidade")
    void testEntrada() throws Exception {
        when(materialService.buscarPorId(1L)).thenReturn(tubo());

        mockMvc.perform(get("/estoque/materiais/1/entrada"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque/entradaMaterial"))
                .andExpect(content().string(containsString("Quantidade recebida")))
                .andExpect(content().string(containsString("Disponível")));

        mockMvc.perform(post("/estoque/materiais/1/entrada").with(csrf()).param("quantidade", "15.5"))
                .andExpect(redirectedUrl("/estoque/materiais/1/extrato"))
                .andExpect(flash().attribute("sucesso",
                        "Entrada de 15,5 barra de TB-PVC-050 registrada. Saldo agora: 120 barra."));

        verify(estoqueService).entradaCompra(1L, new BigDecimal("15.5"));
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("Entrada de compra - Quantidade zero, com 4 casas ou texto volta com o erro no campo")
    void testEntradaInvalida() throws Exception {
        when(materialService.buscarPorId(1L)).thenReturn(tubo());

        for (String invalida : List.of("0", "1.2345", "abc", "")) {
            mockMvc.perform(post("/estoque/materiais/1/entrada").with(csrf()).param("quantidade", invalida))
                    .andExpect(status().isOk())
                    .andExpect(view().name("estoque/entradaMaterial"))
                    .andExpect(model().attributeHasFieldErrors("entrada", "quantidade"));
        }
        verify(estoqueService, never()).entradaCompra(anyLong(), any());
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("Ajuste - Formulário vem com o saldo atual e o resultado mostra a diferença")
    void testAjuste() throws Exception {
        when(materialService.buscarPorId(1L)).thenReturn(tubo());
        when(estoqueService.ajuste(1L, new BigDecimal("130"))).thenReturn(movimentacao(TipoMovimentacao.AJUSTE, "10", null));

        mockMvc.perform(get("/estoque/materiais/1/ajuste"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"120.000\"")));

        mockMvc.perform(post("/estoque/materiais/1/ajuste").with(csrf()).param("novoSaldo", "130"))
                .andExpect(redirectedUrl("/estoque/materiais/1/extrato"))
                .andExpect(flash().attribute("sucesso", "Saldo de TB-PVC-050 ajustado para 120 barra (+10)."));
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("Ajuste - Abaixo do reservado volta à página com a mensagem do service no topo")
    void testAjusteAbaixoDoReservado() throws Exception {
        when(estoqueService.ajuste(1L, new BigDecimal("10")))
                .thenThrow(new SaldoInsuficienteException("O saldo de TB-PVC-050 (Tubo PVC 50 mm) não pode ficar "
                        + "abaixo do reservado, que é 20 barra.", new BigDecimal("20")));

        mockMvc.perform(post("/estoque/materiais/1/ajuste").with(csrf())
                        .header(HttpHeaders.REFERER, "http://localhost/estoque/materiais/1/ajuste")
                        .param("novoSaldo", "10"))
                .andExpect(redirectedUrl("/estoque/materiais/1/ajuste"))
                .andExpect(flash().attribute("erro", "O saldo de TB-PVC-050 (Tubo PVC 50 mm) não pode ficar "
                        + "abaixo do reservado, que é 20 barra."));
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("Entrada e ajuste só por POST com CSRF")
    void testSemCsrf() throws Exception {
        mockMvc.perform(post("/estoque/materiais/1/entrada").param("quantidade", "1"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/estoque/materiais/1/ajuste").param("novoSaldo", "1"))
                .andExpect(status().isForbidden());
        verify(estoqueService, never()).entradaCompra(anyLong(), any());
        verify(estoqueService, never()).ajuste(anyLong(), any());
    }

    // ---------- Extrato ----------

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("Extrato - Movimentações com o efeito no saldo e no reservado, filtros e paginação")
    void testExtrato() throws Exception {
        when(materialService.buscarPorId(1L)).thenReturn(tubo());
        when(estoqueService.projetosDoMaterial(1L)).thenReturn(List.of(projeto()));
        PageRequest segundaPagina = PageRequest.of(1, 20);
        when(estoqueService.extrato(1L, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 7L, segundaPagina))
                .thenReturn(new PageImpl<>(List.of(
                        movimentacao(TipoMovimentacao.SAIDA_PROJETO, "5", projeto()),
                        movimentacao(TipoMovimentacao.RESERVA, "8", projeto())), segundaPagina, 45));

        mockMvc.perform(get("/estoque/materiais/1/extrato")
                        .param("de", "2026-11-01")
                        .param("ate", "2026-11-30")
                        .param("projetoId", "7")
                        .param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("estoque/extratoMaterial"))
                .andExpect(content().string(containsString("Saída para projeto")))
                .andExpect(content().string(containsString("Reserva")))
                .andExpect(content().string(containsString(">-5<")))
                .andExpect(content().string(containsString(">+8<")))
                .andExpect(content().string(containsString("16/11/2026 09:30")))
                .andExpect(content().string(containsString("PRJ-2026-0001")))
                .andExpect(content().string(containsString("Almir Almoxarife")))
                .andExpect(content().string(containsString("Página 2 de 3 · 45 movimentações")))
                .andExpect(content().string(containsString("pagina=0")))
                .andExpect(content().string(containsString("pagina=2")))
                .andExpect(content().string(containsString("projetoId=7")));
    }

    @Test
    @WithMockUser(authorities = "ALMOXARIFE")
    @DisplayName("Extrato - Data final antes da inicial aparece no campo e a consulta não é feita")
    void testExtratoPeriodoInvalido() throws Exception {
        when(materialService.buscarPorId(1L)).thenReturn(tubo());

        mockMvc.perform(get("/estoque/materiais/1/extrato").param("de", "2026-11-20").param("ate", "2026-11-10"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("filtro", "periodoValido"))
                .andExpect(content().string(containsString("A data final não pode ser anterior à inicial.")))
                .andExpect(content().string(containsString("Nenhuma movimentação no período.")));

        verify(estoqueService, never()).extrato(anyLong(), any(), any(), any(), any());
    }

    // ---------- Busca em JSON ----------

    @Test
    @WithMockUser(authorities = "PROJETISTA")
    @DisplayName("GET /estoque/materiais/busca - JSON com o resumo do material, nunca a entidade")
    void testBuscaJson() throws Exception {
        when(materialService.buscarParaListaTecnica("pvc")).thenReturn(List.of(tubo()));

        mockMvc.perform(get("/estoque/materiais/busca").param("q", "pvc"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].codigo").value("TB-PVC-050"))
                .andExpect(jsonPath("$[0].descricao").value("Tubo PVC 50 mm"))
                .andExpect(jsonPath("$[0].unidade").value("barra"))
                .andExpect(jsonPath("$[0].precoVenda").value(62.9))
                .andExpect(jsonPath("$[0].disponivel").value(100.0))
                .andExpect(jsonPath("$[0].saldo").doesNotExist())
                .andExpect(jsonPath("$[0].custoUnitario").doesNotExist())
                .andExpect(jsonPath("$[0].criadoPor").doesNotExist());
    }

    // ---------- Acesso ----------

    @Test
    @WithMockUser(authorities = "PROJETISTA")
    @DisplayName("Projetista usa a busca em JSON, mas não entra nas telas de estoque")
    void testProjetistaSoNaBusca() throws Exception {
        mockMvc.perform(get("/estoque/materiais"))
                .andExpect(status().isForbidden())
                .andExpect(forwardedUrl("/acesso-negado"));
        mockMvc.perform(post("/estoque/materiais/1/entrada").with(csrf()).param("quantidade", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "VENDEDOR")
    @DisplayName("Vendedor não acessa o estoque nem a busca")
    void testVendedorSemAcesso() throws Exception {
        mockMvc.perform(get("/estoque/materiais")).andExpect(status().isForbidden());
        mockMvc.perform(get("/estoque/materiais/busca").param("q", "pvc")).andExpect(status().isForbidden());
        verify(materialService, never()).buscarParaListaTecnica(anyString());
    }
}
