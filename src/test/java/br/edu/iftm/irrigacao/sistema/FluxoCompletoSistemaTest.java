package br.edu.iftm.irrigacao.sistema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;
import br.edu.iftm.irrigacao.dominio.execucao.AceiteService;
import br.edu.iftm.irrigacao.dominio.execucao.MedicaoService;
import br.edu.iftm.irrigacao.dominio.projeto.Apuracao;
import br.edu.iftm.irrigacao.dominio.projeto.Garantia;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.LiberacaoFaturamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.OrcadoRealizadoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Fluxo feliz do TO-BE, da solicitação da visita ao início da garantia (atividades 1 a 24), com cada
 * perfil fazendo a sua parte pelas telas do sistema.
 *
 * <p>Valores: 20 barras de tubo (venda 62,90, custo 44,80), 400 gotejadores (venda 0,95, custo 0,52)
 * e 10 h previstas (venda 85,00, custo 45,00). Subtotal de 2.488,00 com 5% de desconto, o limite da
 * alçada: 2.363,60. Custo orçado de 1.554,00. A execução aponta 8 h, 17 barras e 400 gotejadores:
 * custo realizado de 1.329,60, e sobram 3 barras.
 */
class FluxoCompletoSistemaTest extends ProcessoSistemaBase {

    private static final String CPF_DO_FLUXO = "111.444.777-35";
    private static final String TECNICO_NOVO = "teo.campo";

    @Autowired private MedicaoService medicaoService;
    @Autowired private AceiteService aceiteService;
    @Autowired private LiberacaoFaturamentoService liberacaoService;
    @Autowired private OrcadoRealizadoService orcadoRealizadoService;

    @Test
    @DisplayName("Fluxo feliz: da abertura do projeto à garantia, conferindo estado, histórico e saldos")
    void testFluxoFelizCompleto() throws Exception {
        // RF23: o administrador cadastra o técnico que vai para o campo
        postar(ADMINISTRADOR, "/admin/usuarios", "nome", "Téo Técnico", "login", TECNICO_NOVO, "senha",
                "senha-do-teo-1", "perfil", "TECNICO_CAMPO");
        Long tecnicoId = usuarioRepository.findByLogin(TECNICO_NOVO).orElseThrow().getId();

        // RF06, RF07: o comprador cadastra os materiais e o almoxarife dá entrada no estoque
        Long tuboId = cadastrarMaterial("TB-FLUXO-050", "Tubo PVC 50 mm", "barra", "62.90", "44.80");
        Long gotejadorId = cadastrarMaterial("GT-FLUXO-2L", "Gotejador 2 L/h", "un", "0.95", "0.52");
        postar(ALMOXARIFE, "/estoque/materiais/" + tuboId + "/entrada", "quantidade", "50");
        postar(ALMOXARIFE, "/estoque/materiais/" + gotejadorId + "/entrada", "quantidade", "400");
        conferirSaldo(tuboId, "50", "0");
        conferirSaldo(gotejadorId, "400", "0");

        // Atividade 1 (RF01): produtor, propriedade e abertura do projeto
        String paginaDoProdutor = postar(VENDEDOR, "/comercial/produtores", "nome", "Maria Aparecida Souza", "cpfCnpj",
                CPF_DO_FLUXO);
        Long produtorDoFluxo = idNoFim(paginaDoProdutor.replaceAll("/editar$", ""));
        postar(VENDEDOR, "/comercial/produtores/" + produtorDoFluxo + "/propriedades", "nome", "Sítio Santa Clara",
                "municipio", "Araguari");
        Long propriedadeDoFluxo = propriedadeRepository.findByProdutorIdOrderByNome(produtorDoFluxo).get(0).getId();
        Long projetoId = idNoFim(postar(VENDEDOR, "/comercial/projetos", "produtorId", produtorDoFluxo.toString(),
                "propriedadeId", propriedadeDoFluxo.toString()));
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);

