# Arquitetura — híbrida (Core × Periféricos)

Este documento descreve a fronteira arquitetural do banco digital: **o que é core
(toca o livro-razão) e o que é periférico (suporte)**, e por que essa divisão — e
não "microsserviços por entidade" — é o eixo que permite escalar com segurança.

## 1. O critério do corte

A divisão não é por tipo de serviço nem por entidade: é pela **natureza das
invariantes**.

| Critério | **Core** (dinheiro) | **Periférico** (suporte) |
|---|---|---|
| Consistência | Transação única, `SELECT ... FOR UPDATE`, serialização por conta | Eventual, tolerante a atraso |
| Escala | Particionamento **por conta** (sharding) e verticalização | Horizontal sem limite (read-mostly) |
| Falha | Não depende de ninguém para fechar a operação | Não pode bloquear o core |
| Fonte da verdade | Livro-razão append-only + trilha WORM | Read models derivados (reconstruíveis) |
| Auditoria | Imutabilidade, retenção, leitura adversarial | Métricas, dashboards, notificações |

## 2. Core — ACID e escrita única

| Componente | Pacote | Papel |
|---|---|---|
| Contas e saldos | `balance` (+ `balance.command`) | Débito, crédito, lock pessimista |
| Livro-razão | `ledger` | Entradas append-only, saldo antes/depois |
| Transferências | `transfer` (+ `transfer.command`) | **Uma transação** para as duas pontas |
| Clientes | `user` (+ `user.command`) | Cadastro, unicidade, senha |
| Outbox + auditoria | `common.event`, `audit` | Gravados **na mesma transação** do estado |

Regras do core:

1. **Operação que toca o razão roda numa única transação.** Não existe estado
   intermediário de dinheiro em trânsito dentro do core.
2. **Lock determinístico**: transferência trava as duas contas sempre na mesma
   ordem (por id de conta), o que elimina deadlock entre transferências cruzadas.
3. **Todo efeito é contábil**: compensação não apaga lançamento — cria um novo
   (`TRANSFER_REFUND`). O razão nunca perde informação.
4. **Idempotência por chave de cliente** (`Idempotency-Key` única no banco).

## 3. Periféricos — leitura, suporte e reação

| Componente | Pacote | Papel |
|---|---|---|
| Read models | `*.query`, `*.projection` | Saldo projetado, extrato, listagens |
| Projeção de eventos | `common.projection` | Projetor idempotente, checkpoint, DLQ, replay |
| Operação | `*.admin`, `common.projection.admin`, `saga.admin` | Status, replay, retry |
| Observabilidade | `common.observability` | Métricas, backlog, intervenção pendente |

Regras:

1. **Periférico nunca escreve no core** — só lê (read models) e consome eventos.
2. **Periférico não bloqueia o core**: projeção atrasada é visível nas métricas,
   mas não impede um depósito ou uma transferência.
3. **Read models são descartáveis**: podem ser reconstruídos do log de eventos
   (replay) porque a fonte da verdade é o razão.

## 4. Fronteira verificada por teste

A separação não é convenção de documentação: é **verificada no build** por
`CorePeripheralBoundaryTest` (ArchUnit):

- core não depende de `..query..`, `..projection..` nem `..dto..`;
- lado de leitura não depende de `..command..`;
- periférico não usa os repositórios de escrita (`UserRepository`,
  `BalanceRepository`, `LedgerRepository`, `TransferRepository`);
- contratos de evento (`common.event..`) não conhecem domínio.

## 5. Saga: onde usar e onde **não** usar

> **Saga não substitui transação — saga coordena o que não cabe numa transação.**

- **Não usar no core.** Duas contas do mesmo banco se resolvem com uma transação
  ACID; uma saga ali só introduziria janela de inconsistência e compensação onde
  não é necessária.
- **Usar nas fronteiras entre contextos**: onboarding com KYC externo,
  liquidação com câmara/PIX interbancário, cartão (autorização → captura),
  notificação por provedor externo.
- **Padrão para dinheiro que cruza a fronteira**: **conta de trânsito
  (suspense)** — na mesma transação debita o cliente e credita a conta contábil
  de trânsito. O dinheiro nunca "desaparece"; a liquidação fecha a conta depois.
  Compensação (estorno) continua existindo, mas como exceção.

A infraestrutura de saga (`saga/`) está no repositório e testada, e a variante de
transferência por saga (`transfer.saga`) existe para quando as contas estiverem
em serviços separados. **Ela não é usada no caminho ativo de propósito.**

## 6. Roadmap

| Fase | Entrega |
|---|---|
| **A (feita)** | Transferência de volta ao ACID no core (transação única + lock ordenado) |
| **B (feita)** | Fronteira Core/Periférico verificada por ArchUnit |
| **C** | Reserva (saldo bloqueado) + conta de trânsito, quando existir liquidação externa |
| **D** | Separação física: core com app/banco próprios, periféricos via Kafka, sharding por conta |
| **E** | Sagas nas fronteiras (onboarding, liquidação, cartão) usando a máquina já pronta |
