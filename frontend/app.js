var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
import { FacturaService } from './services/factura.service.js';
const facturaService = new FacturaService();
document.addEventListener('DOMContentLoaded', () => {
    cargarFacturas().catch(err => console.error(err));
    const btnActualizar = document.getElementById('btnActualizar');
    if (btnActualizar) {
        btnActualizar.addEventListener('click', () => {
            cargarFacturas().catch(err => console.error(err));
        });
    }
    const btnAgregarItem = document.getElementById('btnAgregarItem');
    if (btnAgregarItem) {
        btnAgregarItem.addEventListener('click', agregarFilaDetalle);
    }
    const formFactura = document.getElementById('formFactura');
    if (formFactura) {
        formFactura.addEventListener('submit', (e) => {
            guardarFactura(e).catch(err => console.error(err));
        });
    }
});
function cargarFacturas() {
    return __awaiter(this, void 0, void 0, function* () {
        try {
            const facturas = yield facturaService.obtenerFacturas();
            const tbody = document.getElementById('tablaFacturasBody');
            if (!tbody)
                return;
            tbody.innerHTML = '';
            facturas.forEach(f => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                <td>${f.id}</td>
                <td>${f.fechaEmision || 'N/D'}</td>
                <td>${f.estado}</td>
                <td>${f.clienteDenominacion}</td>
                <td>$${f.importeTotal}</td>
                <td>
                  <button class="btn-danger btn-anular" data-id="${f.id}">Anular</button>
                </td>
            `;
                tbody.appendChild(tr);
            });
            document.querySelectorAll('.btn-anular').forEach(button => {
                button.addEventListener('click', (e) => __awaiter(this, void 0, void 0, function* () {
                    const target = e.currentTarget;
                    const idStr = target.getAttribute('data-id');
                    if (idStr) {
                        const id = parseInt(idStr);
                        yield anularFactura(id);
                    }
                }));
            });
        }
        catch (error) {
            console.error('Error al cargar las facturas:', error);
        }
    });
}
function agregarFilaDetalle() {
    const tbody = document.querySelector('#tablaDetalles tbody');
    if (!tbody)
        return;
    const tr = document.createElement('tr');
    tr.innerHTML = `
        <td><input type="number" class="articuloId" value="1" required></td>
        <td><input type="number" class="cantidad" value="1" required></td>
        <td><input type="number" class="precioUnitario" value="1000" step="0.01" required></td>
        <td><button type="button" class="btn-danger" onclick="this.parentElement.parentElement.remove()">X</button></td>
    `;
    tbody.appendChild(tr);
}
function guardarFactura(event) {
    return __awaiter(this, void 0, void 0, function* () {
        event.preventDefault();
        const clienteInput = document.getElementById('clienteId');
        const clienteId = parseInt(clienteInput.value);
        const filas = document.querySelectorAll('#tablaDetalles tbody tr');
        const detalles = [];
        filas.forEach(fila => {
            const articuloId = parseInt(fila.querySelector('.articuloId').value);
            const cantidad = parseInt(fila.querySelector('.cantidad').value);
            const precioUnitario = parseFloat(fila.querySelector('.precioUnitario').value);
            detalles.push({ articuloId, cantidad, precioUnitario });
        });
        if (detalles.length === 0) {
            alert('Debe agregar al menos un ítem a la factura.');
            return;
        }
        const nuevaFactura = {
            clienteId,
            fechaEmision: new Date().toISOString().split('T')[0] + 'T00:00:00',
            estado: 'EMITIDA',
            detalles
        };
        try {
            yield facturaService.crearFactura(nuevaFactura);
            alert('¡Factura creada con éxito!');
            yield cargarFacturas();
            document.getElementById('formFactura').reset();
            document.querySelector('#tablaDetalles tbody').innerHTML = '';
        }
        catch (error) {
            alert('Error al registrar la factura.');
            console.error(error);
        }
    });
}
function anularFactura(id) {
    return __awaiter(this, void 0, void 0, function* () {
        if (confirm(`¿Estás seguro de anular la factura ID ${id}?`)) {
            try {
                yield facturaService.eliminarFactura(id);
                alert('Factura anulada correctamente.');
                yield cargarFacturas();
            }
            catch (error) {
                console.error('DETALLE DEL ERROR AL ELIMINAR:', error);
                alert('Error al anular la factura. Revisa la consola.');
            }
        }
    });
}
