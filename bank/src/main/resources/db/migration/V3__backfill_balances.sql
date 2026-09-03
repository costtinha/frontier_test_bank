-- Invariante de domínio: todo usuário deve possuir um registro de saldo.
-- Usuários criados antes da tabela balances existir (V2) não têm saldo;
-- este backfill garante R$ 0,00 para todos, tornando o lado de leitura 100%
-- read-only (o GET nunca mais precisa criar saldo sob demanda).
INSERT INTO balances (id, user_id, amount, version, created_at, updated_at)
SELECT gen_random_uuid(), u.id, 0.00, 0, u.created_at, u.updated_at
FROM users u
WHERE NOT EXISTS (SELECT 1 FROM balances b WHERE b.user_id = u.id);
