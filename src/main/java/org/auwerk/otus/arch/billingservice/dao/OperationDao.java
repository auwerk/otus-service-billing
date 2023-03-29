package org.auwerk.otus.arch.billingservice.dao;

import java.util.List;
import java.util.UUID;

import org.auwerk.otus.arch.billingservice.domain.Operation;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;

public interface OperationDao {

    Uni<Operation> findById(PgPool pool, UUID id);

    Uni<List<Operation>> findByAccountId(PgPool pool, UUID accountId);

    Uni<UUID> insert(PgPool pool, Operation operation);
}
