package br.edu.iftm.irrigacao.dominio.comercial;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Propriedade rural de um produtor, onde o projeto de irrigação é instalado (RF01).
 */
@Entity
@Table(name = "propriedade")
public class Propriedade extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produtor_id", nullable = false)
    private Produtor produtor;

    @NotBlank
    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @NotBlank
    @Column(name = "municipio", nullable = false, length = 80)
    private String municipio;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "area_total_ha")
    private Double areaTotalHa;

    public Propriedade() {
    }

    public Propriedade(Long id, Produtor produtor, String nome, String municipio, Double latitude,
            Double longitude, Double areaTotalHa) {
        this.id = id;
        this.produtor = produtor;
        this.nome = nome;
        this.municipio = municipio;
        this.latitude = latitude;
        this.longitude = longitude;
        this.areaTotalHa = areaTotalHa;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produtor getProdutor() {
        return produtor;
    }

    public void setProdutor(Produtor produtor) {
        this.produtor = produtor;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
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

    public Double getAreaTotalHa() {
        return areaTotalHa;
    }

    public void setAreaTotalHa(Double areaTotalHa) {
        this.areaTotalHa = areaTotalHa;
    }
}
