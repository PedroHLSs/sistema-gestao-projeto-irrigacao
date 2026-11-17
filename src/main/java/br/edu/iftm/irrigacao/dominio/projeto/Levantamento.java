package br.edu.iftm.irrigacao.dominio.projeto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * Levantamento de campo do projeto, um por projeto (RF02, RF03).
 *
 * <p>O registro nasce no agendamento da visita, só com {@code dataAgendada}. Por isso os demais
 * campos são anuláveis no banco, e a obrigatoriedade é conferida pelo service antes de o projeto
 * sair de EM_LEVANTAMENTO (RF04, G1).
 *
 * <p>{@code fotos} guarda o caminho de cada arquivo dentro da pasta {@code irrigacao.upload.dir},
 * na ordem de envio (coluna {@code ordem}), porque a foto é servida pela posição na lista.
 * {@code vazaoFonte} é em m³/h. {@code topografia} é um acréscimo à Figura 10 (ver docs/modelo.md).
 */
@Entity
@Table(name = "levantamento",
        uniqueConstraints = @UniqueConstraint(name = "uk_levantamento_projeto", columnNames = "projeto_id"))
public class Levantamento extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "projeto_id", nullable = false)
    private Projeto projeto;

    @Column(name = "data_agendada")
    private LocalDateTime dataAgendada;

    @Column(name = "data_visita")
    private LocalDateTime dataVisita;

    @Column(name = "cultura", length = 80)
    private String cultura;

    @Column(name = "area_irrigada_ha")
    private Double areaIrrigadaHa;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonte_agua", length = 30)
    private TipoFonteAgua fonteAgua;

    @Column(name = "vazao_fonte")
    private Double vazaoFonte;

    @Column(name = "topografia", length = 120)
    private String topografia;

    @ElementCollection
    @CollectionTable(name = "levantamento_foto", joinColumns = @JoinColumn(name = "levantamento_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "caminho", nullable = false, length = 255)
    private List<String> fotos = new ArrayList<>();

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public Levantamento() {
    }

    public Levantamento(Long id, Projeto projeto, LocalDateTime dataAgendada, LocalDateTime dataVisita,
            String cultura, Double areaIrrigadaHa, TipoFonteAgua fonteAgua, Double vazaoFonte, String topografia,
            List<String> fotos, Double latitude, Double longitude) {
        this.id = id;
        this.projeto = projeto;
        this.dataAgendada = dataAgendada;
        this.dataVisita = dataVisita;
        this.cultura = cultura;
        this.areaIrrigadaHa = areaIrrigadaHa;
        this.fonteAgua = fonteAgua;
        this.vazaoFonte = vazaoFonte;
        this.topografia = topografia;
        this.fotos = fotos;
        this.latitude = latitude;
        this.longitude = longitude;
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

    public LocalDateTime getDataAgendada() {
        return dataAgendada;
    }

    public void setDataAgendada(LocalDateTime dataAgendada) {
        this.dataAgendada = dataAgendada;
    }

    public LocalDateTime getDataVisita() {
        return dataVisita;
    }

    public void setDataVisita(LocalDateTime dataVisita) {
        this.dataVisita = dataVisita;
    }

    public String getCultura() {
        return cultura;
    }

    public void setCultura(String cultura) {
        this.cultura = cultura;
    }

    public Double getAreaIrrigadaHa() {
        return areaIrrigadaHa;
    }

    public void setAreaIrrigadaHa(Double areaIrrigadaHa) {
        this.areaIrrigadaHa = areaIrrigadaHa;
    }

    public TipoFonteAgua getFonteAgua() {
        return fonteAgua;
    }

    public void setFonteAgua(TipoFonteAgua fonteAgua) {
        this.fonteAgua = fonteAgua;
    }

    public Double getVazaoFonte() {
        return vazaoFonte;
    }

    public void setVazaoFonte(Double vazaoFonte) {
        this.vazaoFonte = vazaoFonte;
    }

    public String getTopografia() {
        return topografia;
    }

    public void setTopografia(String topografia) {
        this.topografia = topografia;
    }

    public List<String> getFotos() {
        return fotos;
    }

    public void setFotos(List<String> fotos) {
        this.fotos = fotos;
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
}
