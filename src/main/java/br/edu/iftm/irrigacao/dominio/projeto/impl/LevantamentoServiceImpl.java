package br.edu.iftm.irrigacao.dominio.projeto.impl;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.projeto.AgendaSemanal;
import br.edu.iftm.irrigacao.dominio.projeto.AgendaSemanal.DiaDaAgenda;
import br.edu.iftm.irrigacao.dominio.projeto.ArmazenamentoDeFotos;
import br.edu.iftm.irrigacao.dominio.projeto.DadosLevantamento;
import br.edu.iftm.irrigacao.dominio.projeto.FotoArmazenada;
import br.edu.iftm.irrigacao.dominio.projeto.FotoEnviada;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.LevantamentoService;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.dominio.projeto.SituacaoProjeto;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFoto;
import br.edu.iftm.irrigacao.persistencia.LevantamentoRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;

/**
 * Agendamento e agenda (RF02), levantamento em campo com fotos (RF03, RNF02) e G1 (RF04). As fotos
 * seguem as regras do {@link ArmazenamentoDeFotos}.
 */
@Service
public class LevantamentoServiceImpl implements LevantamentoService {

    private static final String PASTA_LEVANTAMENTO = "levantamento";

    private final LevantamentoRepository levantamentoRepository;
    private final ProjetoRepository projetoRepository;
    private final Clock clock;
    private final ArmazenamentoDeFotos armazenamento;

    public LevantamentoServiceImpl(LevantamentoRepository levantamentoRepository, ProjetoRepository projetoRepository,
            Clock clock, ArmazenamentoDeFotos armazenamento) {
        this.levantamentoRepository = levantamentoRepository;
        this.projetoRepository = projetoRepository;
        this.clock = clock;
        this.armazenamento = armazenamento;
    }

    // ---------- Agendamento e agenda (RF02) ----------

    @Override
    @Transactional(readOnly = true)
    public Optional<Levantamento> buscarPorProjeto(Long projetoId) {
        return levantamentoRepository.findByProjetoId(projetoId);
    }

    @Override
    public boolean podeAgendarVisita(Projeto projeto, Levantamento levantamento) {
        return projeto.getSituacao() == SituacaoProjeto.EM_LEVANTAMENTO
                && (levantamento == null || levantamento.getDataVisita() == null);
    }

    @Override
    @Transactional
    public Levantamento agendarVisita(Long projetoId, LocalDateTime dataAgendada) {
        Projeto projeto = buscarProjeto(projetoId);
        Levantamento levantamento = levantamentoRepository.findByProjetoId(projetoId).orElse(null);
        if (projeto.getSituacao() != SituacaoProjeto.EM_LEVANTAMENTO) {
            throw new RegraNegocioException("A visita só pode ser agendada com o projeto em \""
                    + SituacaoProjeto.EM_LEVANTAMENTO.getRotulo() + "\". O projeto " + projeto.getCodigo()
                    + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
        if (!podeAgendarVisita(projeto, levantamento)) {
            throw new RegraNegocioException("A visita do projeto " + projeto.getCodigo()
                    + " já foi realizada; o agendamento não muda mais.");
        }
        if (dataAgendada == null) {
            throw new RegraNegocioException("Informe a data e a hora da visita.");
        }
        LocalDateTime data = dataAgendada.truncatedTo(ChronoUnit.MINUTES);
        if (data.isBefore(LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES))) {
            throw new RegraNegocioException("A visita não pode ser agendada para uma data e hora que já passaram.");
        }
        if (levantamento == null) {
            levantamento = new Levantamento();
            levantamento.setProjeto(projeto);
        }
        levantamento.setDataAgendada(data);
        return levantamentoRepository.save(levantamento);
    }

    @Override
    @Transactional(readOnly = true)
    public AgendaSemanal agendaDaSemana(LocalDate dia) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate inicio = (dia == null ? hoje : dia).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate proximaSegunda = inicio.plusWeeks(1);

        Map<LocalDate, List<Levantamento>> porDia = new LinkedHashMap<>();
        for (LocalDate data = inicio; data.isBefore(proximaSegunda); data = data.plusDays(1)) {
            porDia.put(data, new ArrayList<>());
        }
        for (Levantamento visita : levantamentoRepository.listarAgendadosEntre(inicio.atStartOfDay(),
                proximaSegunda.atStartOfDay())) {
            porDia.get(visita.getDataAgendada().toLocalDate()).add(visita);
        }

        List<DiaDaAgenda> dias = porDia.entrySet().stream()
                .map(e -> new DiaDaAgenda(e.getKey(), e.getKey().equals(hoje), e.getValue()))
                .toList();
        return new AgendaSemanal(inicio, dias);
    }

    // ---------- Levantamento em campo (RF03) ----------

