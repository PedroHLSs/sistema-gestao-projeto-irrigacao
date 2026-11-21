from paginas.base_page import BasePage, por_texto_do_botao, por_texto_do_link


class ProdutorPage(BasePage):
    """Produtor e propriedade (RF01), do comercial."""

    def cadastrar(self, nome, cpf_cnpj, telefone="", email=""):
        """@return id do produtor criado"""
        self.ir_para("/comercial/produtores/novo")
        self.preencher_id("nome", nome)
        self.preencher_id("cpfCnpj", cpf_cnpj)
        if telefone:
            self.preencher_id("telefone", telefone)
        if email:
            self.preencher_id("email", email)
        self.enviar(por_texto_do_botao("Cadastrar produtor"))
        self.resultado()
        return self.id_no_fim_do_caminho("produtores")

    def cadastrar_propriedade(self, nome, municipio, area_total_ha=None):
        """Parte da página do produtor, que fica aberta depois do cadastro."""
        self.enviar(por_texto_do_link("Nova propriedade"))
        self.preencher_id("nome", nome)
        self.preencher_id("municipio", municipio)
        if area_total_ha is not None:
            self.preencher_id("areaTotalHa", area_total_ha)
        self.enviar(por_texto_do_botao("Cadastrar propriedade"))
        return self.resultado()

    def abrir_projeto_da_propriedade(self):
        """Link "Abrir projeto" da propriedade (o produtor novo tem uma só): leva ao formulário
        de abertura já com o produtor e a propriedade escolhidos."""
        self.enviar(por_texto_do_link("Abrir projeto"))
        return self
