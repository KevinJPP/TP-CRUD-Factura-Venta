package com.tp.facturacionapi.modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacturaRequestDTO {
    private LocalDateTime fechaEmision;
    private String estado;
    private Long clienteId;
    private Long condicionIvaId;
    private Long puntoVentaId;
    private Long tipoMonedaId;
    private List<FacturaDetalleRequestDTO> detalles;
}