# Teste de sistema (E2E) com Selenium

Testes de ponta a ponta pela interface, em Python com Selenium e pytest, separados do código Java.
Eles abrem o Chrome e usam o sistema como as pessoas usam: login de cada perfil, formulários,
botões, confirmações, foto, coordenada e assinatura no quadro.

| Teste | O que confere |
|---|---|
| `test_fluxo_completo` | O processo TO-BE inteiro, da criação dos usuários ao projeto ENCERRADO, com a medição reprovada (G6 Não) e corrigida antes do aceite, e as etapas da linha do tempo |
| `test_levantamento_incompleto` | G1 Não: o levantamento sem foto e sem topografia volta para o campo, lista o que falta e o projeto continua em levantamento |
| `test_acesso_negado` | O técnico de campo recebe a página de acesso negado em `/admin/usuarios` e `/diretoria/aprovacoes` |

## Estrutura

```
e2e/
  conftest.py        driver do Chrome, configuração, dados únicos e print na falha
  pytest.ini
  requirements.txt
  paginas/           Page Objects, um por tela; o teste só chama métodos deles
  testes/            os testes
  evidencias/        prints gerados (fora do Git)
```

## Pré-requisitos

- Python 3.10 ou mais novo e Google Chrome instalado. O Selenium 4 baixa o ChromeDriver certo
  sozinho (Selenium Manager); na primeira execução isso precisa de internet.
- O sistema rodando no perfil `dev`, com o PostgreSQL do `docker-compose.yml` (veja abaixo).
- A página carrega o Bootstrap e os ícones de CDN, então o navegador precisa de internet.

## 1. Criar o ambiente virtual e instalar

Na raiz do repositório, no PowerShell:

```powershell
python -m venv e2e/venv
e2e/venv/Scripts/Activate.ps1
pip install -r e2e/requirements.txt
```

No Linux ou no macOS, troque a ativação por `source e2e/venv/bin/activate`.

## 2. Subir o sistema

```powershell
docker compose up -d
$env:DB_PASSWORD = "postgres"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

O perfil `dev` cria o usuário `admin` com a senha `admin1234` quando não existe administrador, e a
carga inicial cadastra os materiais. O fluxo usa o `TG-16-30` (tubo gotejador, 10.000 m em estoque),
que tem saldo de sobra para não cair em faltante.

Se já existir um PostgreSQL instalado no Windows usando a porta 5432, a aplicação conecta nele e não
no container. Nesse caso, publique o container em outra porta só nesta máquina, sem mudar o
`docker-compose.yml`, com um arquivo à parte (por exemplo `compose-5433.yml`, fora do repositório):

```yaml
services:
  postgres:
    ports:
      - "5433:5432"
```

```powershell
docker compose -f docker-compose.yml -f compose-5433.yml up -d
$env:DB_URL = "jdbc:postgresql://localhost:5433/irrigacao"
$env:DB_PASSWORD = "postgres"
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Em máquina com pouca memória (Docker, sistema e Chrome juntos), limite a memória do sistema e rode o
Chrome sem janela:

```powershell
mvn spring-boot:run "-Dspring-boot.run.profiles=dev" "-Dspring-boot.run.jvmArguments=-Xmx512m -XX:+UseSerialGC"
$env:E2E_HEADLESS = "1"
```

## 3. Rodar os testes

Com o sistema no ar e o ambiente virtual ativo, na raiz do repositório:

```powershell
pytest e2e --html=e2e/relatorio.html
```

Um teste só: `pytest e2e -k test_fluxo_completo --html=e2e/relatorio.html`.

O relatório `e2e/relatorio.html` sai num arquivo só, com o print da tela anexado em cada falha.

### Variáveis de ambiente

| Variável | Padrão | Para quê |
|---|---|---|
| `E2E_BASE_URL` | `http://localhost:8080` | Endereço do sistema |
| `E2E_ADMIN_LOGIN` | `admin` | Administrador que cria os usuários do teste |
| `E2E_ADMIN_SENHA` | `admin1234` | Senha dele |
| `E2E_HEADLESS` | vazio | `1` roda o Chrome sem janela |
| `E2E_ESPERA` | `30` | Segundos de cada espera explícita |

Exemplo no PowerShell: `$env:E2E_HEADLESS = "1"; pytest e2e --html=e2e/relatorio.html`.

## Como os testes foram escritos

- Só esperas explícitas (`WebDriverWait` com `expected_conditions`); não há `time.sleep` nem
  espera implícita.
- Seletores por `id`, `name` ou texto do botão/link. Onde não havia um estável, o template ganhou um
  atributo `data-testid` (mensagens do topo, situação do projeto e da proposta, linha do tempo, cartão
  da aprovação, caixa de faltantes do levantamento, botões "Apontar" e resultado da medição).
- Cada execução cria os próprios usuários, produtor (com CPF válido gerado na hora), propriedade e
  projeto com sufixo de data e hora, então os testes rodam várias vezes no mesmo banco.
- As telas de campo (levantamento, ordem de serviço, medição e aceite) usam janela de 412 x 915,
  como um celular. O Chrome do Windows não encolhe a janela abaixo de uns 500 px; quando isso
  acontece, a área da página é fixada em 412 x 915 pelo DevTools.
- A coordenada vai pelos campos manuais de latitude e longitude, sem o GPS do aparelho.
- A foto do levantamento é um PNG pequeno gerado na hora na pasta temporária do pytest e enviado
  pelo campo de arquivo.
- A assinatura do aceite é desenhada no quadro com `ActionChains` (clicar, arrastar e soltar).
- As confirmações dos formulários (`window.confirm`) são aceitas pelo teste.

## Evidências

Cada passo importante salva um print da página inteira em `e2e/evidencias/`, com número e nome do
passo. O fluxo completo gera de `01-usuarios-criados.png` a
`21-projeto-encerrado-com-linha-do-tempo.png` (por exemplo, `07-proposta-aguardando-diretoria.png`).
Os outros testes usam os prefixos `g1-` e `acesso-`. Quando um teste falha, o print da tela no
momento da falha vai para `e2e/evidencias/<nome do teste>.png`.