        // Atividade 2 (RF02): agendamento da visita, que aparece na agenda
        passarTempo();
        LocalDateTime visita = LocalDateTime.of(2026, 11, 17, 8, 30);
        postar(VENDEDOR, "/comercial/projetos/" + projetoId + "/visita", "dataAgendada", visita.toString());
        assertThat(abrir(VENDEDOR, "/projetos/agenda?dia=2026-11-17")).contains(projeto(projetoId).getCodigo());

        // Atividades 3 e 4, G1 Sim (RF03, RF04): levantamento completo, com foto e coordenada
        passarTempo();
        enviarLevantamentoCompleto(projetoId);

        // Atividade 5 (RF05): dimensionamento e lista técnica
        passarTempo();
        registrarDimensionamento(projetoId);
        adicionarItem(projetoId, tuboId, "20");
        adicionarItem(projetoId, gotejadorId, "400");
        concluirListaTecnica(projetoId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.ORCANDO);

        // Atividades 6 e 7 (RF08): o estoque cobre a lista, então não há o que cotar
        assertThat(abrir(COMPRADOR, "/comercial/projetos/" + projetoId + "/faltantes")).contains("Nenhum faltante");

        // Atividade 8, G2 Sim (RF09, RF10): orçamento com o desconto no limite da alçada
        passarTempo();
        Long propostaId = gerarProposta(projetoId, "5");
        Proposta gerada = proposta(propostaId);
        assertThat(gerada.getSituacao()).isEqualTo(SituacaoProposta.EM_ELABORACAO);
        assertThat(gerada.getValorTotal()).isEqualByComparingTo("2363.60");

        // Atividades 10 e 11 (RF11): envio ao cliente, sem passar pela diretoria
        passarTempo();
        enviarProposta(propostaId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);
        assertThat(abrir(DIRETORIA, "/diretoria/aprovacoes")).doesNotContain(projeto(projetoId).getCodigo());

        // G3 Sim e G4 Sim (RF12, RF07): aprovação dentro da validade, com todo o material reservado
        passarTempo();
        aprovarProposta(propostaId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
        conferirSaldo(tuboId, "50", "20");
        conferirSaldo(gotejadorId, "400", "400");

        // Atividades 13 e 14, G5 Sim (RF14): separação conferida com a lista técnica
        passarTempo();
        separar(projetoId, tuboId, "20");
        separar(projetoId, gotejadorId, "400");
        conferirSeparacao(projetoId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        assertThat(projeto(projetoId).isSeparacaoConferida()).isTrue();
        conferirSaldo(tuboId, "30", "0");
        conferirSaldo(gotejadorId, "0", "0");

        // Atividade 15 (RF15): programação da equipe e ordem de serviço
        passarTempo();
        postar(PROJETISTA, "/engenharia/projetos/" + projetoId + "/programacao", "dataProgramada", hoje().toString(),
                "tecnicos", tecnicoId.toString());
        Long ordemId = ordemRepository.findByProjetoIdOrderByNumero(projetoId).getLast().getId();
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.EXECUTANDO_INSTALACAO);
        assertThat(abrir(TECNICO_NOVO, "/campo/minhas-os")).contains(projeto(projetoId).getCodigo());

        // Atividades 16 e 17 (RF16): instalação e apontamentos
        passarTempo();
        postar(TECNICO_NOVO, "/campo/ordens/" + ordemId + "/horas", "horas", "8");
        postar(TECNICO_NOVO, "/campo/ordens/" + ordemId + "/materiais", "materialId", tuboId.toString(), "quantidade",
                "17");
        postar(TECNICO_NOVO, "/campo/ordens/" + ordemId + "/materiais", "materialId", gotejadorId.toString(),
                "quantidade", "400");
        postar(TECNICO_NOVO, "/campo/ordens/" + ordemId + "/conclusao");
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.VALIDANDO_MEDICAO);

