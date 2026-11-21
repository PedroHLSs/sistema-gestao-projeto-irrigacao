package br.edu.iftm.irrigacao.dominio.estoque.impl;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.iftm.irrigacao.dominio.RegistroNaoEncontradoException;
import br.edu.iftm.irrigacao.dominio.RegraNegocioException;
import br.edu.iftm.irrigacao.dominio.acesso.Usuario;
import br.edu.iftm.irrigacao.dominio.acesso.UsuarioLogado;
import br.edu.iftm.irrigacao.dominio.estoque.DisponibilidadeItem;
import br.edu.iftm.irrigacao.dominio.estoque.EstoqueService;
import br.edu.iftm.irrigacao.dominio.estoque.ItemProjeto;
import br.edu.iftm.irrigacao.dominio.estoque.Material;
import br.edu.iftm.irrigacao.dominio.estoque.MovimentacaoEstoque;
import br.edu.iftm.irrigacao.dominio.estoque.Quantidade;
import br.edu.iftm.irrigacao.dominio.estoque.SaldoInsuficienteException;
import br.edu.iftm.irrigacao.dominio.estoque.TipoMovimentacao;
import br.edu.iftm.irrigacao.dominio.projeto.Projeto;
import br.edu.iftm.irrigacao.persistencia.ItemProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.MaterialRepository;
import br.edu.iftm.irrigacao.persistencia.MovimentacaoEstoqueRepository;
import br.edu.iftm.irrigacao.persistencia.ProjetoRepository;
import br.edu.iftm.irrigacao.persistencia.UsuarioRepository;

/**
 * Controle de estoque (RF07, RNF05).
 *
 * <p>Nenhuma operação lê o saldo, calcula e salva: quem altera o saldo é o UPDATE condicional do
 * {@link MaterialRepository}, e o service só confere quantas linhas ele alterou. Quando a operação
 * também depende do que o projeto já reservou ou recebeu, essa conta é feita depois do UPDATE: a
 * linha do material já está travada pela transação, então outra operação no mesmo material espera
 * o commit, e a conta não fica velha. Se ela não fechar, a exceção desfaz o UPDATE.
 */
@Service
public class EstoqueServiceImpl implements EstoqueService {

    /**
     * Limites do período quando o extrato não filtra por data. Vão como parâmetro em vez de
     * {@code :inicio is null}, que o PostgreSQL recusa para data sem tipo definido.
     */
    static final LocalDateTime SEM_INICIO = LocalDateTime.of(1900, 1, 1, 0, 0);
    static final LocalDateTime SEM_FIM = LocalDateTime.of(9999, 12, 31, 0, 0);

    private final MaterialRepository materialRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final ItemProjetoRepository itemProjetoRepository;
    private final ProjetoRepository projetoRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioLogado usuarioLogado;
    private final Clock clock;

    public EstoqueServiceImpl(MaterialRepository materialRepository, MovimentacaoEstoqueRepository movimentacaoRepository,
            ItemProjetoRepository itemProjetoRepository, ProjetoRepository projetoRepository,
            UsuarioRepository usuarioRepository, UsuarioLogado usuarioLogado, Clock clock) {
        this.materialRepository = materialRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.itemProjetoRepository = itemProjetoRepository;
        this.projetoRepository = projetoRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioLogado = usuarioLogado;
        this.clock = clock;
    }

    @Override
    @Transactional
    public MovimentacaoEstoque entradaCompra(Long materialId, BigDecimal quantidade) {
        exigirQuantidade(quantidade);
        buscarMaterial(materialId);
        materialRepository.adicionarSaldo(materialId, quantidade);
        return registrar(materialId, null, TipoMovimentacao.ENTRADA_COMPRA, quantidade);
    }

    @Override
    @Transactional
    public MovimentacaoEstoque ajuste(Long materialId, BigDecimal novoSaldo) {
        if (novoSaldo == null || novoSaldo.signum() < 0) {
            throw new RegraNegocioException("Informe o novo saldo, zero ou mais.");
        }
        exigirQueCaiba(novoSaldo);
        Material material = buscarMaterial(materialId);
        BigDecimal saldoAnterior = material.getSaldo();
        BigDecimal diferenca = novoSaldo.subtract(saldoAnterior);
        if (diferenca.signum() == 0) {
            throw new RegraNegocioException("O saldo de " + material.getCodigo() + " já é "
                    + Quantidade.formatar(novoSaldo) + " " + material.getUnidade() + ". Nada foi ajustado.");
        }
        if (materialRepository.ajustarSaldo(materialId, saldoAnterior, novoSaldo) == 0) {
            Material atual = buscarMaterial(materialId);
            if (atual.getSaldoReservado().compareTo(novoSaldo) > 0) {
                throw new SaldoInsuficienteException("O saldo de " + descrever(atual)
                        + " não pode ficar abaixo do reservado, que é " + quantidade(atual.getSaldoReservado(), atual)
                        + ".", atual.getSaldoReservado());
            }
            throw new RegraNegocioException("O saldo de " + atual.getCodigo() + " mudou durante o ajuste e agora é "
                    + quantidade(atual.getSaldo(), atual) + ". Confira a contagem e ajuste de novo.");
        }
        return registrar(materialId, null, TipoMovimentacao.AJUSTE, diferenca);
    }

