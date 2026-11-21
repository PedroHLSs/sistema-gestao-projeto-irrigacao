from selenium.webdriver.common.by import By

from paginas.base_page import BasePage, _literal, por_texto_do_botao


class ProgramacaoPage(BasePage):
    """Programação da equipe e emissão da ordem de serviço pelo projetista (RF15)."""

    def abrir(self, projeto_id):
        return self.ir_para(f"/engenharia/projetos/{projeto_id}/programacao")

    def emitir_ordem(self, data_programada_iso, nome_do_tecnico):
        self.definir_data("dataProgramada", data_programada_iso)
        # O técnico é escolhido pelo nome escrito ao lado da caixa de seleção
        self.clicar((By.XPATH, f"//label[normalize-space(.)={_literal(nome_do_tecnico)}]"))
        self.enviar(por_texto_do_botao("Emitir ordem de serviço"))
        return self.resultado()
