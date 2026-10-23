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
