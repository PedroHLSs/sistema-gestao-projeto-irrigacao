package br.edu.iftm.irrigacao.dominio.projeto;

import br.edu.iftm.irrigacao.dominio.acesso.Perfil;

/**
 * Painel da página inicial por perfil. Todos os números saem de consultas de agregação, sem carregar
 * as listas em memória.
 */
public interface PainelService {

    /**
     * O ADMINISTRADOR vê os painéis das áreas; os de vendedor e técnico são pessoais e não entram.
     *
     * @param perfil    perfil logado; {@code null} devolve o painel vazio
     * @param usuarioId id do usuário logado, usado nos painéis pessoais; pode ser {@code null}
     */
    PainelInicial montar(Perfil perfil, Long usuarioId);
}
