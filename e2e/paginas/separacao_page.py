from paginas.base_page import BasePage, por_texto_do_botao


class SeparacaoPage(BasePage):
    """Separação e conferência do material pelo almoxarife (RF14, G5)."""

    def abrir(self, projeto_id):
        return self.ir_para(f"/estoque/projetos/{projeto_id}/separacao")

    def separar_tudo(self):
        """"Separar tudo o que falta" preenche cada item com o que falta; depois salva."""
        self.clicar(por_texto_do_botao("Separar tudo o que falta"))
        self.enviar(por_texto_do_botao("Salvar separação"))
        return self.resultado()

    def liberar_para_execucao(self):
        """Conferência do G5: com o material separado, libera a execução."""
        self.enviar(por_texto_do_botao("Liberar para a execução"), confirmar=True)
        return self.resultado()
