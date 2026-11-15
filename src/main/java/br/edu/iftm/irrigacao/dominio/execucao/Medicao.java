package br.edu.iftm.irrigacao.dominio.execucao;

import java.time.LocalDateTime;

import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

/**
 * Medição de pressão e vazão feita depois da instalação (RF17). {@code aprovada} guarda o
 * resultado da comparação com os parâmetros do projeto, dentro da tolerância (G6).
 *
 * <p>{@code foto} é o caminho do arquivo dentro da pasta {@code irrigacao.upload.dir}.
 */
@Entity
@Table(name = "medicao")
public class Medicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @NotNull
    @Column(name = "pressao_medida_mca", nullable = false)
    private Double pressaoMedidaMca;

    @NotNull
    @Column(name = "vazao_medida_m3h", nullable = false)
    private Double vazaoMedidaM3h;

    @Column(name = "foto", length = 255)
    private String foto;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @NotNull
    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(name = "aprovada", nullable = false)
    private boolean aprovada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    public Medicao() {
    }

    public Medicao(Long id, Projeto projeto, Double pressaoMedidaMca, Double vazaoMedidaM3h, String foto,
            Double latitude, Double longitude, LocalDateTime dataHora, boolean aprovada, Usuario usuario) {
        this.id = id;
        this.projeto = projeto;
        this.pressaoMedidaMca = pressaoMedidaMca;
        this.vazaoMedidaM3h = vazaoMedidaM3h;
        this.foto = foto;
        this.latitude = latitude;
        this.longitude = longitude;
        this.dataHora = dataHora;
        this.aprovada = aprovada;
        this.usuario = usuario;
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

    public Double getPressaoMedidaMca() {
        return pressaoMedidaMca;
    }

    public void setPressaoMedidaMca(Double pressaoMedidaMca) {
        this.pressaoMedidaMca = pressaoMedidaMca;
    }

    public Double getVazaoMedidaM3h() {
        return vazaoMedidaM3h;
    }

    public void setVazaoMedidaM3h(Double vazaoMedidaM3h) {
        this.vazaoMedidaM3h = vazaoMedidaM3h;
    }

    public String getFoto() {
        return foto;
    }

    public void setFoto(String foto) {
        this.foto = foto;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public boolean isAprovada() {
        return aprovada;
    }

    public void setAprovada(boolean aprovada) {
        this.aprovada = aprovada;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }
}
