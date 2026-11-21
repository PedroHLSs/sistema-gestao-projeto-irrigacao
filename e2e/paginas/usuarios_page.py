from selenium.webdriver.common.by import By

from paginas.base_page import BasePage, _literal, por_texto_do_botao


class UsuariosPage(BasePage):
    """Cadastro e lista de usuários do administrador (RF23)."""

    LISTA = "/admin/usuarios"

    def abrir_lista(self):
        return self.ir_para(self.LISTA)

    def criar(self, nome, login, senha, perfil):
        self.ir_para(self.LISTA + "/novo")
        self.preencher_id("nome", nome)
        self.preencher_id("login", login)
        self.escolher_por_valor("perfil", perfil)
        self.preencher_id("senha", senha)
        self.enviar(por_texto_do_botao("Criar usuário"))
        return self.resultado()

    def usuario_listado(self, login):
        celula = (By.XPATH, f"//td[normalize-space(.)={_literal(login)}]")
        return len(self.driver.find_elements(*celula)) == 1
