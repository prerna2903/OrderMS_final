package com.infosys.controller;

import java.util.Arrays;
import java.util.List;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infosys.dto.CartDTO;
import com.infosys.dto.ProductDTO;
import com.infosys.dto.PlaceOrderDTO;
import com.infosys.dto.OrderDetailsByIdDTO;
import com.infosys.dto.ProductsOrderedDTO;
import com.infosys.service.OrderService;
import com.infosys.validator.Validator;

@RestController
@CrossOrigin
public class OrderController {
	
	Logger logger = LoggerFactory.getLogger(this.getClass());
	
	
	@Value("${USER.CART.uri}")
	String cartUri;
	
	@Value("${Product.uri}")
	String productUri;

	@Value("${USER.Buyer.uri}")
	String buyerUri;
	
	
	@Autowired
	private Environment env;
	
	@Autowired
	private OrderService orderservice;
	
	@Autowired
	private RestTemplate restTemplate;
	
	//FETCHING ALL THE DETAILS BY ORDID
	@GetMapping(value = "api/orders/{orderId}",  produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<OrderDetailsByIdDTO> getOrderDetailsByorderId(@PathVariable int orderId)
	{
		try {
			logger.info("Order Details for specific ID {}",orderId);
			OrderDetailsByIdDTO newdto = orderservice.getOrderDetails(orderId); 
			ResponseEntity<OrderDetailsByIdDTO> response = new ResponseEntity<OrderDetailsByIdDTO>(newdto,HttpStatus.OK);
			String infoForLog=response.toString();
			logger.info(infoForLog);
			return response;
		}
		catch (Exception e) {
			logger.warn("Exception occurs in finding order details by order Id");
			logger.info(e.getMessage());
			ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST,env.getProperty("Service.NO_RECORD_FOUND"));
		    throw exception;
		}
	}
	
	@GetMapping(value = "api/orders/seller/{sellerid}",  produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ProductsOrderedDTO> getOrderDetailsBySellerId(@PathVariable Integer sellerid)
	{
		logger.info("order details by seller id");
		List<ProductsOrderedDTO> productsOrderedDTOs=orderservice.getOrderDetailsBySellerId(sellerid);
		logger.info(productsOrderedDTOs.toString());
		return productsOrderedDTOs;
		
	}
	
	
	
	//CHECKOUT FROM CART FOR PARTICULAR BUYERID
	@DeleteMapping(value = "/orders/{buyerId}")
	public void checkoutFromMyCart(@PathVariable int buyerId)
	{
		String url =cartUri+"/"+buyerId;
		restTemplate.delete(url,buyerId);
	}
	
	//ADDING TO CART
	 @PostMapping(value = "/orders/toCart")
	 public void addToMyCart(@RequestBody CartDTO cart)
		{
			restTemplate.postForObject(cartUri,cart,String.class);
		}
	
	//PLACING ORDER
	@PostMapping(value = "api/orders/placeorder", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> placeOrder(@RequestBody PlaceOrderDTO placeorder) throws JsonMappingException, JsonProcessingException ,Exception
		{	
		String info="Placing order for "+placeorder.toString();
		logger.info(info);
		if(!Validator.validateAddress(placeorder.getAddress())) {
			logger.info("Address is not correct");
		    return new ResponseEntity<String>(env.getProperty("Validator_ADDRESS"),HttpStatus.BAD_REQUEST);
		}
		
		ObjectMapper mapper = new ObjectMapper();
			
		//fetching cart details from userMS
		String url =cartUri+"/"+placeorder.getBuyerId();
		logger.info("hitting url\n"+url);
		String cartlist=restTemplate.getForObject(url,String.class);
			
		List<CartDTO> ppl2 = Arrays.asList(mapper.readValue(cartlist, CartDTO[].class));
		logger.info(ppl2.toString());
			
		//fetching product details from productMS
		String prodDTO = restTemplate.getForObject(productUri,String.class);
		logger.info("hitting url\n"+productUri);
			
		List<ProductDTO> plist2 = Arrays.asList(mapper.readValue(prodDTO, ProductDTO[].class));
		logger.info(plist2.toString());
		
		for(CartDTO cartDTO:ppl2) {
			for(ProductDTO productDTO:plist2) {
				if(cartDTO.getProdId().equals(productDTO.getProdId())) {
					String url_product=productUri+"/"+productDTO.getProdId()+"/quantity/"+cartDTO.getQuantity();
					Boolean returnValue=restTemplate.getForObject(url_product, Boolean.class);
					if(!returnValue) {
						return new ResponseEntity<String>(env.getProperty("API.OUT_OF_STOCK"),HttpStatus.BAD_REQUEST);
						}
					}
				}
			}
			
			logger.info("Calculating amount");
			
			//calculating amount
			Double amount = orderservice.placeOrder(ppl2,plist2);
			
			//fetching the discounted amount and calculating final amount
			String url_discount =buyerUri+placeorder.getBuyerId()+"/discount/"+amount;
			Double discountedAmount = restTemplate.getForObject(url_discount,Double.class);
			Double finalAmount = amount - discountedAmount;
			
			logger.info("amount after discount: "+finalAmount);
			
			//updating reward points
			logger.info("updating reward points");
			String url_reward =buyerUri+placeorder.getBuyerId()+"/rewardpoints/"+amount;
			restTemplate.put(url_reward,ResponseEntity.class);
			
			//ENTERING INTO DATABASES{orderdetails}{productsordered}

			orderservice.settingData(placeorder,ppl2,plist2,finalAmount);
		
			//DELETING FROM CART FOR A PARTICULAR BUYER ID
			logger.info("deleting from cart");
			checkoutFromMyCart(placeorder.getBuyerId());
			return new ResponseEntity<String>(env.getProperty("API.ORDER_PLACED_SUCCESSFULLY"),HttpStatus.OK);
		}

			@PostMapping(value = "api/orders/reorder/{orderId}/{buyerId}")
			public ResponseEntity<String> reOrder(@PathVariable Integer orderId,@PathVariable Integer buyerId) throws Exception
			{
				logger.info("reordering the order for buyer "+buyerId);
				try {
					List<ProductsOrderedDTO> productsOrderedDTOlist=orderservice.getProductsOrdered(orderId);
					for(ProductsOrderedDTO productsOrderedDTO:productsOrderedDTOlist) {
						String url6=productUri+"/"+productsOrderedDTO.getProdid()+"/quantity/"+productsOrderedDTO.getQuantity();
							Boolean returnValue=restTemplate.getForObject(url6, Boolean.class);
							System.out.println(returnValue);
							if(!returnValue) {
								return new ResponseEntity<String>(env.getProperty("API.OUT_OF_STOCK"),HttpStatus.BAD_REQUEST);
							}
						}
						
					orderservice.reOrder(orderId,buyerId);
					return new ResponseEntity<String>(env.getProperty("API.ORDER_PLACED_SUCCESSFULLY"),HttpStatus.OK);

				}catch (Exception e) {
					logger.warn("Exception occurs");
					logger.info(env.getProperty("Service.NO_RECORD_FOUND"));
					return new ResponseEntity<String>(env.getProperty("Service.NO_RECORD_FOUND"),HttpStatus.BAD_REQUEST);
				}
			}
			
			@PutMapping(value="/orders/status/{orderId}/{message}")
			public Boolean setStatus(@PathVariable Integer orderId, @PathVariable String message) {
				logger.info("setting order status");
				return orderservice.setStatus(orderId,message);
			}
			
}		
	
