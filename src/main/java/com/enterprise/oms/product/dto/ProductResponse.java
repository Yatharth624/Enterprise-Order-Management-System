package com.enterprise.oms.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private CategoryResponse category;
    private Integer availableStock;
}
