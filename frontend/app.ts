import { FacturaService } from './services/factura.service.js';
import { FacturaDetalleRequestDTO, FacturaFiltrosDTO, FacturaReporteDTO } from './models/factura.dto.js';

const IVA = 0.21; // mismo porcentaje que usa FacturaServicio en el backend
const service = new FacturaService();
const moneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });

let items: FacturaDetalleRequestDTO[] = [];
let facturas: FacturaReporteDTO[] = [];
const POR_PAGINA = 10;
let pagina = 1;

const $ = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;

function esc(s: unknown): string {
    return String(s ?? '').replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!));
}

function toast(msg: string, tipo: 'exito' | 'error' | '' = '') {
    const el = document.createElement('div');
    el.className = `toast ${tipo}`;
    el.textContent = msg;
    $('toasts').appendChild(el);
    setTimeout(() => el.remove(), 4000);
}

const msgError = (e: unknown) => (e instanceof Error ? e.message : 'Error inesperado');

// ---------- Listado ----------
function leerFiltros(): FacturaFiltrosDTO {
    const desde = $<HTMLInputElement>('fDesde').value;
    const hasta = $<HTMLInputElement>('fHasta').value;
    const monto = $<HTMLInputElement>('fMonto').value;
    return {
        fechaDesde: desde ? `${desde}T00:00:00` : undefined,
        fechaHasta: hasta ? `${hasta}T23:59:59` : undefined,
        estado: $<HTMLSelectElement>('fEstado').value || undefined,
        montoMinimo: monto ? parseFloat(monto) : undefined,
    };
}

function filaMensaje(texto: string) {
    $('tablaFacturasBody').innerHTML = `<tr><td colspan="6" class="vacio">${esc(texto)}</td></tr>`;
}

// El backend no filtra por N.º ni por cliente, así que se hace acá sobre lo ya cargado
function filtrarLocal(): FacturaReporteDTO[] {
    const id = $<HTMLInputElement>('fId').value.trim();
    const cliente = $<HTMLInputElement>('fCliente').value.trim().toLowerCase();
    return facturas.filter(f =>
        (!id || String(f.id) === id) &&
        (!cliente || (f.clienteDenominacion ?? '').toLowerCase().includes(cliente)));
}

function renderResumen(lista: FacturaReporteDTO[]) {
    const vigentes = lista.filter(f => f.estado !== 'ANULADA');
    $('statCantidad').textContent = String(lista.length);
    $('statAnuladas').textContent = String(lista.length - vigentes.length);
    $('statTotal').textContent = moneda.format(vigentes.reduce((a, f) => a + f.importeTotal, 0));
}

function numerosDePagina(actual: number, total: number): (number | '…')[] {
    if (total <= 7) return Array.from({ length: total }, (_, k) => k + 1);
    const set = new Set([1, total, actual - 1, actual, actual + 1].filter(n => n >= 1 && n <= total));
    const orden = [...set].sort((a, b) => a - b);
    const res: (number | '…')[] = [];
    orden.forEach((n, k) => { if (k > 0 && n - orden[k - 1] > 1) res.push('…'); res.push(n); });
    return res;
}

function renderPaginacion(total: number, totalPaginas: number) {
    const cont = $('paginacion');
    if (total === 0) { cont.innerHTML = ''; return; }
    const desde = (pagina - 1) * POR_PAGINA + 1;
    const hasta = Math.min(pagina * POR_PAGINA, total);
    const botones = totalPaginas > 1 ? `<div class="pags">
        <button class="btn sm pag" data-pag="${pagina - 1}" ${pagina === 1 ? 'disabled' : ''}>‹ Anterior</button>
        ${numerosDePagina(pagina, totalPaginas).map(n => n === '…'
            ? '<span class="muted">…</span>'
            : `<button class="btn sm pag" data-pag="${n}" ${n === pagina ? 'aria-current="page"' : ''}>${n}</button>`).join('')}
        <button class="btn sm pag" data-pag="${pagina + 1}" ${pagina === totalPaginas ? 'disabled' : ''}>Siguiente ›</button>
    </div>` : '';
    cont.innerHTML = `<span class="info">Mostrando ${desde}–${hasta} de ${total}</span>${botones}`;
}

