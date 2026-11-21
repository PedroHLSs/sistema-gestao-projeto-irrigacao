"""Teste de sistema do G1 Não: levantamento enviado sem foto e sem topografia volta para o campo."""

from datetime import datetime, timedelta

from paginas import LevantamentoPage, LoginPage, ProdutorPage, ProjetoPage, UsuariosPage


def test_levantamento_incompleto(driver, config, sufixo, cpf_unico, usuarios_para):
    base = config.base_url
    login = LoginPage(driver, base)
    usuarios = UsuariosPage(driver, base)
    produtores = ProdutorPage(driver, base)
    projeto = ProjetoPage(driver, base)
    levantamento = LevantamentoPage(driver, base)

    equipe = usuarios_para("g1", perfis=("VENDEDOR", "TECNICO_CAMPO"))
    vendedor, tecnico = equipe["VENDEDOR"], equipe["TECNICO_CAMPO"]

    login.entrar(config.admin_login, config.admin_senha)
    for pessoa in equipe.values():
        usuarios.criar(pessoa.nome, pessoa.login, pessoa.senha, pessoa.perfil)
    login.sair()

    # Atividades 1 e 2: projeto aberto com a visita agendada
    login.entrar(vendedor.login, vendedor.senha)
    produtores.cadastrar(f"Produtor G1 {sufixo}", cpf_unico)
    produtores.cadastrar_propriedade(f"Fazenda G1 {sufixo}", "Araguari")
    produtores.abrir_projeto_da_propriedade()
    projeto_id, _ = projeto.confirmar_abertura()
    amanha = (datetime.now() + timedelta(days=1)).replace(hour=14, minute=0)
    projeto.agendar_visita(projeto_id, amanha.strftime("%Y-%m-%dT%H:%M"))
    login.sair()

    # Atividade 3 no celular: tudo menos a topografia e a foto
    levantamento.usar_celular()
    login.entrar(tecnico.login, tecnico.senha)
    levantamento.abrir(projeto_id).preencher(
        cultura="Milho", area_irrigada_ha="3", fonte_agua="RIO_CORREGO", vazao_fonte="12",
        latitude="-18.6489", longitude="-48.1934")
    levantamento.enviar_levantamento()

    # G1 Não: o envio volta à tela do levantamento listando o que falta
    assert levantamento.esta_no_levantamento(projeto_id)
    erro = levantamento.mensagem_erro()
    assert "incompleto" in erro and "topografia" in erro and "pelo menos uma foto" in erro
    assert levantamento.campos_faltantes() == ["topografia", "pelo menos uma foto"]
    levantamento.salvar_evidencia("g1-01-levantamento-incompleto")

    # O projeto continua em levantamento
    levantamento.usar_computador()
    assert projeto.situacao_do_projeto(projeto_id) == "EM_LEVANTAMENTO"
    projeto.salvar_evidencia("g1-02-projeto-continua-em-levantamento")
