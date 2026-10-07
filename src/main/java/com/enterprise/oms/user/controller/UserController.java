package com.enterprise.oms.user.controller;

import com.enterprise.oms.common.api.ApiResponse;
import com.enterprise.oms.common.api.CorrelationIdFilter;
import com.enterprise.oms.common.util.SecurityUtils;
import com.enterprise.oms.user.dto.UserDto;
import com.enterprise.oms.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for logged in user info")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(HttpServletRequest servletRequest) {
        String requestId = (String) servletRequest.getAttribute(CorrelationIdFilter.CORRELATION_ID_KEY);
        String email = SecurityUtils.getCurrentUserEmail();
        UserDto user = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(user, requestId));
    }
}