function renderFacturas() {
    const lista = filtrarLocal();
    renderResumen(lista);

    const totalPaginas = Math.max(1, Math.ceil(lista.length / POR_PAGINA));
    pagina = Math.min(Math.max(1, pagina), totalPaginas);
    renderPaginacion(lista.length, totalPaginas);
    if (lista.length === 0) return filaMensaje('No hay comprobantes para los filtros elegidos.');

    const visibles = lista.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA);
    $('tablaFacturasBody').innerHTML = visibles.map(f => {
        const anulada = f.estado === 'ANULADA';
        const fecha = f.fechaEmision ? new Date(f.fechaEmision).toLocaleDateString('es-AR') : 'N/D';
        return `<tr>
            <td>#${f.id}</td>
            <td>${fecha}</td>
            <td>${esc(f.clienteDenominacion)}</td>
            <td><span class="pill ${anulada ? 'bad' : 'ok'}">${esc(f.estado)}</span></td>
            <td class="num">${moneda.format(f.importeTotal)}</td>
            <td class="num"><button class="btn sm danger" data-anular="${f.id}" ${anulada ? 'disabled' : ''}>Anular</button></td>
        </tr>`;
    }).join('');
}

async function cargarFacturas(resetPagina = true) {
    if (resetPagina) pagina = 1;
    filaMensaje('Cargando…');
    try {
        facturas = await service.obtenerFacturas(leerFiltros());
        renderFacturas();
        marcarApi(true);
    } catch (e) {
        facturas = [];
        renderResumen([]);
        filaMensaje('No se pudo cargar el listado.');
        marcarApi(false);
        toast(msgError(e), 'error');
    }
}

function marcarApi(ok: boolean) {
    const el = $('estadoApi');
    el.textContent = ok ? 'API conectada' : 'API sin conexión';
    el.className = `pill ${ok ? 'ok' : 'bad'}`;
}

async function anularFactura(id: number) {
    if (!confirm(`¿Anular la factura #${id}? Esta acción cambia su estado a ANULADA.`)) return;
    try {
        await service.eliminarFactura(id);
        toast(`Factura #${id} anulada.`, 'exito');
        await cargarFacturas(false);
    } catch (e) {
        toast(msgError(e), 'error');
    }
}

// ---------- Alta de factura ----------
async function cargarClientes(seleccionarId?: number) {
    const select = $<HTMLSelectElement>('clienteId');
    try {
        const clientes = await service.obtenerClientes();
        select.innerHTML = '<option value="">Seleccioná un cliente…</option>' + clientes.map(c =>
            `<option value="${c.id}">${esc(c.denominacion)}${c.cuit ? ' — ' + esc(c.cuit) : ''}</option>`).join('');
        if (seleccionarId) select.value = String(seleccionarId);
        marcarApi(true);
    } catch (e) {
        select.innerHTML = '<option value="">No se pudieron cargar los clientes</option>';
        marcarApi(false);
        toast(msgError(e), 'error');
    }
}

function panelCliente(abrir: boolean) {
    $('panelCliente').hidden = !abrir;
    if (abrir) $<HTMLInputElement>('nDenominacion').focus();
    else ['nDenominacion', 'nCuit', 'nDomicilio'].forEach(id => $<HTMLInputElement>(id).value = '');
}

async function guardarCliente() {
    const denominacion = $<HTMLInputElement>('nDenominacion').value.trim();
    if (!denominacion) return toast('La denominación del cliente es obligatoria.', 'error');

    const btn = $<HTMLButtonElement>('btnGuardarCliente');
    btn.disabled = true;
    try {
        const c = await service.crearCliente({
            denominacion,
            cuit: $<HTMLInputElement>('nCuit').value.trim() || undefined,
            domicilio: $<HTMLInputElement>('nDomicilio').value.trim() || undefined,
        });
        await cargarClientes(c.id);
        panelCliente(false);
        toast(`Cliente "${c.denominacion}" creado y seleccionado.`, 'exito');
    } catch (e) {
        toast(msgError(e), 'error');
    } finally {
        btn.disabled = false;
    }
}

// ---------- Pestañas ----------
function mostrarVista(vista: 'nueva' | 'lista') {
    $('vistaNueva').hidden = vista !== 'nueva';
    $('vistaLista').hidden = vista !== 'lista';
    document.querySelectorAll<HTMLElement>('.tab').forEach(t =>
        t.setAttribute('aria-selected', String(t.dataset.vista === vista)));
    if (vista === 'lista') cargarFacturas();
}

