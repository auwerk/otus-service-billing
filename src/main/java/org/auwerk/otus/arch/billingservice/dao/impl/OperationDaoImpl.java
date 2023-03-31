package org.auwerk.otus.arch.billingservice.dao.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.billingservice.dao.OperationDao;
import org.auwerk.otus.arch.billingservice.domain.Operation;
import org.auwerk.otus.arch.billingservice.domain.OperationType;
import org.auwerk.otus.arch.billingservice.exception.DaoException;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

@ApplicationScoped
public class OperationDaoImpl implements OperationDao {

    @Override
    public Uni<Operation> findById(PgPool pool, UUID id) {
        return pool.preparedQuery("SELECT * FROM operations WHERE id=$1")
                .execute(Tuple.of(id))
                .map(rowSet -> {
                    final var rowSetIterator = rowSet.iterator();
                    if (!rowSetIterator.hasNext()) {
                        throw new NoSuchElementException("operation not found, id=" + id);
                    }
                    return mapRow(rowSetIterator.next());
                });
    }

    @Override
    public Uni<Long> countByRelatedTo(PgPool pool, UUID relatedTo) {
        return pool.preparedQuery("SELECT COUNT(*) FROM operations WHERE related_to=$1")
                .execute(Tuple.of(relatedTo))
                .map(rowSet -> {
                    if (rowSet.rowCount() != 1) {
                        throw new DaoException("counting realted operations failed");
                    }
                    return rowSet.iterator().next().getLong(0);
                });
    }

    @Override
    public Uni<List<Operation>> findByAccountId(PgPool pool, UUID accountId) {
        return pool.preparedQuery("SELECT * FROM operations WHERE account_id=$1")
                .execute(Tuple.of(accountId))
                .map(rowSet -> {
                    final var result = new ArrayList<Operation>(rowSet.rowCount());
                    final var rowSetIterator = rowSet.iterator();
                    while (rowSetIterator.hasNext()) {
                        result.add(mapRow(rowSetIterator.next()));
                    }
                    return result;
                });
    }

    @Override
    public Uni<UUID> insert(PgPool pool, Operation operation) {
        Object[] parameters = { UUID.randomUUID(), operation.getAccountId(), operation.getRelatedTo(),
                operation.getType().name(), operation.getAmount(), operation.getComment(), LocalDateTime.now() };

        return pool.preparedQuery(
                "INSERT INTO operations(id, account_id, related_to, type, amount, comment, created_at) "
                        + "VALUES($1, $2, $3, $4, $5, $6, $7) RETURNING id")
                .execute(Tuple.tuple(Arrays.asList(parameters)))
                .map(rowSet -> {
                    final var rowSetIterator = rowSet.iterator();
                    if (!rowSetIterator.hasNext()) {
                        throw new DaoException("operation insertion failed");
                    }
                    return rowSetIterator.next().getUUID("id");
                });
    }

    @Override
    public Uni<Void> deleteByAccountId(PgPool pool, UUID accountId) {
        return pool.preparedQuery("DELETE FROM operations WHERE account_id=$1")
                .execute(Tuple.of(accountId))
                .replaceWithVoid();
    }

    private static Operation mapRow(Row row) {
        return Operation.builder()
                .id(row.getUUID("id"))
                .accountId(row.getUUID("account_id"))
                .relatedTo(row.getUUID("related_to"))
                .type(OperationType.valueOf(row.getString("type")))
                .amount(row.getBigDecimal("amount"))
                .comment(row.getString("comment"))
                .createdAt(row.getLocalDateTime("created_at"))
                .build();
    }
}
