package com.enterprise.oms.product.controller;

import com.enterprise.oms.common.api.ApiResponse;
import com.enterprise.oms.common.api.CorrelationIdFilter;
import com.enterprise.oms.product.dto.CategoryRequest;
import com.enterprise.oms.product.dto.CategoryResponse;
import com.enterprise.oms.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Endpoints for product categories")
public class CategoryController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories(HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        List<CategoryResponse> categories = productService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, requestId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new category (ADMIN only)")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest request,
                                                                         HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        CategoryResponse category = productService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Category created successfully", requestId));
    }
}
