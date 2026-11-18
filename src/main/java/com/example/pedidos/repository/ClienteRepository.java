package com.example.pedidos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.pedidos.entity.Cliente;

@Repository
public interface ClienteRepository
        extends JpaRepository<Cliente, Long> {

}