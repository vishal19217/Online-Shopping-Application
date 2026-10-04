package com.ordermanagement.inventory_service.service;

import com.ordermanagement.inventory_service.repository.InventoryRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@AllArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    public boolean isInStock(String skuCode,Integer quantity){
        //Find an inventory for a given skuCode where quantity >= 0
        return inventoryRepository.existsBySkuCodeAndQuantityIsGreaterThanEqual(skuCode,quantity);

    }

}
