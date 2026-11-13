package com.tp.facturacionapi.repositorio;

import com.tp.facturacionapi.modelo.ListaPrecio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ListaPrecioRepositorio extends JpaRepository<ListaPrecio, Long> {
}
