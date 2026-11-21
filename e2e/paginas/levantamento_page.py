from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC

from paginas.base_page import BasePage, por_testid, por_texto_do_botao


class LevantamentoPage(BasePage):
    """Levantamento em campo (RF03, RF04, G1), tela de celular."""

    def abrir(self, projeto_id):
        return self.ir_para(f"/campo/projetos/{projeto_id}/levantamento")

    def preencher(self, cultura=None, area_irrigada_ha=None, fonte_agua=None, vazao_fonte=None,
                  topografia=None, latitude=None, longitude=None, data_visita_iso=None):
        """Preenche só o que for informado. A coordenada vai pelos campos manuais, sem o GPS."""
        if data_visita_iso is not None:
            self.definir_data("dataVisita", data_visita_iso)
        campos = {"cultura": cultura, "areaIrrigadaHa": area_irrigada_ha, "vazaoFonte": vazao_fonte,
                  "topografia": topografia, "latitude": latitude, "longitude": longitude}
        for id_campo, valor in campos.items():
            if valor is not None:
                self.preencher_id(id_campo, valor)
        if fonte_agua is not None:
            self.escolher_por_valor("fonteAgua", fonte_agua)
        return self

    def anexar_foto(self, caminho):
        """Envia o arquivo pelo input de fotos (o mesmo que abre a câmera no celular)."""
        campo = self.wait.until(EC.presence_of_element_located((By.ID, "novasFotos")))
        campo.send_keys(str(caminho))
        return self

    def enviar_levantamento(self):
        self.enviar(por_texto_do_botao("Enviar levantamento"))
        return self

    def campos_faltantes(self):
        """Itens da caixa "Para enviar, falta:" do G1."""
        caixa = self._visivel(por_testid("levantamento-faltantes"))
        return [item.text.strip() for item in caixa.find_elements(By.TAG_NAME, "li")]

    def esta_no_levantamento(self, projeto_id):
        return self.caminho_atual() == f"/campo/projetos/{projeto_id}/levantamento"
