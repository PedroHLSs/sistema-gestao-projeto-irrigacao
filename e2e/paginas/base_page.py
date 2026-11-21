"""Base dos Page Objects: esperas explícitas, envio de formulário, mensagens do topo e prints.

Regras do teste de sistema:
- só WebDriverWait com expected_conditions (nada de time.sleep nem espera implícita);
- seletor por id, name ou texto do botão/link, e data-testid onde não há outro estável;
- o teste chama só os métodos dos Page Objects, nunca um seletor.
"""

import base64
import os
from pathlib import Path

from selenium.common.exceptions import TimeoutException, WebDriverException
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import Select, WebDriverWait

PASTA_E2E = Path(__file__).resolve().parent.parent
PASTA_EVIDENCIAS = PASTA_E2E / "evidencias"

ESPERA_PADRAO = int(os.environ.get("E2E_ESPERA", "30"))

# Telas de campo (RNF06): janela de celular
CELULAR = (412, 915)
COMPUTADOR = (1366, 900)

MENSAGEM_SUCESSO = (By.CSS_SELECTOR, "[data-testid='mensagem-sucesso']")
MENSAGEM_AVISO = (By.CSS_SELECTOR, "[data-testid='mensagem-aviso']")
MENSAGEM_ERRO = (By.CSS_SELECTOR, "[data-testid='mensagem-erro']")
ACESSO_NEGADO = (By.CSS_SELECTOR, "[data-testid='acesso-negado']")
PAGINA = (By.TAG_NAME, "html")
CORPO = (By.TAG_NAME, "body")


def por_texto_do_botao(texto):
    """Botão pelo texto que o usuário lê nele (ignora ícones e espaços)."""
    return (By.XPATH, f"//button[contains(normalize-space(.), {_literal(texto)})]")


def por_texto_do_link(texto):
    return (By.XPATH, f"//a[contains(normalize-space(.), {_literal(texto)})]")


def por_testid(valor):
    return (By.CSS_SELECTOR, f"[data-testid='{valor}']")


def _literal(texto):
    """Literal XPath seguro para textos com aspas simples."""
    if "'" not in texto:
        return f"'{texto}'"
    partes = texto.split("'")
    return "concat(" + ", \"'\", ".join(f"'{p}'" for p in partes) + ")"


def _pagina_trocou(pagina_antiga):
    """EC.staleness_of da página anterior. No meio da navegação o ChromeDriver às vezes responde
    "Node with given id does not belong to the document" em vez de StaleElementReference: é o
    mesmo caso (o elemento já não está no documento) e conta como página trocada."""
    condicao = EC.staleness_of(pagina_antiga)

    def verificar(driver):
        try:
            return condicao(driver)
        except WebDriverException as erro:
            if "does not belong to the document" in str(erro):
                return True
            raise
    return verificar


