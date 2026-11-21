from selenium.webdriver.common.by import By

from paginas.base_page import BasePage, por_testid, por_texto_do_botao

PREFIXO_SITUACAO = "proposta-"


class PropostaPage(BasePage):
    """Orçamento e proposta do vendedor (RF09 a RF12)."""

    def abrir_orcamento(self, projeto_id):
        return self.ir_para(f"/comercial/projetos/{projeto_id}/orcamento")

    def gerar(self, horas_previstas, percentual_desconto):
        """Atualiza a prévia com as horas e o desconto e gera a proposta. @return id da proposta"""
        self.preencher_id("horasPrevistas", horas_previstas)
        self.preencher_id("percentualDesconto", percentual_desconto)
        self.enviar(por_texto_do_botao("Atualizar prévia"))
        self.enviar(por_texto_do_botao("Gerar proposta"))
        self.resultado()
        return self.id_no_fim_do_caminho("propostas")

    def abrir(self, proposta_id):
        return self.ir_para(f"/comercial/propostas/{proposta_id}")

    def situacao(self):
        """Situação da proposta pelo nome do enum (ex.: AGUARDANDO_DIRETORIA)."""
        cabecalho = self._visivel(por_testid("cabecalho-proposta"))
        badge = cabecalho.find_element(By.CSS_SELECTOR, f"[data-testid^='{PREFIXO_SITUACAO}']")
        return badge.get_attribute("data-testid")[len(PREFIXO_SITUACAO):]

    def pedir_aprovacao_da_diretoria(self):
        self.enviar(por_texto_do_botao("Pedir aprovação da diretoria"))
        return self.resultado()

    def enviar_ao_cliente(self):
        self.enviar(por_texto_do_botao("Enviar ao cliente"), confirmar=True)
        return self.resultado()

    def registrar_aprovacao_do_cliente(self):
        self.enviar(por_texto_do_botao("Cliente aprovou"), confirmar=True)
        return self.resultado()
