package br.edu.iftm.irrigacao.dominio.projeto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import br.edu.iftm.irrigacao.dominio.projeto.HistoricoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TransicaoInvalidaException;
import br.edu.iftm.irrigacao.persistencia.HistoricoProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.PropriedadeRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Abertura do projeto (RF01) e {@code transitar} (RF21) com os repositórios mockados e o relógio
 * parado em 16/11/2026 10:00.
 */
@ExtendWith(MockitoExtension.class)
class ProjetoServiceImplTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 16, 10, 0);

    @Mock
    private ProjetoRepository projetoRepository;

    @Mock
    private PropriedadeRepository propriedadeRepository;

    @Mock
    private HistoricoProjetoRepository historicoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioLogado usuarioLogado;

    private ProjetoServiceImpl service;

    private final Usuario ana = new Usuario(7L, "Ana Vendedora", "ana", "hash", Perfil.VENDEDOR, true);
    private final Produtor produtor = new Produtor(3L, "João Batista Ferreira", "52998224725", null, null);
    private final Propriedade propriedade = new Propriedade(5L, produtor, "Fazenda Boa Vista", "Uberlândia", null,
            null, null);

    @BeforeEach
    void criarService() {
        service = servicoNo(AGORA);
    }

    private ProjetoServiceImpl servicoNo(LocalDateTime dataHora) {
        ZoneId zona = ZoneId.systemDefault();
        Clock relogio = Clock.fixed(dataHora.atZone(zona).toInstant(), zona);
        return new ProjetoServiceImpl(projetoRepository, propriedadeRepository, historicoRepository, usuarioRepository,
                usuarioLogado, relogio);
    }

    private Projeto projetoEm(SituacaoProjeto situacao) {
        Projeto projeto = new Projeto();
        projeto.setId(1L);
        projeto.setCodigo("PRJ-2026-0001");
        projeto.setPropriedade(propriedade);
        projeto.setResponsavel(ana);
        projeto.setDataAbertura(LocalDate.of(2026, 3, 2));
        projeto.setSituacao(situacao);
        return projeto;
    }

    private void anaLogada() {
        when(usuarioLogado.idAtual()).thenReturn(Optional.of(7L));
        when(usuarioRepository.getReferenceById(7L)).thenReturn(ana);
    }

    private void prepararAbertura(String ultimoCodigoDoAno, String prefixo) {
        when(propriedadeRepository.findById(5L)).thenReturn(Optional.of(propriedade));
        anaLogada();
        when(projetoRepository.ultimoCodigo(prefixo)).thenReturn(ultimoCodigoDoAno);
        when(projetoRepository.saveAndFlush(any(Projeto.class))).thenAnswer(chamada -> {
            Projeto projeto = chamada.getArgument(0);
            projeto.setId(1L);
            return projeto;
        });
    }

    private HistoricoProjeto historicoGravado() {
        ArgumentCaptor<HistoricoProjeto> captor = ArgumentCaptor.forClass(HistoricoProjeto.class);
        verify(historicoRepository).save(captor.capture());
        return captor.getValue();
    }

    // ---------- Transição 1: abertura ----------

    @Test
    @DisplayName("Transição 1: abrir cria o projeto em EM_LEVANTAMENTO e grava o histórico sem situação anterior")
    void testAbertura() {
        prepararAbertura(null, "PRJ-2026-%");

        Projeto projeto = service.abrir(3L, 5L);

        assertThat(projeto.getCodigo()).isEqualTo("PRJ-2026-0001");
        assertThat(projeto.getDataAbertura()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(projeto.getResponsavel()).isSameAs(ana);
        assertThat(projeto.getPropriedade()).isSameAs(propriedade);
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);

        HistoricoProjeto historico = historicoGravado();
        assertThat(historico.getProjeto()).isSameAs(projeto);
        assertThat(historico.getSituacaoAnterior()).isNull();
        assertThat(historico.getSituacaoNova()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        assertThat(historico.getUsuario()).isSameAs(ana);
        assertThat(historico.getDataHora()).isEqualTo(AGORA);
        assertThat(historico.getObservacao()).isEqualTo("Projeto aberto.");
    }

    @Test
    @DisplayName("O código segue o maior número já usado no ano")
    void testCodigoSequencial() {
        prepararAbertura("PRJ-2026-0041", "PRJ-2026-%");

        assertThat(service.abrir(3L, 5L).getCodigo()).isEqualTo("PRJ-2026-0042");
    }

    @Test
    @DisplayName("Na virada do ano o número volta para 0001, com o ano novo no código")
    void testCodigoNaViradaDoAno() {
        service = servicoNo(LocalDateTime.of(2027, 1, 1, 0, 0));
        prepararAbertura(null, "PRJ-2027-%");

        Projeto projeto = service.abrir(3L, 5L);

        assertThat(projeto.getCodigo()).isEqualTo("PRJ-2027-0001");
        assertThat(projeto.getDataAbertura()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    @Test
    @DisplayName("No último instante do ano o código ainda é do ano que termina")
    void testCodigoNoUltimoInstanteDoAno() {
        service = servicoNo(LocalDateTime.of(2026, 12, 31, 23, 59, 59));
        prepararAbertura("PRJ-2026-0187", "PRJ-2026-%");

        Projeto projeto = service.abrir(3L, 5L);

        assertThat(projeto.getCodigo()).isEqualTo("PRJ-2026-0188");
        assertThat(projeto.getDataAbertura()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("Depois do projeto 9999 do ano a abertura é recusada")
    void testCodigoEsgotado() {
        when(propriedadeRepository.findById(5L)).thenReturn(Optional.of(propriedade));
        anaLogada();
        when(projetoRepository.ultimoCodigo("PRJ-2026-%")).thenReturn("PRJ-2026-9999");

        assertThatThrownBy(() -> service.abrir(3L, 5L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("PRJ-2026-9999");
        verify(projetoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Código repetido por abertura simultânea vira mensagem para tentar de novo")
    void testCodigoRepetidoNoMesmoInstante() {
        when(propriedadeRepository.findById(5L)).thenReturn(Optional.of(propriedade));
        anaLogada();
        when(projetoRepository.ultimoCodigo("PRJ-2026-%")).thenReturn(null);
        when(projetoRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_projeto_codigo"));

        assertThatThrownBy(() -> service.abrir(3L, 5L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Outro projeto recebeu o código PRJ-2026-0001 no mesmo instante. Tente abrir de novo.");
        verifyNoInteractions(historicoRepository);
    }

    @Test
    @DisplayName("A propriedade tem de ser do produtor escolhido")
    void testPropriedadeDeOutroProdutor() {
        when(propriedadeRepository.findById(5L)).thenReturn(Optional.of(propriedade));

        assertThatThrownBy(() -> service.abrir(99L, 5L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A propriedade escolhida não pertence a esse produtor.");
        verify(projetoRepository, never()).saveAndFlush(any());
        verifyNoInteractions(historicoRepository);
    }

    @Test
    @DisplayName("Propriedade que não existe responde como registro não encontrado")
    void testPropriedadeInexistente() {
        when(propriedadeRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abrir(3L, 5L)).isInstanceOf(RegistroNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Sem usuário logado não há responsável, e o projeto não é aberto")
    void testAberturaSemUsuario() {
        when(propriedadeRepository.findById(5L)).thenReturn(Optional.of(propriedade));
        when(usuarioLogado.idAtual()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.abrir(3L, 5L))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Entre no sistema para abrir um projeto.");
        verify(projetoRepository, never()).saveAndFlush(any());
    }

    // ---------- Transições 2 a 17 ----------

    @ParameterizedTest(name = "transição {0}: {1} -> {2}")
    @MethodSource("br.edu.iftm.irrigacao.dominio.projeto.TabelaDoModelo#mudancas")
    @DisplayName("Cada transição permitida muda a situação e grava o histórico na mesma chamada")
    void testTransicaoPermitida(int numero, SituacaoProjeto de, SituacaoProjeto para) {
        Projeto projeto = projetoEm(de);
        when(projetoRepository.buscarParaAlterarSituacao(1L)).thenReturn(Optional.of(projeto));
        anaLogada();

        Projeto resultado = service.transitar(1L, para, "  Transição " + numero + " ");

        assertThat(resultado.getSituacao()).isEqualTo(para);
        verify(projetoRepository).save(projeto);
        HistoricoProjeto historico = historicoGravado();
        assertThat(historico.getProjeto()).isSameAs(projeto);
        assertThat(historico.getSituacaoAnterior()).isEqualTo(de);
        assertThat(historico.getSituacaoNova()).isEqualTo(para);
        assertThat(historico.getUsuario()).isSameAs(ana);
        assertThat(historico.getDataHora()).isEqualTo(AGORA);
        assertThat(historico.getObservacao()).isEqualTo("Transição " + numero);
    }

    @Test
    @DisplayName("Mudança feita sem usuário logado grava o histórico sem usuário e sem observação")
    void testTransicaoSemUsuario() {
        Projeto projeto = projetoEm(SituacaoProjeto.AGUARDANDO_RESPOSTA);
        when(projetoRepository.buscarParaAlterarSituacao(1L)).thenReturn(Optional.of(projeto));
        when(usuarioLogado.idAtual()).thenReturn(Optional.empty());

        service.transitar(1L, SituacaoProjeto.ORCANDO, "   ");

        HistoricoProjeto historico = historicoGravado();
        assertThat(historico.getUsuario()).isNull();
        assertThat(historico.getObservacao()).isNull();
    }

    // ---------- Transições proibidas ----------

    @Test
    @DisplayName("EM_LEVANTAMENTO -> ENCERRADO é recusada com mensagem clara, sem alterar nada")
    void testLevantamentoParaEncerrado() {
        Projeto projeto = projetoEm(SituacaoProjeto.EM_LEVANTAMENTO);
        when(projetoRepository.buscarParaAlterarSituacao(1L)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> service.transitar(1L, SituacaoProjeto.ENCERRADO, null))
                .isInstanceOf(TransicaoInvalidaException.class)
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O projeto PRJ-2026-0001 está em \"Em levantamento\" e não pode passar para "
                        + "\"Encerrado\". Daqui ele só pode seguir para \"Orçando\".");

        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.EM_LEVANTAMENTO);
        verify(projetoRepository, never()).save(any());
        verifyNoInteractions(historicoRepository);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("br.edu.iftm.irrigacao.dominio.projeto.TabelaDoModelo#saidasDasFinais")
    @DisplayName("Nenhuma saída de ENCERRADO ou CANCELADO é aceita")
    void testSaidaDeSituacaoFinal(SituacaoProjeto fim, SituacaoProjeto para) {
        Projeto projeto = projetoEm(fim);
        when(projetoRepository.buscarParaAlterarSituacao(1L)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> service.transitar(1L, para, null))
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessage("O projeto PRJ-2026-0001 está \"" + fim.getRotulo()
                        + "\", que é uma situação final: ele não muda mais de situação.");

        assertThat(projeto.getSituacao()).isEqualTo(fim);
        verify(projetoRepository, never()).save(any());
        verifyNoInteractions(historicoRepository);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("br.edu.iftm.irrigacao.dominio.projeto.TabelaDoModelo#proibidas")
    @DisplayName("Todo par fora da tabela é recusado, sem alterar nada")
    void testTransicaoProibida(SituacaoProjeto de, SituacaoProjeto para) {
        Projeto projeto = projetoEm(de);
        when(projetoRepository.buscarParaAlterarSituacao(1L)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> service.transitar(1L, para, "não deve gravar"))
                .isInstanceOfSatisfying(TransicaoInvalidaException.class, e -> {
                    assertThat(e.getSituacaoAtual()).isEqualTo(de);
                    assertThat(e.getSituacaoPedida()).isEqualTo(para);
                });

        assertThat(projeto.getSituacao()).isEqualTo(de);
        verify(projetoRepository, never()).save(any());
        verifyNoInteractions(historicoRepository);
    }

    @Test
    @DisplayName("Pedir a situação em que o projeto já está é recusado")
    void testMesmaSituacao() {
        when(projetoRepository.buscarParaAlterarSituacao(1L))
                .thenReturn(Optional.of(projetoEm(SituacaoProjeto.ORCANDO)));

        assertThatThrownBy(() -> service.transitar(1L, SituacaoProjeto.ORCANDO, null))
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessage("O projeto PRJ-2026-0001 já está em \"Orçando\".");
    }

    @Test
    @DisplayName("Situação nova vazia é recusada")
    void testSituacaoNovaVazia() {
        when(projetoRepository.buscarParaAlterarSituacao(1L))
                .thenReturn(Optional.of(projetoEm(SituacaoProjeto.ORCANDO)));

        assertThatThrownBy(() -> service.transitar(1L, null, null))
                .isInstanceOf(TransicaoInvalidaException.class)
                .hasMessage("Informe a nova situação do projeto PRJ-2026-0001.");
        verifyNoInteractions(historicoRepository);
    }

    @Test
    @DisplayName("Observação com mais de 500 caracteres é recusada, sem alterar nada")
    void testObservacaoLonga() {
        Projeto projeto = projetoEm(SituacaoProjeto.ORCANDO);
        when(projetoRepository.buscarParaAlterarSituacao(1L)).thenReturn(Optional.of(projeto));

        assertThatThrownBy(() -> service.transitar(1L, SituacaoProjeto.AGUARDANDO_RESPOSTA, "x".repeat(501)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A observação pode ter até 500 caracteres.");
        assertThat(projeto.getSituacao()).isEqualTo(SituacaoProjeto.ORCANDO);
        verifyNoInteractions(historicoRepository);
    }

    @Test
    @DisplayName("Projeto que não existe responde como registro não encontrado")
    void testProjetoInexistente() {
        when(projetoRepository.buscarParaAlterarSituacao(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transitar(1L, SituacaoProjeto.ORCANDO, null))
                .isInstanceOf(RegistroNaoEncontradoException.class);
    }
}
