package br.edu.iftm.irrigacao.apresentacao.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.edu.iftm.irrigacao.apresentacao.web.ProximoPasso.Acao;
import br.edu.iftm.irrigacao.apresentacao.web.ProximoPasso.Quem;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;

/**
 * Próximo passo no topo da página do projeto: o que falta, com quem está e o botão para quem pode.
 */
class ProximoPassoTest {

    private static final Usuario TITO = new Usuario(21L, "Tito Técnico", "tito", "x", Perfil.TECNICO_CAMPO, true);
    private static final Usuario TANIA = new Usuario(22L, "Tânia Técnica", "tania", "x", Perfil.TECNICO_CAMPO, true);

    private static Quem quem(Perfil perfil) {
        return new Quem(EnumSet.of(perfil), 1L);
    }

    private static Quem tecnico(Usuario usuario) {
        return new Quem(EnumSet.of(Perfil.TECNICO_CAMPO), usuario.getId());
    }

    private static Projeto projeto(SituacaoProjeto situacao) {
        Projeto projeto = new Projeto();
        projeto.setId(7L);
        projeto.setSituacao(situacao);
        return projeto;
    }

    private static Levantamento agendado() {
        Levantamento levantamento = new Levantamento();
        levantamento.setDataAgendada(LocalDateTime.of(2026, 11, 18, 8, 30));
        return levantamento;
    }

    private static Proposta proposta(SituacaoProposta situacao, LocalDate dataEnvio) {
        return new Proposta(40L, null, 2, BigDecimal.TEN, BigDecimal.ZERO, dataEnvio, 15, situacao,
                BigDecimal.ONE, null, null);
    }

    private static OrdemServico ordem(long id, int numero, LocalDateTime instalacaoConcluidaEm, Usuario... tecnicos) {
        return new OrdemServico(id, null, numero, LocalDate.of(2026, 11, 20), null, instalacaoConcluidaEm,
                List.of(tecnicos));
    }

    private static ProximoPasso passo(Projeto projeto, Quem quem) {
        return ProximoPasso.de(projeto, null, false, null, List.of(), quem).orElseThrow();
    }

    @Test
    @DisplayName("Levantamento: agendar a visita, depois preencher, depois dimensionar, cada um com o seu perfil")
    void testEmLevantamento() {
        Projeto projeto = projeto(SituacaoProjeto.EM_LEVANTAMENTO);

        ProximoPasso agendar = passo(projeto, quem(Perfil.VENDEDOR));
        assertThat(agendar.titulo()).isEqualTo("agendar a visita técnica");
        assertThat(agendar.responsavel()).isEqualTo("o comercial");
        assertThat(agendar.acao()).isEqualTo(new Acao("Agendar visita", "/comercial/projetos/7/visita"));
        assertThat(passo(projeto, quem(Perfil.TECNICO_CAMPO)).acao()).isNull();

        ProximoPasso preencher = ProximoPasso.de(projeto, agendado(), false, null, List.of(),
                quem(Perfil.TECNICO_CAMPO)).orElseThrow();
        assertThat(preencher.titulo()).isEqualTo("fazer a visita e preencher o levantamento");
        assertThat(preencher.responsavel()).isEqualTo("a equipe de campo");
        assertThat(preencher.detalhe()).isEqualTo("Visita agendada para quarta-feira, 18/11/2026 às 08:30.");
        assertThat(preencher.acao().url()).isEqualTo("/campo/projetos/7/levantamento");
        assertThat(ProximoPasso.de(projeto, agendado(), false, null, List.of(), quem(Perfil.VENDEDOR))
                .orElseThrow().acao()).isNull();

        ProximoPasso dimensionar = ProximoPasso.de(projeto, agendado(), true, null, List.of(),
                quem(Perfil.PROJETISTA)).orElseThrow();
        assertThat(dimensionar.titulo()).isEqualTo("dimensionar e montar a lista técnica");
        assertThat(dimensionar.responsavel()).isEqualTo("a engenharia");
        assertThat(dimensionar.acao().url()).isEqualTo("/engenharia/projetos/7/dimensionamento");
    }

