package br.edu.iftm.irrigacao.dominio.comercial;

import br.edu.iftm.irrigacao.dominio.EntidadeAuditada;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;

/**
 * Produtor rural, o cliente da empresa (RF01).
 */
@Entity
@Table(name = "produtor",
        uniqueConstraints = @UniqueConstraint(name = "uk_produtor_cpf_cnpj", columnNames = "cpf_cnpj"))
public class Produtor extends EntidadeAuditada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank
    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @NotBlank
    @Column(name = "cpf_cnpj", nullable = false, length = 18)
    private String cpfCnpj;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "email", length = 120)
    private String email;

    public Produtor() {
    }

    public Produtor(Long id, String nome, String cpfCnpj, String telefone, String email) {
        this.id = id;
        this.nome = nome;
        this.cpfCnpj = cpfCnpj;
        this.telefone = telefone;
        this.email = email;
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
