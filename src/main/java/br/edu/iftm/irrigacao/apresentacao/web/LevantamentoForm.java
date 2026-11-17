package br.edu.iftm.irrigacao.apresentacao.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import br.edu.iftm.irrigacao.dominio.projeto.DadosLevantamento;
import br.edu.iftm.irrigacao.dominio.projeto.FotoEnviada;
import br.edu.iftm.irrigacao.dominio.projeto.Levantamento;
import br.edu.iftm.irrigacao.dominio.projeto.TipoFonteAgua;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Formulário do levantamento em campo (RF03, RNF06). Nenhum campo é obrigatório aqui, porque o
 * rascunho é salvo como estiver; o que falta é apontado pelo G1 (RF04). As restrições abaixo só
 * barram valor fora da faixa.
 */
public class LevantamentoForm {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataVisita;

    @Size(max = 80, message = "A cultura pode ter até 80 caracteres.")
    private String cultura;

    @Positive(message = "A área deve ser maior que zero.")
    private Double areaIrrigadaHa;

    private TipoFonteAgua fonteAgua;

    @Positive(message = "A vazão deve ser maior que zero.")
    private Double vazaoFonte;

    @Size(max = 120, message = "A topografia pode ter até 120 caracteres.")
    private String topografia;

    @DecimalMin(value = "-90", message = "A latitude vai de -90 a 90.")
    @DecimalMax(value = "90", message = "A latitude vai de -90 a 90.")
    private Double latitude;

    @DecimalMin(value = "-180", message = "A longitude vai de -180 a 180.")
    @DecimalMax(value = "180", message = "A longitude vai de -180 a 180.")
    private Double longitude;

    /** Fotos escolhidas neste envio; as já gravadas ficam no levantamento. */
    @FotosValidas
    private List<MultipartFile> novasFotos = new ArrayList<>();

    public LevantamentoForm() {
    }

    public LevantamentoForm(Levantamento levantamento) {
        this.dataVisita = levantamento.getDataVisita();
        this.cultura = levantamento.getCultura();
        this.areaIrrigadaHa = levantamento.getAreaIrrigadaHa();
        this.fonteAgua = levantamento.getFonteAgua();
        this.vazaoFonte = levantamento.getVazaoFonte();
        this.topografia = levantamento.getTopografia();
        this.latitude = levantamento.getLatitude();
        this.longitude = levantamento.getLongitude();
    }

    public DadosLevantamento dados() {
        return new DadosLevantamento(dataVisita, cultura, areaIrrigadaHa, fonteAgua, vazaoFonte, topografia, latitude,
                longitude);
    }

    /** As fotos escolhidas, sem as partes vazias que o navegador manda quando nenhum arquivo é escolhido. */
    public List<FotoEnviada> fotosEnviadas() {
        if (novasFotos == null) {
            return List.of();
        }
        return novasFotos.stream()
                .filter(arquivo -> arquivo != null && !arquivo.isEmpty())
                .map(arquivo -> {
                    try {
                        return new FotoEnviada(arquivo.getOriginalFilename(), arquivo.getBytes());
                    } catch (IOException e) {
                        throw new UncheckedIOException("Não foi possível ler a foto enviada.", e);
                    }
                })
                .toList();
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

    public List<MultipartFile> getNovasFotos() {
        return novasFotos;
    }

    public void setNovasFotos(List<MultipartFile> novasFotos) {
        this.novasFotos = novasFotos;
    }
}
