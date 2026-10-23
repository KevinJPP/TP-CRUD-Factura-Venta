package com.tp.facturacionapi.controlador;

import com.tp.facturacionapi.modelo.FacturaReporteDTO;
import com.tp.facturacionapi.repositorio.FacturaVentaRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/facturas")
public class FacturaRestController {

    @Autowired
    private FacturaVentaRepositorio facturaVentaRepositorio;

    @GetMapping
    public List<FacturaReporteDTO> listarFacturasFiltradas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) Double montoMinimo) {

        return facturaVentaRepositorio.filtrarFacturas(fechaDesde, fechaHasta, estado, montoMinimo);
    }
}