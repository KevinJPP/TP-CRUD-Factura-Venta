package com.tp.facturacionapi.repositorio;

import com.tp.facturacionapi.modelo.FacturaReporteDTO;
import com.tp.facturacionapi.modelo.FacturaVenta;
import com.tp.facturacionapi.modelo.ResumenFacturasDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface FacturaVentaRepositorio extends JpaRepository<FacturaVenta, Long> {

    // Filtros compartidos por la consulta de la página, el conteo y el resumen.
    // LEFT JOIN: un comprobante sin cliente no debe desaparecer del listado.
    String FILTROS =
            "WHERE (:fechaDesde IS NULL OR f.fechaEmision >= :fechaDesde) " +
            "AND (:fechaHasta IS NULL OR f.fechaEmision <= :fechaHasta) " +
            "AND (:estado IS NULL OR f.estado = :estado) " +
            "AND (:montoMinimo IS NULL OR f.importeTotal >= :montoMinimo) " +
            "AND (:id IS NULL OR f.id = :id) " +
            "AND (:cliente IS NULL OR LOWER(c.denominacion) LIKE LOWER(CONCAT('%', :cliente, '%')))";

    // Solo trae UNA página (LIMIT/OFFSET en SQL), nunca la tabla completa.
    // El orden es obligatorio para que las páginas sean estables: más nuevos primero.
    @Query(value = "SELECT new com.tp.facturacionapi.modelo.FacturaReporteDTO(f.id, f.fechaEmision, f.estado, f.importeTotal, c.denominacion) " +
            "FROM FacturaVenta f LEFT JOIN f.cliente c " + FILTROS + " ORDER BY f.id DESC",
            countQuery = "SELECT COUNT(f) FROM FacturaVenta f LEFT JOIN f.cliente c " + FILTROS)
    Page<FacturaReporteDTO> filtrarFacturas(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("estado") String estado,
            @Param("montoMinimo") Double montoMinimo,
            @Param("id") Long id,
            @Param("cliente") String cliente,
            Pageable pageable
    );

    // Totales de todos los resultados filtrados, calculados por la base de datos (una sola fila).
    @Query("SELECT new com.tp.facturacionapi.modelo.ResumenFacturasDTO(" +
            "COUNT(CASE WHEN f.estado = 'ANULADA' THEN 1 END), " +
            "COALESCE(SUM(CASE WHEN f.estado <> 'ANULADA' THEN f.importeTotal ELSE 0.0 END), 0.0)) " +
            "FROM FacturaVenta f LEFT JOIN f.cliente c " + FILTROS)
    ResumenFacturasDTO resumirFacturas(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("estado") String estado,
            @Param("montoMinimo") Double montoMinimo,
            @Param("id") Long id,
            @Param("cliente") String cliente
    );
}
