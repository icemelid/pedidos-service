
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PedidoProveedorTimeoutIntegrationTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PedidoService pedidoService;

    @MockitoBean(name = "proveedorClientLentoSimulado")
    private ProveedorClient proveedorClient;

    @Test
    void debeMarcarPedidoComoFallidoTrasTresTimeoutsDelProveedor()
            throws InterruptedException {

        // 1. Creamos un cliente real en H2.
        Cliente cliente = new Cliente(
                "Cliente timeout",
                "timeout@test.com"
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

        // 3. Simulamos un proveedor que tarda 7 segundos.
        // El servicio solo espera 6 segundos por intento.
        when(proveedorClient.calcularCosteEnvio(
                eq(pedidoId),
                any(BigDecimal.class)
        )).thenAnswer(invocation -> {

            Thread.sleep(7000);

            return new BigDecimal("5.95");
        });

        // 4. Ejecutamos la confirmación.
        IllegalStateException excepcion = assertThrows(
                IllegalStateException.class,
                () -> pedidoService.confirmarPedido(pedidoId)
        );

        assertEquals(
                "El proveedor no ha podido confirmar el pedido.",
                excepcion.getMessage()
        );

        assertTrue(
                excepcion.getCause().getMessage().contains("Timeout")
        );

        // 5. Verificamos los tres intentos.
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

        // 8. Comprobamos que los importes no han cambiado.
        assertNull(pedidoFinal.getCosteEnvio());
        assertNull(pedidoFinal.getImporteTotal());

        assertEquals(
                new BigDecimal("100.00"),
                pedidoFinal.getImporteProductos()
        );
    }
}
