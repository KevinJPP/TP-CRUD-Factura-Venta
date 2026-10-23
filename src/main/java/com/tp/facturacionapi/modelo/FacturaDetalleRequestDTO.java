package com.tp.facturacionapi.modelo;

public class FacturaDetalleRequestDTO {
    private Long articuloId;
    private Double cantidad;
    private Double precioUnitario;

    public FacturaDetalleRequestDTO() {
    }

    public FacturaDetalleRequestDTO(Long articuloId, Double cantidad, Double precioUnitario) {
        this.articuloId = articuloId;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Long getArticuloId() {
        return articuloId;
    }

    public void setArticuloId(Long articuloId) {
        this.articuloId = articuloId;
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
}