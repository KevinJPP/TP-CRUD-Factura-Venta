package com.tp.facturacionapi.modelo;

import jakarta.persistence.*;

@Entity
@Table(name = "factura_venta_detalle")
public class FacturaVentaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "articulo_id")
    private Articulo articulo;

    private Double cantidad;
    // Copia del precio al momento de facturar (no cambia aunque la lista se actualice)
    private Double precioUnitario;

    // Precio de lista del que salió este renglón (trazabilidad)
    @ManyToOne
    @JoinColumn(name = "lista_precio_articulo_id")
    private ListaPrecioArticulo listaPrecioArticulo;

    @ManyToOne
    @JoinColumn(name = "factura_venta_id")
    private FacturaVenta facturaVenta;

    public FacturaVentaDetalle() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Articulo getArticulo() {
        return articulo;
    }

    public void setArticulo(Articulo articulo) {
        this.articulo = articulo;
    }

    public Double getCantidad() {
        return cantidad;
    }

    public void setCantidad(Double cantidad) {
        this.cantidad = cantidad;
    }

    public Double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(Double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public ListaPrecioArticulo getListaPrecioArticulo() {
        return listaPrecioArticulo;
    }

    public void setListaPrecioArticulo(ListaPrecioArticulo listaPrecioArticulo) {
        this.listaPrecioArticulo = listaPrecioArticulo;
    }

    public FacturaVenta getFacturaVenta() {
        return facturaVenta;
    }

    public void setFacturaVenta(FacturaVenta facturaVenta) {
        this.facturaVenta = facturaVenta;
    }
}