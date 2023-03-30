package org.auwerk.otus.arch.billingservice.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.auwerk.otus.arch.billingservice.api.dto.CreateUserAccountResponseDto;
import org.auwerk.otus.arch.billingservice.exception.AccountAlreadyExistsException;
import org.auwerk.otus.arch.billingservice.exception.AccountNotFoundException;
import org.auwerk.otus.arch.billingservice.service.BillingService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@Path("/management")
@RequiredArgsConstructor
public class ManagementResource {

    private final BillingService billingService;

    @POST
    @Path("/{userName}")
    public Uni<Response> createUserAccount(@PathParam("userName") String userName) {
        return billingService.createUserAccount(userName)
                .map(accountId -> Response.ok(new CreateUserAccountResponseDto(accountId)).build())
                .onFailure(AccountAlreadyExistsException.class)
                .recoverWithItem(failure -> Response.status(Status.CONFLICT).entity(failure.getMessage()).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }

    @DELETE
    @Path("/{userName}")
    public Uni<Response> deleteUserAccount(@PathParam("userName") String userName) {
        return billingService.deleteUserAccount(userName)
                .replaceWith(Response.ok().build())
                .onFailure(AccountNotFoundException.class)
                .recoverWithItem(failure -> Response.status(Status.NOT_FOUND).entity(failure.getMessage()).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }
}
