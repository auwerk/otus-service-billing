package org.auwerk.otus.arch.billingservice.api;

import java.util.UUID;

import org.auwerk.otus.arch.billingservice.exception.AccountAlreadyExistsException;
import org.auwerk.otus.arch.billingservice.exception.AccountNotFoundException;
import org.auwerk.otus.arch.billingservice.service.BillingService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Uni;

@QuarkusTest
@TestHTTPEndpoint(AccountManagementResource.class)
public class AccountManagementResourceTest {

    private static final String USERNAME = "customer";

    @InjectMock
    BillingService billingService;

    @Test
    void createUserAccount_success() {
        final var accountId = UUID.randomUUID();

        Mockito.when(billingService.createUserAccount(USERNAME))
                .thenReturn(Uni.createFrom().item(accountId));

        RestAssured.given()
                .post("/{userName}", USERNAME)
                .then()
                .statusCode(200)
                .body("accountId", Matchers.is(accountId.toString()));
    }

    @Test
    void createUserAccount_accountAlreadyExists() {
        Mockito.when(billingService.createUserAccount(USERNAME))
                .thenReturn(Uni.createFrom().failure(new AccountAlreadyExistsException()));

        RestAssured.given()
                .post("/{userName}", USERNAME)
                .then()
                .statusCode(409)
                .body(Matchers.is("account already exists"));
    }

    @Test
    void createUserAccount_serverError() {
        final var errorMessage = "test error";

        Mockito.when(billingService.createUserAccount(USERNAME))
                .thenReturn(Uni.createFrom().failure(new RuntimeException(errorMessage)));

        RestAssured.given()
                .post("/{userName}", USERNAME)
                .then()
                .statusCode(500)
                .body(Matchers.is(errorMessage));
    }

    @Test
    void deleteUserAccount_success() {
        Mockito.when(billingService.deleteUserAccount(USERNAME))
                .thenReturn(Uni.createFrom().voidItem());

        RestAssured.given()
                .delete("/{userName}", USERNAME)
                .then()
                .statusCode(200);
    }

    @Test
    void deleteUserAccount_accountNotFound() {
        Mockito.when(billingService.deleteUserAccount(USERNAME))
                .thenReturn(Uni.createFrom().failure(new AccountNotFoundException()));

        RestAssured.given()
                .delete("/{userName}", USERNAME)
                .then()
                .statusCode(404)
                .body(Matchers.is("account not found"));
    }

    @Test
    void deleteUserAccount_serverError() {
        final var errorMessage = "test error";

        Mockito.when(billingService.deleteUserAccount(USERNAME))
                .thenReturn(Uni.createFrom().failure(new RuntimeException(errorMessage)));

        RestAssured.given()
                .delete("/{userName}", USERNAME)
                .then()
                .statusCode(500)
                .body(Matchers.is(errorMessage));
    }
}
