package com.futbol.tokenmarket.controller;

import com.futbol.tokenmarket.dto.PortfolioEntryResponse;
import com.futbol.tokenmarket.dto.TransactionResponse;
import com.futbol.tokenmarket.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "Portfolio e historial de operaciones del usuario")
public class UserController {

    private final OrderService orderService;

    public UserController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{id}/portfolio")
    @Operation(summary = "Portfolio del usuario",
               description = "Devuelve los tokens que posee el usuario con su valor actual.")
    public ResponseEntity<List<PortfolioEntryResponse>> getPortfolio(
            @Parameter(description = "ID del usuario") @PathVariable String id) {
        return ResponseEntity.ok(orderService.getPortfolio(id));
    }

    @GetMapping("/{id}/transactions")
    @Operation(summary = "Historial de operaciones del usuario",
               description = "Devuelve todas las compras y ventas del usuario, ordenadas por fecha descendente.")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @Parameter(description = "ID del usuario") @PathVariable String id) {
        return ResponseEntity.ok(orderService.getTransactions(id));
    }
}
