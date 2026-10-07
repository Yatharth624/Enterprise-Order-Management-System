package com.enterprise.oms.product.controller;

import com.enterprise.oms.common.api.ApiResponse;
import com.enterprise.oms.common.api.CorrelationIdFilter;
import com.enterprise.oms.product.dto.ProductRequest;
import com.enterprise.oms.product.dto.ProductResponse;
import com.enterprise.oms.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Endpoints for managing products catalog and search")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Search and list products with pagination")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            HttpServletRequest servletRequest) {

        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Page<ProductResponse> products = productService.searchProducts(query, categoryId, PageRequest.of(page, size, sort));
        return ResponseEntity.ok(ApiResponse.success(products, requestId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details by ID (cached with Redis)")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id,
                                                                        HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product, requestId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new product (ADMIN only)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request,
                                                                       HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(product, "Product created successfully", requestId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing product (ADMIN only)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(@PathVariable UUID id,
                                                                       @Valid @RequestBody ProductRequest request,
                                                                       HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully", requestId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product (ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id,
                                                           HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully", requestId));
    }
}
