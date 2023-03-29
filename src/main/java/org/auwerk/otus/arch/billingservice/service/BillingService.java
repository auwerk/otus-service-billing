package org.auwerk.otus.arch.billingservice.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.auwerk.otus.arch.billingservice.domain.Account;
import org.auwerk.otus.arch.billingservice.domain.OperationType;

import io.smallrye.mutiny.Uni;

public interface BillingService {

    /**
     * Создание лицевого счёта для авторизованного пользователя
     * 
     * @return уникальный идентификатор созданного счёта
     */
    Uni<UUID> createUserAccount();

    /**
     * Получение информации о счёте авторизованного пользователя
     * 
     * @param fetchOperations требуется ли выбрать из БД историю операций по счёту
     * @return счёт
     */
    Uni<Account> getUserAccount(boolean fetchOperations);

    /**
     * Исполнение операции со счётом авторизованного пользователя
     * 
     * @param type   тип операции
     * @param amount количество
     * @return уникальный идентификатор операции
     */
    Uni<UUID> executeOperation(OperationType type, BigDecimal amount);
}
