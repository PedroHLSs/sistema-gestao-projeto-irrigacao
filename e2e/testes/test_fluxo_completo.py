"""Teste de sistema: o processo TO-BE inteiro pela interface, da criação dos usuários ao encerramento
do projeto, com cada perfil fazendo a sua parte (atividades 1 a 24, gateways G1 a G8).

Desvio pelo G6: a primeira medição fica fora da tolerância e volta para a instalação.
"""

from datetime import date, datetime, timedelta

from paginas import (AprovacoesPage, DimensionamentoPage, LevantamentoPage, LiberacaoPage, LoginPage,
                     OrdemServicoPage, ProdutorPage, ProgramacaoPage, ProjetoPage, PropostaPage,
                     SeparacaoPage, SobrasPage, UsuariosPage)

# Material da carga inicial com saldo de sobra (10.000 m): o fluxo não cai em faltante (G4 Sim)
MATERIAL = "TG-16-30"
QUANTIDADE_PREVISTA = "20"
QUANTIDADE_APONTADA = "18"  # sobram 2 m para a devolução (G8)

# Parâmetros de projeto: 30 mca, 6 m³/h e 2% de tolerância (faixa de 29,4 a 30,6 mca e 5,88 a 6,12 m³/h)
PRESSAO, VAZAO, TOLERANCIA = "30", "6", "2"
LATITUDE, LONGITUDE = "-18.9113", "-48.2622"

DESCONTO_ACIMA_DA_ALCADA = "8"  # alçada do vendedor: 5%

ETAPAS_ESPERADAS = [
    "EM_LEVANTAMENTO", "ORCANDO", "AGUARDANDO_RESPOSTA", "PREPARANDO_MATERIAL", "PROGRAMANDO_EXECUCAO",
    "EXECUTANDO_INSTALACAO", "VALIDANDO_MEDICAO", "CORRIGINDO_INSTALACAO", "EXECUTANDO_INSTALACAO",
    "VALIDANDO_MEDICAO", "COLHENDO_ACEITE", "FINALIZANDO_EXECUCAO", "ENCERRADO",
]


