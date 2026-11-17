package br.edu.iftm.irrigacao.dominio.projeto.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.projeto.ArmazenamentoDeFotos;
import br.edu.iftm.irrigacao.dominio.projeto.DadosLevantamento;
import br.edu.iftm.irrigacao.dominio.projeto.FotoEnviada;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFonteAgua;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFoto;
import br.edu.iftm.irrigacao.persistencia.LevantamentoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * G1 (RF04) e a conferência das fotos do levantamento (RF03), com os repositórios mockados e a
 * pasta de uploads numa pasta temporária.
 */
@ExtendWith(MockitoExtension.class)
class LevantamentoCampoServiceTest {

    private static final byte[] JPEG = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10, 'J', 'F', 'I', 'F' };
    private static final byte[] PNG = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 0x0D };
    private static final byte[] GIF = { 'G', 'I', 'F', '8', '9', 'a', 1, 0, 1, 0 };

    @Mock
    private LevantamentoRepository levantamentoRepository;

    @Mock
    private ProjetoRepository projetoRepository;

    @TempDir
    private Path uploads;

    private LevantamentoServiceImpl service;

    @BeforeEach
    void criarService() {
        ZoneId zona = ZoneId.systemDefault();
        service = new LevantamentoServiceImpl(levantamentoRepository, projetoRepository,
                Clock.fixed(LocalDateTime.of(2026, 11, 16, 9, 0).atZone(zona).toInstant(), zona), new ArmazenamentoDeFotos(uploads.toString()));
    }

    private static Levantamento completo() {
        return new Levantamento(9L, null, null, LocalDateTime.of(2026, 11, 16, 8, 40), "Café", 12.5,
                TipoFonteAgua.POCO_ARTESIANO, 8.0, "Plana, com leve declive para o córrego",
                new ArrayList<>(List.of("levantamento/1/a.jpg")), -18.9113, -48.2622);
    }

    private static DadosLevantamento dadosCompletos() {
        return new DadosLevantamento(LocalDateTime.of(2026, 11, 16, 8, 40), "Café", 12.5, TipoFonteAgua.RIO_CORREGO,
                8.0, "Plana", -18.9113, -48.2622);
    }

    private Projeto projetoEm(SituacaoProjeto situacao) {
        Projeto projeto = new Projeto();
        projeto.setId(1L);
        projeto.setCodigo("PRJ-2026-0001");
        projeto.setSituacao(situacao);
        return projeto;
    }

    // ---------- G1 ----------

    static Stream<Arguments> camposObrigatorios() {
        return Stream.of(
                Arguments.of("data da visita", (Consumer<Levantamento>) l -> l.setDataVisita(null)),
                Arguments.of("cultura", (Consumer<Levantamento>) l -> l.setCultura("   ")),
                Arguments.of("área irrigada", (Consumer<Levantamento>) l -> l.setAreaIrrigadaHa(null)),
                Arguments.of("fonte de água", (Consumer<Levantamento>) l -> l.setFonteAgua(null)),
                Arguments.of("vazão da fonte", (Consumer<Levantamento>) l -> l.setVazaoFonte(null)),
                Arguments.of("topografia", (Consumer<Levantamento>) l -> l.setTopografia(null)),
                Arguments.of("latitude", (Consumer<Levantamento>) l -> l.setLatitude(null)),
                Arguments.of("longitude", (Consumer<Levantamento>) l -> l.setLongitude(null)),
                Arguments.of("pelo menos uma foto", (Consumer<Levantamento>) l -> l.getFotos().clear()));
    }

    @ParameterizedTest(name = "sem {0}")
    @MethodSource("camposObrigatorios")
    @DisplayName("G1 aponta exatamente o campo que falta")
    void testCadaCampoFaltando(String campo, Consumer<Levantamento> tirar) {
        Levantamento levantamento = completo();
        tirar.accept(levantamento);

        assertThat(service.validarCamposObrigatorios(levantamento)).containsExactly(campo);
    }

    @Test
    @DisplayName("G1 com tudo preenchido não aponta nada")
    void testCompleto() {
        assertThat(service.validarCamposObrigatorios(completo())).isEmpty();
    }

    @Test
    @DisplayName("Sem levantamento, ou só agendado, falta tudo, na ordem da tela")
    void testSemNada() {
        List<String> tudo = List.of("data da visita", "cultura", "área irrigada", "fonte de água", "vazão da fonte",
                "topografia", "latitude", "longitude", "pelo menos uma foto");

        assertThat(service.validarCamposObrigatorios(null)).containsExactlyElementsOf(tudo);
        assertThat(service.validarCamposObrigatorios(new Levantamento())).containsExactlyElementsOf(tudo);
    }

    // ---------- Fotos ----------

    @Test
    @DisplayName("Formato reconhecido pelos primeiros bytes, não pelo nome")
    void testDeteccaoDoFormato() {
        assertThat(TipoFoto.detectar(JPEG)).contains(TipoFoto.JPEG);
        assertThat(TipoFoto.detectar(PNG)).contains(TipoFoto.PNG);
        assertThat(TipoFoto.detectar(GIF)).isEmpty();
        assertThat(TipoFoto.detectar("texto qualquer".getBytes())).isEmpty();
        assertThat(TipoFoto.detectar(new byte[] { (byte) 0xFF, (byte) 0xD8 })).isEmpty();
        assertThat(TipoFoto.detectar(null)).isEmpty();
    }

    @Test
    @DisplayName("Arquivo que não é JPEG nem PNG é recusado com o nome dele, sem gravar nada")
    void testArquivoQueNaoEFoto() throws Exception {
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projetoEm(SituacaoProjeto.EM_LEVANTAMENTO)));

        assertThatThrownBy(() -> service.salvar(1L, dadosCompletos(),
                List.of(new FotoEnviada("C:\\fotos\\poço.jpg", "não sou uma foto".getBytes()))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O arquivo \"poço.jpg\" não é uma foto JPEG nem PNG.");
        assertThatThrownBy(() -> service.salvar(1L, dadosCompletos(), List.of(new FotoEnviada("animada.gif", GIF))))
                .isInstanceOf(RegraNegocioException.class);

        verify(levantamentoRepository, never()).save(any());
        assertThat(Files.exists(uploads.resolve("levantamento"))).isFalse();
    }

    @Test
    @DisplayName("Foto com mais de 5 MB é recusada")
    void testFotoGrandeDemais() {
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projetoEm(SituacaoProjeto.EM_LEVANTAMENTO)));
        byte[] grande = Arrays.copyOf(JPEG, 5 * 1024 * 1024 + 1);

        assertThatThrownBy(() -> service.salvar(1L, dadosCompletos(), List.of(new FotoEnviada("grande.jpg", grande))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A foto \"grande.jpg\" passa de 5 MB.");
        verify(levantamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Uma foto inválida no meio do envio impede a gravação de todas")
    void testUmaInvalidaBarraTodas() {
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projetoEm(SituacaoProjeto.EM_LEVANTAMENTO)));

        assertThatThrownBy(() -> service.salvar(1L, dadosCompletos(), List.of(new FotoEnviada("boa.jpg", JPEG),
                new FotoEnviada("ruim.txt", "texto".getBytes()), new FotoEnviada("boa.png", PNG))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ruim.txt");

        assertThat(Files.exists(uploads.resolve("levantamento"))).isFalse();
    }

    @Test
    @DisplayName("A foto é gravada com nome UUID e a extensão do formato, na pasta do projeto")
    void testNomeGeradoPeloSistema() throws Exception {
        Projeto projeto = projetoEm(SituacaoProjeto.EM_LEVANTAMENTO);
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projeto));
        when(levantamentoRepository.findByProjetoId(1L)).thenReturn(Optional.empty());
        when(levantamentoRepository.save(any(Levantamento.class))).thenAnswer(chamada -> chamada.getArgument(0));

        // O nome original tenta sair da pasta e ainda vem com a extensão errada
        Levantamento salvo = service.salvar(1L, dadosCompletos(),
                List.of(new FotoEnviada("../../config/foto.gif", PNG), new FotoEnviada("IMG_0001.JPG", JPEG)));

        assertThat(salvo.getFotos()).hasSize(2);
        assertThat(salvo.getFotos().get(0)).matches("levantamento/1/[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.png");
        assertThat(salvo.getFotos().get(1)).matches("levantamento/1/[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.jpg");
        assertThat(Files.readAllBytes(uploads.resolve(salvo.getFotos().get(0)))).isEqualTo(PNG);
        assertThat(Files.readAllBytes(uploads.resolve(salvo.getFotos().get(1)))).isEqualTo(JPEG);
        try (Stream<Path> arquivos = Files.list(uploads.resolve("levantamento/1"))) {
            assertThat(arquivos.map(p -> p.getFileName().toString()))
                    .hasSize(2)
                    .noneMatch(nome -> nome.contains("foto") || nome.contains("IMG_0001"));
        }
        assertThat(salvo.getCultura()).isEqualTo("Café");
        assertThat(salvo.getProjeto()).isSameAs(projeto);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = SituacaoProjeto.class, names = "EM_LEVANTAMENTO", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Fora de EM_LEVANTAMENTO o levantamento não muda")
    void testForaDoLevantamento(SituacaoProjeto situacao) {
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projetoEm(situacao)));

        assertThatThrownBy(() -> service.salvar(1L, dadosCompletos(), List.of(new FotoEnviada("a.jpg", JPEG))))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageStartingWith("O levantamento só pode ser alterado com o projeto em \"Em levantamento\".");
        verify(levantamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Área, vazão e coordenada fora da faixa são recusadas")
    void testValoresForaDaFaixa() {
        when(projetoRepository.findById(1L)).thenReturn(Optional.of(projetoEm(SituacaoProjeto.EM_LEVANTAMENTO)));

        assertThatThrownBy(() -> service.salvar(1L, new DadosLevantamento(null, null, 0.0, null, null, null, null, null),
                List.of())).hasMessage("A área irrigada deve ser maior que zero.");
        assertThatThrownBy(() -> service.salvar(1L, new DadosLevantamento(null, null, null, null, -1.0, null, null, null),
                List.of())).hasMessage("A vazão da fonte deve ser maior que zero.");
        assertThatThrownBy(() -> service.salvar(1L, new DadosLevantamento(null, null, null, null, null, null, 91.0, null),
                List.of())).hasMessage("A latitude vai de -90 a 90.");
        assertThatThrownBy(() -> service.salvar(1L, new DadosLevantamento(null, null, null, null, null, null, null, 181.0),
                List.of())).hasMessage("A longitude vai de -180 a 180.");
        verify(levantamentoRepository, never()).save(any());
    }
}
