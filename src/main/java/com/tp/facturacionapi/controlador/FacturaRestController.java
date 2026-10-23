package com.tp.facturacionapi.controlador;

import com.tp.facturacionapi.modelo.FacturaReporteDTO;
import com.tp.facturacionapi.modelo.FacturaRequestDTO;
import com.tp.facturacionapi.repositorio.FacturaVentaRepositorio;
import com.tp.facturacionapi.servicio.FacturaServicio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@CrossOrigin(origins = "*")
public class FacturaRestController {

    @Autowired
    private FacturaVentaRepositorio facturaVentaRepositorio;

    @Autowired
    private FacturaServicio facturaServicio;

    @GetMapping
    public List<FacturaReporteDTO> listarFacturasFiltradas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) {

        return facturaVentaRepositorio.filtrarFacturas(fechaDesde, fechaHasta, estado, montoMinimo);
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