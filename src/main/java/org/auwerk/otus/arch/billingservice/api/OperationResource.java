package org.auwerk.otus.arch.billingservice.api;

import java.util.UUID;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.auwerk.otus.arch.billingservice.api.dto.CancelOperationRequestDto;
import org.auwerk.otus.arch.billingservice.api.dto.ExecuteOperationRequestDto;
import org.auwerk.otus.arch.billingservice.api.dto.OperationResponseDto;
import org.auwerk.otus.arch.billingservice.exception.AccountNotFoundException;
import org.auwerk.otus.arch.billingservice.exception.InsufficentAccountBalanceException;
import org.auwerk.otus.arch.billingservice.exception.OperationAlreadyCanceledException;
import org.auwerk.otus.arch.billingservice.exception.OperationExecutedByDifferentUserException;
import org.auwerk.otus.arch.billingservice.exception.OperationNotFoundException;
import org.auwerk.otus.arch.billingservice.service.BillingService;

import io.smallrye.mutiny.Uni;
import lombok.RequiredArgsConstructor;

@Path("/operation")
@RolesAllowed("${otus.role.customer}")
@RequiredArgsConstructor
public class OperationResource {

    private final BillingService billingService;

    @POST
    public Uni<Response> executeOperation(ExecuteOperationRequestDto request) {
        return billingService.executeOperation(request.getType(), request.getAmount(), request.getComment())
                .map(operationId -> Response.ok(new OperationResponseDto(operationId)).build())
                .onFailure(AccountNotFoundException.class)
                .recoverWithItem(failure -> Response.status(Status.NOT_FOUND).entity(failure.getMessage()).build())
                .onFailure(InsufficentAccountBalanceException.class)
                .recoverWithItem(failure -> Response.status(Status.FORBIDDEN).entity(failure.getMessage()).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }

    @DELETE
    @Path("/{operationId}")
    public Uni<Response> cancelOperation(@PathParam("operationId") UUID operationId,
            CancelOperationRequestDto request) {
        return billingService.cancelOperation(operationId, request.getComment())
                .map(cancelOperationId -> Response.ok(new OperationResponseDto(cancelOperationId)).build())
                .onFailure(OperationNotFoundException.class)
                .recoverWithItem(failure -> Response.status(Status.NOT_FOUND).entity(failure.getMessage()).build())
                .onFailure(OperationAlreadyCanceledException.class)
                .recoverWithItem(failure -> Response.status(Status.CONFLICT).entity(failure.getMessage()).build())
                .onFailure(AccountNotFoundException.class)
                .recoverWithItem(failure -> Response.status(Status.NOT_FOUND).entity(failure.getMessage()).build())
                .onFailure(OperationExecutedByDifferentUserException.class)
                .recoverWithItem(failure -> Response.status(Status.FORBIDDEN).entity(failure.getMessage()).build())
                .onFailure()
                .recoverWithItem(failure -> Response.serverError().entity(failure.getMessage()).build());
    }
}
