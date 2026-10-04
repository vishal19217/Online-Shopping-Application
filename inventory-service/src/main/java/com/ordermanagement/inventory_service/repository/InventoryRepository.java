package com.ordermanagement.inventory_service.repository;

import com.ordermanagement.inventory_service.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findBySkuCodeIn(List<String> skuCode);
    boolean existsBySkuCodeAndQuantityIsGreaterThanEqual(String skuCode,Integer quantity);
}
