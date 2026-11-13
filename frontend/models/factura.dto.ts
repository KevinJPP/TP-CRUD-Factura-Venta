export type EstadoFactura = 'EMITIDA' | 'ANULADA' | string;

export interface FacturaDetalleRequestDTO {
    articuloId: number;
    cantidad: number;
    precioUnitario: number;
}

export interface FacturaRequestDTO {
    fechaEmision?: string; // ISO: "2026-10-23T00:00:00"
    estado?: EstadoFactura;
    clienteId?: number;
    puntoVentaId?: number;
    condicionIvaId?: number;
    tipoMonedaId?: number;
    detalles?: FacturaDetalleRequestDTO[];
}

export interface FacturaReporteDTO {
    id: number;
    fechaEmision: string;
    estado: EstadoFactura;
    importeTotal: number;
    clienteDenominacion: string;
}

// Espeja los @RequestParam de GET /api/facturas
export interface FacturaFiltrosDTO {
    fechaDesde?: string;
    fechaHasta?: string;
    estado?: string;
    montoMinimo?: number;
    id?: number | string;  // N.º de comprobante
    cliente?: string;      // texto contenido en la denominación del cliente
    page?: number;         // número de página, base 0
    size?: number;         // filas por página
}

// Totales de TODOS los resultados filtrados (no solo de la página visible)
export interface ResumenFacturasDTO {
    anuladas: number;
    importeVigente: number;
}

// Respuesta paginada de GET /api/facturas?page=&size=
export interface PaginaDTO<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    resumen?: ResumenFacturasDTO;
}

// Espeja la entidad Cliente que devuelve GET /api/clientes
export interface ClienteDTO {
    id: number;
    denominacion: string;
    cuit?: string;
    domicilio?: string;
}

// Cuerpo de POST /api/clientes
export interface ClienteRequestDTO {
    denominacion: string;
    cuit?: string;
    domicilio?: string;
}

// Espeja ArticuloDTO de GET /api/articulos (artículo + precio de venta vigente)
export interface ArticuloDTO {
    id: number;
    codigo: string;
    denominacion: string;
    precio: number;
}

// Comprobante completo que devuelve GET /api/facturas/{id}
export interface FacturaComprobanteItemDTO {
    articuloId: number | null;
    codigo: string | null;
    descripcion: string;
    cantidad: number;
    precioUnitario: number;
    subtotal: number;
}

export interface FacturaComprobanteDTO {
    id: number;
    numero: number;
    puntoVentaNumero: number | null;
    puntoVentaDomicilio: string | null;
    fechaEmision: string | null;
    estado: EstadoFactura;
    condicionIva: string | null;
    moneda: string | null;
    cae: string | null;
    caeFechaVencimiento: string | null;
    clienteDenominacion: string | null;
    clienteCuit: string | null;
    clienteDomicilio: string | null;
    items: FacturaComprobanteItemDTO[];
    importeNeto: number;
    importeIva: number;
    importeTotal: number;
}
