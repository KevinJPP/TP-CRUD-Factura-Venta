package com.tp.facturacionapi.modelo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Respuesta paginada de GET /api/facturas?page=&size=
 * Espeja PaginaDTO del frontend.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginaDTO<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private ResumenFacturasDTO resumen;
}
