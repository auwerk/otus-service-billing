package org.auwerk.otus.arch.billingservice.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import java.math.BigDecimal;
import java.util.UUID;

import org.auwerk.otus.arch.billingservice.api.dto.ExecuteOperationRequestDto;
import org.auwerk.otus.arch.billingservice.domain.OperationType;
import org.auwerk.otus.arch.billingservice.exception.AccountNotFoundException;
import org.auwerk.otus.arch.billingservice.exception.InsufficentAccountBalanceException;
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

        Mockito.when(billingService.executeOperation(any(OperationType.class), any(BigDecimal.class), anyString()))
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

        Mockito.when(billingService.executeOperation(any(OperationType.class), any(BigDecimal.class), anyString()))
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

        Mockito.when(billingService.executeOperation(any(OperationType.class), any(BigDecimal.class), anyString()))
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

        Mockito.when(billingService.executeOperation(any(OperationType.class), any(BigDecimal.class), anyString()))
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
}
