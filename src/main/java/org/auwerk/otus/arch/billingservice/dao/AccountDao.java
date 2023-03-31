package org.auwerk.otus.arch.billingservice.dao;

import java.math.BigDecimal;
import java.util.UUID;

import org.auwerk.otus.arch.billingservice.domain.Account;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

public interface AccountDao {

    Uni<Account> findById(PgPool pool, UUID id);
    
    Uni<Account> findByUserName(PgPool pool, String userName);

    Uni<UUID> insert(PgPool pool, String userName);

    Uni<Void> updateBalanceById(PgPool pool, UUID id, BigDecimal balance);

    Uni<Void> deleteById(PgPool pool, UUID id);
}
