package org.auwerk.otus.arch.billingservice.dao.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
        return pool.preparedQuery(
                "INSERT INTO operations(id, account_id, related_to, type, amount, comment, created_at) VALUES($1, $2, $3, $4, $5, $6, $7) RETURNING id")
                .execute(Tuple.of(List.of(UUID.randomUUID(), operation.getAccountId(), operation.getRelatedTo(),
                        operation.getType().name(), operation.getAmount(), operation.getComment(),
                        LocalDateTime.now())))
                .map(rowSet -> {
                    final var rowSetIterator = rowSet.iterator();
                    if (!rowSetIterator.hasNext()) {
                        throw new DaoException("operation insertion failed");
                    }
                    return rowSetIterator.next().getUUID("id");
                });
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
