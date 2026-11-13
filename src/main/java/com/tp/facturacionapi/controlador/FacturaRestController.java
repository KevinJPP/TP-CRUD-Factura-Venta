package com.tp.facturacionapi.controlador;

import com.tp.facturacionapi.modelo.FacturaComprobanteDTO;
import com.tp.facturacionapi.modelo.FacturaReporteDTO;
import com.tp.facturacionapi.modelo.PaginaDTO;
import com.tp.facturacionapi.modelo.ResumenFacturasDTO;
import com.tp.facturacionapi.modelo.FacturaRequestDTO;
import com.tp.facturacionapi.repositorio.FacturaVentaRepositorio;
import com.tp.facturacionapi.servicio.FacturaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaRestController {

    @Autowired
    private FacturaVentaRepositorio facturaVentaRepositorio;

    @Autowired
    private FacturaServicio facturaServicio;

    private static final int TAMANO_MAXIMO = 100; // tope para que nadie pida "size=1000000"

    // Paginado en la base de datos: solo se leen las filas de la página pedida.
    // page es base 0 (igual que Spring Data y que el frontend).
    @GetMapping
    public PaginaDTO<FacturaReporteDTO> listarFacturasFiltradas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String cliente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        String estadoFiltro = vacioANulo(estado);
        String clienteFiltro = vacioANulo(cliente);
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), TAMANO_MAXIMO));

        Page<FacturaReporteDTO> resultado = facturaVentaRepositorio.filtrarFacturas(
                fechaDesde, fechaHasta, estadoFiltro, montoMinimo, id, clienteFiltro, pageable);
        ResumenFacturasDTO resumen = facturaVentaRepositorio.resumirFacturas(
                fechaDesde, fechaHasta, estadoFiltro, montoMinimo, id, clienteFiltro);

        return new PaginaDTO<>(resultado.getContent(), resultado.getTotalElements(), resultado.getTotalPages(), resumen);
    }

    private static String vacioANulo(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // Comprobante completo (cabecera + ítems + totales) para verlo e imprimirlo
    @GetMapping("/{id}")
    public ResponseEntity<FacturaComprobanteDTO> obtenerComprobante(@PathVariable Long id) {
        return ResponseEntity.ok(facturaServicio.obtenerComprobante(id));
    }

    @PostMapping
    public ResponseEntity<FacturaReporteDTO> crearFactura(@RequestBody FacturaRequestDTO requestDTO) {
        FacturaReporteDTO nuevaFactura = facturaServicio.crearFactura(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaFactura);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacturaReporteDTO> actualizarFactura(@PathVariable Long id, @RequestBody FacturaRequestDTO requestDTO) {
        FacturaReporteDTO facturaActualizada = facturaServicio.actualizarFactura(id, requestDTO);
        return ResponseEntity.ok(facturaActualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Long id) {
        facturaServicio.eliminarFactura(id);
        return ResponseEntity.noContent().build();
    }
}