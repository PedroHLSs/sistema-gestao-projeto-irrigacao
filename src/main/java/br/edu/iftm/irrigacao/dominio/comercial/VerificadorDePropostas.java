package br.edu.iftm.irrigacao.dominio.comercial;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Vence as propostas enviadas que passaram da validade (RF11): todo dia às 6h e sempre que a lista
 * de propostas é aberta. Cada proposta vence na própria transação, então um erro numa não impede as
 * outras.
 *
 * <p>O horário vem de {@code irrigacao.proposta.verificacao-cron}; no perfil {@code test} ele é
 * {@code -}, que desliga o agendamento, e os testes chamam {@link #verificar()} direto.
 */
@Component
public class VerificadorDePropostas {

    private static final Logger log = LoggerFactory.getLogger(VerificadorDePropostas.class);

    private final PropostaService propostaService;

    public VerificadorDePropostas(PropostaService propostaService) {
        this.propostaService = propostaService;
    }

    /** @return quantas propostas venceram nesta passada */
    @Scheduled(cron = "${irrigacao.proposta.verificacao-cron:0 0 6 * * *}")
    public int verificar() {
        int vencidas = 0;
        for (Long id : propostaService.enviadasVencidas()) {
            try {
                if (propostaService.vencer(id).isPresent()) {
                    vencidas++;
                }
            } catch (RuntimeException e) {
                // Outra verificação ao mesmo tempo já pode ter vencido esta proposta
                log.warn("Não foi possível vencer a proposta {}: {}", id, e.getMessage());
            }
        }
        if (vencidas > 0) {
            log.info("Verificação de propostas: {} proposta(s) vencida(s) e refeita(s) com os preços atuais.", vencidas);
        }
        return vencidas;
    }
}
