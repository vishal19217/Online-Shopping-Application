package com.ordermanagement.order_service.service;

import com.ordermanagement.order_service.client.InventoryClientNew;
import com.ordermanagement.order_service.dto.OrderLineItemsDto;
import com.ordermanagement.order_service.dto.OrderRequest;
import com.ordermanagement.order_service.model.Order;
import com.ordermanagement.order_service.model.OrderLineItems;
import com.ordermanagement.order_service.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClientNew inventoryClient;
    @Transactional
    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(this::mapToDto)
                .toList();
        order.setOrderLineItemsList(orderLineItems);

        orderRepository.save(order);
    }
    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());

        // 1. Mockito for mocking the APIs
        var isProductInStock = inventoryClient.isInStock(orderLineItemsDto.getSkuCode(),orderLineItemsDto.getQuantity());


        if(!isProductInStock)throw  new RuntimeException("Product with SkuCode "+orderLineItemsDto.getSkuCode() + "is not in stock");
        return orderLineItems;
    }
}
