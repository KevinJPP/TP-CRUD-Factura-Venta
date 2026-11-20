package com.tp.facturacionapi.modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Comprobante completo (cabecera + ítems + totales) para ver e imprimir una factura.
 * Lo devuelve GET /api/facturas/{id}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaComprobanteDTO {

    private Long id;
    private Long numero;
    private Integer puntoVentaNumero;
    private String puntoVentaDomicilio;
    private LocalDateTime fechaEmision;
    private String estado;
    private String condicionIva;
    private String moneda;
    private String cae;
    private LocalDateTime caeFechaVencimiento;

    private String clienteDenominacion;
    private String clienteCuit;
    private String clienteDomicilio;

    private List<Item> items;

    private Double importeNeto;
    private Double importeIva;
    private Double importeTotal;

    // Resultado del pedido de CAE a ARCA (para saber por qué un comprobante quedó sin CAE)
    private String resultadoAfip;
    private String motivoRechazo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private Long articuloId;
        private String codigo;
        private String descripcion;
        private Double cantidad;
        private Double precioUnitario;
        private Double subtotal;
    }
}
