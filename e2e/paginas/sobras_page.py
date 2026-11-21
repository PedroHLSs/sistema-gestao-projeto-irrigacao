from paginas.base_page import BasePage, por_texto_do_botao


class SobrasPage(BasePage):
    """Devolução das sobras ao estoque pelo almoxarife (RF19, G8)."""

    def abrir(self, projeto_id):
        return self.ir_para(f"/estoque/projetos/{projeto_id}/sobras")

    def confirmar_devolucao(self):
        """Cada campo já vem com a sobra do item (separado menos apontado); confirma assim."""
        self.enviar(por_texto_do_botao("Confirmar devolução"))
        return self.resultado()
