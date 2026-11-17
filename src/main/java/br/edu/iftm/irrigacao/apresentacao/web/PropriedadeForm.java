package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.comercial.Propriedade;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Formulário de inclusão e edição da propriedade de um produtor (RF01). Coordenadas e área são
 * opcionais.
 */
public class PropriedadeForm {

    /** Vazio na inclusão. Na edição vem do {@code {id}} da URL. */
    private Long id;

    @NotBlank(message = "Informe o nome da propriedade.")
    @Size(max = 120, message = "O nome pode ter até 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o município.")
    @Size(max = 80, message = "O município pode ter até 80 caracteres.")
    private String municipio;

    @DecimalMin(value = "-90", message = "A latitude vai de -90 a 90.")
    @DecimalMax(value = "90", message = "A latitude vai de -90 a 90.")
    private Double latitude;

    @DecimalMin(value = "-180", message = "A longitude vai de -180 a 180.")
    @DecimalMax(value = "180", message = "A longitude vai de -180 a 180.")
    private Double longitude;

    @Positive(message = "A área deve ser maior que zero.")
    private Double areaTotalHa;

    public PropriedadeForm() {
    }

    public PropriedadeForm(Propriedade propriedade) {
        this.id = propriedade.getId();
        this.nome = propriedade.getNome();
        this.municipio = propriedade.getMunicipio();
        this.latitude = propriedade.getLatitude();
        this.longitude = propriedade.getLongitude();
        this.areaTotalHa = propriedade.getAreaTotalHa();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
