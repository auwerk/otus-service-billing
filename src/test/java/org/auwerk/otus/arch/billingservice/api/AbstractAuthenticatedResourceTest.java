package org.auwerk.otus.arch.billingservice.api;

import io.quarkus.test.keycloak.client.KeycloakTestClient;

public abstract class AbstractAuthenticatedResourceTest {

    private final KeycloakTestClient keycloakTestClient = new KeycloakTestClient();

    protected String getAccessToken(String userName) {
        return keycloakTestClient.getAccessToken(userName);
    }
}
