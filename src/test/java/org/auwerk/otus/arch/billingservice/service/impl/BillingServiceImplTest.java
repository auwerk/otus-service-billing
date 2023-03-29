package org.auwerk.otus.arch.billingservice.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;

import org.auwerk.otus.arch.billingservice.dao.AccountDao;
import org.auwerk.otus.arch.billingservice.dao.OperationDao;
import org.auwerk.otus.arch.billingservice.domain.Account;
import org.auwerk.otus.arch.billingservice.domain.Operation;
import org.auwerk.otus.arch.billingservice.domain.OperationType;
import org.auwerk.otus.arch.billingservice.exception.AccountAlreadyExistsException;
import org.auwerk.otus.arch.billingservice.exception.AccountNotFoundException;
import org.auwerk.otus.arch.billingservice.service.BillingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.helpers.test.UniAssertSubscriber;
import io.vertx.mutiny.pgclient.PgPool;
import io.vertx.mutiny.sqlclient.SqlConnection;

public class BillingServiceImplTest {

        private static final String USERNAME = "user";

        private static final UUID ACCOUNT_ID = UUID.randomUUID();

        private final PgPool pool = mock(PgPool.class);
        private final AccountDao accountDao = mock(AccountDao.class);
        private final OperationDao operationDao = mock(OperationDao.class);
        private final SecurityIdentity securityIdentity = mock(SecurityIdentity.class);
        private final BillingService billingService = new BillingServiceImpl(pool, accountDao, operationDao,
                        securityIdentity);

    @BeforeEach
    void mockTransaction() {
        when(pool.withTransaction(any()))
        .then(inv -> {
            final Function<SqlConnection, Uni<Account>> f = inv.getArgument(0);
            return f.apply(null);
        });
    }

        @BeforeEach
        void mockUser() {
                var principal = mock(Principal.class);
                when(principal.getName()).thenReturn(USERNAME);
                when(securityIdentity.getPrincipal()).thenReturn(principal);
        }

    @Test
    void createUserAccount_success() {
        // when
        when(accountDao.findByUserName(pool, USERNAME))
                .thenReturn(Uni.createFrom().failure(new NoSuchElementException()));
        when(accountDao.insert(pool, USERNAME))
                .thenReturn(Uni.createFrom().item(ACCOUNT_ID));
        final var subscriber = billingService.createUserAccount(USERNAME).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertItem(ACCOUNT_ID);
    }

        @Test
        void createUserAccount_alreadyExists() {
                // given
                final var account = buildAccount();

                // when
                when(accountDao.findByUserName(pool, USERNAME))
                                .thenReturn(Uni.createFrom().item(account));
                final var subscriber = billingService.createUserAccount(USERNAME).subscribe()
                                .withSubscriber(UniAssertSubscriber.create());

                // then
                subscriber.assertFailedWith(AccountAlreadyExistsException.class);

                verify(accountDao, never()).insert(pool, USERNAME);
        }

        @Test
        void getUserAccount_success() {
                // given
                final var account = buildAccount();

                // when
                when(accountDao.findByUserName(pool, USERNAME))
                                .thenReturn(Uni.createFrom().item(account));
                final var subscriber = billingService.getUserAccount(false).subscribe()
                                .withSubscriber(UniAssertSubscriber.create());

                // then
                subscriber.assertItem(account);

                verify(operationDao, never()).findByAccountId(pool, ACCOUNT_ID);
        }

    @Test
    void getUserAccount_accountNotFound() {
        // when
        when(accountDao.findByUserName(pool, USERNAME))
                .thenReturn(Uni.createFrom().failure(new NoSuchElementException()));
        final var subscriber = billingService.getUserAccount(false).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertFailedWith(AccountNotFoundException.class);

        verify(operationDao, never()).findByAccountId(pool, ACCOUNT_ID);
    }

        @Test
        void getUserAccountWithOperations_success() {
                // given
                final var account = buildAccount();
                final var operations = List.of(
                                Operation.builder().build(),
                                Operation.builder().build(),
                                Operation.builder().build());

                // when
                when(accountDao.findByUserName(pool, USERNAME))
                                .thenReturn(Uni.createFrom().item(account));
                when(operationDao.findByAccountId(pool, ACCOUNT_ID))
                                .thenReturn(Uni.createFrom().item(operations));
                final var subscriber = billingService.getUserAccount(true).subscribe()
                                .withSubscriber(UniAssertSubscriber.create());

                // then
                subscriber.assertItem(account);

                verify(operationDao, times(1)).findByAccountId(pool, ACCOUNT_ID);
        }

    @Test
    void getUserAccountWithOperations_accountNotFound() {
        // when
        when(accountDao.findByUserName(pool, USERNAME))
                .thenReturn(Uni.createFrom().failure(new NoSuchElementException()));
        final var subscriber = billingService.getUserAccount(true).subscribe()
                .withSubscriber(UniAssertSubscriber.create());

        // then
        subscriber.assertFailedWith(AccountNotFoundException.class);
    }

        @ParameterizedTest
        @EnumSource(OperationType.class)
        void executeOperation_success(OperationType operationType) {
                // given
                final var account = buildAccount();
                final var amount = BigDecimal.TEN;
                final var comment = "test operation";
                final var targetBalance = switch (operationType) {
                        case WITHDRAW -> account.getBalance().subtract(amount);
                        case CANCEL_WITHDRAW -> account.getBalance().add(amount);
                };

                // when
                when(accountDao.findByUserName(pool, USERNAME))
                                .thenReturn(Uni.createFrom().item(account));
                final var subscriber = billingService.executeOperation(operationType, amount, comment)
                                .subscribe()
                                .withSubscriber(UniAssertSubscriber.create());

                // then
                subscriber.assertCompleted();

                verify(accountDao, times(1))
                                .updateBalanceById(pool, account.getId(), targetBalance);
                verify(operationDao, times(1))
                                .insert(eq(pool), argThat(op -> operationType.equals(op.getType())
                                                && account.getId().equals(op.getAccountId())
                                                && amount.equals(op.getAmount()) && comment.equals(op.getComment())));
        }

        @ParameterizedTest
        @EnumSource(OperationType.class)
        void executeOperation_accountNotFound(OperationType operationType) {
                // given
                final var account = buildAccount();
                final var amount = BigDecimal.TEN;

                // when
                when(accountDao.findByUserName(pool, USERNAME))
                                .thenReturn(Uni.createFrom().failure(new NoSuchElementException()));
                final var subscriber = billingService.executeOperation(operationType, amount, "")
                                .subscribe()
                                .withSubscriber(UniAssertSubscriber.create());

                // then
                subscriber.assertFailedWith(AccountNotFoundException.class);

                verify(accountDao, never())
                                .updateBalanceById(eq(pool), eq(account.getId()), any(BigDecimal.class));
                verify(operationDao, never())
                                .insert(eq(pool), any(Operation.class));
        }

        private static Account buildAccount() {
                return Account.builder()
                                .id(ACCOUNT_ID)
                                .balance(BigDecimal.TEN)
                                .build();
        }
}
