package br.edu.iftm.irrigacao.sistema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
import br.edu.iftm.irrigacao.dominio.estoque.Cotacao;
import br.edu.iftm.irrigacao.dominio.estoque.CotacaoService;
import br.edu.iftm.irrigacao.dominio.estoque.SituacaoCotacao;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;
import br.edu.iftm.irrigacao.dominio.execucao.AceiteService;
import br.edu.iftm.irrigacao.dominio.execucao.MedicaoAvaliada;
import br.edu.iftm.irrigacao.dominio.execucao.MedicaoService;
import br.edu.iftm.irrigacao.dominio.execucao.Pendencia;
import br.edu.iftm.irrigacao.dominio.execucao.TipoPendencia;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Saída negativa de cada gateway do TO-BE (G1 a G8), pelas telas do sistema. Cada teste confere o que
 * o sistema bloqueia e, depois, o caminho de volta que o diagrama desenha.
 */
class GatewaysNegativosSistemaTest extends ProcessoSistemaBase {

    @Autowired private CotacaoService cotacaoService;
    @Autowired private MedicaoService medicaoService;
    @Autowired private AceiteService aceiteService;

    // ---------- G1: levantamento completo? Não: volta à atividade 3 ----------

    @Test
    @DisplayName("G1 Não: levantamento incompleto volta ao campo, segura o dimensionamento e segue depois de completo")
    void testG1LevantamentoIncompleto() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "10");
        Long projetoId = abrirProjeto();
        agendarVisita(projetoId);

        mockMvc.perform(multipart("/campo/projetos/" + projetoId + "/levantamento")
                .param("acao", "enviar").param("cultura", "Café").param("areaIrrigadaHa", "12.5")
                .with(csrf()).with(logado(TECNICO)))
                .andExpect(redirectedUrl("/campo/projetos/" + projetoId + "/levantamento"))
                .andExpect(flash().attribute("erro", allOf(containsString("incompleto"), containsString("fonte de água"),
                        containsString("topografia"), containsString("pelo menos uma foto"))));

        String engenharia = "/engenharia/projetos/" + projetoId;
        postarRecusado(PROJETISTA, engenharia + "/dimensionamento", "ainda está incompleto", "pressaoProjetoMca", "30",
                "vazaoProjetoM3h", "6", "toleranciaPercentual", "2");
        postarRecusado(PROJETISTA, engenharia + "/concluir", "completar o levantamento");
        assertThat(projeto(projetoId).getPressaoProjetoMca()).isNull();
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);

        // Volta à atividade 3: completo, o dimensionamento fica liberado
        preencherLevantamento(projetoId);
        registrarDimensionamento(projetoId);
        adicionarItem(projetoId, materialId, "5");
        concluirListaTecnica(projetoId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.ORCANDO);
    }

    // ---------- G2: desconto dentro da alçada? Não: atividade 9, diretoria ----------

    @Test
    @DisplayName("G2 Não: desconto acima da alçada bloqueia o envio até a diretoria aprovar")
    void testG2DescontoAprovadoPelaDiretoria() throws Exception {
        Long projetoId = projetoOrcando(materialComSaldo("10.00", "6.00", "10"), "5");
        Long propostaId = gerarProposta(projetoId, "5.01");
        String proposta = "/comercial/propostas/" + propostaId;

        postarRecusado(VENDEDOR, proposta + "/enviar", "passa da alçada");
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.ORCANDO);

        postar(VENDEDOR, proposta + "/pedir-aprovacao");
        assertThat(proposta(propostaId).getSituacao()).isEqualTo(SituacaoProposta.AGUARDANDO_DIRETORIA);
        assertThat(abrir(DIRETORIA, "/diretoria/aprovacoes")).contains(projeto(projetoId).getCodigo());
        postarRecusado(VENDEDOR, proposta + "/enviar", "não pode ser enviada");
        postarProibido(VENDEDOR, "/diretoria/aprovacoes/" + propostaId + "/aprovar");

        postar(DIRETORIA, "/diretoria/aprovacoes/" + propostaId + "/aprovar", "observacao", "Cliente estratégico.");
        Proposta aprovada = proposta(propostaId);
        assertThat(aprovada.getSituacao()).isEqualTo(SituacaoProposta.EM_ELABORACAO);
        assertThat(aprovada.getAprovadoPor()).isNotNull();

        // Junta com o caminho do Sim: a proposta vai ao cliente (atividade 10)
        enviarProposta(propostaId);
        assertThat(proposta(propostaId).getSituacao()).isEqualTo(SituacaoProposta.ENVIADA);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);
    }

    @Test
    @DisplayName("G2 Não: desconto negado volta ao vendedor com o motivo e só sai depois de refeito")
    void testG2DescontoNegadoPelaDiretoria() throws Exception {
        Long projetoId = projetoOrcando(materialComSaldo("10.00", "6.00", "10"), "5");
        Long propostaId = gerarProposta(projetoId, "20");
        String proposta = "/comercial/propostas/" + propostaId;
        postar(VENDEDOR, proposta + "/pedir-aprovacao");

        postarRecusado(DIRETORIA, "/diretoria/aprovacoes/" + propostaId + "/negar", "Informe o motivo");
        postar(DIRETORIA, "/diretoria/aprovacoes/" + propostaId + "/negar", "motivo", "Margem abaixo do mínimo.");

        Proposta negada = proposta(propostaId);
        assertThat(negada.getSituacao()).isEqualTo(SituacaoProposta.EM_ELABORACAO);
        assertThat(negada.getAprovadoPor()).isNull();
        assertThat(abrir(VENDEDOR, proposta)).contains("Margem abaixo do mínimo.");
        postarRecusado(VENDEDOR, proposta + "/enviar", "passa da alçada");

        postar(VENDEDOR, proposta, "horasPrevistas", "10", "percentualDesconto", "5");
        enviarProposta(propostaId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);
    }

    // ---------- G3: aprovada dentro da validade? Não: recusa encerra, vencimento recalcula ----------

    @Test
    @DisplayName("G3 Não: a recusa do cliente encerra o processo em \"Proposta recusada\", sem reservar estoque")
    void testG3PropostaRecusada() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "10");
        Long projetoId = projetoOrcando(materialId, "5");
        Long propostaId = gerarProposta(projetoId, "0");
        enviarProposta(propostaId);
        String proposta = "/comercial/propostas/" + propostaId;

        postarRecusado(VENDEDOR, proposta + "/recusa", "Informe o motivo");
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);

        postar(VENDEDOR, proposta + "/recusa", "motivo", "Preço acima do concorrente.");

        assertThat(proposta(propostaId).getSituacao()).isEqualTo(SituacaoProposta.RECUSADA);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.CANCELADO);
        assertThat(historicoRepository.listarPorProjeto(projetoId).getLast().getObservacao())
                .contains("Preço acima do concorrente.");
        conferirSaldo(materialId, "10", "0");
        // CANCELADO é final: não há nova proposta nem aprovação depois da recusa
        postarRecusado(VENDEDOR, "/comercial/projetos/" + projetoId + "/propostas", "com o projeto em", "horasPrevistas",
                "10", "percentualDesconto", "0");
        postarRecusado(VENDEDOR, proposta + "/aprovacao-cliente", "não pode ser aprovada");
        postarRecusado(COMPRADOR, "/comercial/projetos/" + projetoId + "/faltantes/cotacoes", "As cotações são registradas",
                "materialId", materialId.toString(), "fornecedor", "AgroTubos", "precoUnitario", "5.50", "quantidade", "1");
    }

    @Test
    @DisplayName("G3 Não: proposta vencida recusa a aprovação e volta ao orçamento com os preços atuais")
    void testG3PropostaVencida() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "10");
        Long projetoId = projetoOrcando(materialId, "5");
        Long propostaId = gerarProposta(projetoId, "0");
        enviarProposta(propostaId);
        // 5 x 10,00 + 10 h x 85,00
        assertThat(proposta(propostaId).getValorTotal()).isEqualByComparingTo("900.00");
        materialService.atualizar(materialId, material(materialId).getCodigo(), material(materialId).getDescricao(),
                "un", new BigDecimal("12.00"), new BigDecimal("6.00"));

        // Validade de 15 dias: o último dia ainda vale, o dia seguinte não
        relogio.definir(LocalDateTime.now(relogio).plusDays(16));
        postarRecusado(VENDEDOR, "/comercial/propostas/" + propostaId + "/aprovacao-cliente", "venceu");
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.AGUARDANDO_RESPOSTA);

        abrir(VENDEDOR, "/comercial/propostas");

        assertThat(proposta(propostaId).getSituacao()).isEqualTo(SituacaoProposta.VENCIDA);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.ORCANDO);
        List<Proposta> versoes = propostaRepository.findByProjetoIdOrderByNumeroDesc(projetoId);
        assertThat(versoes).hasSize(2);
        Proposta refeita = versoes.get(0);
        assertThat(refeita.getNumero()).isEqualTo(2);
        assertThat(refeita.getSituacao()).isEqualTo(SituacaoProposta.EM_ELABORACAO);
        // 5 x 12,00 + 10 h x 85,00
        assertThat(refeita.getValorTotal()).isEqualByComparingTo("910.00");

        // A nova versão segue o caminho normal até a aprovação
        enviarProposta(refeita.getId());
        aprovarProposta(refeita.getId());
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
    }

    // ---------- G4: todo material disponível? Não: atividade 12, requisição de compra ----------

    @Test
    @DisplayName("G4 Não: o faltante cotado no orçamento vira requisição, é recebido, reservado e separado")
    void testG4RequisicaoDoFaltanteCotado() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "4");
        Long projetoId = projetoOrcando(materialId, "10");
        String cotacoes = "/comercial/projetos/" + projetoId + "/faltantes";
        assertThat(abrir(COMPRADOR, cotacoes)).contains("Faltam 6 un");
        postar(COMPRADOR, cotacoes + "/cotacoes", "materialId", materialId.toString(), "fornecedor", "AgroTubos",
                "precoUnitario", "5.50", "prazoDias", "3", "quantidade", "6");
        Long cotacaoId = cotacaoService.listar(projetoId).get(0).getId();
        Long propostaId = gerarProposta(projetoId, "0");
        enviarProposta(propostaId);
        aprovarProposta(propostaId);

        // Só o que havia foi reservado, e a separação espera a compra
        conferirSaldo(materialId, "4", "4");
        postarRecusado(ALMOXARIFE, "/estoque/projetos/" + projetoId + "/separacao", "Receba a compra antes de separar",
                "quantidades[" + item(projetoId, materialId).getId() + "]", "10");

        postarProibido(ALMOXARIFE, "/estoque/projetos/" + projetoId + "/requisicoes", "cotacaoId", cotacaoId.toString());
        postar(COMPRADOR, "/estoque/projetos/" + projetoId + "/requisicoes", "cotacaoId", cotacaoId.toString());
        postar(ALMOXARIFE, "/estoque/requisicoes/" + cotacaoId + "/recebimento");

        assertThat(cotacaoService.listar(projetoId).get(0).getSituacao()).isEqualTo(SituacaoCotacao.RECEBIDA);
        conferirSaldo(materialId, "10", "10");

        // Junta com o caminho do Sim: separação (atividade 13)
        separar(projetoId, materialId, "10");
        liberarExecucao(projetoId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        conferirSaldo(materialId, "0", "0");
    }

    @Test
    @DisplayName("G4 Não: o que passou a faltar depois do orçamento ainda pode ser cotado e comprado")
    void testG4FaltanteSurgidoDepoisDoOrcamento() throws Exception {
        // Os dois projetos são orçados com o mesmo saldo: a reserva só acontece na aprovação
        Long materialId = materialComSaldo("10.00", "6.00", "50");
        Long primeiro = projetoOrcando(materialId, "40");
        Long segundo = projetoOrcando(materialId, "40");
        Long propostaDoPrimeiro = gerarProposta(primeiro, "0");
        Long propostaDoSegundo = gerarProposta(segundo, "0");
        enviarProposta(propostaDoPrimeiro);
        enviarProposta(propostaDoSegundo);
        aprovarProposta(propostaDoPrimeiro);
        aprovarProposta(propostaDoSegundo);
        conferirSaldo(materialId, "50", "50");
        assertThat(cotacaoService.listar(segundo)).isEmpty();
        assertThat(abrir(COMPRADOR, "/estoque/projetos/" + segundo + "/separacao")).contains("Registrar cotação");
        assertThat(abrir(COMPRADOR, "/comercial/projetos/" + segundo + "/faltantes")).contains("Faltam 30 un");

        postar(COMPRADOR, "/comercial/projetos/" + segundo + "/faltantes/cotacoes", "materialId", materialId.toString(),
                "fornecedor", "AgroTubos", "precoUnitario", "5.50", "prazoDias", "3", "quantidade", "30");
        Cotacao cotacao = cotacaoService.listar(segundo).get(0);
        postar(COMPRADOR, "/estoque/projetos/" + segundo + "/requisicoes", "cotacaoId", cotacao.getId().toString());
        postar(ALMOXARIFE, "/estoque/requisicoes/" + cotacao.getId() + "/recebimento");

        separar(segundo, materialId, "40");
        liberarExecucao(segundo);
        assertThat(situacao(segundo)).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        assertThat(movimentos(segundo, materialId).get(TipoMovimentacao.SAIDA_PROJETO)).isEqualByComparingTo("40");
    }

    @Test
    @DisplayName("G4 Não: o material que entrou no estoque depois da aprovação é reservado ao separar")
    void testG4EntradaDepoisDaAprovacao() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "4");
        Long projetoId = projetoAprovado(materialId, "10");
        conferirSaldo(materialId, "4", "4");

        postar(ALMOXARIFE, "/estoque/materiais/" + materialId + "/entrada", "quantidade", "6");
        conferirSaldo(materialId, "10", "4");

        separar(projetoId, materialId, "10");
        conferirSaldo(materialId, "10", "10");
        liberarExecucao(projetoId);

        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        conferirSaldo(materialId, "0", "0");
    }

    // ---------- G5: há material separado para a obra começar? Não: volta à atividade 13 ----------

    @Test
    @DisplayName("G5 Não: sem item separado a execução não é liberada; com parte separada a obra começa e o resto sai depois")
    void testG5LiberacaoComSeparacaoParcial() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "50");
        Long projetoId = projetoAprovado(materialId, "20");

        // Nada separado: a liberação é recusada e nada muda
        mockMvc.perform(post("/estoque/projetos/" + projetoId + "/separacao/liberacao")
                .with(csrf()).with(logado(ALMOXARIFE)))
                .andExpect(flash().attribute("erro", containsString("Separe pelo menos um item")));
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.PREPARANDO_MATERIAL);
        assertThat(projeto(projetoId).isSeparacaoConferida()).isFalse();
        conferirSaldo(materialId, "50", "20");
        assertThat(movimentos(projetoId, materialId)).doesNotContainKey(TipoMovimentacao.SAIDA_PROJETO);

        // Com 18 das 20 separadas a obra começa: só as 18 saem, e as 2 restantes ficam reservadas
        separar(projetoId, materialId, "18");
        liberarExecucao(projetoId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        conferirSaldo(materialId, "32", "2");

        // As 2 que faltavam são separadas durante a execução e saem do estoque na hora
        separar(projetoId, materialId, "2");
        conferirSaldo(materialId, "30", "0");
    }

    // ---------- G6: teste atendeu ao projeto? Não: volta à atividade 16 ----------

    @Test
    @DisplayName("G6 Não: medição fora do parâmetro volta à instalação e só colhe o aceite depois de aprovada")
    void testG6MedicaoReprovada() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "50");
        Long projetoId = projetoSeparado(materialId, "20");
        Long ordemId = emitirOrdem(projetoId);
        apontarMaterial(ordemId, materialId, "20");
        concluirInstalacao(ordemId);

        // 30,61 mca passa do limite de 30,6 (30 + 2%)
        mockMvc.perform(post("/campo/ordens/" + ordemId + "/medicoes").param("pressaoMedidaMca", "30.61")
                .param("vazaoMedidaM3h", "6").with(csrf()).with(logado(TECNICO)))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("erro", containsString("fora do parâmetro")));

        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.CORRIGINDO_INSTALACAO);
        assertThat(medicaoService.doProjeto(projetoId)).extracting(MedicaoAvaliada::getMedicao)
                .singleElement().satisfies(medicao -> assertThat(medicao.isAprovada()).isFalse());
        postarRecusado(TECNICO, "/campo/ordens/" + ordemId + "/aceite", "O aceite é colhido", "nomeResponsavel", CLIENTE,
                "assinatura", assinatura());

        // Volta à atividade 16: corrige, aponta de novo e mede outra vez
        postar(TECNICO, "/campo/ordens/" + ordemId + "/correcao", "descricao", "Regulador de pressão ajustado.");
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.EXECUTANDO_INSTALACAO);
        apontarHoras(ordemId, "2");
        concluirInstalacao(ordemId);
        medirDentroDoParametro(ordemId);

        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.COLHENDO_ACEITE);
        assertThat(medicaoService.doProjeto(projetoId)).hasSize(2);
        assertThat(caminho(projetoId)).containsSubsequence(SituacaoProjeto.VALIDANDO_MEDICAO,
                SituacaoProjeto.CORRIGINDO_INSTALACAO, SituacaoProjeto.EXECUTANDO_INSTALACAO,
                SituacaoProjeto.VALIDANDO_MEDICAO, SituacaoProjeto.COLHENDO_ACEITE);
    }

    // ---------- G7: cliente assinou o aceite? Não: atividade 21 e volta à 20 ----------

    @Test
    @DisplayName("G7 Não: sem assinatura registra a pendência, reprograma a visita e volta a colher o aceite")
    void testG7PendenciaAdministrativa() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "50");
        Long projetoId = projetoSeparado(materialId, "20");
        Long ordemId = ordemComMedicaoAprovada(projetoId, materialId, "20");
        LocalDateTime novaVisita = LocalDateTime.now(relogio).plusDays(3).withSecond(0).withNano(0);

        postar(TECNICO, "/campo/ordens/" + ordemId + "/pendencia", "tipo", "ADMINISTRATIVA", "descricao",
                "Responsável viajando.", "dataReprogramada", novaVisita.toString());

        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.TRATANDO_PENDENCIA);
        Pendencia pendencia = aceiteService.pendenciaAberta(projetoId).orElseThrow();
        assertThat(pendencia.getTipo()).isEqualTo(TipoPendencia.ADMINISTRATIVA);
        assertThat(pendencia.getDataReprogramada()).isEqualTo(novaVisita);
        assertThat(aceiteService.aceiteDoProjeto(projetoId)).isEmpty();
        postarRecusado(TECNICO, "/campo/ordens/" + ordemId + "/aceite", "O aceite é colhido", "nomeResponsavel", CLIENTE,
                "assinatura", assinatura());

        // Volta à atividade 20
        postar(TECNICO, "/campo/ordens/" + ordemId + "/pendencia/resolucao");
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.COLHENDO_ACEITE);
        assertThat(aceiteService.pendenciaAberta(projetoId)).isEmpty();
        colherAceite(ordemId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);
    }

    @Test
    @DisplayName("G7 Não: a pendência técnica volta para a medição antes de colher o aceite de novo")
    void testG7PendenciaTecnica() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "50");
        Long projetoId = projetoSeparado(materialId, "20");
        Long ordemId = ordemComMedicaoAprovada(projetoId, materialId, "20");

        postar(TECNICO, "/campo/ordens/" + ordemId + "/pendencia", "tipo", "TECNICA", "descricao",
                "Cliente pediu novo teste de vazão.", "dataReprogramada",
                LocalDateTime.now(relogio).plusDays(1).withSecond(0).withNano(0).toString());
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.TRATANDO_PENDENCIA);

        postar(TECNICO, "/campo/ordens/" + ordemId + "/pendencia/resolucao");
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.VALIDANDO_MEDICAO);

        medirDentroDoParametro(ordemId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.COLHENDO_ACEITE);
        colherAceite(ordemId);
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.FINALIZANDO_EXECUCAO);
    }

    // ---------- G8: houve sobra? Não: segue direto para a atividade 23 ----------

    @Test
    @DisplayName("G8 Não: sem sobra não há devolução ao estoque e o faturamento pode ser liberado")
    void testG8SemSobra() throws Exception {
        Long materialId = materialComSaldo("10.00", "6.00", "50");
        Long projetoId = projetoSeparado(materialId, "20");
        Long ordemId = ordemComMedicaoAprovada(projetoId, materialId, "20");
        colherAceite(ordemId);

        // Antes de o almoxarifado confirmar, o financeiro não libera
        postarRecusado(FINANCEIRO, "/financeiro/liberacoes/" + projetoId, "sobras", "dataInicioGarantia",
                hoje().toString(), "prazoGarantiaMeses", "12");

        postar(ALMOXARIFE, "/estoque/projetos/" + projetoId + "/sobras");

        Projeto semSobra = projeto(projetoId);
        assertThat(semSobra.isSobrasDevolvidas()).isTrue();
        assertThat(movimentos(projetoId, materialId)).doesNotContainKey(TipoMovimentacao.DEVOLUCAO);
        conferirSaldo(materialId, "30", "0");

        postar(FINANCEIRO, "/financeiro/liberacoes/" + projetoId, "dataInicioGarantia", hoje().toString(),
                "prazoGarantiaMeses", "12");
        assertThat(situacao(projetoId)).isEqualTo(SituacaoProjeto.ENCERRADO);
    }
}
