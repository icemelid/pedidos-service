package com.example.pedidos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.pedidos.entity.Pedido;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.pedidos.entity.EstadoPedido;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

        List<Pedido> findByClienteId(Long clienteId);        

        @Modifying
        @Query("""
        UPDATE Pedido p
        SET p.estado = :nuevoEstado
        WHERE p.id = :pedidoId
        AND p.estado = :estadoActual
        """)
        int actualizarEstadoSiCoincide(
                @Param("pedidoId") Long pedidoId,
                @Param("estadoActual") EstadoPedido estadoActual,
                @Param("nuevoEstado") EstadoPedido nuevoEstado
        );        

}