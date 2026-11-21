package br.edu.iftm.irrigacao.apresentacao.web;

import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.ADMINISTRADOR;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.ALMOXARIFE;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.COMPRADOR;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.DIRETORIA;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.FINANCEIRO;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.PROJETISTA;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.TECNICO_CAMPO;
import static br.edu.iftm.irrigacao.dominio.acesso.Perfil.VENDEDOR;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioAutenticado;
import br.edu.iftm.irrigacao.dominio.comercial.Proposta;
import br.edu.iftm.irrigacao.dominio.comercial.SituacaoProposta;
import br.edu.iftm.irrigacao.dominio.execucao.OrdemServico;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;

/**
 * O que falta fazer no projeto e com quem está, no topo da página do projeto. Todo perfil vê o
 * passo; só quem pode fazê-lo vê o botão ({@link #acao()}), que leva à tela da etapa.
 *
 * <p>Os perfis de cada botão seguem os do {@code SecurityConfig} para o endereço de destino, para o
 * botão nunca levar ao acesso negado. Encerrado ou cancelado, o projeto não tem próximo passo.
 *
 * @param titulo      o que fazer, começando em minúscula: vem depois de "Próximo passo:"
 * @param responsavel quem faz, para "Com ..." e "Aguardando ...": "o almoxarifado", "Tito Técnico"
 * @param detalhe     frase de apoio, ou {@code null}
 * @param acao        o botão, só para quem pode fazer o passo; {@code null} para os outros perfis
 */
public record ProximoPasso(String titulo, String responsavel, String detalhe, Acao acao) {

    public record Acao(String rotulo, String url) {
    }

    /** Quem está vendo a página: os perfis e, para o técnico, o id que diz em quais OS ele está escalado. */
    public record Quem(Set<Perfil> perfis, Long usuarioId) {

        public static Quem de(Authentication autenticacao) {
            if (autenticacao == null) {
                return new Quem(Set.of(), null);
            }
            Set<String> nomes = autenticacao.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());
            Set<Perfil> perfis = EnumSet.noneOf(Perfil.class);
            for (Perfil perfil : Perfil.values()) {
                if (nomes.contains(perfil.name())) {
                    perfis.add(perfil);
                }
            }
            Long id = autenticacao.getPrincipal() instanceof UsuarioAutenticado usuario ? usuario.getId() : null;
            return new Quem(perfis, id);
        }

        boolean temAlgum(Set<Perfil> permitidos) {
            return perfis.contains(ADMINISTRADOR) || perfis.stream().anyMatch(permitidos::contains);
        }

