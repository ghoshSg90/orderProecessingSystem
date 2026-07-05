package com.test.orderProcessingSystem.controller;

import com.test.orderProcessingSystem.dto.AddressResponse;
import com.test.orderProcessingSystem.dto.CreateAddressRequest;
import com.test.orderProcessingSystem.dto.ErrorResponse;
import com.test.orderProcessingSystem.dto.UpdateAddressRequest;
import com.test.orderProcessingSystem.security.SecurityUtils;
import com.test.orderProcessingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User Account Services", description = " - APIs for customer account and address management")
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Delete own account",
            description = "Deletes the authenticated customer's account (cascades to addresses and orders). "
                    + "Allowed only when all orders are CANCELLED or DELIVERED.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No content - account deleted"),
            @ApiResponse(responseCode = "400", description = "Bad request - account has non-terminal orders",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot delete another customer's account",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - user does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(required = true, description = "Id of the account to delete (must be the caller's own id)")
            @PathVariable Long userId) {
        requireSelf(userId);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List own addresses",
            description = "Returns all addresses belonging to the authenticated customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - addresses retrieved",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot view another customer's addresses",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - user does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}/addresses")
    public ResponseEntity<List<AddressResponse>> listAddresses(
            @Parameter(required = true, description = "Id of the caller's own account") @PathVariable Long userId) {
        requireSelf(userId);
        return ResponseEntity.ok(userService.listAddresses(userId));
    }

    @Operation(summary = "Delete an address",
            description = "Deletes one of the authenticated customer's addresses. "
                    + "Not allowed if the address is referenced by any order.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "No content - address deleted"),
            @ApiResponse(responseCode = "400", description = "Bad request - address is used by one or more orders",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot modify another customer's account",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - address does not exist for this user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @Parameter(required = true, description = "Id of the caller's own account") @PathVariable Long userId,
            @Parameter(required = true, description = "Id of the address to delete") @PathVariable Long addressId) {
        requireSelf(userId);
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Add an address",
            description = "Adds a new address to the authenticated customer's account (e.g. right after registration).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Created - address added",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad request - validation failed",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot add to another customer's account",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - user does not exist",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{userId}/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @Parameter(required = true, description = "Id of the caller's own account") @PathVariable Long userId,
            @Valid @RequestBody CreateAddressRequest request) {
        requireSelf(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addAddress(userId, request));
    }

    @Operation(summary = "Update an address",
            description = "Updates one of the authenticated customer's own addresses.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK - address updated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden - cannot modify another customer's account",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found - address does not exist for this user",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{userId}/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @Parameter(required = true, description = "Id of the caller's own account") @PathVariable Long userId,
            @Parameter(required = true, description = "Id of the address to update") @PathVariable Long addressId,
            @Valid @RequestBody UpdateAddressRequest request) {
        requireSelf(userId);
        return ResponseEntity.ok(userService.updateAddress(userId, addressId, request));
    }

    private void requireSelf(Long userId) {
        if (!userId.equals(SecurityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("You are not allowed to modify another customer's account");
        }
    }
}
