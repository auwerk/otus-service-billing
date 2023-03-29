package org.auwerk.otus.arch.billingservice.api;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.auwerk.otus.arch.billingservice.api.dto.CreateUserAccountRequestDto;
import org.auwerk.otus.arch.billingservice.api.dto.CreateUserAccountResponseDto;
import org.auwerk.otus.arch.billingservice.exception.AccountAlreadyExistsException;
import org.auwerk.otus.arch.billingservice.exception.AccountNotFoundException;
import org.auwerk.otus.arch.billingservice.mapper.AccountMapper;
import org.auwerk.otus.arch.billingservice.service.BillingService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@Path("/account")
@RequiredArgsConstructor
public class AccountResource {

    protected static final String DEFAULT_FETCH_OPERATIONS = "true";

    private final BillingService billingService;
    private final AccountMapper accountMapper;

    @GET
    @RolesAllowed("${otus.role.customer}")
    public Uni<Response> getUserAccount(
            @QueryParam("fetchOperations") @DefaultValue(DEFAULT_FETCH_OPERATIONS) boolean fetchOperations) {
        return billingService.getUserAccount(fetchOperations)
                .map(account -> Response.ok(accountMapper.toDto(account)).build())
                .onFailure(AccountNotFoundException.class)
                .recoverWithItem(failure -> Response.status(Status.NOT_FOUND).entity(failure.getMessage()).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }

    @POST
    public Uni<Response> createUserAccount(CreateUserAccountRequestDto request) {
        return billingService.createUserAccount(request.getUserName())
                .map(accountId -> Response.ok(new CreateUserAccountResponseDto(accountId)).build())
                .onFailure(AccountAlreadyExistsException.class)
                .recoverWithItem(failure -> Response.status(Status.CONFLICT).entity(failure.getMessage()).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }
}
