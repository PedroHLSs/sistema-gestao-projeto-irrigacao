from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC

from paginas.base_page import BasePage, _literal, por_testid, por_texto_do_botao

QUADRO_ASSINATURA = (By.ID, "quadroAssinatura")


class OrdemServicoPage(BasePage):
    """Ordem de serviço no campo (RF16 a RF18): apontamentos, conclusão, medição, correção e aceite.
    Tela de celular."""

    def abrir_minhas_os(self):
        return self.ir_para("/campo/minhas-os")

    def abrir_ordem_do_projeto(self, codigo_projeto):
        """Botão "Abrir ordem" do cartão da OS daquele projeto. @return id da ordem"""
        cartao = (f"//h2[contains(normalize-space(.), {_literal(codigo_projeto)})]"
                  "/ancestor::div[.//a[contains(normalize-space(.), 'Abrir ordem')]][1]")
        self.enviar((By.XPATH, cartao + "//a[contains(normalize-space(.), 'Abrir ordem')]"))
        return self.id_no_fim_do_caminho("ordens")

    def apontar_horas(self, horas):
        self.preencher_id("horas", horas)
        self.enviar(por_testid("apontar-horas"))
        return self.resultado()

    def apontar_material(self, codigo, quantidade):
        opcao = (By.XPATH, f"//select[@id='materialId']/option[starts-with(normalize-space(.), {_literal(codigo + ' · ')})]")
        valor = self.wait.until(EC.presence_of_element_located(opcao)).get_attribute("value")
        self.escolher_por_valor("materialId", valor)
        self.wait.until(EC.element_located_to_be_selected(opcao))
        self.preencher_id("quantidade", quantidade)
        self.enviar(por_testid("apontar-material"))
        return self.resultado()

    def concluir_instalacao(self):
        self.enviar(por_texto_do_botao("Concluir instalação"), confirmar=True)
        return self.resultado()

    def preencher_medicao(self, pressao_mca, vazao_m3h, latitude, longitude):
        """Coordenada pelos campos manuais, sem o GPS do aparelho."""
        self.preencher_id("pressaoMedidaMca", pressao_mca)
        self.preencher_id("vazaoMedidaM3h", vazao_m3h)
        self.preencher_id("latitude", latitude)
        self.preencher_id("longitude", longitude)
        return self

    def registrar_medicao(self):
        """Envia a medição. Fora da tolerância o sistema grava como reprovada e avisa no topo
        como erro, por isso aqui não se espera a mensagem de sucesso."""
        self.enviar(por_texto_do_botao("Registrar medição"))
        return self

    def medicoes_aprovadas(self):
        return len(self.driver.find_elements(*por_testid("medicao-aprovada")))

    def medicoes_reprovadas(self):
        return len(self.driver.find_elements(*por_testid("medicao-reprovada")))

    def registrar_correcao(self, descricao):
        self.preencher_id("descricao", descricao)
        self.enviar(por_texto_do_botao("Registrar correção"))
        return self.resultado()

    def preencher_aceite(self, nome_responsavel):
        self.preencher_id("nomeResponsavel", nome_responsavel)
        self.assinar()
        return self

    def assinar(self):
        """Desenha a assinatura no quadro: clica, arrasta em zigue-zague e solta."""
        quadro = self._rolar_ate(self._visivel(QUADRO_ASSINATURA))
        largura = quadro.size["width"]
        passo = max(int(largura * 0.12), 10)
        (ActionChains(self.driver)
         .move_to_element_with_offset(quadro, -int(largura * 0.35), 0)
         .click_and_hold()
         .move_by_offset(passo, -15)
         .move_by_offset(passo, 25)
         .move_by_offset(passo, -25)
         .move_by_offset(passo, 20)
         .move_by_offset(passo, -10)
         .release()
         .perform())
        return self

    def salvar_aceite(self):
        self.enviar(por_texto_do_botao("Salvar aceite"))
        return self.resultado()
