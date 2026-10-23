package com.tp.facturacionapi.modelo;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "factura_venta_detalles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FacturaVentaDetalle extends AuditoriaApp {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "factura_venta_id", nullable = false)
    private FacturaVenta facturaVenta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lista_precio_articulo_id", nullable = false)
    private ListaPrecioArticulo listaPrecioArticulo;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private double precioUnitario;

    @Column(nullable = false)
    private double importeSubtotal;

    private String descripcion;
}