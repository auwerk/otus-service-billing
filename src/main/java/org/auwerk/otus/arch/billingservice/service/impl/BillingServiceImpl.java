package org.auwerk.otus.arch.billingservice.service.impl;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;

import org.auwerk.otus.arch.billingservice.dao.AccountDao;
import org.auwerk.otus.arch.billingservice.dao.OperationDao;
import org.auwerk.otus.arch.billingservice.domain.Account;
import org.auwerk.otus.arch.billingservice.domain.Operation;
import org.auwerk.otus.arch.billingservice.domain.OperationType;
import org.auwerk.otus.arch.billingservice.exception.AccountAlreadyExistsException;
import org.auwerk.otus.arch.billingservice.exception.AccountNotFoundException;
import org.auwerk.otus.arch.billingservice.exception.InsufficentAccountBalanceException;
import org.auwerk.otus.arch.billingservice.exception.OperationAlreadyCanceledException;
import org.auwerk.otus.arch.billingservice.exception.OperationExecutedByDifferentUserException;
import org.auwerk.otus.arch.billingservice.exception.OperationNotFoundException;
import org.auwerk.otus.arch.billingservice.service.BillingService;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final PgPool pool;
    private final AccountDao accountDao;
    private final OperationDao operationDao;
    private final SecurityIdentity securityIdentity;

    @Override
    public Uni<UUID> createUserAccount(String userName) {
        return accountDao.findByUserName(pool, userName)
                .onItemOrFailure()
                .transformToUni((account, failure) -> {
                    if (account != null) {
                        throw new AccountAlreadyExistsException();
                    }
                    return accountDao.insert(pool, userName);
                });
    }

    @Override
    public Uni<Account> getUserAccount(boolean fetchOperations) {
        if (fetchOperations) {
            return accountDao.findByUserName(pool, getUserName())
                    .call(account -> operationDao.findByAccountId(pool, account.getId())
                            .invoke(operations -> account.setOperations(operations)))
                    .onFailure(NoSuchElementException.class)
                    .transform(ex -> new AccountNotFoundException());
        } else {
            return accountDao.findByUserName(pool, getUserName())
                    .onFailure(NoSuchElementException.class)
                    .transform(ex -> new AccountNotFoundException());
        }
    }

    @Override
    public Uni<UUID> executeOperation(OperationType type, BigDecimal amount, String comment) {
        return pool.withTransaction(conn -> accountDao.findByUserName(pool, getUserName())
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new AccountNotFoundException())
                .flatMap(account -> doExecuteOperation(account, Optional.empty(), type, amount, comment)));
    }

    @Override
    public Uni<UUID> cancelOperation(UUID operationId, String comment) {
        return pool.withTransaction(conn -> operationDao.findById(pool, operationId)
                .invoke(operation -> {
                    if (operation.getRelatedTo() != null) {
                        throw new OperationAlreadyCanceledException(operation.getId());
                    }
                })
                .flatMap(operation -> accountDao.findById(pool, operation.getAccountId())
                        .flatMap(account -> {
                            if (!getUserName().equals(account.getUserName())) {
                                throw new OperationExecutedByDifferentUserException(operation.getId());
                            }
                            return switch (operation.getType()) {
                                case CREDIT -> doExecuteOperation(account, Optional.of(operation),
                                        OperationType.WITHDRAW, operation.getAmount(), comment);
                                case WITHDRAW ->
                                    doExecuteOperation(account, Optional.of(operation), OperationType.CREDIT,
                                            operation.getAmount(), comment);
                            };
                        })
                        .onFailure(NoSuchElementException.class)
                        .transform(ex -> new AccountNotFoundException(operation.getAccountId())))
                .onFailure(NoSuchElementException.class)
                .transform(ex -> new OperationNotFoundException(operationId)));
    }

    private Uni<UUID> doExecuteOperation(Account account, Optional<Operation> relatedTo, OperationType type,
            BigDecimal amount, String comment) {
        final var targetBalance = doCalculations(account.getBalance(), type, amount);
        if (targetBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficentAccountBalanceException();
        }
        final var operation = Operation.builder()
                .type(type)
                .accountId(account.getId())
                .relatedTo(relatedTo.map(op -> op.getId()).orElse(null))
                .amount(amount)
                .comment(comment)
                .build();
        return accountDao
                .updateBalanceById(pool, account.getId(), targetBalance)
                .chain(() -> operationDao.insert(pool, operation));
    }

    private BigDecimal doCalculations(BigDecimal balance, OperationType operationType, BigDecimal amount) {
        switch (operationType) {
            case WITHDRAW:
                return balance.subtract(amount);
            case CREDIT:
                return balance.add(amount);
            default:
                return balance;
        }
    }

    private String getUserName() {
        return securityIdentity.getPrincipal().getName();
    }
}
