
package com.example.pedidos.client;

import java.math.BigDecimal;

public interface ProveedorClient {

    BigDecimal calcularCosteEnvio(
            Long pedidoId,
            BigDecimal importeProductos
    );

}
