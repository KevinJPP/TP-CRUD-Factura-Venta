package com.tp.facturacionapi.servicio;

import com.tp.facturacionapi.modelo.*;
import com.tp.facturacionapi.repositorio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FacturaServicio {

    @Autowired
    private FacturaVentaRepositorio facturaVentaRepositorio;

    @Autowired
    private ClienteRepositorio clienteRepositorio;

    @Autowired
    private ArticuloRepositorio articuloRepositorio;

    @Transactional
    public FacturaReporteDTO crearFactura(FacturaRequestDTO dto) {
        Cliente cliente = clienteRepositorio.findById(dto.getClienteId())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + dto.getClienteId()));

        FacturaVenta factura = new FacturaVenta();
        factura.setNumero(System.currentTimeMillis());
        factura.setFechaEmision(dto.getFechaEmision() != null ? dto.getFechaEmision() : LocalDateTime.now());
        factura.setEstado(dto.getEstado() != null ? dto.getEstado() : "EMITIDA");
        factura.setCliente(cliente);

        PuntoVenta puntoVenta = new PuntoVenta();
        puntoVenta.setId(1L);
        factura.setPuntoVenta(puntoVenta);

        CondicionIva condicionIva = new CondicionIva();
        condicionIva.setId(1L);
        factura.setCondicionIva(condicionIva);

        TipoMoneda tipoMoneda = new TipoMoneda();
        tipoMoneda.setId(1L);
        factura.setTipoMoneda(tipoMoneda);

        double subtotalNeto = 0.0;
        double totalIva = 0.0;
        List<FacturaVentaDetalle> detalles = new ArrayList<>();

        if (dto.getDetalles() != null) {
            for (FacturaDetalleRequestDTO itemDto : dto.getDetalles()) {
                Articulo articulo = articuloRepositorio.findById(itemDto.getArticuloId())
                        .orElseThrow(() -> new RuntimeException("Artículo no encontrado con ID: " + itemDto.getArticuloId()));

                FacturaVentaDetalle detalle = new FacturaVentaDetalle();
                detalle.setArticulo(articulo);
                detalle.setCantidad(itemDto.getCantidad());
                detalle.setPrecioUnitario(itemDto.getPrecioUnitario());

                double subtotalItem = itemDto.getCantidad() * itemDto.getPrecioUnitario();
                subtotalNeto += subtotalItem;
                double ivaItem = subtotalItem * 0.21;
                totalIva += ivaItem;

                detalle.setFacturaVenta(factura);
                detalles.add(detalle);
            }
        }

        factura.setDetalles(detalles);
        double importeTotal = subtotalNeto + totalIva;
        factura.setImporteTotal(importeTotal);

        factura.setImporteCobrado(0.0);
        factura.setImporteSaldo(importeTotal);

        FacturaVenta facturaGuardada = facturaVentaRepositorio.save(factura);

        return new FacturaReporteDTO(
                facturaGuardada.getId(),
                facturaGuardada.getFechaEmision(),
                facturaGuardada.getEstado(),
                facturaGuardada.getImporteTotal(),
                cliente.getDenominacion()
        );
    }

    @Transactional
    public FacturaReporteDTO actualizarFactura(Long id, FacturaRequestDTO dto) {
        FacturaVenta factura = facturaVentaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + id));

        if (dto.getFechaEmision() != null) {
            factura.setFechaEmision(dto.getFechaEmision());
        }
        if (dto.getEstado() != null) {
            factura.setEstado(dto.getEstado());
        }
        if (dto.getClienteId() != null) {
            Cliente cliente = clienteRepositorio.findById(dto.getClienteId())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado con ID: " + dto.getClienteId()));
            factura.setCliente(cliente);
        }

        FacturaVenta facturaActualizada = facturaVentaRepositorio.save(factura);

        return new FacturaReporteDTO(
                facturaActualizada.getId(),
                facturaActualizada.getFechaEmision(),
                facturaActualizada.getEstado(),
                facturaActualizada.getImporteTotal(),
                facturaActualizada.getCliente().getDenominacion()
        );
    }

    @Transactional
    public void eliminarFactura(Long id) {
        FacturaVenta factura = facturaVentaRepositorio.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + id));

        factura.setEstado("ANULADA");
        facturaVentaRepositorio.save(factura);
    }
}