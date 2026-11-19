package com.example.pedidos.client;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.concurrent.ExecutionException;

@Component
public class ProveedorClientLentoSimulado implements ProveedorClient {

    private final AtomicInteger contadorLlamadas =
        new AtomicInteger(0);

    private final AtomicInteger contadorProcesamientos =
        new AtomicInteger(0);

    private final ConcurrentMap<Long, CompletableFuture<BigDecimal>> operaciones =
        new ConcurrentHashMap<>();

    private final ExecutorService executorProveedor =
        Executors.newFixedThreadPool(2);        

    @Override
    public BigDecimal calcularCosteEnvio(
            Long pedidoId,
            BigDecimal importeProductos) {

        int llamada = contadorLlamadas.incrementAndGet();

        System.out.println(
            ">>> LLAMADA Nº " + llamada
            + " PARA PEDIDO " + pedidoId
        );

        CompletableFuture<BigDecimal> operacion =
                operaciones.computeIfAbsent(
                        pedidoId,
                        id -> CompletableFuture.supplyAsync(() -> {

                            int procesamiento =
                                    contadorProcesamientos.incrementAndGet();

                            System.out.println(
                                ">>> PROCESAMIENTO Nº " + procesamiento
                                + " PARA PEDIDO " + id
                            );

                            try {
                                Thread.sleep(7000);
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt();
                                throw new IllegalStateException(
                                    "Se interrumpió la operación del proveedor.",
                                    ex
                                );
                            }

                            return new BigDecimal("5.95");

                        }, executorProveedor)
                );

        try {
            return operacion.get();

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Se interrumpió la espera del proveedor.", ex
            );

        } catch (ExecutionException ex) {
            throw new IllegalStateException(
                "Error procesando la operación del proveedor.",
                ex.getCause()
            );
        }
    }
}