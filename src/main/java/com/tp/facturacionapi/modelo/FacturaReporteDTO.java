package com.tp.facturacionapi.modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacturaReporteDTO {
    private Long id;
    private LocalDateTime fechaEmision;
    private String estado;
    private Double importeTotal;
    private String clienteDenominacion;
}