function renderItems() {
    $('sinItems').hidden = items.length > 0;
    $('tablaDetalles').hidden = items.length === 0;
    document.querySelector('#tablaDetalles tbody')!.innerHTML = items.map((it, i) => `<tr data-i="${i}">
        <td><input type="number" data-campo="articuloId" min="1" step="1" value="${it.articuloId}" required></td>
        <td><input type="number" data-campo="cantidad" min="0.01" step="0.01" value="${it.cantidad}" required></td>
        <td><input type="number" data-campo="precioUnitario" min="0" step="0.01" value="${it.precioUnitario}" required></td>
        <td class="num" data-subtotal></td>
        <td class="num"><button type="button" class="btn sm danger" data-quitar title="Quitar ítem" aria-label="Quitar ítem">✕</button></td>
    </tr>`).join('');
    actualizarTotales();
}

function actualizarTotales() {
    let neto = 0;
    document.querySelectorAll<HTMLElement>('#tablaDetalles tbody tr').forEach(tr => {
        const it = items[Number(tr.dataset.i)];
        const sub = (it.cantidad || 0) * (it.precioUnitario || 0);
        neto += sub;
        tr.querySelector('[data-subtotal]')!.textContent = moneda.format(sub);
    });
    $('totNeto').textContent = moneda.format(neto);
    $('totIva').textContent = moneda.format(neto * IVA);
    $('totTotal').textContent = moneda.format(neto * (1 + IVA));
}

async function guardarFactura(e: Event) {
    e.preventDefault();
    if (items.length === 0) return toast('Agregá al menos un ítem.', 'error');

    const btn = $<HTMLButtonElement>('btnGuardar');
    btn.disabled = true;
    try {
        const f = await service.crearFactura({
            clienteId: Number($<HTMLSelectElement>('clienteId').value),
            fechaEmision: `${new Date().toISOString().split('T')[0]}T00:00:00`,
            estado: 'EMITIDA',
            detalles: items,
        });
        toast(`Factura #${f.id} creada por ${moneda.format(f.importeTotal)}.`, 'exito');
        items = [];
        $<HTMLFormElement>('formFactura').reset();
        renderItems();
        cargarFacturas();
    } catch (err) {
        toast(msgError(err), 'error');
    } finally {
        btn.disabled = false;
    }
}

// ---------- Eventos ----------
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll<HTMLElement>('.tab').forEach(t =>
        t.addEventListener('click', () => mostrarVista(t.dataset.vista as 'nueva' | 'lista')));

    $('formFiltros').addEventListener('submit', e => { e.preventDefault(); cargarFacturas(); });
    $('btnLimpiar').addEventListener('click', () => { $<HTMLFormElement>('formFiltros').reset(); cargarFacturas(); });
    // N.º y cliente se filtran al escribir, sin volver a pedir datos al servidor
    ['fId', 'fCliente'].forEach(id => $(id).addEventListener('input', () => { pagina = 1; renderFacturas(); }));
    $('paginacion').addEventListener('click', e => {
        const btn = (e.target as HTMLElement).closest<HTMLButtonElement>('[data-pag]');
        if (btn && !btn.disabled) { pagina = Number(btn.dataset.pag); renderFacturas(); }
    });

    $('btnNuevoCliente').addEventListener('click', () => panelCliente(!!$('panelCliente').hidden));
    $('btnCancelarCliente').addEventListener('click', () => panelCliente(false));
    $('btnGuardarCliente').addEventListener('click', guardarCliente);

    // Delegación: un solo listener para todos los botones "Anular"
    $('tablaFacturasBody').addEventListener('click', e => {
        const btn = (e.target as HTMLElement).closest<HTMLElement>('[data-anular]');
        if (btn) anularFactura(Number(btn.dataset.anular));
    });

    $('btnAgregarItem').addEventListener('click', () => {
        items.push({ articuloId: 1, cantidad: 1, precioUnitario: 0 });
        renderItems();
    });

    const tbodyItems = document.querySelector('#tablaDetalles tbody')!;
    tbodyItems.addEventListener('input', e => {
        const input = e.target as HTMLInputElement;
        const tr = input.closest<HTMLElement>('tr');
        const campo = input.dataset.campo as keyof FacturaDetalleRequestDTO | undefined;
        if (!tr || !campo) return;
        items[Number(tr.dataset.i)][campo] = parseFloat(input.value) || 0;
        actualizarTotales(); // no re-renderiza las filas, así no se pierde el foco
    });
    tbodyItems.addEventListener('click', e => {
        const tr = (e.target as HTMLElement).closest<HTMLElement>('tr');
        if (tr && (e.target as HTMLElement).closest('[data-quitar]')) {
            items.splice(Number(tr.dataset.i), 1);
            renderItems();
        }
    });

    $('formFactura').addEventListener('submit', guardarFactura);

    renderItems();
    cargarClientes();
});
