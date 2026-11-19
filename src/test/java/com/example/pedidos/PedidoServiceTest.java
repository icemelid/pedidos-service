
package com.example.pedidos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import com.example.pedidos.client.ProveedorClient;
import com.example.pedidos.repository.PedidoRepository;

import static org.mockito.Mockito.mock;

class PedidoServiceTest {

    @Test
    void debeCalcularCorrectamenteElImporteTotal() {

        // Arrange: preparamos el servicio y los importes.
        PedidoRepository repository = mock(PedidoRepository.class);
        ProveedorClient proveedor = mock(ProveedorClient.class);
        PlatformTransactionManager transactionManager =
                mock(PlatformTransactionManager.class);

        PedidoService service = new PedidoService(
                repository,
                proveedor,
                transactionManager
        );

        BigDecimal importeProductos = new BigDecimal("100.00");
        BigDecimal costeEnvio = new BigDecimal("5.95");

        // Act: ejecutamos el cálculo.
        BigDecimal resultado = service.calcularImporteTotal(
                importeProductos,
                costeEnvio
        );

        // Assert: comprobamos el resultado esperado.
        assertEquals(
                new BigDecimal("105.95"),
                resultado
        );
    }

        @Test
        void debeCalcularCorrectamenteConEnvioGratuito() {

        PedidoService service = crearServicio();

        BigDecimal resultado = service.calcularImporteTotal(
                new BigDecimal("50.00"),
                new BigDecimal("0.00")
        );

        assertEquals(new BigDecimal("50.00"), resultado);
        }

        @Test
        void debeCalcularCorrectamenteSinImporteDeProductos() {

        PedidoService service = crearServicio();

        BigDecimal resultado = service.calcularImporteTotal(
                new BigDecimal("0.00"),
                new BigDecimal("5.95")
        );

        assertEquals(new BigDecimal("5.95"), resultado);
        }

        private PedidoService crearServicio() {

                return new PedidoService(
                        mock(PedidoRepository.class),
                        mock(ProveedorClient.class),
                        mock(PlatformTransactionManager.class)
                );
        }
}
