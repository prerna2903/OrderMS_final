package com.infosys;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import com.infosys.dto.CartDTO;
import com.infosys.dto.OrderDetailsByIdDTO;
import com.infosys.dto.ProductDTO;
import com.infosys.dto.ProductsOrderedDTO;
import com.infosys.repository.OrderDetailsRepo;
import com.infosys.repository.ProductsOrderedRepo;
import com.infosys.service.OrderService;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class OrderMsApplicationTests {

	@Mock
	private ProductsOrderedRepo ordrepo;
	
	@InjectMocks
	private OrderService orderService=new OrderService();
	
	
	
	@Test
	public void getOrderDetailsBySellerIdTest() {
		List<ProductsOrderedDTO> productsOrderedDTOList=new ArrayList<>();
		List<ProductsOrderedDTO> productsOrderedDTOs=orderService.getOrderDetailsBySellerId(1);
		Assertions.assertEquals(productsOrderedDTOs.size(), 0);
	}
	
	@Test
	public void getOrderDetailsBySellerIdTestInvalid() {
		List<ProductsOrderedDTO> productsOrderedDTOList=new ArrayList<>();
		Mockito.lenient().when(orderService.getOrderDetailsBySellerId(16)).thenReturn(productsOrderedDTOList);
		List<ProductsOrderedDTO> productsOrderedDTOs=orderService.getOrderDetailsBySellerId(16);
		Assertions.assertEquals(productsOrderedDTOs.size(), 0);
	}
	
	@Test
	public void getProductsOrderedTest() {
		List<ProductsOrderedDTO> productsOrderedDTOList=new ArrayList<>();
		Mockito.lenient().when(orderService.getProductsOrdered(6)).thenReturn(productsOrderedDTOList);
		List<ProductsOrderedDTO> productsOrderedDTOs=orderService.getProductsOrdered(1);
		Assertions.assertEquals(productsOrderedDTOs.size(), 0);
		
		
	}
	
	
	@Test
	public void getProductsOrderedTestIvalid() {
		List<ProductsOrderedDTO> productsOrderedDTOList=new ArrayList<>();
		Mockito.lenient().when(orderService.getProductsOrdered(1610)).thenReturn(productsOrderedDTOList);
		List<ProductsOrderedDTO> productsOrderedDTOs=orderService.getProductsOrdered(1);
		Assertions.assertEquals(productsOrderedDTOs.size(), 0);
		
		
	}


}
