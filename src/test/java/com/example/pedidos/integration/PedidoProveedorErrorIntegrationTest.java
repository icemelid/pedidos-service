
package com.example.pedidos.integration;

import com.example.pedidos.client.ProveedorClient;
import com.example.pedidos.entity.Cliente;
import com.example.pedidos.entity.EstadoPedido;
import com.example.pedidos.entity.Pedido;
import com.example.pedidos.repository.ClienteRepository;
import com.example.pedidos.repository.PedidoRepository;
import com.example.pedidos.service.PedidoService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PedidoProveedorErrorIntegrationTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoService pedidoService;

    @MockitoBean(name = "proveedorClientLentoSimulado")
    private ProveedorClient proveedorClient;

    @Test
    void debeMarcarPedidoComoFallidoTrasTresErroresDelProveedor() {

        // 1. Creamos y guardamos un cliente real en H2.
        Cliente cliente = new Cliente(
                "Cliente de prueba",
                "integracion@test.com"
        );

        cliente = clienteRepository.save(cliente);

        // 2. Creamos un pedido en estado CREATED.
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.CREATED);
        pedido.setFecha(LocalDateTime.now());
        pedido.setImporteProductos(new BigDecimal("100.00"));

        pedido = pedidoRepository.save(pedido);

        Long pedidoId = pedido.getId();

        // 3. Simulamos un error del proveedor en cada llamada.
        when(proveedorClient.calcularCosteEnvio(
                eq(pedidoId),
                any(BigDecimal.class)
        )).thenThrow(
                new IllegalStateException("Error simulado del proveedor")
        );

        // 4. El servicio debe agotar los tres intentos.
        assertThrows(
                IllegalStateException.class,
                () -> pedidoService.confirmarPedido(pedidoId)
        );

        // 5. Comprobamos que el proveedor recibió tres llamadas.
        verify(proveedorClient, times(3))
                .calcularCosteEnvio(
                        eq(pedidoId),
                        any(BigDecimal.class)
                );

        // 6. Recuperamos el pedido desde H2.
        Pedido pedidoFinal = pedidoRepository.findById(pedidoId)
                .orElseThrow();

        // 7. Comprobamos el estado final.
        assertEquals(
                EstadoPedido.CONFIRMATION_FAILED,
                pedidoFinal.getEstado()
        );

        // 8. Los importes no deben haberse modificado.
        assertNull(pedidoFinal.getCosteEnvio());
        assertNull(pedidoFinal.getImporteTotal());
        assertEquals(
                new BigDecimal("100.00"),
                pedidoFinal.getImporteProductos()
        );
    }
}
