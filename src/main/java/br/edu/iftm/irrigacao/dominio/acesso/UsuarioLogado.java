package br.edu.iftm.irrigacao.dominio.acesso;

import java.util.Optional;

import org.springframework.stereotype.Component;

/**
 * Informa quem é o usuário da requisição atual. É a fonte do {@code criadoPor} e do
 * {@code alteradoPor} da auditoria (RNF03).
 *
 * <p>Etapa 2: ainda não existe login, então a resposta é sempre vazia. A etapa 3 liga este
 * componente ao Spring Security.
 */
@Component
public class UsuarioLogado {

    /**
     * @return o id do {@code Usuario} autenticado, ou vazio quando não há ninguém logado
     */
    public Optional<Long> idAtual() {
        return Optional.empty();
    }
}
