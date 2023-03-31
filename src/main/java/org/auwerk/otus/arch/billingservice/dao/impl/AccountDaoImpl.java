package org.auwerk.otus.arch.billingservice.dao.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.billingservice.dao.AccountDao;
import org.auwerk.otus.arch.billingservice.domain.Account;
import org.auwerk.otus.arch.billingservice.exception.DaoException;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;

@ApplicationScoped
public class AccountDaoImpl implements AccountDao {

    @Override
    public Uni<Account> findById(PgPool pool, UUID id) {
        return pool.preparedQuery("SELECT * FROM accounts WHERE id=$1")
                .execute(Tuple.of(id))
                .map(rowSet -> {
                    final var rowSetIterator = rowSet.iterator();
                    if (!rowSetIterator.hasNext()) {
                        throw new NoSuchElementException("account not found, id=" + id);
                    }
                    return mapRow(rowSetIterator.next());
                });
    }

    @Override
    public Uni<Account> findByUserName(PgPool pool, String userName) {
        return pool.preparedQuery("SELECT * FROM accounts WHERE username=$1")
                .execute(Tuple.of(userName))
                .map(rowSet -> {
                    final var rowSetIterator = rowSet.iterator();
                    if (!rowSetIterator.hasNext()) {
                        throw new NoSuchElementException("account not found, username=" + userName);
                    }
                    return mapRow(rowSetIterator.next());
                });
    }

    @Override
    public Uni<UUID> insert(PgPool pool, String userName) {
        return pool
                .preparedQuery(
                        "INSERT INTO accounts(id, username, balance, created_at) VALUES($1, $2, $3, $4) RETURNING id")
                .execute(Tuple.of(UUID.randomUUID(), userName, BigDecimal.ZERO, LocalDateTime.now()))
                .map(rowSet -> {
                    if (rowSet.rowCount() != 1) {
                        throw new DaoException("account insertion failed, username=" + userName);
                    }
                    return rowSet.iterator().next().getUUID("id");
                });
    }

    @Override
    public Uni<Void> updateBalanceById(PgPool pool, UUID id, BigDecimal balance) {
        return pool.preparedQuery("UPDATE accounts SET balance=$1 WHERE id=$2")
                .execute(Tuple.of(balance, id))
                .invoke(rowSet -> {
                    if (rowSet.rowCount() != 1) {
                        throw new DaoException("account balance update failed, id=" + id);
                    }
                })
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> deleteById(PgPool pool, UUID id) {
        return pool.preparedQuery("DELETE FROM accounts WHERE id=$1")
                .execute(Tuple.of(id))
                .invoke(rowSet -> {
                    if (rowSet.rowCount() != 1) {
                        throw new DaoException("account deletion failed, id=" + id);
                    }
                })
                .replaceWithVoid();
    }

    private static Account mapRow(Row row) {
        return Account.builder()
                .id(row.getUUID("id"))
                .userName(row.getString("username"))
                .balance(row.getBigDecimal("balance"))
                .createdAt(row.getLocalDateTime("created_at"))
                .build();
    }
}
