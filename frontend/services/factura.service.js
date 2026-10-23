var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
export class FacturaService {
    constructor() {
        this.apiUrl = 'http://localhost:8080/api/facturas';
    }
    obtenerFacturas() {
        return __awaiter(this, void 0, void 0, function* () {
            try {
                const response = yield fetch(this.apiUrl);
                if (!response.ok) {
                    throw new Error(`Error al obtener las facturas: ${response.statusText}`);
                }
                return yield response.json();
            }
            catch (error) {
                console.error('Error en obtenerFacturas:', error);
                throw error;
            }
        });
    }
    crearFactura(factura) {
        return __awaiter(this, void 0, void 0, function* () {
            try {
                const response = yield fetch(this.apiUrl, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(factura),
                });
                if (!response.status.toString().startsWith('2')) {
                    throw new Error('Error al crear la factura');
                }
                return yield response.json();
            }
            catch (error) {
                console.error('Error en crearFactura:', error);
                throw error;
            }
        });
    }
    actualizarFactura(id, factura) {
        return __awaiter(this, void 0, void 0, function* () {
            try {
                const response = yield fetch(`${this.apiUrl}/${id}`, {
                    method: 'PUT',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(factura),
                });
                if (!response.ok) {
                    throw new Error(`Error al actualizar la factura con ID ${id}`);
                }
                return yield response.json();
            }
            catch (error) {
                console.error('Error en actualizarFactura:', error);
                throw error;
            }
        });
    }
    eliminarFactura(id) {
        return __awaiter(this, void 0, void 0, function* () {
            try {
                const response = yield fetch(`${this.apiUrl}/${id}`, {
                    method: 'DELETE',
                });
                if (!response.ok) {
                    throw new Error(`Error al eliminar la factura con ID ${id}`);
                }
            }
            catch (error) {
                console.error('Error en eliminarFactura:', error);
                throw error;
            }
        });
    }
}
