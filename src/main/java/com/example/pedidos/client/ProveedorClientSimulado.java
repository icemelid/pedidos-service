
package com.example.pedidos.client;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

@Component
public class ProveedorClientSimulado implements ProveedorClient {

    /*
    @Override
    public BigDecimal calcularCosteEnvio(
            Long pedidoId,
            BigDecimal importeProductos) {

        return new BigDecimal("5.95");
    }
    */

   // Timeout
    @Override
    public BigDecimal calcularCosteEnvio(
            Long pedidoId,
            BigDecimal importeProductos) {

        throw new RuntimeException(
            "Timeout simulado del proveedor"
        );
    }   


}
