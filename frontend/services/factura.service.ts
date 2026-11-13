import {
    ArticuloDTO, ClienteDTO, ClienteRequestDTO, FacturaComprobanteDTO, FacturaFiltrosDTO, FacturaReporteDTO, FacturaRequestDTO, PaginaDTO
} from '../models/factura.dto.js';

export class FacturaService {
    private readonly baseUrl = 'http://localhost:8080/api';

    private async request<T>(path: string, init?: RequestInit): Promise<T> {
        let res: Response;
        try {
            res = await fetch(`${this.baseUrl}${path}`, init);
        } catch {
            throw new Error('No se pudo conectar con el servidor (¿está corriendo en el puerto 8080?).');
        }
        if (!res.ok) {
            let detalle = '';
            try { detalle = (await res.json()).message ?? ''; } catch { /* sin cuerpo JSON */ }
            throw new Error(detalle || `El servidor respondió ${res.status} ${res.statusText}`.trim());
        }
        return res.status === 204 ? (undefined as T) : res.json();
    }

    private json(method: string, body: unknown): RequestInit {
        return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
    }

    // Devuelve una página (backend nuevo) o el listado completo (backend viejo)
    obtenerFacturas(filtros: FacturaFiltrosDTO = {}): Promise<PaginaDTO<FacturaReporteDTO> | FacturaReporteDTO[]> {
        const params = new URLSearchParams();
        Object.entries(filtros).forEach(([k, v]) => {
            if (v !== undefined && v !== null && v !== '') params.append(k, String(v));
        });
        const qs = params.toString();
        return this.request(`/facturas${qs ? `?${qs}` : ''}`);
    }

    // Comprobante completo (cabecera + ítems + totales) para ver/imprimir
    obtenerComprobante(id: number): Promise<FacturaComprobanteDTO> {
        return this.request(`/facturas/${id}`);
    }

    obtenerArticulos(): Promise<ArticuloDTO[]> {
        return this.request('/articulos');
    }

    obtenerClientes(): Promise<ClienteDTO[]> {
        return this.request('/clientes');
    }

    crearCliente(cliente: ClienteRequestDTO): Promise<ClienteDTO> {
        return this.request('/clientes', this.json('POST', cliente));
    }

    crearFactura(factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
        return this.request('/facturas', this.json('POST', factura));
    }

    actualizarFactura(id: number, factura: FacturaRequestDTO): Promise<FacturaReporteDTO> {
        return this.request(`/facturas/${id}`, this.json('PUT', factura));
    }

    actualizarEstado(id: number, estado: string): Promise<FacturaReporteDTO> {
        return this.actualizarFactura(id, { estado });
    }

    // El backend hace una anulación lógica (estado = "ANULADA")
    eliminarFactura(id: number): Promise<void> {
        return this.request(`/facturas/${id}`, { method: 'DELETE' });
    }
}
