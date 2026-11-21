from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC

from paginas.base_page import BasePage, por_texto_do_botao


class LoginPage(BasePage):
    """Tela de entrada (acesso/login.html)."""

    def entrar(self, login, senha):
        self.ir_para("/login")
        self.preencher_id("username", login)
        self.preencher_id("password", senha)
        self.enviar(por_texto_do_botao("Entrar"))
        if "/login" in self.caminho_atual():
            raise AssertionError(f"Não foi possível entrar com o login {login}")
        return self

    def sair(self):
        """Encerra a sessão do navegador. O botão Sair fica no menu lateral, que no celular só
        aparece aberto; apagar o cookie de sessão tem o mesmo efeito para o próximo login."""
        self.driver.delete_all_cookies()
        self.ir_para("/login")
        self.wait.until(EC.visibility_of_element_located((By.ID, "username")))
        return self
