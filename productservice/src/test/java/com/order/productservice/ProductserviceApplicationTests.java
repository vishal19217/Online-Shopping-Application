package com.order.productservice;

import com.order.productservice.dto.ProductRequest;
import com.order.productservice.repositry.ProductRepository;
import io.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MockMvcBuilder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import tools.jackson.databind.ObjectMapper;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductserviceApplicationTests {


	@ServiceConnection
	static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0.5");


	@LocalServerPort
	private Integer port;
	@BeforeEach
	void setup(){
		RestAssured.baseURI = "http://localhost";
		RestAssured.port = port;
	}

	static {
		mongoDBContainer.start();
	}


	@Autowired
	ProductRepository productRepository;
	// converts pojo object ot json and json to pojo object
	@Test
	void shouldCreateProduct() throws Exception {
		// MockMVC project
//		ProductRequest productRequest = getProductRequest();
//		mockMvc.perform(MockMvcRequestBuilders.post("/api/product")
//				.contentType(MediaType.APPLICATION_JSON)
//				.content(objectMapper.writeValueAsString(productRequest)))
//		.andExpect(status().isCreated());
//
//		Assertions.assertEquals(1,productRepository.findAll().size());


		String requestBody = """
				{
				    "name":"iPhone 15",
				    "description":"It is pen",
				    "price":12
				}
				""";
		RestAssured.given()
				.contentType("application/json")
				.body(requestBody)
				.when()
				.post("api/product")
				.then()
				.statusCode(201)
				.body("id", Matchers.notNullValue())
				.body("name",Matchers.equalTo("iPhone 15"))
				.body("price.toString()",Matchers.equalTo("12"));

	}
	private ProductRequest getProductRequest(){
		return ProductRequest.builder()
				.name("Iphone Duo")
				.description("Fold Phone")
				.price(BigDecimal.valueOf(1200))
				.build();
	}

}