    @Override
    public boolean podePreencher(Projeto projeto) {
        return projeto.getSituacao() == SituacaoProjeto.EM_LEVANTAMENTO;
    }

    @Override
    @Transactional
    public Levantamento salvar(Long projetoId, DadosLevantamento dados, List<FotoEnviada> fotos) {
        Projeto projeto = buscarProjeto(projetoId);
        if (!podePreencher(projeto)) {
            throw new RegraNegocioException("O levantamento só pode ser alterado com o projeto em \""
                    + SituacaoProjeto.EM_LEVANTAMENTO.getRotulo() + "\". O projeto " + projeto.getCodigo()
                    + " está em \"" + projeto.getSituacao().getRotulo() + "\".");
        }
        conferirValores(dados);
        // Todas as fotos são conferidas antes de gravar a primeira
        List<TipoFoto> tipos = fotos.stream().map(armazenamento::conferir).toList();

        Levantamento levantamento = levantamentoRepository.findByProjetoId(projetoId).orElseGet(() -> {
            Levantamento novo = new Levantamento();
            novo.setProjeto(projeto);
            return novo;
        });
        levantamento.setDataVisita(dados.dataVisita() == null ? null : dados.dataVisita().truncatedTo(ChronoUnit.MINUTES));
        levantamento.setCultura(texto(dados.cultura()));
        levantamento.setAreaIrrigadaHa(dados.areaIrrigadaHa());
        levantamento.setFonteAgua(dados.fonteAgua());
        levantamento.setVazaoFonte(dados.vazaoFonte());
        levantamento.setTopografia(texto(dados.topografia()));
        levantamento.setLatitude(dados.latitude());
        levantamento.setLongitude(dados.longitude());
        for (int i = 0; i < fotos.size(); i++) {
            levantamento.getFotos().add(armazenamento.gravar(PASTA_LEVANTAMENTO + "/" + projetoId, fotos.get(i),
                    tipos.get(i)));
        }
        return levantamentoRepository.save(levantamento);
    }

    @Override
    public List<String> validarCamposObrigatorios(Levantamento levantamento) {
        List<String> faltam = new ArrayList<>();
        Levantamento l = levantamento == null ? new Levantamento() : levantamento;
        if (l.getDataVisita() == null) {
            faltam.add("data da visita");
        }
        if (vazio(l.getCultura())) {
            faltam.add("cultura");
        }
        if (l.getAreaIrrigadaHa() == null) {
            faltam.add("área irrigada");
        }
        if (l.getFonteAgua() == null) {
            faltam.add("fonte de água");
        }
        if (l.getVazaoFonte() == null) {
            faltam.add("vazão da fonte");
        }
        if (vazio(l.getTopografia())) {
            faltam.add("topografia");
        }
        if (l.getLatitude() == null) {
            faltam.add("latitude");
        }
        if (l.getLongitude() == null) {
            faltam.add("longitude");
        }
        if (l.getFotos() == null || l.getFotos().isEmpty()) {
            faltam.add("pelo menos uma foto");
        }
        return faltam;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> camposFaltantes(Long projetoId) {
        return validarCamposObrigatorios(levantamentoRepository.findByProjetoId(projetoId).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public FotoArmazenada foto(Long projetoId, int indice) {
        List<String> fotos = levantamentoRepository.findByProjetoId(projetoId)
                .map(Levantamento::getFotos)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Levantamento não encontrado."));
        if (indice < 0 || indice >= fotos.size()) {
            throw new RegistroNaoEncontradoException("Foto não encontrada.");
        }
        return armazenamento.abrir(fotos.get(indice));
    }

    // ---------- Apoio ----------

    private Projeto buscarProjeto(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    private static void conferirValores(DadosLevantamento dados) {
        exigirPositivo(dados.areaIrrigadaHa(), "A área irrigada deve ser maior que zero.");
        exigirPositivo(dados.vazaoFonte(), "A vazão da fonte deve ser maior que zero.");
        if (dados.latitude() != null && (dados.latitude() < -90 || dados.latitude() > 90)) {
            throw new RegraNegocioException("A latitude vai de -90 a 90.");
        }
        if (dados.longitude() != null && (dados.longitude() < -180 || dados.longitude() > 180)) {
            throw new RegraNegocioException("A longitude vai de -180 a 180.");
        }
        if (dados.cultura() != null && dados.cultura().strip().length() > 80) {
            throw new RegraNegocioException("A cultura pode ter até 80 caracteres.");
        }
        if (dados.topografia() != null && dados.topografia().strip().length() > 120) {
            throw new RegraNegocioException("A topografia pode ter até 120 caracteres.");
        }
    }

    private static void exigirPositivo(Double valor, String mensagem) {
        if (valor != null && valor <= 0) {
            throw new RegraNegocioException(mensagem);
        }
    }

    private static String texto(String valor) {
        return vazio(valor) ? null : valor.strip();
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
