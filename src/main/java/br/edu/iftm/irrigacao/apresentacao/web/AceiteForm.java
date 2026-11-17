package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.execucao.AceiteService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Aceite do cliente (RF18): nome de quem assina e a assinatura feita no quadro, em PNG base64. Que
 * a imagem tem traço, quem confere é o {@code AceiteService}.
 */
public class AceiteForm {

    @NotBlank(message = "Informe o nome de quem assina pelo cliente.")
    @Size(max = 120, message = "Use até 120 caracteres.")
    private String nomeResponsavel;

    @NotBlank(message = "Sem a assinatura do cliente o aceite não é gravado.")
    @Size(max = AceiteService.TAMANHO_MAXIMO_ASSINATURA + 30, message = "A assinatura é grande demais. Limpe o quadro e assine de novo.")
    private String assinatura;

    public AceiteForm() {
    }

    public AceiteForm(String nomeResponsavel, String assinatura) {
        this.nomeResponsavel = nomeResponsavel;
        this.assinatura = assinatura;
    }

    public String getNomeResponsavel() {
        return nomeResponsavel;
    }

    public void setNomeResponsavel(String nomeResponsavel) {
        this.nomeResponsavel = nomeResponsavel;
    }

    public String getAssinatura() {
        return assinatura;
    }

    public void setAssinatura(String assinatura) {
        this.assinatura = assinatura;
    }
}
