package br.edu.iftm.irrigacao.apresentacao.web;

import br.edu.iftm.irrigacao.dominio.comercial.CpfCnpj;
import br.edu.iftm.irrigacao.dominio.comercial.Produtor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Formulário de inclusão e edição do produtor (RF01).
 */
@CpfCnpjUnico
public class ProdutorForm {

    /** Vazio na inclusão. Na edição vem do {@code {id}} da URL. */
    private Long id;

    @NotBlank(message = "Informe o nome.")
    @Size(max = 120, message = "O nome pode ter até 120 caracteres.")
    private String nome;

    @NotBlank(message = "Informe o CPF ou o CNPJ.")
    @CpfCnpjValido
    private String cpfCnpj;

    @Size(max = 20, message = "O telefone pode ter até 20 caracteres.")
    private String telefone;

    @Email(message = "Informe um e-mail válido, como nome@exemplo.com.")
    @Size(max = 120, message = "O e-mail pode ter até 120 caracteres.")
    private String email;

    public ProdutorForm() {
    }

    public ProdutorForm(Produtor produtor) {
        this.id = produtor.getId();
        this.nome = produtor.getNome();
        this.cpfCnpj = CpfCnpj.formatar(produtor.getCpfCnpj());
        this.telefone = produtor.getTelefone();
        this.email = produtor.getEmail();
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

    public String getCpfCnpj() {
        return cpfCnpj;
    }

    public void setCpfCnpj(String cpfCnpj) {
        this.cpfCnpj = cpfCnpj;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
