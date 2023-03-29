package org.auwerk.otus.arch.billingservice.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.math.BigDecimal;
import java.util.UUID;

import org.auwerk.otus.arch.billingservice.api.dto.CancelOperationRequestDto;
import org.auwerk.otus.arch.billingservice.api.dto.ExecuteOperationRequestDto;
import org.auwerk.otus.arch.billingservice.domain.OperationType;
import org.auwerk.otus.arch.billingservice.exception.AccountNotFoundException;
import org.auwerk.otus.arch.billingservice.exception.InsufficentAccountBalanceException;
import org.auwerk.otus.arch.billingservice.exception.OperationExecutedByDifferentUserException;
import org.auwerk.otus.arch.billingservice.exception.OperationNotFoundException;
import org.auwerk.otus.arch.billingservice.service.BillingService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@TestHTTPEndpoint(OperationResource.class)
public class OperationResourceTest extends AbstractAuthenticatedResourceTest {

    private static final String USERNAME = "customer";

    @InjectMock
    BillingService billingService;

    @Test
    void executeOperation_success() {
        final var operationId = UUID.randomUUID();
        final var request = new ExecuteOperationRequestDto(OperationType.WITHDRAW, BigDecimal.TEN, "");

        Mockito.when(billingService.executeOperation(any(OperationType.class), any(BigDecimal.class),
                anyString()))
                .thenReturn(Uni.createFrom().item(operationId));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(200)
                .body("operationId", Matchers.is(operationId.toString()));
    }

    @Test
    void executeOperation_accountNotFound() {
        final var request = new ExecuteOperationRequestDto(OperationType.WITHDRAW, BigDecimal.TEN, "");

        Mockito.when(billingService.executeOperation(any(OperationType.class), any(BigDecimal.class),
                anyString()))
                .thenReturn(Uni.createFrom().failure(new AccountNotFoundException()));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(404)
                .body(Matchers.is("account not found"));
    }

    @Test
    void executeOperation_insufficentAccountBalance() {
        final var request = new ExecuteOperationRequestDto(OperationType.WITHDRAW, BigDecimal.TEN, "");

        Mockito.when(billingService.executeOperation(any(OperationType.class), any(BigDecimal.class),
                anyString()))
                .thenReturn(Uni.createFrom().failure(new InsufficentAccountBalanceException()));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(403)
                .body(Matchers.is("insufficent account balance"));
    }

    @Test
    void executeOperation_serverError() {
        final var errorMessage = "test error";
        final var request = new ExecuteOperationRequestDto(OperationType.CREDIT, BigDecimal.TEN, "");

        Mockito.when(billingService.executeOperation(any(OperationType.class), any(BigDecimal.class),
                anyString()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException(errorMessage)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(request)
                .post()
                .then()
                .statusCode(500)
                .body(Matchers.is(errorMessage));
    }

    @Test
    void cancelOperation_success() {
        final var operationId = UUID.randomUUID();
        final var cancelOperationId = UUID.randomUUID();
        final var request = new CancelOperationRequestDto("cancel operation comment");

        Mockito.when(billingService.cancelOperation(operationId, request.getComment()))
                .thenReturn(Uni.createFrom().item(cancelOperationId));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(request)
                .delete("/{operationId}", operationId)
                .then()
                .statusCode(200)
                .body("operationId", Matchers.is(cancelOperationId.toString()));
    }

    @Test
    void cancelOperation_operationNotFound() {
        final var operationId = UUID.randomUUID();
        final var request = new CancelOperationRequestDto("cancel operation comment");

        Mockito.when(billingService.cancelOperation(operationId, request.getComment()))
                .thenReturn(Uni.createFrom().failure(new OperationNotFoundException(operationId)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(request)
                .delete("/{operationId}", operationId)
                .then()
                .statusCode(404)
                .body(Matchers.is("operation not found, id=" + operationId));
    }

    @Test
    void cancelOperation_accountNotFound() {
        final var operationId = UUID.randomUUID();
        final var accountId = UUID.randomUUID();
        final var request = new CancelOperationRequestDto("cancel operation comment");

        Mockito.when(billingService.cancelOperation(operationId, request.getComment()))
                .thenReturn(Uni.createFrom().failure(new AccountNotFoundException(accountId)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(request)
                .delete("/{operationId}", operationId)
                .then()
                .statusCode(404)
                .body(Matchers.is("account not found, id=" + accountId));
    }

    @Test
    void cancelOperation_operationExecutedByDifferentUser() {
        final var operationId = UUID.randomUUID();
        final var request = new CancelOperationRequestDto("cancel operation comment");

        Mockito.when(billingService.cancelOperation(operationId, request.getComment()))
                .thenReturn(Uni.createFrom().failure(new OperationExecutedByDifferentUserException(operationId)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(request)
                .delete("/{operationId}", operationId)
                .then()
                .statusCode(403)
                .body(Matchers.is("operation executed by different user, id=" + operationId));
    }

    @Test
    void cancelOperation_serverError() {
        final var errorMessage = "test error";
        final var operationId = UUID.randomUUID();
        final var request = new CancelOperationRequestDto("cancel operation comment");

        Mockito.when(billingService.cancelOperation(operationId, request.getComment()))
                .thenReturn(Uni.createFrom().failure(new RuntimeException(errorMessage)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .contentType(ContentType.JSON)
                .body(request)
                .delete("/{operationId}", operationId)
                .then()
                .statusCode(500)
                .body(Matchers.is(errorMessage));
    }
}
