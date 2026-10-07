package com.enterprise.oms.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private String productSku;
    private UUID warehouseId;
    private String warehouseName;
    private Integer quantityAvailable;
    private Integer quantityReserved;
}
