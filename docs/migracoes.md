# Migrações do banco

[← Voltar ao README](../README.md)

O schema é versionado com o Flyway (`src/main/resources/db/migration`, `V<N>__nome_em_snake_case.sql`). O Hibernate roda com `ddl-auto: validate`: ele só **confere** que as entidades batem com o banco, nunca cria nem altera nada.

## Regras

- **Nunca edite uma migration já aplicada** (o Flyway guarda o checksum de cada uma e se recusa a subir se mudar). Corrigir é criar a próxima.
- **Uma mudança de schema = uma migration nova**, e a entidade JPA e o mapeador mudam no mesmo marco (o `validate` reprova se um dos lados ficar para trás).
- Dado que muda de significado entra na própria migration (ex.: o `ADMIN` virar `SUPER_ADMIN` na `V27`).
- Restrições que o domínio já exige (`CHECK`) também vão para o banco: a regra vale mesmo para quem escreve nele sem passar pela aplicação (`V27`: status e papel; `V29`: preço não negativo).

## Adicionar uma coluna obrigatória: *expand / contract*

Uma coluna `NOT NULL` não pode nascer assim numa tabela que já tem linhas. A técnica, em três passos:

1. **Expandir:** `ADD COLUMN` **sem** `NOT NULL`.
2. **Preencher:** um `UPDATE` com o melhor valor que se pode dar às linhas que já existem.
3. **Contrair:** `SET NOT NULL` (e o `CHECK`), agora que nenhuma linha está vazia.

A `V29` (preço congelado na reserva) é o exemplo:

```sql
ALTER TABLE booking ADD COLUMN price NUMERIC(12, 2);
UPDATE booking SET price = bookable.price FROM bookable WHERE bookable.id = booking.bookable_id;
ALTER TABLE booking ALTER COLUMN price SET NOT NULL, ADD CONSTRAINT ck_booking_price CHECK (price >= 0);
```

Aqui os três passos cabem numa migration só porque a tabela é pequena. **Numa tabela grande**, o `UPDATE` e o `SET NOT NULL` seguram bloqueios longos e o preenchimento deve ser feito em lotes, em migrations/deploys separados: primeiro a aplicação passa a escrever a coluna, depois se preenche o histórico em lotes, e só então se contrai.

## O que a V29 deixa de fora de propósito

As reservas que já existiam recebem o preço **atual** do voo: não há como saber o preço de quando foram feitas. Daí em diante, cada reserva guarda o seu.
