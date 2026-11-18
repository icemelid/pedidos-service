
package com.example.pedidos.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal importeProductos;

    @Column(precision = 12, scale = 2)
    private BigDecimal costeEnvio;

    @Column(precision = 12, scale = 2)
    private BigDecimal importeTotal;

    @OneToMany(
        mappedBy = "pedido",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<LineaPedido> lineas = new ArrayList<>();

    public Pedido() {
    }

    // Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
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

    public List<LineaPedido> getLineas() {
        return lineas;
    }    

    public void addLinea(LineaPedido linea) {
    lineas.add(linea);
    linea.setPedido(this);
    }

    public void removeLinea(LineaPedido linea) {
        lineas.remove(linea);
        linea.setPedido(null);
    }


}
