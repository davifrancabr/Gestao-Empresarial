-- Lojas iniciais de demonstração. Usuários e catálogo de exemplo são criados
-- pelo DemoDataSeeder, que utiliza o mesmo algoritmo BCrypt da aplicação.
insert into store (id, name, document, active, created_at, updated_at) values
    ('11111111-1111-4111-8111-111111111111', 'Loja 1 - Matriz', '12.345.678/0001-10', true, now(), now()),
    ('22222222-2222-4222-8222-222222222222', 'Loja 2 - Filial Centro', '12.345.678/0002-00', true, now(), now());
