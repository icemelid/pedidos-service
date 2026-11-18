package com.example.pedidos.dto;

import com.example.pedidos.entity.EstadoPedido;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class PedidoResponse {

    private Long id;

    private Long clienteId;

    private EstadoPedido estado;

    private LocalDateTime fecha;

    private BigDecimal importeProductos;

    private BigDecimal costeEnvio;

    private BigDecimal importeTotal;


    public PedidoResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getImporteProductos() {
        return importeProductos;
    }

    public void setImporteProductos(BigDecimal importeProductos) {
        this.importeProductos = importeProductos;
    }

    public BigDecimal getCosteEnvio() {
        return costeEnvio;
    }

    public void setCosteEnvio(BigDecimal costeEnvio) {
        this.costeEnvio = costeEnvio;
    }

    public BigDecimal getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }

}