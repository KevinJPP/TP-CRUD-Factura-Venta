package com.tp.modelo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class AuditoriaApp extends EntityId {

    @Column(nullable = false)
    protected LocalDateTime fechaAlta;

    protected LocalDateTime fechaBaja;

    @Column(nullable = false)
    protected LocalDateTime fechaModificacion;

    @ManyToOne
    @JoinColumn(name = "usuario_carga_id", nullable = false)
    protected Usuario usuarioCarga;

    @ManyToOne
    @JoinColumn(name = "usuario_baja_id")
    protected Usuario usuarioBaja;

    @ManyToOne
    @JoinColumn(name = "usuario_modificacion_id", nullable = false)
    protected Usuario usuarioModificacion;

    public LocalDateTime getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(LocalDateTime fechaAlta) { this.fechaAlta = fechaAlta; }

    public LocalDateTime getFechaBaja() { return fechaBaja; }
    public void setFechaBaja(LocalDateTime fechaBaja) { this.fechaBaja = fechaBaja; }

    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }

    public Usuario getUsuarioCarga() { return usuarioCarga; }
    public void setUsuarioCarga(Usuario usuarioCarga) { this.usuarioCarga = usuarioCarga; }

    public Usuario getUsuarioBaja() { return usuarioBaja; }
    public void setUsuarioBaja(Usuario usuarioBaja) { this.usuarioBaja = usuarioBaja; }

    public Usuario getUsuarioModificacion() { return usuarioModificacion; }
    public void setUsuarioModificacion(Usuario usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }
}