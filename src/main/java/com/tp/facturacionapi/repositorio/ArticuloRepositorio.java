package com.tp.facturacionapi.repositorio;

import com.tp.facturacionapi.modelo.Articulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticuloRepositorio extends JpaRepository<Articulo, Long> {
}