    @Override
    @Transactional
    public MovimentacaoEstoque reservar(Long materialId, Long projetoId, BigDecimal quantidade) {
        exigirQuantidade(quantidade);
        buscarMaterial(materialId);
        buscarProjeto(projetoId);
        if (materialRepository.reservar(materialId, quantidade) == 0) {
            Material atual = buscarMaterial(materialId);
            BigDecimal disponivel = disponivel(atual);
            throw new SaldoInsuficienteException("Saldo insuficiente de " + descrever(atual) + ": pedido "
                    + quantidade(quantidade, atual) + ", disponível " + quantidade(disponivel, atual) + ".", disponivel);
        }
        return registrar(materialId, projetoId, TipoMovimentacao.RESERVA, quantidade);
    }

    @Override
    @Transactional
    public MovimentacaoEstoque baixarReservaParaProjeto(Long materialId, Long projetoId, BigDecimal quantidade) {
        exigirQuantidade(quantidade);
        buscarMaterial(materialId);
        Projeto projeto = buscarProjeto(projetoId);
        if (materialRepository.baixarReserva(materialId, quantidade) == 0) {
            Material atual = buscarMaterial(materialId);
            throw new SaldoInsuficienteException("Reserva insuficiente de " + descrever(atual) + ": pedido "
                    + quantidade(quantidade, atual) + ", reservado " + quantidade(atual.getSaldoReservado(), atual)
                    + ".", atual.getSaldoReservado());
        }
        // Conta feita com a linha do material já travada pelo UPDATE acima
        BigDecimal reservadoNoProjeto = somar(projetoId, materialId, TipoMovimentacao.RESERVA)
                .subtract(somar(projetoId, materialId, TipoMovimentacao.SAIDA_PROJETO));
        if (reservadoNoProjeto.compareTo(quantidade) < 0) {
            Material material = buscarMaterial(materialId);
            throw new SaldoInsuficienteException("O projeto " + projeto.getCodigo() + " tem "
                    + quantidade(reservadoNoProjeto, material) + " de " + descrever(material)
                    + " reservado(s); não dá para baixar " + quantidade(quantidade, material) + ".", reservadoNoProjeto);
        }
        return registrar(materialId, projetoId, TipoMovimentacao.SAIDA_PROJETO, quantidade);
    }

    @Override
    @Transactional
    public MovimentacaoEstoque devolver(Long materialId, Long projetoId, BigDecimal quantidade) {
        exigirQuantidade(quantidade);
        buscarMaterial(materialId);
        Projeto projeto = buscarProjeto(projetoId);
        materialRepository.adicionarSaldo(materialId, quantidade);
        // Conta feita com a linha do material já travada pelo UPDATE acima
        BigDecimal emPoderDoProjeto = somar(projetoId, materialId, TipoMovimentacao.SAIDA_PROJETO)
                .subtract(somar(projetoId, materialId, TipoMovimentacao.DEVOLUCAO));
        if (emPoderDoProjeto.compareTo(quantidade) < 0) {
            Material material = buscarMaterial(materialId);
            throw new RegraNegocioException("O projeto " + projeto.getCodigo() + " tem "
                    + quantidade(emPoderDoProjeto, material) + " de " + descrever(material)
                    + " para devolver; não dá para devolver " + quantidade(quantidade, material) + ".");
        }
        return registrar(materialId, projetoId, TipoMovimentacao.DEVOLUCAO, quantidade);
    }

    @Override
    @Transactional
    public void acertarBaixaAlemDoSeparado(Long materialId, Long projetoId, BigDecimal apontada,
            BigDecimal separada) {
        BigDecimal alvo = apontada.subtract(separada).max(BigDecimal.ZERO);
        // Durante a instalação nenhuma sobra foi devolvida ainda: o que saiu e não voltou, menos o
        // separado, é o que já foi baixado além dele
        BigDecimal jaBaixado = somar(projetoId, materialId, TipoMovimentacao.SAIDA_PROJETO)
                .subtract(somar(projetoId, materialId, TipoMovimentacao.DEVOLUCAO))
                .subtract(separada)
                .max(BigDecimal.ZERO);
        BigDecimal diferenca = alvo.subtract(jaBaixado);
        if (diferenca.signum() > 0) {
            baixarParaProjeto(materialId, projetoId, diferenca);
        } else if (diferenca.signum() < 0) {
            devolver(materialId, projetoId, diferenca.negate());
        }
    }

