# Sistema de Gestão de Projetos de Irrigação

TCD de Engenharia de Software III (IFTM). Java 21, Spring Boot 3.5, Spring Data JPA, Thymeleaf,
Spring Security e PostgreSQL.

## Requisitos

- JDK 21
- Maven 3.9 ou mais novo
- PostgreSQL 16 instalado, ou Docker para subir o banco pelo `docker-compose.yml`

## Subir o banco

```
docker compose up -d
```

Cria o banco `irrigacao` em `localhost:5432`, com usuário e senha `postgres`.

Para usar outro PostgreSQL, defina as variáveis de ambiente `DB_URL`, `DB_USER` e `DB_PASSWORD`
antes de rodar a aplicação.

## Rodar

```
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

A aplicação abre em http://localhost:8080. As tabelas são criadas pelo Hibernate na primeira execução.

## Testes

```
mvn verify
```

Os testes usam H2 em memória (perfil `test`) e não dependem do Docker nem do PostgreSQL.
