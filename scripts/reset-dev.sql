-- Apaga TODAS as tabelas e TODOS os dados do banco de desenvolvimento.
-- Na próxima subida da aplicação o Hibernate recria as tabelas e a carga inicial
-- insere os materiais de novo.
--
-- Use sempre que mudar tipo, nome ou restrição de uma coluna numa entidade. O
-- ddl-auto=update só acrescenta tabela e coluna nova; ele não altera nem remove
-- o que já existe.
--
-- Como rodar (com a aplicação parada):
--   psql -h localhost -U postgres -d irrigacao -f scripts/reset-dev.sql
--
-- Nunca rode em um banco que não seja o de desenvolvimento.

DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
