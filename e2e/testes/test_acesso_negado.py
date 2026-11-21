"""Teste de sistema do controle de acesso por perfil (RNF04): o técnico de campo não abre as telas
do administrador nem da diretoria."""

from paginas import AprovacoesPage, LoginPage, UsuariosPage


def test_acesso_negado(driver, config, usuarios_para):
    base = config.base_url
    login = LoginPage(driver, base)
    usuarios = UsuariosPage(driver, base)
    aprovacoes = AprovacoesPage(driver, base)

    tecnico = usuarios_para("acesso", perfis=("TECNICO_CAMPO",))["TECNICO_CAMPO"]
    login.entrar(config.admin_login, config.admin_senha)
    usuarios.criar(tecnico.nome, tecnico.login, tecnico.senha, tecnico.perfil)
    login.sair()

    login.entrar(tecnico.login, tecnico.senha)

    usuarios.abrir_lista()
    assert usuarios.acesso_negado_exibido(), "o técnico abriu /admin/usuarios"
    usuarios.salvar_evidencia("acesso-01-tecnico-em-admin-usuarios")

    aprovacoes.abrir()
    assert aprovacoes.acesso_negado_exibido(), "o técnico abriu /diretoria/aprovacoes"
    aprovacoes.salvar_evidencia("acesso-02-tecnico-em-diretoria-aprovacoes")