        /** O técnico de campo só abre as ordens em que está escalado; o projetista abre todas. */
        boolean abre(OrdemServico ordem) {
            if (temAlgum(EnumSet.of(PROJETISTA))) {
                return true;
            }
            return perfis.contains(TECNICO_CAMPO) && usuarioId != null
                    && ordem.getTecnicos().stream().anyMatch(t -> usuarioId.equals(t.getId()));
        }
    }

    private static final Set<Perfil> COMERCIAL = EnumSet.of(VENDEDOR, DIRETORIA);
    private static final Set<Perfil> CAMPO = EnumSet.of(TECNICO_CAMPO, PROJETISTA);
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_E_HORA = DateTimeFormatter
            .ofPattern("EEEE, dd/MM/yyyy 'às' HH:mm", Locale.of("pt", "BR"));

    /**
     * @param levantamento         o levantamento do projeto, ou {@code null} se ainda não existe
     * @param levantamentoCompleto o levantamento não tem mais campo faltando
     * @param ultimaProposta       a versão mais nova da proposta, ou {@code null}
     * @param ordens               as ordens de serviço do projeto
     * @return o passo, ou vazio com o projeto encerrado ou cancelado
     */
    public static Optional<ProximoPasso> de(Projeto projeto, Levantamento levantamento, boolean levantamentoCompleto,
            Proposta ultimaProposta, List<OrdemServico> ordens, Quem quem) {
        Long id = projeto.getId();
        return Optional.ofNullable(switch (projeto.getSituacao()) {
            case EM_LEVANTAMENTO -> {
                if (levantamento == null || levantamento.getDataAgendada() == null) {
                    yield passo("agendar a visita técnica", "o comercial", null,
                            quem, COMERCIAL, "Agendar visita", "/comercial/projetos/" + id + "/visita");
                }
                if (!levantamentoCompleto) {
                    yield passo("fazer a visita e preencher o levantamento", "a equipe de campo",
                            "Visita agendada para " + DIA_E_HORA.format(levantamento.getDataAgendada()) + ".",
                            quem, CAMPO, "Preencher levantamento", "/campo/projetos/" + id + "/levantamento");
                }
                yield passo("dimensionar e montar a lista técnica", "a engenharia", null,
                        quem, EnumSet.of(PROJETISTA), "Dimensionar", "/engenharia/projetos/" + id + "/dimensionamento");
            }
            case ORCANDO -> {
                SituacaoProposta situacao = ultimaProposta == null ? null : ultimaProposta.getSituacao();
                if (situacao == SituacaoProposta.AGUARDANDO_DIRETORIA) {
                    yield passo("aprovar ou negar o desconto da proposta " + ultimaProposta.getNumero(), "a diretoria",
                            "O desconto passou da alçada do vendedor.",
                            quem, EnumSet.of(DIRETORIA), "Abrir aprovações", "/diretoria/aprovacoes");
                }
                if (situacao == SituacaoProposta.EM_ELABORACAO) {
                    yield passo("revisar a proposta " + ultimaProposta.getNumero() + " e enviar ao cliente",
                            "o comercial", null, quem, COMERCIAL, "Abrir a proposta " + ultimaProposta.getNumero(),
                            "/comercial/propostas/" + ultimaProposta.getId());
                }
                yield passo("gerar o orçamento e a proposta", "o comercial", null,
                        quem, COMERCIAL, "Gerar orçamento", "/comercial/projetos/" + id + "/orcamento");
            }
            case AGUARDANDO_RESPOSTA -> ultimaProposta == null
                    ? new ProximoPasso("registrar a resposta do cliente", "o comercial", null, null)
                    : passo("registrar a resposta do cliente à proposta " + ultimaProposta.getNumero(), "o comercial",
                            ultimaProposta.getDataValidade() == null ? null
                                    : "A proposta vale até " + DATA.format(ultimaProposta.getDataValidade()) + ".",
                            quem, COMERCIAL, "Abrir a proposta " + ultimaProposta.getNumero(),
                            "/comercial/propostas/" + ultimaProposta.getId());
            case PREPARANDO_MATERIAL -> passo("separar o material e liberar a execução", "o almoxarifado",
                    "A obra pode começar com parte do material: basta um item separado.",
                    quem, EnumSet.of(ALMOXARIFE, COMPRADOR), "Abrir a separação", "/estoque/projetos/" + id + "/separacao");
            case PROGRAMANDO_EXECUCAO -> passo("programar a execução e emitir a ordem de serviço", "a engenharia", null,
                    quem, EnumSet.of(PROJETISTA), "Programar execução", "/engenharia/projetos/" + id + "/programacao");
            case EXECUTANDO_INSTALACAO -> noCampo("instalar e apontar as horas e o material", ordens,
                    ordem -> ordem.getInstalacaoConcluidaEm() == null, quem);
            case VALIDANDO_MEDICAO -> noCampo("medir a pressão e a vazão", ordens, ordem -> true, quem);
            case CORRIGINDO_INSTALACAO -> noCampo("corrigir a instalação e registrar a correção", ordens,
                    ordem -> true, quem);
            case COLHENDO_ACEITE -> noCampo("colher o aceite do cliente", ordens, ordem -> true, quem);
            case TRATANDO_PENDENCIA -> noCampo("resolver a pendência e marcar como resolvida", ordens,
                    ordem -> true, quem);
            case FINALIZANDO_EXECUCAO -> projeto.isSobrasDevolvidas()
                    ? passo("liberar o faturamento e registrar a garantia", "o financeiro", null,
                            quem, EnumSet.of(FINANCEIRO), "Liberar faturamento", "/financeiro/liberacoes/" + id)
                    // Ver as sobras o comprador também vê; devolver, só o almoxarife
                    : passo("conferir e devolver as sobras ao estoque", "o almoxarifado", null,
                            quem, EnumSet.of(ALMOXARIFE), "Devolver sobras", "/estoque/projetos/" + id + "/sobras");
            case ENCERRADO, CANCELADO -> null;
        });
    }

    private static ProximoPasso passo(String titulo, String responsavel, String detalhe, Quem quem,
            Set<Perfil> permitidos, String rotulo, String url) {
        return new ProximoPasso(titulo, responsavel, detalhe, quem.temAlgum(permitidos) ? new Acao(rotulo, url) : null);
    }

    /**
     * Passo feito na ordem de serviço, pelos técnicos escalados. O botão abre a primeira ordem
     * aberta que o usuário pode abrir.
     *
     * @param pendente quais ordens abertas ainda têm o passo por fazer
     */
    private static ProximoPasso noCampo(String titulo, List<OrdemServico> ordens, Predicate<OrdemServico> pendente,
            Quem quem) {
        List<OrdemServico> abertas = ordens.stream()
                .filter(ordem -> ordem.getDataConclusao() == null)
                .filter(pendente)
                .sorted(Comparator.comparing(OrdemServico::getNumero))
                .toList();
        List<String> tecnicos = abertas.stream()
                .flatMap(ordem -> ordem.getTecnicos().stream())
                .map(Usuario::getNome)
                .distinct()
                .toList();
        String detalhe = abertas.size() > 1
                ? "Nas OS " + juntar(abertas.stream().map(ordem -> String.valueOf(ordem.getNumero())).toList()) + "."
                : null;
        Acao acao = abertas.stream()
                .filter(quem::abre)
                .findFirst()
                .map(ordem -> new Acao("Abrir a OS " + ordem.getNumero(), "/campo/ordens/" + ordem.getId()))
                .orElse(null);
        return new ProximoPasso(titulo, tecnicos.isEmpty() ? "a equipe de campo" : juntar(tecnicos), detalhe, acao);
    }

    /** "A", "A e B", "A, B e C". */
    private static String juntar(Collection<String> partes) {
        List<String> lista = List.copyOf(partes);
        if (lista.size() == 1) {
            return lista.get(0);
        }
        return String.join(", ", lista.subList(0, lista.size() - 1)) + " e " + lista.get(lista.size() - 1);
    }
}
