package com.enterprise.oms.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private ApiError error;
    private String timestamp;
    private String requestId;

    public static <T> ApiResponse<T> success(T data, String message, String requestId) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(Instant.now().toString())
                .requestId(requestId)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String requestId) {
        return success(data, "Operation successful", requestId);
    }

    public static <T> ApiResponse<T> error(String code, String message, String requestId) {
        return ApiResponse.<T>builder()
                .success(false)
                .error(new ApiError(code, message))
                .timestamp(Instant.now().toString())
                .requestId(requestId)
                .build();
    }
}
