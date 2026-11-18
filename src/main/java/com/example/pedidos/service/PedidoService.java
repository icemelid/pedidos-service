
package com.example.pedidos.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.example.pedidos.client.ProveedorClient;
import com.example.pedidos.dto.PedidoResponse;
import com.example.pedidos.entity.EstadoPedido;
import com.example.pedidos.entity.Pedido;
import com.example.pedidos.repository.PedidoRepository;

@Service
public class PedidoService {

    private static final int MAX_INTENTOS = 3;
    private static final long ESPERA_REINTENTO_MS = 500;

    private final PedidoRepository pedidoRepository;
    private final ProveedorClient proveedorClient;
    private final TransactionTemplate transactionTemplate;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ProveedorClient proveedorClient,
            PlatformTransactionManager transactionManager) {

        this.pedidoRepository = pedidoRepository;
        this.proveedorClient = proveedorClient;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorCliente(Long clienteId) {

        List<Pedido> pedidos =
                pedidoRepository.findByClienteId(clienteId);

        return pedidos.stream()
                .map(this::convertirAResponse)
                .toList();
    }

    public PedidoResponse confirmarPedido(Long pedidoId) {

        // 1. Reservamos el pedido en una transacción corta.
        if (!reservarConfirmacion(pedidoId)) {
            throw new IllegalStateException(
                    "El pedido no existe o ya está siendo confirmado."
            );
        }

        try {
            // 2. Contactamos con el proveedor fuera de la transacción.
            BigDecimal costeEnvio =
                    calcularCosteEnvioConReintentos(pedidoId);

            // 3. Guardamos el resultado en otra transacción.
            return transactionTemplate.execute(status -> {

                Pedido pedido = pedidoRepository.findById(pedidoId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No se encuentra el pedido."
                                )
                        );

                pedido.setCosteEnvio(costeEnvio);

                pedido.setImporteTotal(
                        pedido.getImporteProductos().add(costeEnvio)
                );

                pedido.setEstado(EstadoPedido.CONFIRMED);

                Pedido guardado = pedidoRepository.save(pedido);

                return convertirAResponse(guardado);
            });

        } catch (RuntimeException ex) {

            // 4. Si falla la confirmación, registramos el fallo.
            transactionTemplate.execute(status -> {

                Pedido pedido = pedidoRepository.findById(pedidoId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No se encuentra el pedido."
                                )
                        );

                if (pedido.getEstado() == EstadoPedido.CONFIRMING) {
                    pedido.setEstado(
                            EstadoPedido.CONFIRMATION_FAILED
                    );
                    pedidoRepository.save(pedido);
                }

                return null;
            });

            throw ex;
        }
    }

    public boolean reservarConfirmacion(Long pedidoId) {

        Integer actualizados = transactionTemplate.execute(status ->
                pedidoRepository.actualizarEstadoSiCoincide(
                        pedidoId,
                        EstadoPedido.CREATED,
                        EstadoPedido.CONFIRMING
                )
        );

        return actualizados != null && actualizados == 1;
    }

    private BigDecimal calcularCosteEnvioConReintentos(Long pedidoId) {

        RuntimeException ultimoError = null;

        for (int intento = 1; intento <= MAX_INTENTOS; intento++) {

            try {
                Pedido pedido = pedidoRepository.findById(pedidoId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "No se encuentra el pedido."
                                )
                        );

                return proveedorClient.calcularCosteEnvio(
                        pedidoId,
                        pedido.getImporteProductos()
                );

            } catch (RuntimeException ex) {

                ultimoError = ex;

                if (intento < MAX_INTENTOS) {
                    try {
                        Thread.sleep(ESPERA_REINTENTO_MS);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(
                                "Se interrumpió la espera del reintento.",
                                interrupted
                        );
                    }
                }
            }
        }

        throw new IllegalStateException(
                "El proveedor no ha podido confirmar el pedido.",
                ultimoError
        );
    }

    private PedidoResponse convertirAResponse(Pedido pedido) {

        PedidoResponse response = new PedidoResponse();

        response.setId(pedido.getId());
        response.setClienteId(pedido.getCliente().getId());
        response.setEstado(pedido.getEstado());
        response.setFecha(pedido.getFecha());
        response.setImporteProductos(pedido.getImporteProductos());
        response.setCosteEnvio(pedido.getCosteEnvio());
        response.setImporteTotal(pedido.getImporteTotal());

        return response;
    }
}