    @Test
    @DisplayName("Orçando: gerar o orçamento; com a proposta aberta, revisar e enviar; acima da alçada, a diretoria")
    void testOrcando() {
        Projeto projeto = projeto(SituacaoProjeto.ORCANDO);

        ProximoPasso gerar = passo(projeto, quem(Perfil.VENDEDOR));
        assertThat(gerar.titulo()).isEqualTo("gerar o orçamento e a proposta");
        assertThat(gerar.acao().url()).isEqualTo("/comercial/projetos/7/orcamento");

        ProximoPasso revisar = ProximoPasso.de(projeto, null, true, proposta(SituacaoProposta.EM_ELABORACAO, null),
                List.of(), quem(Perfil.VENDEDOR)).orElseThrow();
        assertThat(revisar.titulo()).isEqualTo("revisar a proposta 2 e enviar ao cliente");
        assertThat(revisar.acao()).isEqualTo(new Acao("Abrir a proposta 2", "/comercial/propostas/40"));

        // Proposta vencida não está mais aberta: o vendedor gera outro orçamento
        assertThat(ProximoPasso.de(projeto, null, true, proposta(SituacaoProposta.VENCIDA, LocalDate.of(2026, 10, 1)),
                List.of(), quem(Perfil.VENDEDOR)).orElseThrow().titulo()).isEqualTo("gerar o orçamento e a proposta");

        Proposta naDiretoria = proposta(SituacaoProposta.AGUARDANDO_DIRETORIA, null);
        ProximoPasso aprovar = ProximoPasso.de(projeto, null, true, naDiretoria, List.of(), quem(Perfil.DIRETORIA))
                .orElseThrow();
        assertThat(aprovar.titulo()).isEqualTo("aprovar ou negar o desconto da proposta 2");
        assertThat(aprovar.responsavel()).isEqualTo("a diretoria");
        assertThat(aprovar.acao().url()).isEqualTo("/diretoria/aprovacoes");
        assertThat(ProximoPasso.de(projeto, null, true, naDiretoria, List.of(), quem(Perfil.VENDEDOR))
                .orElseThrow().acao()).isNull();
    }

    @Test
    @DisplayName("Aguardando resposta: registrar a resposta do cliente, com a validade da proposta")
    void testAguardandoResposta() {
        ProximoPasso passo = ProximoPasso.de(projeto(SituacaoProjeto.AGUARDANDO_RESPOSTA), null, true,
                proposta(SituacaoProposta.ENVIADA, LocalDate.of(2026, 11, 16)), List.of(), quem(Perfil.VENDEDOR))
                .orElseThrow();

        assertThat(passo.titulo()).isEqualTo("registrar a resposta do cliente à proposta 2");
        assertThat(passo.detalhe()).isEqualTo("A proposta vale até 01/12/2026.");
        assertThat(passo.acao().url()).isEqualTo("/comercial/propostas/40");
    }

    @Test
    @DisplayName("Material e programação: almoxarifado e comprador separam; o projetista programa")
    void testMaterialEProgramacao() {
        Projeto preparando = projeto(SituacaoProjeto.PREPARANDO_MATERIAL);
        assertThat(passo(preparando, quem(Perfil.ALMOXARIFE)).acao().url()).isEqualTo("/estoque/projetos/7/separacao");
        assertThat(passo(preparando, quem(Perfil.COMPRADOR)).acao()).isNotNull();
        assertThat(passo(preparando, quem(Perfil.VENDEDOR)).acao()).isNull();
        assertThat(passo(preparando, quem(Perfil.VENDEDOR)).responsavel()).isEqualTo("o almoxarifado");

        Projeto programando = projeto(SituacaoProjeto.PROGRAMANDO_EXECUCAO);
        assertThat(passo(programando, quem(Perfil.PROJETISTA)).acao().url()).isEqualTo("/engenharia/projetos/7/programacao");
        assertThat(passo(programando, quem(Perfil.TECNICO_CAMPO)).acao()).isNull();
    }

    @Test
    @DisplayName("Instalação: com os técnicos das OS que faltam; o técnico só abre a OS em que está escalado")
    void testInstalacao() {
        Projeto projeto = projeto(SituacaoProjeto.EXECUTANDO_INSTALACAO);
        List<OrdemServico> ordens = List.of(
                ordem(101L, 1, LocalDateTime.of(2026, 11, 20, 16, 0), TITO),
                ordem(102L, 2, null, TANIA),
                ordem(103L, 3, null, TITO, TANIA));

        ProximoPasso daTania = ProximoPasso.de(projeto, null, true, null, ordens, tecnico(TANIA)).orElseThrow();
        // A OS 1 já concluiu a instalação: fica de fora
        assertThat(daTania.responsavel()).isEqualTo("Tânia Técnica e Tito Técnico");
        assertThat(daTania.detalhe()).isEqualTo("Nas OS 2 e 3.");
        assertThat(daTania.acao()).isEqualTo(new Acao("Abrir a OS 2", "/campo/ordens/102"));

        assertThat(ProximoPasso.de(projeto, null, true, null, ordens, tecnico(TITO)).orElseThrow().acao())
                .isEqualTo(new Acao("Abrir a OS 3", "/campo/ordens/103"));
        assertThat(ProximoPasso.de(projeto, null, true, null, ordens, quem(Perfil.PROJETISTA)).orElseThrow().acao())
                .isEqualTo(new Acao("Abrir a OS 2", "/campo/ordens/102"));
        assertThat(ProximoPasso.de(projeto, null, true, null, ordens, quem(Perfil.VENDEDOR)).orElseThrow().acao())
                .isNull();

        Usuario outro = new Usuario(23L, "Outro Técnico", "outro", "x", Perfil.TECNICO_CAMPO, true);
        assertThat(ProximoPasso.de(projeto, null, true, null, ordens, tecnico(outro)).orElseThrow().acao()).isNull();
    }

