"""Configuração do teste de sistema (E2E) com Selenium.

Variáveis de ambiente:
- E2E_BASE_URL: endereço do sistema (padrão http://localhost:8080)
- E2E_ADMIN_LOGIN / E2E_ADMIN_SENHA: administrador da carga inicial (padrão admin / admin1234)
- E2E_HEADLESS=1: Chrome sem janela
"""

import base64
import os
import struct
import zlib
from datetime import datetime
from types import SimpleNamespace

import pytest
from selenium import webdriver

from paginas.base_page import PASTA_EVIDENCIAS

PERFIS = ("ADMINISTRADOR", "VENDEDOR", "PROJETISTA", "COMPRADOR", "ALMOXARIFE", "TECNICO_CAMPO",
          "FINANCEIRO", "DIRETORIA")
NOMES_DOS_PERFIS = {
    "ADMINISTRADOR": "Administrador", "VENDEDOR": "Vendedor", "PROJETISTA": "Projetista",
    "COMPRADOR": "Comprador", "ALMOXARIFE": "Almoxarife", "TECNICO_CAMPO": "Técnico de Campo",
    "FINANCEIRO": "Financeiro", "DIRETORIA": "Diretoria",
}
SENHA_DOS_USUARIOS = "senha-e2e-123"


# ---------- Configuração ----------

@pytest.fixture(scope="session")
def config():
    return SimpleNamespace(
        base_url=os.environ.get("E2E_BASE_URL", "http://localhost:8080").rstrip("/"),
        admin_login=os.environ.get("E2E_ADMIN_LOGIN", "admin"),
        admin_senha=os.environ.get("E2E_ADMIN_SENHA", "admin1234"),
        headless=os.environ.get("E2E_HEADLESS") == "1",
    )


@pytest.fixture
def driver(config):
    opcoes = webdriver.ChromeOptions()
    if config.headless:
        opcoes.add_argument("--headless=new")
    opcoes.add_argument("--window-size=1366,900")
    opcoes.add_argument("--lang=pt-BR")
    # Sem os avisos de senha do Chrome, que abrem por cima da página com senhas de teste
    opcoes.add_argument("--disable-features=PasswordLeakDetection")
    opcoes.add_experimental_option("prefs", {
        "intl.accept_languages": "pt-BR,pt",
        "credentials_enable_service": False,
        "profile.password_manager_enabled": False,
        "profile.password_manager_leak_detection": False,
    })
    navegador = webdriver.Chrome(options=opcoes)  # o Selenium Manager baixa o driver certo
    navegador.implicitly_wait(0)  # só esperas explícitas
    navegador.set_page_load_timeout(60)  # página que não termina de carregar falha em vez de travar
    yield navegador
    navegador.quit()


@pytest.hookimpl(hookwrapper=True, tryfirst=True)
def pytest_runtest_makereport(item, call):
    """Na falha, salva o print da tela em e2e/evidencias/{nome do teste}.png e anexa ao relatório."""
    resultado = yield
    relatorio = resultado.get_result()
    if relatorio.when not in ("setup", "call") or not relatorio.failed:
        return
    navegador = item.funcargs.get("driver")
    if navegador is None:
        return
    PASTA_EVIDENCIAS.mkdir(parents=True, exist_ok=True)
    caminho = PASTA_EVIDENCIAS / f"{item.name}.png"
    try:
        navegador.save_screenshot(str(caminho))
    except Exception:  # o navegador pode já ter caído; o relatório segue sem o print
        return
    html = item.config.pluginmanager.getplugin("html")
    if html is not None:
        extras = getattr(relatorio, "extras", [])
        extras.append(html.extras.png(base64.b64encode(caminho.read_bytes()).decode("ascii")))
        extras.append(html.extras.url(navegador.current_url, name="Página da falha"))
        relatorio.extras = extras


# ---------- Dados únicos de cada execução ----------

@pytest.fixture
def sufixo():
    """Data e hora da execução: deixa logins, nomes e documentos únicos no mesmo banco."""
    return datetime.now().strftime("%Y%m%d%H%M%S")


def cpf_valido(base_nove_digitos):
    """CPF com os dígitos verificadores certos a partir de 9 dígitos."""
    digitos = [int(d) for d in base_nove_digitos]
    for tamanho in (9, 10):
        soma = sum(d * peso for d, peso in zip(digitos, range(tamanho + 1, 1, -1)))
        resto = (soma * 10) % 11
        digitos.append(0 if resto == 10 else resto)
    texto = "".join(str(d) for d in digitos)
    return f"{texto[:3]}.{texto[3:6]}.{texto[6:9]}-{texto[9:]}"


@pytest.fixture
def cpf_unico():
    agora = datetime.now()
    base = f"{agora:%d%H%M%S}{agora.microsecond // 1000 % 10}"  # 9 dígitos
    if len(set(base)) == 1:  # CPF com todos os dígitos iguais é inválido
        base = base[:-1] + str((int(base[-1]) + 1) % 10)
    return cpf_valido(base)


def png_de_teste(largura=32, altura=24):
    """PNG pequeno gerado na hora, sem biblioteca de imagem: faixas coloridas, 8 bits RGB."""
    linhas = []
    for y in range(altura):
        cor = (30, 120, 200) if y < altura // 2 else (60, 170, 80)
        linhas.append(b"\x00" + bytes(cor) * largura)

    def bloco(tipo, dados):
        return (struct.pack(">I", len(dados)) + tipo + dados
                + struct.pack(">I", zlib.crc32(tipo + dados) & 0xFFFFFFFF))

    cabecalho = struct.pack(">IIBBBBB", largura, altura, 8, 2, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + bloco(b"IHDR", cabecalho)
            + bloco(b"IDAT", zlib.compress(b"".join(linhas))) + bloco(b"IEND", b""))


@pytest.fixture
def foto_do_levantamento(tmp_path):
    """Foto da fonte de água para o levantamento, num arquivo da pasta temporária do pytest."""
    caminho = tmp_path / "fonte-de-agua.png"
    caminho.write_bytes(png_de_teste())
    return caminho


@pytest.fixture
def usuarios_para(sufixo):
    """Dados de um usuário por perfil, com login e nome únicos nesta execução."""
    def criar(etiqueta, perfis=PERFIS):
        return {
            perfil: SimpleNamespace(
                perfil=perfil,
                login=f"{perfil.lower().replace('_', '.')}.{etiqueta}.{sufixo}",
                nome=f"{NOMES_DOS_PERFIS[perfil]} E2E {etiqueta.upper()} {sufixo}",
                senha=SENHA_DOS_USUARIOS,
            )
            for perfil in perfis
        }
    return criar
