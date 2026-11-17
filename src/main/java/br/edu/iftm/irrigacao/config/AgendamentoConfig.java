package br.edu.iftm.irrigacao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga as tarefas agendadas ({@code @Scheduled}), como a verificação diária de vencimento das
 * propostas (RF11).
 */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
