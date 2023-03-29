package org.auwerk.otus.arch.billingservice.service.impl;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
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
                .flatMap(account -> {
                    final var targetBalance = doCalculations(account.getBalance(), type, amount);
                    if (targetBalance.compareTo(BigDecimal.ZERO) < 0) {
                        throw new InsufficentAccountBalanceException();
                    }
                    final var operation = Operation.builder()
                            .type(type)
                            .accountId(account.getId())
                            .amount(amount)
                            .comment(comment)
                            .build();
                    return accountDao
                            .updateBalanceById(pool, account.getId(), targetBalance)
                            .chain(() -> operationDao.insert(pool, operation));
                }));
    }

    private BigDecimal doCalculations(BigDecimal balance, OperationType operationType, BigDecimal amount) {
        switch (operationType) {
            case WITHDRAW:
            case CANCEL_CREDIT:
                return balance.subtract(amount);
            case CREDIT:
            case CANCEL_WITHDRAW:
                return balance.add(amount);
            default:
                return balance;
        }
    }

    private String getUserName() {
        return securityIdentity.getPrincipal().getName();
    }
}