    @Test
    @DisplayName("Medição, correção, aceite e pendência: feitos na OS aberta, pela equipe dela")
    void testDepoisDaInstalacao() {
        List<OrdemServico> ordens = List.of(ordem(101L, 1, LocalDateTime.of(2026, 11, 20, 16, 0), TITO));

        assertThat(ProximoPasso.de(projeto(SituacaoProjeto.VALIDANDO_MEDICAO), null, true, null, ordens, tecnico(TITO))
                .orElseThrow()).isEqualTo(new ProximoPasso("medir a pressão e a vazão", "Tito Técnico", null,
                        new Acao("Abrir a OS 1", "/campo/ordens/101")));
        assertThat(ProximoPasso.de(projeto(SituacaoProjeto.CORRIGINDO_INSTALACAO), null, true, null, ordens,
                tecnico(TITO)).orElseThrow().titulo()).isEqualTo("corrigir a instalação e registrar a correção");
        assertThat(ProximoPasso.de(projeto(SituacaoProjeto.COLHENDO_ACEITE), null, true, null, ordens, tecnico(TITO))
                .orElseThrow().titulo()).isEqualTo("colher o aceite do cliente");
        assertThat(ProximoPasso.de(projeto(SituacaoProjeto.TRATANDO_PENDENCIA), null, true, null, ordens,
                tecnico(TITO)).orElseThrow().titulo()).isEqualTo("resolver a pendência e marcar como resolvida");

        // Sem ordem aberta (não deveria acontecer), o passo continua com a equipe de campo, sem botão
        ProximoPasso semOrdem = passo(projeto(SituacaoProjeto.COLHENDO_ACEITE), tecnico(TITO));
        assertThat(semOrdem.responsavel()).isEqualTo("a equipe de campo");
        assertThat(semOrdem.acao()).isNull();
    }

    @Test
    @DisplayName("Finalizando: primeiro as sobras, pelo almoxarife; depois a liberação, pelo financeiro")
    void testFinalizando() {
        Projeto projeto = projeto(SituacaoProjeto.FINALIZANDO_EXECUCAO);

        ProximoPasso sobras = passo(projeto, quem(Perfil.ALMOXARIFE));
        assertThat(sobras.titulo()).isEqualTo("conferir e devolver as sobras ao estoque");
        assertThat(sobras.acao().url()).isEqualTo("/estoque/projetos/7/sobras");
        // O comprador vê as sobras, mas só o almoxarife devolve
        assertThat(passo(projeto, quem(Perfil.COMPRADOR)).acao()).isNull();

        projeto.setSobrasDevolvidas(true);
        ProximoPasso liberar = passo(projeto, quem(Perfil.FINANCEIRO));
        assertThat(liberar.titulo()).isEqualTo("liberar o faturamento e registrar a garantia");
        assertThat(liberar.responsavel()).isEqualTo("o financeiro");
        assertThat(liberar.acao().url()).isEqualTo("/financeiro/liberacoes/7");
        assertThat(passo(projeto, quem(Perfil.DIRETORIA)).acao()).isNull();
    }

    @Test
    @DisplayName("O administrador tem o botão de todo passo; encerrado ou cancelado, não há passo")
    void testAdministradorEFinais() {
        for (SituacaoProjeto situacao : List.of(SituacaoProjeto.EM_LEVANTAMENTO, SituacaoProjeto.ORCANDO,
                SituacaoProjeto.PREPARANDO_MATERIAL, SituacaoProjeto.PROGRAMANDO_EXECUCAO,
                SituacaoProjeto.FINALIZANDO_EXECUCAO)) {
            assertThat(passo(projeto(situacao), quem(Perfil.ADMINISTRADOR)).acao()).as(situacao.name()).isNotNull();
        }
        assertThat(ProximoPasso.de(projeto(SituacaoProjeto.ENCERRADO), null, true, null, List.of(),
                quem(Perfil.ADMINISTRADOR))).isEmpty();
        assertThat(ProximoPasso.de(projeto(SituacaoProjeto.CANCELADO), null, true, null, List.of(),
                quem(Perfil.ADMINISTRADOR))).isEmpty();
    }
}