class BasePage:
    def __init__(self, driver, base_url):
        self.driver = driver
        self.base_url = base_url.rstrip("/")
        self.wait = WebDriverWait(driver, ESPERA_PADRAO)

    # ---------- Navegação ----------

    def ir_para(self, caminho):
        self.driver.get(self.base_url + caminho)
        self.wait.until(EC.presence_of_element_located(CORPO))
        return self

    def caminho_atual(self):
        url = self.driver.current_url
        sem_base = url[len(self.base_url):] if url.startswith(self.base_url) else url
        return sem_base.split("#")[0].split("?")[0]

    def id_no_fim_do_caminho(self, depois_de):
        """Id numérico que vem logo depois de um trecho do caminho, como /projetos/{id}."""
        partes = self.caminho_atual().strip("/").split("/")
        indice = partes.index(depois_de.strip("/"))
        return int(partes[indice + 1])

    # ---------- Janela ----------

    def usar_celular(self):
        """Janela de 412 x 915, como celular. Se o Chrome com interface não encolher tanto
        (o Windows impõe largura mínima), a área da página é fixada pelo DevTools."""
        self.driver.set_window_size(*CELULAR)
        largura = self.driver.execute_script("return window.innerWidth")
        if largura != CELULAR[0]:
            self.driver.execute_cdp_cmd("Emulation.setDeviceMetricsOverride", {
                "width": CELULAR[0], "height": CELULAR[1], "deviceScaleFactor": 1, "mobile": True})
        return self

    def usar_computador(self):
        try:
            self.driver.execute_cdp_cmd("Emulation.clearDeviceMetricsOverride", {})
        except WebDriverException:
            pass
        self.driver.set_window_size(*COMPUTADOR)
        return self

    # ---------- Campos ----------

    def _visivel(self, localizador):
        return self.wait.until(EC.visibility_of_element_located(localizador))

    def _rolar_ate(self, elemento):
        # A barra fixa do celular pode cobrir o elemento: centraliza antes de clicar. A rolagem é
        # instantânea porque o Bootstrap liga scroll-behavior: smooth e o clique cairia no meio dela.
        self.driver.execute_script("arguments[0].scrollIntoView({block: 'center', behavior: 'instant'});",
                                   elemento)
        return elemento

    def digitar(self, localizador, valor):
        campo = self._rolar_ate(self._visivel(localizador))
        campo.clear()
        campo.send_keys(str(valor))
        self.wait.until(EC.text_to_be_present_in_element_value(localizador, str(valor)))
        return campo

    def preencher_id(self, id_campo, valor):
        return self.digitar((By.ID, id_campo), valor)

    def definir_data(self, id_campo, valor_iso):
        """Campos date e datetime-local: a digitação depende do idioma do navegador, então o valor
        vai no formato ISO, o mesmo que o navegador envia (yyyy-MM-dd ou yyyy-MM-ddTHH:mm)."""
        localizador = (By.ID, id_campo)
        campo = self._rolar_ate(self._visivel(localizador))
        self.driver.execute_script(
            "arguments[0].value = arguments[1];"
            "arguments[0].dispatchEvent(new Event('input', {bubbles: true}));"
            "arguments[0].dispatchEvent(new Event('change', {bubbles: true}));",
            campo, valor_iso)
        self.wait.until(EC.text_to_be_present_in_element_value(localizador, valor_iso))

    def escolher_por_valor(self, id_campo, valor):
        Select(self._rolar_ate(self._visivel((By.ID, id_campo)))).select_by_value(valor)

    def clicar(self, localizador):
        elemento = self._rolar_ate(self.wait.until(EC.presence_of_element_located(localizador)))
        self.wait.until(EC.element_to_be_clickable(localizador)).click()
        return elemento

    def enviar(self, localizador, confirmar=False):
        """Clica no botão que envia o formulário e espera a página nova carregar. Com confirmar,
        aceita a pergunta do formulário data-confirmar (window.confirm do app.js)."""
        pagina_antiga = self.driver.find_element(*PAGINA)
        self.clicar(localizador)
        if confirmar:
            self.wait.until(EC.alert_is_present()).accept()
        self.wait.until(_pagina_trocou(pagina_antiga))
        self.wait.until(EC.presence_of_element_located(CORPO))
        return self

    # ---------- Mensagens do topo (fragments/mensagens.html) ----------

    def resultado(self):
        """Espera a mensagem do topo depois de uma ação e devolve o texto de sucesso ou de aviso.
        Se o sistema recusou, falha mostrando o motivo que ele deu."""
        try:
            self.wait.until(EC.any_of(EC.visibility_of_element_located(MENSAGEM_SUCESSO),
                                      EC.visibility_of_element_located(MENSAGEM_AVISO),
                                      EC.visibility_of_element_located(MENSAGEM_ERRO)))
        except TimeoutException as erro:
            raise AssertionError(f"Nenhuma mensagem no topo da página {self.caminho_atual()}") from erro
        erros = self.driver.find_elements(*MENSAGEM_ERRO)
        if erros:
            raise AssertionError(f"O sistema recusou a ação em {self.caminho_atual()}: {erros[0].text}")
        mensagens = self.driver.find_elements(*MENSAGEM_SUCESSO) + self.driver.find_elements(*MENSAGEM_AVISO)
        return " ".join(m.text for m in mensagens)

    def mensagem_erro(self):
        return self._visivel(MENSAGEM_ERRO).text

    def acesso_negado_exibido(self):
        try:
            self.wait.until(EC.visibility_of_element_located(ACESSO_NEGADO))
            return True
        except TimeoutException:
            return False

    # ---------- Evidências ----------

    def salvar_evidencia(self, nome):
        """Print da página inteira (não só a parte visível) em e2e/evidencias/{nome}.png.

        A área da página cresce até a altura do conteúdo só durante o print e depois volta ao
        tamanho anterior. Assim o menu lateral e a barra do topo, que são fixos, ficam no lugar
        (a captura "além da área visível" do Chrome desenha os dois por cima do conteúdo)."""
        PASTA_EVIDENCIAS.mkdir(parents=True, exist_ok=True)
        caminho = PASTA_EVIDENCIAS / f"{nome}.png"
        largura = self.driver.execute_script("return window.innerWidth")
        celular = largura <= CELULAR[0]
        try:
            self.driver.execute_script("window.scrollTo(0, 0);")
            altura = self.driver.execute_script(
                "return Math.max(document.documentElement.scrollHeight, document.body.scrollHeight);")
            self.driver.execute_cdp_cmd("Emulation.setDeviceMetricsOverride", {
                "width": largura, "height": altura, "deviceScaleFactor": 1, "mobile": celular})
            imagem = self.driver.execute_cdp_cmd("Page.captureScreenshot", {"format": "png"})
            caminho.write_bytes(base64.b64decode(imagem["data"]))
        except (WebDriverException, KeyError):
            self.driver.save_screenshot(str(caminho))
        finally:
            if celular:
                self.driver.execute_cdp_cmd("Emulation.setDeviceMetricsOverride", {
                    "width": CELULAR[0], "height": CELULAR[1], "deviceScaleFactor": 1, "mobile": True})
            else:
                self.driver.execute_cdp_cmd("Emulation.clearDeviceMetricsOverride", {})
        return caminho
