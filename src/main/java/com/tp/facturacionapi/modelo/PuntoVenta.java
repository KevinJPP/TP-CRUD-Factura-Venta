package com.tp.facturacionapi.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "puntos_venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PuntoVenta extends AuditoriaApp {

    @Column(nullable = false)
    private int numero;

    private String descripcion;
    private String tipoEmision;
    private String domicilioComercial;

}