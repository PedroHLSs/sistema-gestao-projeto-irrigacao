package br.edu.iftm.irrigacao.apresentacao.web;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.web.multipart.MultipartFile;

import br.edu.iftm.irrigacao.dominio.projeto.FotoEnviada;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Medição de pressão e vazão na instalação (RF17). A foto é opcional e segue a regra das fotos do
 * levantamento.
 */
public class MedicaoForm {

    @NotNull(message = "Informe a pressão medida.")
    @Positive(message = "A pressão deve ser maior que zero.")
    private Double pressaoMedidaMca;

    @NotNull(message = "Informe a vazão medida.")
    @Positive(message = "A vazão deve ser maior que zero.")
    private Double vazaoMedidaM3h;

    @FotosValidas
    private MultipartFile foto;

    @DecimalMin(value = "-90", message = "A latitude vai de -90 a 90.")
    @DecimalMax(value = "90", message = "A latitude vai de -90 a 90.")
    private Double latitude;

    @DecimalMin(value = "-180", message = "A longitude vai de -180 a 180.")
    @DecimalMax(value = "180", message = "A longitude vai de -180 a 180.")
    private Double longitude;

    public MedicaoForm() {
    }

    /** A foto lida do envio, ou {@code null} quando o campo veio vazio. */
    public FotoEnviada fotoEnviada() {
        if (foto == null || foto.isEmpty()) {
            return null;
        }
        try {
            return new FotoEnviada(foto.getOriginalFilename(), foto.getBytes());
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler a foto enviada.", e);
        }
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

    public MultipartFile getFoto() {
        return foto;
    }

    public void setFoto(MultipartFile foto) {
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
}