        // Atividades 18 e 19, G6 Sim (RF17): medição dentro da tolerância de 2%
        passarTempo();
        postar(TECNICO_NOVO, "/campo/ordens/" + ordemId + "/medicoes", "pressaoMedidaMca", "30.6", "vazaoMedidaM3h",
                "5.88", "latitude", "-18.91", "longitude", "-48.26");
        assertThat(medicaoService.doProjeto(projetoId)).hasSize(1);
        assertThat(medicaoService.doProjeto(projetoId).get(0).getMedicao().isAprovada()).isTrue();
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.COLHENDO_ACEITE);

        // Atividade 20, G7 Sim (RF18): o cliente assina o termo
        passarTempo();
        postar(TECNICO_NOVO, "/campo/ordens/" + ordemId + "/aceite", "nomeResponsavel", "Maria Aparecida Souza",
                "assinatura", assinatura());
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);
        assertThat(aceiteService.aceiteDoProjeto(projetoId).orElseThrow().getNomeResponsavel())
                .isEqualTo("Maria Aparecida Souza");
        assertThat(ordemRepository.findById(ordemId).orElseThrow().getDataConclusao()).isEqualTo(hoje());

        // Atividade 22, G8 Sim (RF19): as 3 barras que sobraram voltam ao estoque
        passarTempo();
        postar(ALMOXARIFE, "/estoque/projetos/" + projetoId + "/sobras",
                "devolvidas[" + item(projetoId, tuboId).getId() + "]", "3");
        assertThat(projeto(projetoId).isSobrasDevolvidas()).isTrue();

        // Atividades 23 e 24 (RF20, RF21): faturamento liberado e garantia registrada
        passarTempo();
        LocalDate inicioDaGarantia = hoje();
        postar(FINANCEIRO, "/financeiro/liberacoes/" + projetoId, "dataInicioGarantia", inicioDaGarantia.toString(),
                "prazoGarantiaMeses", "12");

        conferirEstadoFinal(projetoId, propostaId, inicioDaGarantia);
        conferirHistorico(projetoId);
        conferirEstoqueFinal(projetoId, tuboId, gotejadorId);
        conferirCustos(projetoId);
    }

    private Long cadastrarMaterial(String codigo, String descricao, String unidade, String preco, String custo)
            throws Exception {
        postar(COMPRADOR, "/estoque/materiais", "codigo", codigo, "descricao", descricao, "unidade", unidade,
                "precoVenda", preco, "custoUnitario", custo);
        return materialRepository.findByCodigo(codigo).orElseThrow().getId();
    }

    private void enviarLevantamentoCompleto(Long projetoId) throws Exception {
        MockMultipartFile foto = new MockMultipartFile("novasFotos", "fonte.png", "image/png",
                java.util.Base64.getDecoder().decode(assinatura().substring("data:image/png;base64,".length())));
        mockMvc.perform(multipart("/campo/projetos/" + projetoId + "/levantamento").file(foto)
                .param("acao", "enviar")
                .param("dataVisita", "2026-11-16T09:30")
                .param("cultura", "Café")
                .param("areaIrrigadaHa", "12.5")
                .param("fonteAgua", "POCO_ARTESIANO")
                .param("vazaoFonte", "8")
                .param("topografia", "Plana, com leve declive")
                .param("latitude", "-18.91")
                .param("longitude", "-48.26")
                .with(csrf()).with(logado(TECNICO_NOVO)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("sucesso"));
    }

    private void conferirEstadoFinal(Long projetoId, Long propostaId, LocalDate inicioDaGarantia) throws Exception {
        Projeto projeto = projeto(projetoId);
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.ENCERRADO);
        assertThat(projeto.getDataLiberacaoFaturamento()).isEqualTo(hoje());
        assertThat(projeto.getDataInicioGarantia()).isEqualTo(inicioDaGarantia);
        assertThat(projeto.getPrazoGarantiaMeses()).isEqualTo(12);
        assertThat(proposta(propostaId).getSituacao()).isEqualTo(SituacaoProposta.APROVADA);

        Garantia garantia = liberacaoService.garantia(projeto).orElseThrow();
        assertThat(garantia.fim()).isEqualTo(inicioDaGarantia.plusMonths(12));
        assertThat(garantia.vigente()).isTrue();

        assertThat(abrir(DIRETORIA, "/projetos/" + projetoId)).contains("Encerrado", "Sobra devolvida ao estoque");
    }

    private void conferirHistorico(Long projetoId) {
        List<HistoricoProjeto> historico = historicoRepository.listarPorProjeto(projetoId);

        assertThat(historico).extracting(HistoricoProjeto::getSituacaoNova).containsExactly(
                SituacaoProjeto.EM_LEVANTAMENTO, SituacaoProjeto.ORCANDO, SituacaoProjeto.AGUARDANDO_RESPOSTA,
                SituacaoProjeto.PREPARANDO_MATERIAL, SituacaoProjeto.PROGRAMANDO_EXECUCAO,
                SituacaoProjeto.EXECUTANDO_INSTALACAO, SituacaoProjeto.VALIDANDO_MEDICAO,
                SituacaoProjeto.COLHENDO_ACEITE, SituacaoProjeto.FINALIZANDO_EXECUCAO, SituacaoProjeto.ENCERRADO);
        assertThat(historico.get(0).getSituacaoAnterior()).isNull();
        for (int i = 1; i < historico.size(); i++) {
            assertThat(historico.get(i).getSituacaoAnterior()).isEqualTo(historico.get(i - 1).getSituacaoNova());
        }
        assertThat(historico).extracting(registro -> registro.getUsuario().getLogin()).containsExactly(VENDEDOR,
                PROJETISTA, VENDEDOR, VENDEDOR, ALMOXARIFE, PROJETISTA, TECNICO_NOVO, TECNICO_NOVO, TECNICO_NOVO,
                FINANCEIRO);
        assertThat(historico).allSatisfy(registro -> assertThat(registro.getDataHora()).isNotNull());
        assertThat(historico.getLast().getObservacao()).contains("Faturamento liberado", "12 meses");
    }

    private void conferirEstoqueFinal(Long projetoId, Long tuboId, Long gotejadorId) {
        // Tubo: 50 de entrada, 20 para o projeto e 3 de volta
        conferirSaldo(tuboId, "33", "0");
        // Gotejador: os 400 saíram e foram todos usados
        conferirSaldo(gotejadorId, "0", "0");

        assertThat(movimentos(projetoId, tuboId)).hasSize(3)
                .hasEntrySatisfying(TipoMovimentacao.RESERVA, q -> assertThat(q).isEqualByComparingTo("20"))
                .hasEntrySatisfying(TipoMovimentacao.SAIDA_PROJETO, q -> assertThat(q).isEqualByComparingTo("20"))
                .hasEntrySatisfying(TipoMovimentacao.DEVOLUCAO, q -> assertThat(q).isEqualByComparingTo("3"));
        assertThat(movimentos(projetoId, gotejadorId)).hasSize(2)
                .hasEntrySatisfying(TipoMovimentacao.RESERVA, q -> assertThat(q).isEqualByComparingTo("400"))
                .hasEntrySatisfying(TipoMovimentacao.SAIDA_PROJETO, q -> assertThat(q).isEqualByComparingTo("400"))
                .doesNotContainKey(TipoMovimentacao.DEVOLUCAO);
        assertThat(item(projetoId, tuboId).getQuantidadeApontada()).isEqualByComparingTo(new BigDecimal("17"));
        assertThat(item(projetoId, tuboId).getQuantidadeSeparada()).isEqualByComparingTo(new BigDecimal("20"));
    }

    private void conferirCustos(Long projetoId) {
        Apuracao apuracao = orcadoRealizadoService.doProjeto(projetoId).apuracao();

        assertThat(apuracao.valorTotal()).isEqualByComparingTo("2363.60");
        assertThat(apuracao.custoOrcado()).isEqualByComparingTo("1554.00");
        assertThat(apuracao.custoRealizado()).isEqualByComparingTo("1329.60");
        assertThat(apuracao.getMargemReal()).isEqualByComparingTo("1034.00");
    }
}
