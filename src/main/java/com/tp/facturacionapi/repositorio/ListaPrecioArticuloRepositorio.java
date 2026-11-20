package com.tp.facturacionapi.repositorio;

import com.tp.facturacionapi.modelo.ListaPrecioArticulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListaPrecioArticuloRepositorio extends JpaRepository<ListaPrecioArticulo, Long> {

    // Ordenados por id: si un artículo tiene varios precios, el último cargado es el vigente
    List<ListaPrecioArticulo> findAllByOrderByIdAsc();

    // Precio vigente de un artículo: el último cargado
    Optional<ListaPrecioArticulo> findFirstByArticuloIdOrderByIdDesc(Long articuloId);
}
