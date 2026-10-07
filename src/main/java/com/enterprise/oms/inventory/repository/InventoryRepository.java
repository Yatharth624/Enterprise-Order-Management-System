package com.enterprise.oms.inventory.repository;

import com.enterprise.oms.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    Optional<Inventory> findByProductIdAndWarehouseId(UUID productId, UUID warehouseId);
    List<Inventory> findByProductId(UUID productId);

    @Query("SELECT SUM(i.quantityAvailable) FROM Inventory i WHERE i.product.id = :productId")
    Integer getTotalAvailableStockByProduct(@Param("productId") UUID productId);

    @Query("SELECT i FROM Inventory i WHERE i.quantityAvailable < :threshold")
    List<Inventory> findLowStockInventory(@Param("threshold") int threshold);
}