def test_fluxo_completo(driver, config, sufixo, cpf_unico, foto_do_levantamento, usuarios_para):
    base = config.base_url
    login = LoginPage(driver, base)
    usuarios = UsuariosPage(driver, base)
    produtores = ProdutorPage(driver, base)
    projeto = ProjetoPage(driver, base)
    levantamento = LevantamentoPage(driver, base)
    dimensionamento = DimensionamentoPage(driver, base)
    propostas = PropostaPage(driver, base)
    aprovacoes = AprovacoesPage(driver, base)
    separacao = SeparacaoPage(driver, base)
    programacao = ProgramacaoPage(driver, base)
    ordem = OrdemServicoPage(driver, base)
    sobras = SobrasPage(driver, base)
    liberacao = LiberacaoPage(driver, base)

    equipe = usuarios_para("fluxo")
    vendedor, projetista, almoxarife = equipe["VENDEDOR"], equipe["PROJETISTA"], equipe["ALMOXARIFE"]
    tecnico, financeiro, diretoria = equipe["TECNICO_CAMPO"], equipe["FINANCEIRO"], equipe["DIRETORIA"]
    cliente = f"Produtor E2E {sufixo}"

    # RF23: o administrador cria um usuário de cada perfil
    login.entrar(config.admin_login, config.admin_senha)
    for pessoa in equipe.values():
        assert f"Usuário {pessoa.login} criado" in usuarios.criar(pessoa.nome, pessoa.login, pessoa.senha,
                                                                   pessoa.perfil)
    assert all(usuarios.usuario_listado(pessoa.login) for pessoa in equipe.values())
    usuarios.salvar_evidencia("01-usuarios-criados")
    login.sair()

    # Atividade 1 (RF01): produtor, propriedade e abertura do projeto
    login.entrar(vendedor.login, vendedor.senha)
    produtores.cadastrar(cliente, cpf_unico, telefone="(34) 99999-0000")
    produtores.cadastrar_propriedade(f"Sítio E2E {sufixo}", "Uberlândia", area_total_ha="40")
    produtores.salvar_evidencia("02-produtor-e-propriedade-cadastrados")
    produtores.abrir_projeto_da_propriedade()
    projeto_id, codigo = projeto.confirmar_abertura()
    assert projeto.situacao() == "EM_LEVANTAMENTO"
    projeto.salvar_evidencia("03-projeto-aberto")

    # Atividade 2 (RF02): agendamento da visita técnica
    amanha = (datetime.now() + timedelta(days=1)).replace(hour=8, minute=30)
    projeto.agendar_visita(projeto_id, amanha.strftime("%Y-%m-%dT%H:%M"))
    projeto.salvar_evidencia("04-visita-agendada")
    login.sair()

    # Atividades 3 e 4, G1 Sim (RF03, RF04): levantamento completo no celular, com foto e coordenada
    levantamento.usar_celular()
    login.entrar(tecnico.login, tecnico.senha)
    levantamento.abrir(projeto_id).preencher(
        cultura="Café", area_irrigada_ha="0.5", fonte_agua="POCO_ARTESIANO", vazao_fonte="8",
        topografia="Plana, com leve declive", latitude=LATITUDE, longitude=LONGITUDE)
    levantamento.anexar_foto(foto_do_levantamento)
    levantamento.salvar_evidencia("05-levantamento-preenchido-no-celular")
    levantamento.enviar_levantamento()
    assert "Levantamento completo e enviado" in levantamento.resultado()
    login.sair()
    levantamento.usar_computador()

    # Atividade 5 (RF05): pressão, vazão e tolerância, e lista técnica pela busca de material
    login.entrar(projetista.login, projetista.senha)
    dimensionamento.abrir(projeto_id)
    dimensionamento.registrar_parametros(PRESSAO, VAZAO, TOLERANCIA)
    dimensionamento.adicionar_material(MATERIAL, QUANTIDADE_PREVISTA)
    dimensionamento.concluir_lista_tecnica()
    dimensionamento.salvar_evidencia("06-lista-tecnica-concluida")
    assert projeto.situacao_do_projeto(projeto_id) == "ORCANDO"
    login.sair()

    # Atividades 8 e 9, G2 Não (RF09, RF10): desconto acima da alçada vai para a diretoria
    login.entrar(vendedor.login, vendedor.senha)
    proposta_id = propostas.abrir_orcamento(projeto_id).gerar("10", DESCONTO_ACIMA_DA_ALCADA)
    assert propostas.situacao() == "EM_ELABORACAO"
    propostas.pedir_aprovacao_da_diretoria()
    assert propostas.situacao() == "AGUARDANDO_DIRETORIA"
    propostas.salvar_evidencia("07-proposta-aguardando-diretoria")
    login.sair()

    login.entrar(diretoria.login, diretoria.senha)
    assert aprovacoes.abrir().pendente(proposta_id)
    aprovacoes.salvar_evidencia("08-desconto-na-fila-da-diretoria")
    aprovacoes.aprovar(proposta_id, "Cliente estratégico, desconto aprovado.")
    assert not aprovacoes.pendente(proposta_id)
    login.sair()

    # Atividades 10 a 12, G3 Sim (RF11, RF12): envio ao cliente e aprovação dentro da validade
    login.entrar(vendedor.login, vendedor.senha)
    propostas.abrir(proposta_id)
    assert propostas.situacao() == "EM_ELABORACAO"  # de volta ao vendedor, já aprovada pela diretoria
    propostas.enviar_ao_cliente()
    assert propostas.situacao() == "ENVIADA"
    propostas.salvar_evidencia("09-proposta-enviada")
    propostas.registrar_aprovacao_do_cliente()  # o sistema leva à página do projeto
    assert projeto.situacao() == "PREPARANDO_MATERIAL"
    projeto.salvar_evidencia("10-proposta-aprovada-pelo-cliente")
    assert propostas.abrir(proposta_id).situacao() == "APROVADA"
    login.sair()

    # Atividades 13 e 14, G4 Sim e G5 Sim (RF14): separação e conferência
    login.entrar(almoxarife.login, almoxarife.senha)
    separacao.abrir(projeto_id).separar_tudo()
    separacao.liberar_para_execucao()  # leva à página do projeto
    assert projeto.situacao() == "PROGRAMANDO_EXECUCAO"
    separacao.salvar_evidencia("11-separacao-conferida")
    login.sair()

    # Atividade 15 (RF15): ordem de serviço para o técnico
    login.entrar(projetista.login, projetista.senha)
    programacao.abrir(projeto_id).emitir_ordem(date.today().isoformat(), tecnico.nome)  # vai ao projeto
    assert projeto.situacao() == "EXECUTANDO_INSTALACAO"
    programacao.salvar_evidencia("12-ordem-de-servico-emitida")
    login.sair()

    # Atividades 16 a 20 no celular (RF16, RF17, RF18)
    ordem.usar_celular()
    login.entrar(tecnico.login, tecnico.senha)
    ordem.abrir_minhas_os().abrir_ordem_do_projeto(codigo)
    ordem.apontar_horas("8")
    ordem.apontar_material(MATERIAL, QUANTIDADE_APONTADA)
    ordem.salvar_evidencia("13-horas-e-material-apontados")
    ordem.concluir_instalacao()

    # G6 Não: 31 mca passa do limite de 30,6 mca; a medição fica reprovada e volta para a correção
    ordem.preencher_medicao("31", VAZAO, LATITUDE, LONGITUDE).registrar_medicao()
    assert "fora do parâmetro" in ordem.mensagem_erro()
    assert ordem.medicoes_reprovadas() == 1
    ordem.salvar_evidencia("14-medicao-reprovada")
    ordem.registrar_correcao("Regulador de pressão ajustado na entrada do setor.")
    ordem.salvar_evidencia("15-correcao-registrada")
    ordem.concluir_instalacao()  # corrigida, a instalação é concluída de novo para ir à medição

    # G6 Sim: nova medição dentro da tolerância
    ordem.preencher_medicao("30.2", "6.05", LATITUDE, LONGITUDE).registrar_medicao()
    ordem.resultado()
    assert ordem.medicoes_aprovadas() == 1 and ordem.medicoes_reprovadas() == 1
    ordem.salvar_evidencia("16-medicao-aprovada")

    # G7 Sim: o cliente assina o termo de aceite no quadro
    ordem.preencher_aceite(cliente)
    ordem.salvar_evidencia("17-assinatura-do-aceite")
    ordem.salvar_aceite()
    ordem.salvar_evidencia("18-aceite-registrado")
    login.sair()
    ordem.usar_computador()

    # Atividade 22, G8 Sim (RF19): a sobra volta ao estoque
    login.entrar(almoxarife.login, almoxarife.senha)
    assert projeto.situacao_do_projeto(projeto_id) == "FINALIZANDO_EXECUCAO"
    sobras.abrir(projeto_id).confirmar_devolucao()
    sobras.salvar_evidencia("19-sobras-confirmadas")
    login.sair()

    # Atividades 23 e 24 (RF20, RF21): faturamento liberado e garantia registrada
    login.entrar(financeiro.login, financeiro.senha)
    liberacao.abrir(projeto_id).liberar(date.today().isoformat(), "12")  # vai ao projeto
    liberacao.salvar_evidencia("20-faturamento-liberado")

    # Conferência final na página do projeto, aberta de novo, sem a mensagem do topo
    projeto.abrir(projeto_id)
    assert projeto.situacao() == "ENCERRADO"
    assert projeto.etapas_da_linha_do_tempo() == ETAPAS_ESPERADAS
    projeto.salvar_evidencia("21-projeto-encerrado-com-linha-do-tempo")
