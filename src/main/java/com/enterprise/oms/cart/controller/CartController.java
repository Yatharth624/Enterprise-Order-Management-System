package com.enterprise.oms.cart.controller;

import com.enterprise.oms.cart.dto.AddCartItemRequest;
import com.enterprise.oms.cart.dto.CartResponse;
import com.enterprise.oms.cart.service.CartService;
import com.enterprise.oms.common.api.ApiResponse;
import com.enterprise.oms.common.api.CorrelationIdFilter;
import com.enterprise.oms.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Endpoints for managing customer shopping cart")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get active user shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        String userEmail = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.getOrCreateCart(userEmail);
        return ResponseEntity.ok(ApiResponse.success(cart, requestId));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@Valid @RequestBody AddCartItemRequest request,
                                                             HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        String userEmail = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.addItemToCart(userEmail, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart", requestId));
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "Remove item from shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable UUID id,
                                                                HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        String userEmail = SecurityUtils.getCurrentUserEmail();
        CartResponse cart = cartService.removeItemFromCart(userEmail, id);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item removed from cart", requestId));
    }
}
