export class FacturaService {
    constructor() {
        this.baseUrl = 'http://localhost:8080/api';
    }
    async request(path, init) {
        let res;
        try {
            res = await fetch(`${this.baseUrl}${path}`, init);
        }
        catch {
            throw new Error('No se pudo conectar con el servidor (¿está corriendo en el puerto 8080?).');
        }
        if (!res.ok) {
            let detalle = '';
            try {
                detalle = (await res.json()).message ?? '';
            }
            catch { /* sin cuerpo JSON */ }
            throw new Error(detalle || `El servidor respondió ${res.status} ${res.statusText}`.trim());
        }
        return res.status === 204 ? undefined : res.json();
    }
    json(method, body) {
        return { method, headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) };
    }
    obtenerFacturas(filtros = {}) {
        const params = new URLSearchParams();
        Object.entries(filtros).forEach(([k, v]) => {
            if (v !== undefined && v !== null && v !== '')
                params.append(k, String(v));
        });
        const qs = params.toString();
        return this.request(`/facturas${qs ? `?${qs}` : ''}`);
    }
    obtenerClientes() {
        return this.request('/clientes');
    }
    crearCliente(cliente) {
        return this.request('/clientes', this.json('POST', cliente));
    }
    crearFactura(factura) {
        return this.request('/facturas', this.json('POST', factura));
    }
    actualizarFactura(id, factura) {
        return this.request(`/facturas/${id}`, this.json('PUT', factura));
    }
    actualizarEstado(id, estado) {
        return this.actualizarFactura(id, { estado });
    }
    // El backend hace una anulación lógica (estado = "ANULADA")
    eliminarFactura(id) {
        return this.request(`/facturas/${id}`, { method: 'DELETE' });
    }
}
