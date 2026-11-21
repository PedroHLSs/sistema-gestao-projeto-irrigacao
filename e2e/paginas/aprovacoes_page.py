from selenium.webdriver.common.by import By

from paginas.base_page import BasePage, por_testid, por_texto_do_botao


class AprovacoesPage(BasePage):
    """Fila de descontos acima da alçada esperando a diretoria (RF10, G2)."""

    CAMINHO = "/diretoria/aprovacoes"

    def abrir(self):
        return self.ir_para(self.CAMINHO)

    def pendente(self, proposta_id):
        return len(self.driver.find_elements(*por_testid(f"aprovacao-{proposta_id}"))) == 1

    def aprovar(self, proposta_id, observacao=""):
        if observacao:
            self.preencher_id(f"observacao-{proposta_id}", observacao)
        cartao = f"//*[@data-testid='aprovacao-{proposta_id}']"
        self.enviar((By.XPATH, cartao + "//button[contains(normalize-space(.), 'Aprovar desconto')]"))
        return self.resultado()
