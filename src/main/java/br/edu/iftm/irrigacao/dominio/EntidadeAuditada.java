package br.edu.iftm.irrigacao.dominio;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * Superclasse das entidades que são editadas depois de criadas (estereótipo «auditavel» da
 * Figura 16). Guarda quem criou, quando criou, quem alterou por último e quando (RNF03).
 *
 * <p>Os quatro campos são preenchidos pelo Spring Data JPA e são anuláveis: {@code criadoPor} e
 * {@code alteradoPor} ficam vazios quando não há usuário logado, como na carga inicial. O valor
 * gravado é o id do {@code Usuario}.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class EntidadeAuditada {

    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private Long criadoPor;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime criadoEm;

    @LastModifiedBy
    @Column(name = "alterado_por")
    private Long alteradoPor;

    @LastModifiedDate
    @Column(name = "alterado_em")
    private LocalDateTime alteradoEm;

    public Long getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(Long criadoPor) {
        this.criadoPor = criadoPor;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Long getAlteradoPor() {
        return alteradoPor;
    }

    public void setAlteradoPor(Long alteradoPor) {
        this.alteradoPor = alteradoPor;
    }

    public LocalDateTime getAlteradoEm() {
        return alteradoEm;
    }

    public void setAlteradoEm(LocalDateTime alteradoEm) {
        this.alteradoEm = alteradoEm;
    }
}
