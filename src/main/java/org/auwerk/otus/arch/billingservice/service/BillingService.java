package org.auwerk.otus.arch.billingservice.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.auwerk.otus.arch.billingservice.domain.Account;
import org.auwerk.otus.arch.billingservice.domain.OperationType;

import io.smallrye.mutiny.Uni;

public interface BillingService {

    /**
     * Создание лицевого счёта для пользователя
     * 
     * @param userName имя пользователя, для которого создается счёт
     * @return уникальный идентификатор созданного счёта
     */
    Uni<UUID> createUserAccount(String userName);

    /**
     * Удаление лицевого счёта авторизованного пользователя
     * 
     * @return
     */
    Uni<Void> deleteUserAccount();

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
     * @param type    тип операции
     * @param amount  количество
     * @param comment комментарий к операции
     * @return уникальный идентификатор операции
     */
    Uni<UUID> executeOperation(OperationType type, BigDecimal amount, String comment);

    /**
     * Отмена операции
     * 
     * @param operationId уникальный идентификатор операции
     * @param comment     комментарий к отмене операции
     * @return уникальный идентификатор зеркальной операции
     */
    Uni<UUID> cancelOperation(UUID operationId, String comment);
}
