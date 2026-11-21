from selenium.webdriver.common.by import By

from paginas.base_page import BasePage, por_testid, por_texto_do_botao

PREFIXO_SITUACAO = "situacao-"
BADGE_SITUACAO = (By.CSS_SELECTOR, f"[data-testid^='{PREFIXO_SITUACAO}']")


class ProjetoPage(BasePage):
    """Abertura do projeto, agendamento da visita e página do projeto com a linha do tempo."""

    def confirmar_abertura(self):
        """Botão do formulário de abertura. @return (id, código) do projeto aberto"""
        self.enviar(por_texto_do_botao("Abrir projeto"))
        self.resultado()
        return self.id_no_fim_do_caminho("projetos"), self.codigo()

    def agendar_visita(self, projeto_id, data_hora_iso):
        self.ir_para(f"/comercial/projetos/{projeto_id}/visita")
        self.definir_data("dataAgendada", data_hora_iso)
        self.enviar(por_texto_do_botao("Agendar visita"))
        return self.resultado()

    def abrir(self, projeto_id):
        return self.ir_para(f"/projetos/{projeto_id}")

    def codigo(self):
        return self._visivel(por_testid("codigo-projeto")).text.strip()

    def situacao(self):
        """Situação do projeto no cabeçalho, pelo nome do enum (ex.: ENCERRADO)."""
        cabecalho = self._visivel(por_testid("cabecalho-projeto"))
        badge = cabecalho.find_element(*BADGE_SITUACAO)
        return badge.get_attribute("data-testid")[len(PREFIXO_SITUACAO):]

    def situacao_do_projeto(self, projeto_id):
        return self.abrir(projeto_id).situacao()

    def etapas_da_linha_do_tempo(self):
        """Situações por onde o projeto passou, na ordem da linha do tempo. Em cada mudança o último
        badge é a situação nova; eventos sem mudança de situação (apontamento, medição...) não entram."""
        linha = self._visivel(por_testid("linha-do-tempo"))
        etapas = []
        for evento in linha.find_elements(*por_testid("evento-linha-tempo")):
            badges = evento.find_elements(*BADGE_SITUACAO)
            if badges:
                etapas.append(badges[-1].get_attribute("data-testid")[len(PREFIXO_SITUACAO):])
        return etapas
