# Sistema de gestão de projetos de irrigação

Trabalho de Conclusão de Disciplina de Engenharia de Software III (IFTM). Aplicação web em Java 21 com Spring Boot 3.5, Spring Data JPA, Thymeleaf e PostgreSQL.

## Como rodar

É preciso ter Java 21, Maven e um PostgreSQL com um banco chamado `irrigacao`. Quem não tem o PostgreSQL instalado pode subir um pelo Docker:

```
docker compose up -d
```

A conexão usa `localhost:5432`, usuário `postgres`. Para outro endereço, usuário ou senha, defina as variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD` antes de subir.

```
mvn spring-boot:run
```

A aplicação abre em http://localhost:8080. Na primeira subida o Hibernate cria as 18 tabelas e a carga inicial cadastra 22 materiais de irrigação. Nas subidas seguintes a carga só insere o material cujo código ainda não existe.

## Login e usuários

Todas as páginas exigem login. Não existe auto-registro: quem cria os usuários é o administrador, em Usuários (`/admin/usuarios`).

Na subida, se ainda não houver nenhum usuário com perfil ADMINISTRADOR, a carga inicial cria o login `admin` com a senha da variável de ambiente `IRRIGACAO_ADMIN_SENHA` (no mínimo 8 caracteres) e avisa no log. Sem a variável o usuário não é criado. Só o perfil `dev` tem senha padrão, `admin123`:

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Troque a senha do `admin` em Usuários depois do primeiro acesso. O perfil do usuário define o menu e as áreas que ele acessa; a troca de perfil e a desativação valem a partir do próximo login.

## Testes

```
mvn verify
```

Os testes usam o H2 em memória (perfil `test`). Não dependem do PostgreSQL nem do Docker.

## Quando zerar o banco de desenvolvimento

As tabelas são criadas pelo Hibernate com `ddl-auto=update`. Esse modo só acrescenta: cria tabela nova e coluna nova, mas não altera nem apaga o que já existe. Por isso, rode o `scripts/reset-dev.sql` sempre que mudar numa entidade:

- o tipo de uma coluna (por exemplo, de `Double` para `BigDecimal`, ou o tamanho de um `String`);
- o nome de uma coluna ou de uma tabela (a antiga fica no banco, e se for `NOT NULL` as inclusões passam a falhar);
- uma restrição: `nullable`, unicidade, o `@Check` de saldo do material ou os valores de um enum. O Hibernate grava no PostgreSQL uma restrição com a lista de valores de cada enum, então um valor novo é recusado pelo banco até a tabela ser recriada.

Coluna nova que aceita nulo e tabela nova não precisam de reset.

Com a aplicação parada:

```
psql -h localhost -U postgres -d irrigacao -f scripts/reset-dev.sql
```

O script apaga todas as tabelas e todos os dados (`DROP SCHEMA public CASCADE`). Na subida seguinte as tabelas e os materiais da carga inicial voltam, e o resto do que estava cadastrado se perde. Ele é só para o banco de desenvolvimento.

## Parâmetros de negócio

Ficam em `src/main/resources/application.properties`:

| Propriedade | Para que serve |
|---|---|
| `irrigacao.proposta.validade-dias` | validade padrão da proposta, em dias |
| `irrigacao.proposta.dias-alerta` | quantos dias antes do vencimento a proposta entra em alerta |
| `irrigacao.proposta.verificacao-cron` | quando as propostas enviadas que passaram da validade são vencidas (padrão: todo dia às 6h; `-` desliga) |
| `irrigacao.alcada-desconto.vendedor` | desconto máximo, em %, que o vendedor dá sem a diretoria |
| `irrigacao.mao-obra.preco-hora` | preço de venda da hora de mão de obra, em R$ |
| `irrigacao.mao-obra.custo-hora` | custo da hora de mão de obra, em R$ |
| `irrigacao.upload.dir` | pasta das fotos enviadas (fica fora do Git) |
