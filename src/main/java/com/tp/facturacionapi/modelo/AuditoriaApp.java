package com.tp.facturacionapi.modelo;

import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class AuditoriaApp extends EntityId {

    private LocalDateTime fechaAlta;
    private LocalDateTime fechaModificacion;

    @ManyToOne
    @JoinColumn(name = "usuario_carga_id")
    private Usuario usuarioCarga;

    @ManyToOne
    @JoinColumn(name = "usuario_modificacion_id")
    private Usuario usuarioModificacion;

    @PrePersist
    public void prePersist() {
        if (this.fechaAlta == null) {
            this.fechaAlta = LocalDateTime.now();
        }
        if (this.fechaModificacion == null) {
            this.fechaModificacion = LocalDateTime.now();
        }
    }
}