from paginas.base_page import BasePage, por_texto_do_botao


class LiberacaoPage(BasePage):
    """Liberação do faturamento e início da garantia pelo financeiro (RF20, RF21)."""

    def abrir(self, projeto_id):
        return self.ir_para(f"/financeiro/liberacoes/{projeto_id}")

    def liberar(self, inicio_garantia_iso, prazo_meses):
        self.definir_data("dataInicioGarantia", inicio_garantia_iso)
        self.preencher_id("prazoGarantiaMeses", prazo_meses)
        self.enviar(por_texto_do_botao("Liberar e encerrar o projeto"))
        return self.resultado()
