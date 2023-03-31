package org.auwerk.otus.arch.billingservice.api;

import java.util.UUID;

import org.auwerk.otus.arch.billingservice.domain.Account;
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
@TestHTTPEndpoint(AccountResource.class)
public class AccountResourceTest extends AbstractAuthenticatedResourceTest {

    private static final String USERNAME = "customer";

    @InjectMock
    BillingService billingService;

    @Test
    void getUserAccount_success() {
        final var account = buildAccount();

        Mockito.when(billingService.getUserAccount(true))
                .thenReturn(Uni.createFrom().item(account));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .get()
                .then()
                .statusCode(200);
    }

    @Test
    void getUserAccount_accountNotFound() {
        Mockito.when(billingService.getUserAccount(true))
                .thenReturn(Uni.createFrom().failure(new AccountNotFoundException()));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .get()
                .then()
                .statusCode(404)
                .body(Matchers.is("account not found"));
    }

    @Test
    void getUserAccount_serverError() {
        final var errorMessage = "test error";

        Mockito.when(billingService.getUserAccount(true))
                .thenReturn(Uni.createFrom().failure(new RuntimeException(errorMessage)));

        RestAssured.given()
                .auth().oauth2(getAccessToken(USERNAME))
                .get()
                .then()
                .statusCode(500)
                .body(Matchers.is(errorMessage));
    }

    private static Account buildAccount() {
        return Account.builder()
                .id(UUID.randomUUID())
                .build();
    }
}
