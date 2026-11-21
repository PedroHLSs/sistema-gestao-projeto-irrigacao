package br.edu.iftm.irrigacao.dominio.execucao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Ordem de serviço da instalação, com a data programada e a equipe de técnicos (RF15).
 * A ordem está aberta enquanto {@code dataConclusao} está vazia.
 *
 * <p>Um projeto pode ter várias ordens abertas ao mesmo tempo, uma por equipe ou etapa. Cada uma
 * marca em {@code instalacaoConcluidaEm} quando a sua parte da instalação terminou; a medição só
 * começa quando todas as abertas terminaram (RF16).
 */
@Entity
@Table(name = "ordem_servico")
public class OrdemServico extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @NotNull
    @Column(name = "numero", nullable = false)
    private Integer numero;

    @NotNull
    @Column(name = "data_programada", nullable = false)
    private LocalDate dataProgramada;

    @Column(name = "data_conclusao")
    private LocalDate dataConclusao;

    /** Quando a equipe desta ordem terminou a sua parte da instalação; vazio enquanto trabalha. */
    @Column(name = "instalacao_concluida_em")
    private LocalDateTime instalacaoConcluidaEm;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "ordem_servico_tecnico",
            joinColumns = @JoinColumn(name = "ordem_servico_id"),
            inverseJoinColumns = @JoinColumn(name = "usuario_id"))
    private List<Usuario> tecnicos = new ArrayList<>();

    public OrdemServico() {
    }

    public OrdemServico(Long id, Projeto projeto, Integer numero, LocalDate dataProgramada,
            LocalDate dataConclusao, LocalDateTime instalacaoConcluidaEm, List<Usuario> tecnicos) {
        this.id = id;
        this.projeto = projeto;
        this.numero = numero;
        this.dataProgramada = dataProgramada;
        this.dataConclusao = dataConclusao;
        this.instalacaoConcluidaEm = instalacaoConcluidaEm;
        this.tecnicos = tecnicos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Projeto getProjeto() {
        return projeto;
    }

    public void setProjeto(Projeto projeto) {
        this.projeto = projeto;
    }

    public Integer getNumero() {
        return numero;
    }

    public void setNumero(Integer numero) {
        this.numero = numero;
    }

    public LocalDate getDataProgramada() {
        return dataProgramada;
    }

    public void setDataProgramada(LocalDate dataProgramada) {
        this.dataProgramada = dataProgramada;
    }

    public LocalDate getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(LocalDate dataConclusao) {
        this.dataConclusao = dataConclusao;
    }

    public LocalDateTime getInstalacaoConcluidaEm() {
        return instalacaoConcluidaEm;
    }

    public void setInstalacaoConcluidaEm(LocalDateTime instalacaoConcluidaEm) {
        this.instalacaoConcluidaEm = instalacaoConcluidaEm;
    }

    public List<Usuario> getTecnicos() {
        return tecnicos;
    }

    public void setTecnicos(List<Usuario> tecnicos) {
        this.tecnicos = tecnicos;
    }
}
