package com.ordermanagement.order_service;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ordermanagement.order_service.dto.OrderLineItemsDto;
import com.ordermanagement.order_service.dto.OrderRequest;
import com.ordermanagement.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for placing an order.
 * <p>
 * The Inventory service is an external dependency and calling it for real is
 * expensive, so instead of hitting it we stand up a WireMock "stub" that
 * answers the {@code GET /api/inventory} call the {@code InventoryClient} makes.
 * The flow under test is: place order -> service checks inventory (stub) ->
 * if in stock the order is persisted.
 * <p>
 * WireMock listens on port 8082 to match the hard-coded URL on the Feign
 * {@code InventoryClient}. MySQL is provided by Testcontainers so the order can
 * actually be saved.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class OrderControllerIntegrationTest {

    @Container
    static MySQLContainer<?> mySQLContainer = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mySQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mySQLContainer::getUsername);
        registry.add("spring.datasource.password", mySQLContainer::getPassword);
    }

    // Inventory service stub. Fixed to port 8082 to match InventoryClient's url.
    @RegisterExtension
    static WireMockExtension inventoryStub = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8082))
            .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanUp() {
        orderRepository.deleteAll();
    }

    @Test
    void shouldPlaceOrderWhenProductIsInStock() throws Exception {
        // Given: the inventory stub reports the SKU is in stock
        stubInventory("iphone_13", 2, true);

        String requestBody = objectMapper.writeValueAsString(buildOrderRequest("iphone_13", 2));

        // When: we place the order
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                // Then: the order is accepted
                .andExpect(status().isCreated())
                .andExpect(content().string("Order placed Successfully"));

        // And: the inventory stub was actually consulted before saving
        inventoryStub.verify(getRequestedFor(urlPathEqualTo("/api/inventory"))
                .withQueryParam("skuCode", equalTo("iphone_13"))
                .withQueryParam("quantity", equalTo("2")));

        // And: the order was persisted
        assertThat(orderRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldNotPlaceOrderWhenProductIsOutOfStock() throws Exception {
        // Given: the inventory stub reports the SKU is NOT in stock
        stubInventory("iphone_13", 2, false);

        String requestBody = objectMapper.writeValueAsString(buildOrderRequest("iphone_13", 2));

        // When / Then: placing the order fails because the product is out of stock.
        // The service throws a raw RuntimeException (there is no @ControllerAdvice),
        // so MockMvc propagates it instead of returning a 500 response.
        assertThatThrownBy(() -> mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)))
                .hasRootCauseInstanceOf(RuntimeException.class)
                .rootCause()
                .hasMessageContaining("is not in stock");

        // And: nothing is persisted (the @Transactional placeOrder rolled back)
        assertThat(orderRepository.findAll()).isEmpty();
    }

    private void stubInventory(String skuCode, int quantity, boolean inStock) {
        inventoryStub.stubFor(get(urlPathEqualTo("/api/inventory"))
                .withQueryParam("skuCode", equalTo(skuCode))
                .withQueryParam("quantity", equalTo(String.valueOf(quantity)))
                .willReturn(aResponse()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody(String.valueOf(inStock))));
    }

    private OrderRequest buildOrderRequest(String skuCode, int quantity) {
        OrderLineItemsDto lineItem = new OrderLineItemsDto();
        lineItem.setSkuCode(skuCode);
        lineItem.setPrice(BigDecimal.valueOf(1200));
        lineItem.setQuantity(quantity);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setOrderLineItemsDtoList(List.of(lineItem));
        return orderRequest;
    }
}
