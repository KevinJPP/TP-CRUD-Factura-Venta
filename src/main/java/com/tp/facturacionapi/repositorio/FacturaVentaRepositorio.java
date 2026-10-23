package com.tp.facturacionapi.repositorio;

import com.tp.facturacionapi.modelo.FacturaReporteDTO;
import com.tp.facturacionapi.modelo.FacturaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FacturaVentaRepositorio extends JpaRepository<FacturaVenta, Long> {

    @Query("SELECT new com.tp.facturacionapi.modelo.FacturaReporteDTO(f.id, f.fechaEmision, f.estado, f.importeTotal, f.cliente.denominacion) " +
            "FROM FacturaVenta f WHERE " +
            "(:fechaDesde IS NULL OR f.fechaEmision >= :fechaDesde) AND " +
            "(:fechaHasta IS NULL OR f.fechaEmision <= :fechaHasta) AND " +
            "(:estado IS NULL OR f.estado = :estado) AND " +
            "(:montoMinimo IS NULL OR f.importeTotal >= :montoMinimo)")
    List<FacturaReporteDTO> filtrarFacturas(
            @Param("fechaDesde") LocalDateTime fechaDesde,
            @Param("fechaHasta") LocalDateTime fechaHasta,
            @Param("estado") String estado,
            @Param("montoMinimo") Double montoMinimo
    );
}