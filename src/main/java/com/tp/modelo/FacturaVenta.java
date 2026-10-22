package com.tp.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "facturas_venta")
public class FacturaVenta extends AuditoriaApp {

    private Long numero;

    @Column(nullable = false)
    private LocalDateTime fechaEmision;

    @ManyToOne
    @JoinColumn(name = "punto_venta_id", nullable = false)
    private PuntoVenta puntoVenta;

    private double importeCobrado;
    private double importeSaldo;

    @Column(nullable = false)
    private double importeTotal;

    private String cae;
    private LocalDateTime caeFechaVencimiento;
    private String resultadoAfip;
    private String motivoRechazo;

    @Column(nullable = false)
    private String estado;

    private LocalDateTime fechaAnulacion;
    private String observaciones;

    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacturaVentaDetalle> detalles = new ArrayList<>();

    public void addDetalle(FacturaVentaDetalle detalle) {
        detalles.add(detalle);
        detalle.setFactura(this);
    }

    public Long getNumero() { return numero; }
    public void setNumero(Long numero) { this.numero = numero; }

    public LocalDateTime getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(LocalDateTime fechaEmision) { this.fechaEmision = fechaEmision; }

    public PuntoVenta getPuntoVenta() { return puntoVenta; }
    public void setPuntoVenta(PuntoVenta puntoVenta) { this.puntoVenta = puntoVenta; }

    public double getImporteCobrado() { return importeCobrado; }
    public void setImporteCobrado(double importeCobrado) { this.importeCobrado = importeCobrado; }

    public double getImporteSaldo() { return importeSaldo; }
    public void setImporteSaldo(double importeSaldo) { this.importeSaldo = importeSaldo; }

    public double getImporteTotal() { return importeTotal; }
    public void setImporteTotal(double importeTotal) { this.importeTotal = importeTotal; }

    public String getCae() { return cae; }
    public void setCae(String cae) { this.cae = cae; }

    public LocalDateTime getCaeFechaVencimiento() { return caeFechaVencimiento; }
    public void setCaeFechaVencimiento(LocalDateTime caeFechaVencimiento) { this.caeFechaVencimiento = caeFechaVencimiento; }

    public String getResultadoAfip() { return resultadoAfip; }
    public void setResultadoAfip(String resultadoAfip) { this.resultadoAfip = resultadoAfip; }

    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivoRechazo) { this.motivoRechazo = motivoRechazo; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(LocalDateTime fechaAnulacion) { this.fechaAnulacion = fechaAnulacion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public List<FacturaVentaDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<FacturaVentaDetalle> detalles) { this.detalles = detalles; }
}
