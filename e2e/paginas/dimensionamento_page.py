from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC

from paginas.base_page import BasePage, _literal, por_texto_do_botao

BUSCA = (By.ID, "buscaMaterial")


class DimensionamentoPage(BasePage):
    """Dimensionamento e lista técnica do projetista (RF05)."""

    def abrir(self, projeto_id):
        return self.ir_para(f"/engenharia/projetos/{projeto_id}/dimensionamento")

    def registrar_parametros(self, pressao_mca, vazao_m3h, tolerancia_percentual):
        self.preencher_id("pressaoProjetoMca", pressao_mca)
        self.preencher_id("vazaoProjetoM3h", vazao_m3h)
        self.preencher_id("toleranciaPercentual", tolerancia_percentual)
        self.enviar(por_texto_do_botao("Gravar"))
        return self.resultado()

    def adicionar_material(self, codigo, quantidade):
        """Busca o material pelo código, escolhe o resultado da lista e lança a quantidade."""
        self.digitar(BUSCA, codigo)
        resultado = (By.XPATH, "//div[@id='resultadosMaterial']//button"
                               f"[contains(normalize-space(.), {_literal(codigo + ' · ')})]")
        self.clicar(resultado)
        # Escolhido, o campo de busca passa a mostrar "código · descrição"
        self.wait.until(EC.text_to_be_present_in_element_value(BUSCA, codigo + " · "))
        self.preencher_id("quantidadePrevista", quantidade)
        self.enviar(por_texto_do_botao("Adicionar"))
        return self.resultado()

    def concluir_lista_tecnica(self):
        self.enviar(por_texto_do_botao("Concluir lista técnica"), confirmar=True)
        return self.resultado()
