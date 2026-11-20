package com.tp.facturacionapi.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Cliente extends EntityId {

    @Column(nullable = false)
    private String denominacion;

    private String cuit;

    // Responsable Inscripto, Monotributista, Consumidor Final, Exento...
    private String condicionIva;

    private String email;

    private String telefono;

    private String domicilio;

    private String localidad;

    private String provincia;

    private String codigoPostal;

    @Column(length = 500)
    private String observaciones;
}
