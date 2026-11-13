# Sistema de Gestão de Projetos de Irrigação (TCD ESOF3 - IFTM)

## Fonte da verdade
- docs/TCD.pdf: requisitos (RF01-RF23, RNF01-RNF10), BPMN TO-BE (atividades 1-24, gateways G1-G8) e descrição dos diagramas UML.
- docs/diagramas/: imagens dos diagramas. Antes de criar entidade, service ou tela, confira o diagrama correspondente.
- Se o código precisar divergir de um diagrama, pare e me pergunte antes de implementar.

## Projeto de referência
- https://github.com/PedroHLSs/webIIspringboot (Spring Boot + Spring Data JPA/Hibernate + Thymeleaf + Spring Security).
- Siga as convenções dele: entidades com anotações jakarta.persistence, construtor vazio e construtor completo,
  getters e setters explícitos; um JpaRepository por entidade; service como interface + classe Impl com @Service;
  controllers com injeção pelo construtor; fragments Thymeleaf de head, header e footer; Bootstrap 5.3 e
  Font Awesome via CDN; testes com H2 no perfil test, MockMvc e Mockito.
- Não copie dele: senha escrita no application.properties, MySQL, RuntimeException genérica, exclusão por GET.

## Stack (não trocar)
- Java 21, Maven, Spring Boot 3.5.x (web, thymeleaf, data-jpa, security, validation).
- PostgreSQL com Hibernate via Spring Data JPA. H2 só nos testes.
- Proibido: Flyway, Liquibase, Lombok, JdbcTemplate/JdbcClient.
- Front: Thymeleaf + HTML, CSS e JavaScript puro + Bootstrap 5.3 via CDN. Sem jQuery, React, Angular ou Vue.

## Banco e JPA
- Tabelas criadas pelo Hibernate (spring.jpa.hibernate.ddl-auto=update). Sem schema.sql e sem data.sql.
- Nome de tabela e de coluna em snake_case, sem acento, sempre explícito em @Table e @Column.
- Id Long com @GeneratedValue(strategy = GenerationType.IDENTITY).
- @ManyToOne(fetch = FetchType.LAZY) em toda associação. Lista que vai para tela usa JOIN FETCH ou
  @EntityGraph para não cair em N+1.
- Enum com @Enumerated(EnumType.STRING).
- Dinheiro em BigDecimal com @Column(precision = 14, scale = 2). Datas em LocalDate e LocalDateTime.
- Imagem (foto, assinatura) em byte[] sem @Lob, com @Column(length = 6_000_000): vira bytea no PostgreSQL
  e funciona no H2.
- Carga inicial em ApplicationRunner, idempotente (só insere o que não existe).
- Consulta por método derivado ou @Query em JPQL. SQL nativo só se JPQL não resolver, com parâmetros nomeados.

## Pacotes (seguem o diagrama de pacotes do TCD)
br.edu.iftm.irrigacao
  apresentacao.web     controllers e objetos de formulário
  dominio.projeto      Projeto, Levantamento, Dimensionamento, EstadoProjeto, HistoricoProjeto e services
  dominio.comercial    Produtor, Propriedade, Orcamento, Proposta e services
  dominio.estoque      Material, ItemProjeto, MovimentacaoEstoque, Cotacao e services
  dominio.execucao     OrdemServico, Apontamento, Medicao, Aceite, Pendencia e services
  dominio.acesso       Usuario, Perfil, segurança e services
  persistencia         interfaces JpaRepository
  config               SecurityConfig, AppConfig, CargaInicial
A interface do service fica no pacote do domínio e a implementação no subpacote impl
(ex.: dominio.estoque.EstoqueService e dominio.estoque.impl.EstoqueServiceImpl).
Regra de negócio fica no service. Controller só recebe, chama service e devolve a tela. Repositório só consulta.

## Regras que não podem quebrar
- Toda mudança de estado do Projeto passa por ProjetoService.transitar(...), que valida a transição e grava
  HistoricoProjeto na mesma transação.
- Movimentação de estoque é atômica e nunca deixa saldo negativo: @Modifying @Query com UPDATE condicional,
  conferir as linhas afetadas, tudo em @Transactional (RNF05). Material tem @Check do Hibernate para o saldo.
- Entidades auditadas estendem EntidadeAuditada (@MappedSuperclass com @CreatedBy, @CreatedDate,
  @LastModifiedBy, @LastModifiedDate e @EntityListeners(AuditingEntityListener.class)) (RNF03).
- O sistema não calcula dimensionamento hidráulico. Só registra o que o projetista informa.
- Data e hora sempre via bean java.time.Clock injetado, para os testes controlarem o tempo.
- Erro de negócio com exceção própria (RegraNegocioException e filhas) tratada em @ControllerAdvice.
- Ação que altera dado só por POST, com CSRF.

## Interface
- Textos em português do Brasil.
- Telas de campo (levantamento, apontamento, medição, aceite) pensadas para celular: uma coluna, poucos
  campos, botões grandes (RNF06).
- Toda página usa os fragments de templates/fragments/, com menu de acordo com o perfil.
- Erro de validação aparece ao lado do campo e mensagem de sucesso no topo da página.

## Forma de trabalhar
- Uma etapa por vez. Não implemente nada que não esteja no prompt da etapa.
- Ao terminar: mvn -q verify passando, resumo do que foi feito e do que ficou pendente, commit "etapa N: descrição".
- Cite o RF ou RNF no Javadoc da classe ou do método que o implementa.