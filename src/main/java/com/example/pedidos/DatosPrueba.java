
package com.example.pedidos;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.pedidos.entity.Cliente;
import com.example.pedidos.entity.EstadoPedido;
import com.example.pedidos.entity.LineaPedido;
import com.example.pedidos.entity.Pedido;
import com.example.pedidos.repository.ClienteRepository;
import com.example.pedidos.repository.PedidoRepository;

@Configuration
public class DatosPrueba {

    @Bean
    CommandLineRunner inicializarDatos(
            ClienteRepository clienteRepository,
            PedidoRepository pedidoRepository) {

        return args -> {

            if (clienteRepository.findAll().isEmpty()) {

                Cliente cliente = clienteRepository.save(
                    new Cliente(
                        "Cliente de prueba",
                        "prueba@empresa.com"
                    )
                );

                Pedido pedido = new Pedido();

                pedido.setCliente(cliente);
                pedido.setEstado(EstadoPedido.CREATED);
                pedido.setFecha(LocalDateTime.now());
                pedido.setImporteProductos(
                    new BigDecimal("49.90")
                );

                LineaPedido linea = new LineaPedido();
                linea.setProducto("Teclado");
                linea.setCantidad(2);
                linea.setPrecioUnitario(
                    new BigDecimal("24.95")
                );

                pedido.addLinea(linea);

                pedidoRepository.save(pedido);
            }
        };
    }
}
