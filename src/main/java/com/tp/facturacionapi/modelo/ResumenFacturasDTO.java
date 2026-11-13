package com.tp.facturacionapi.modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Totales de TODOS los comprobantes que cumplen los filtros (no solo los de la página visible).
 * Espeja ResumenFacturasDTO del frontend.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResumenFacturasDTO {
    private Long anuladas;
    private Double importeVigente;
}
