package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.dto.OrderRequest;
import com.futbol.tokenmarket.dto.OrderResponse;
import com.futbol.tokenmarket.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Compra y venta de tokens de jugadores")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/buy")
    @Operation(summary = "Comprar tokens de un jugador",
               description = "El usuario autenticado compra tokens del jugador al precio actual de cotización.")
    public ResponseEntity<OrderResponse> buy(
            @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.buy(userDetails.getUsername(), request));
    }

    @PostMapping("/sell")
    @Operation(summary = "Vender tokens de un jugador",
               description = "El usuario autenticado vende tokens del jugador al precio actual de cotización.")
    public ResponseEntity<OrderResponse> sell(
            @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.sell(userDetails.getUsername(), request));
    }
}
