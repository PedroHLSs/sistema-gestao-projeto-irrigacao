package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Agendamento da visita técnica e agenda (RF02), registro do levantamento em campo com fotos e
 * coordenada (RF03, RNF02) e conferência dos campos obrigatórios (RF04, G1).
 *
 * <p>O {@link Levantamento} nasce no agendamento, só com {@code dataAgendada}. Os dados de campo são
 * preenchidos depois, na visita, e só enquanto o projeto está em EM_LEVANTAMENTO.
 */
public interface LevantamentoService {

    /** Tamanho máximo de cada foto, em bytes: 5 MB. */
    long TAMANHO_MAXIMO_FOTO = 5L * 1024 * 1024;

    Optional<Levantamento> buscarPorProjeto(Long projetoId);

    /** O levantamento de cada projeto que já tem um, por id do projeto, numa consulta só. */
    Map<Long, Levantamento> buscarPorProjetos(Collection<Long> projetoIds);

    /**
     * A visita pode ser agendada, ou reagendada, enquanto o projeto está em EM_LEVANTAMENTO e a
     * visita ainda não aconteceu.
     *
     * @param levantamento o levantamento do projeto; {@code null} se ainda não foi agendado
     */
    boolean podeAgendarVisita(Projeto projeto, Levantamento levantamento);

    /**
     * Cria o levantamento só com a data agendada ou, se ele já existe e a visita não aconteceu,
     * troca a data.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException se a data já passou ou se o
     *                                                             projeto não permite agendamento
     */
    Levantamento agendarVisita(Long projetoId, LocalDateTime dataAgendada);

    /**
     * Visitas da semana (segunda a domingo) que contém o dia informado.
     *
     * @param dia qualquer dia da semana desejada; {@code null} é a semana de hoje
     */
    AgendaSemanal agendaDaSemana(LocalDate dia);

    /** O levantamento só é preenchido e alterado com o projeto em EM_LEVANTAMENTO (RF03). */
    boolean podePreencher(Projeto projeto);

    /**
     * Grava os dados de campo e acrescenta as fotos novas, como rascunho: nada é obrigatório aqui
     * (RF03). Cria o levantamento se a visita não tinha sido agendada.
     *
     * <p>Cada foto precisa ser JPEG ou PNG, reconhecida pelos primeiros bytes, com até
     * {@link #TAMANHO_MAXIMO_FOTO} bytes. É gravada em
     * {@code {irrigacao.upload.dir}/levantamento/{projetoId}/} com um nome UUID gerado pelo sistema.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegraNegocioException fora de EM_LEVANTAMENTO, com
     *                                                             valor fora da faixa ou com foto
     *                                                             inválida; nesse caso nenhuma foto
     *                                                             é gravada
     */
    Levantamento salvar(Long projetoId, DadosLevantamento dados, List<FotoEnviada> fotos);

    /**
     * G1 (RF04), a operação {@code validarCamposObrigatorios()} da Figura 10. Em vez de só dizer se o
     * levantamento está completo, devolve o que falta, para a tela mostrar: data da visita, cultura,
     * área irrigada, fonte de água, vazão da fonte, topografia, latitude, longitude e pelo menos uma
     * foto.
     *
     * @param levantamento {@code null} quando a visita nem foi agendada: falta tudo
     * @return os nomes dos campos que faltam, na ordem da tela; vazio quando está completo
     */
    List<String> validarCamposObrigatorios(Levantamento levantamento);

    /** O que falta no levantamento do projeto; vazio quando ele está completo (G1). */
    List<String> camposFaltantes(Long projetoId);

    /**
     * Foto de posição {@code indice} do levantamento do projeto, para ser servida.
     *
     * @throws br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException se o projeto, a posição
     *                                                                     ou o arquivo não existir
     */
    FotoArmazenada foto(Long projetoId, int indice);
}
