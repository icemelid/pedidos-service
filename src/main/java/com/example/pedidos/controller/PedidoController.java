package com.example.pedidos.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.pedidos.service.PedidoService;
import com.example.pedidos.dto.PedidoResponse;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping("/{id}/confirmacion")
    public PedidoResponse confirmarPedido(@PathVariable Long id) {

        return pedidoService.confirmarPedido(id);
    }
}