    @Override
    @Transactional
    public void baixarParaProjeto(Long materialId, Long projetoId, BigDecimal quantidade) {
        exigirQuantidade(quantidade);
        // Com a separação parcial, o item ainda tem reserva do projeto que não saiu: ela é usada primeiro
        BigDecimal aReservar = quantidade.subtract(reservaPendente(projetoId, materialId).max(BigDecimal.ZERO));
        if (aReservar.signum() > 0) {
            reservar(materialId, projetoId, aReservar);
        }
        baixarReservaParaProjeto(materialId, projetoId, quantidade);
    }

    @Override
    @Transactional
    public BigDecimal liberarReservaPendente(Long materialId, Long projetoId) {
        buscarMaterial(materialId);
        buscarProjeto(projetoId);
        BigDecimal pendente = reservaPendente(projetoId, materialId);
        if (pendente.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (materialRepository.liberarReserva(materialId, pendente) == 0) {
            Material atual = buscarMaterial(materialId);
            throw new SaldoInsuficienteException("Reserva insuficiente de " + descrever(atual) + ": liberar "
                    + quantidade(pendente, atual) + ", reservado " + quantidade(atual.getSaldoReservado(), atual)
                    + ".", atual.getSaldoReservado());
        }
        registrar(materialId, projetoId, TipoMovimentacao.RESERVA, pendente.negate());
        return pendente;
    }

    /** O que foi reservado para o projeto e ainda não saiu do estoque. */
    private BigDecimal reservaPendente(Long projetoId, Long materialId) {
        return somar(projetoId, materialId, TipoMovimentacao.RESERVA)
                .subtract(somar(projetoId, materialId, TipoMovimentacao.SAIDA_PROJETO));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DisponibilidadeItem> disponibilidade(Long projetoId) {
        buscarProjeto(projetoId);
        return itemProjetoRepository.disponibilidade(projetoId, TipoMovimentacao.RESERVA).stream()
                .map(linha -> {
                    ItemProjeto item = (ItemProjeto) linha[0];
                    return new DisponibilidadeItem(item, disponivel(item.getMaterial()), decimal(linha[1]));
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MovimentacaoEstoque> extrato(Long materialId, LocalDate de, LocalDate ate, Long projetoId,
            Pageable pageable) {
        LocalDateTime inicio = de == null ? SEM_INICIO : de.atStartOfDay();
        LocalDateTime fim = ate == null ? SEM_FIM : ate.plusDays(1).atStartOfDay();
        return movimentacaoRepository.extrato(materialId, inicio, fim, projetoId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Projeto> projetosDoMaterial(Long materialId) {
        return movimentacaoRepository.projetosDoMaterial(materialId);
    }

    private MovimentacaoEstoque registrar(Long materialId, Long projetoId, TipoMovimentacao tipo, BigDecimal quantidade) {
        // Depois do UPDATE o contexto foi limpo: as associações vão por referência, sem nova consulta
        Projeto projeto = projetoId == null ? null : projetoRepository.getReferenceById(projetoId);
        Usuario usuario = usuarioLogado.idAtual().map(usuarioRepository::getReferenceById).orElse(null);
        return movimentacaoRepository.save(new MovimentacaoEstoque(null, materialRepository.getReferenceById(materialId),
                projeto, tipo, quantidade, LocalDateTime.now(clock), usuario));
    }

    private Material buscarMaterial(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Material não encontrado."));
    }

    private Projeto buscarProjeto(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RegistroNaoEncontradoException("Projeto não encontrado."));
    }

    private BigDecimal somar(Long projetoId, Long materialId, TipoMovimentacao tipo) {
        return decimal(movimentacaoRepository.somar(projetoId, materialId, tipo));
    }

    private static void exigirQuantidade(BigDecimal quantidade) {
        if (quantidade == null || quantidade.signum() <= 0) {
            throw new RegraNegocioException("Informe uma quantidade maior que zero.");
        }
        exigirQueCaiba(quantidade);
    }

    private static void exigirQueCaiba(BigDecimal valor) {
        if (!Quantidade.cabeNaColuna(valor)) {
            throw new RegraNegocioException("Use até " + Quantidade.CASAS_DECIMAIS + " casas decimais e até "
                    + Quantidade.DIGITOS_INTEIROS + " dígitos antes da vírgula.");
        }
    }

    private static BigDecimal disponivel(Material material) {
        return material.getSaldo().subtract(material.getSaldoReservado());
    }

    private static String descrever(Material material) {
        return material.getCodigo() + " (" + material.getDescricao() + ")";
    }

    private static String quantidade(BigDecimal valor, Material material) {
        return Quantidade.formatar(valor) + " " + material.getUnidade();
    }

    /** A soma vem do banco como BigDecimal; o {@code coalesce} com 0 pode chegar como outro número. */
    private static BigDecimal decimal(Object valor) {
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }
}
