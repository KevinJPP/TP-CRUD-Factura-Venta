import { FacturaService } from './services/factura.service.js';
const IVA = 0.21; // mismo porcentaje que usa FacturaServicio en el backend
const service = new FacturaService();
const moneda = new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' });
let items = [];
let clientes = [];
let catalogo = [];
const cantidadesModal = new Map(); // cantidad elegida en el catálogo antes de agregar
let facturas = []; // modo servidor: solo la página actual; modo local: todas
const POR_PAGINA = 10;
let pagina = 1;
// Paginación en el servidor: el backend recibe page/size y devuelve solo la página pedida.
// Si el backend todavía devuelve un array completo, se pagina en el navegador (modo local).
let modoServidor = false;
let totalFacturas = 0;
let totalPaginas = 1;
let resumen = null;
let timerBusqueda;
const $ = (id) => document.getElementById(id);
function esc(s) {
    return String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function toast(msg, tipo = '') {
    const el = document.createElement('div');
    el.className = `toast ${tipo}`;
    el.textContent = msg;
    $('toasts').appendChild(el);
    setTimeout(() => el.remove(), 4000);
}
const msgError = (e) => (e instanceof Error ? e.message : 'Error inesperado');
// ---------- Listado ----------
function leerFiltros() {
    const desde = $('fDesde').value;
    const hasta = $('fHasta').value;
    const monto = $('fMonto').value;
    return {
        fechaDesde: desde ? `${desde}T00:00:00` : undefined,
        fechaHasta: hasta ? `${hasta}T23:59:59` : undefined,
        estado: $('fEstado').value || undefined,
        montoMinimo: monto ? parseFloat(monto) : undefined,
        id: $('fId').value.trim() || undefined,
        cliente: $('fCliente').value.trim() || undefined,
    };
}
function filaMensaje(texto) {
    $('tablaFacturasBody').innerHTML = `<tr><td colspan="6" class="vacio">${esc(texto)}</td></tr>`;
}
// Solo modo local (backend viejo que devuelve todo): filtra por N.º y cliente sobre lo ya cargado
function filtrarLocal() {
    const id = $('fId').value.trim();
    const cliente = $('fCliente').value.trim().toLowerCase();
    return facturas.filter(f => (!id || String(f.id) === id) &&
        (!cliente || (f.clienteDenominacion ?? '').toLowerCase().includes(cliente)));
}
function renderResumen(lista) {
    if (modoServidor) {
        // Los totales deben salir de TODOS los resultados filtrados, no solo de la página visible
        $('statCantidad').textContent = String(totalFacturas);
        $('statAnuladas').textContent = resumen ? String(resumen.anuladas) : '—';
        $('statTotal').textContent = resumen ? moneda.format(resumen.importeVigente) : '—';
        return;
    }
    const vigentes = lista.filter(f => f.estado !== 'ANULADA');
    $('statCantidad').textContent = String(lista.length);
    $('statAnuladas').textContent = String(lista.length - vigentes.length);
    $('statTotal').textContent = moneda.format(vigentes.reduce((a, f) => a + f.importeTotal, 0));
}
function numerosDePagina(actual, total) {
    if (total <= 7)
        return Array.from({ length: total }, (_, k) => k + 1);
    const set = new Set([1, total, actual - 1, actual, actual + 1].filter(n => n >= 1 && n <= total));
    const orden = [...set].sort((a, b) => a - b);
    const res = [];
    orden.forEach((n, k) => { if (k > 0 && n - orden[k - 1] > 1)
        res.push('…'); res.push(n); });
    return res;
}
function renderPaginacion(total, totalPaginas) {
    const cont = $('paginacion');
    if (total === 0) {
        cont.innerHTML = '';
        return;
    }
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
    let visibles;
    if (modoServidor) {
        renderResumen(facturas);
        visibles = facturas;
    }
    else {
        const lista = filtrarLocal();
        totalFacturas = lista.length;
        totalPaginas = Math.max(1, Math.ceil(lista.length / POR_PAGINA));
        pagina = Math.min(Math.max(1, pagina), totalPaginas);
        renderResumen(lista);
        visibles = lista.slice((pagina - 1) * POR_PAGINA, pagina * POR_PAGINA);
    }
    renderPaginacion(totalFacturas, totalPaginas);
    if (visibles.length === 0)
        return filaMensaje('No hay comprobantes para los filtros elegidos.');
    $('tablaFacturasBody').innerHTML = visibles.map(f => {
        const anulada = f.estado === 'ANULADA';
        const fecha = f.fechaEmision ? new Date(f.fechaEmision).toLocaleDateString('es-AR') : 'N/D';
        return `<tr>
            <td>#${f.id}</td>
            <td>${fecha}</td>
            <td>${esc(f.clienteDenominacion)}</td>
            <td><span class="pill ${anulada ? 'bad' : 'ok'}">${esc(f.estado)}</span></td>
            <td class="num">${moneda.format(f.importeTotal)}</td>
            <td class="num acciones-fila">
                <button class="btn sm" data-ver="${f.id}">Ver</button>
                <button class="btn sm" data-imprimir="${f.id}" title="Imprimir o guardar como PDF">PDF</button>
                <button class="btn sm danger" data-anular="${f.id}" ${anulada ? 'disabled' : ''}>Anular</button>
            </td>
        </tr>`;
    }).join('');
}
async function cargarFacturas(resetPagina = true) {
    if (resetPagina)
        pagina = 1;
    filaMensaje('Cargando…');
    try {
        const resp = await service.obtenerFacturas({ ...leerFiltros(), page: pagina - 1, size: POR_PAGINA });
        if (Array.isArray(resp)) {
            modoServidor = false;
            facturas = resp;
            resumen = null;
        }
        else {
            modoServidor = true;
            facturas = resp.content;
            totalFacturas = resp.totalElements;
            totalPaginas = Math.max(1, resp.totalPages);
            resumen = resp.resumen ?? null;
            // Si la página pedida ya no existe (p. ej. se anuló la última), volver a la última válida
            if (pagina > totalPaginas) {
                pagina = totalPaginas;
                return cargarFacturas(false);
            }
        }
        renderFacturas();
        marcarApi(true);
    }
    catch (e) {
        facturas = [];
        modoServidor = false;
        renderResumen([]);
        renderPaginacion(0, 1);
        filaMensaje('No se pudo cargar el listado.');
        marcarApi(false);
        toast(msgError(e), 'error');
    }
}
function marcarApi(ok) {
    const el = $('estadoApi');
    el.textContent = ok ? 'API conectada' : 'API sin conexión';
    el.className = `pill ${ok ? 'ok' : 'bad'}`;
}
async function anularFactura(id) {
    if (!confirm(`¿Anular la factura #${id}? Esta acción cambia su estado a ANULADA.`))
        return;
    try {
        await service.eliminarFactura(id);
        toast(`Factura #${id} anulada.`, 'exito');
        await cargarFacturas(false);
    }
    catch (e) {
        toast(msgError(e), 'error');
    }
}
// ---------- Ver / imprimir comprobante ----------
let comprobanteActual = null;
let focoPrevio = null;
const pad = (n, largo) => String(n ?? 0).padStart(largo, '0');
const fechaAR = (iso) => iso ? new Date(iso).toLocaleDateString('es-AR') : 'N/D';
const numeroComprobante = (c) => `${pad(c.puntoVentaNumero ?? 1, 4)}-${pad(c.id, 8)}`;
function htmlComprobante(c) {
    const anulada = c.estado === 'ANULADA';
    const filas = c.items.length
        ? c.items.map(it => `<tr>
            <td>${esc(it.codigo ?? it.articuloId ?? '')}</td>
            <td class="desc">${esc(it.descripcion)}</td>
            <td class="num">${it.cantidad.toLocaleString('es-AR')}</td>
            <td class="num">${moneda.format(it.precioUnitario)}</td>
            <td class="num">${moneda.format(it.subtotal)}</td>
        </tr>`).join('')
        : '<tr><td colspan="5" class="vacio">El comprobante no tiene ítems.</td></tr>';
    return `
    ${anulada ? '<div class="sello-anulada" aria-hidden="true">ANULADA</div>' : ''}
    <header class="cp-cabecera">
        <div class="cp-emisor">
            <strong class="cp-razon">Módulo de Facturación</strong>
            ${c.puntoVentaDomicilio ? `<span>${esc(c.puntoVentaDomicilio)}</span>` : ''}
        </div>
        <div class="cp-letra" aria-label="Tipo de comprobante">B</div>
        <div class="cp-datos">
            <strong>FACTURA</strong>
            <span>N.º ${numeroComprobante(c)}</span>
            <span>Fecha: ${fechaAR(c.fechaEmision)}</span>
            <span class="pill ${anulada ? 'bad' : 'ok'}">${esc(c.estado)}</span>
        </div>
    </header>

    <section class="cp-cliente">
        <div><span>Cliente</span><strong>${esc(c.clienteDenominacion ?? 'N/D')}</strong></div>
        <div><span>CUIT</span><strong>${esc(c.clienteCuit || '—')}</strong></div>
        <div><span>Domicilio</span><strong>${esc(c.clienteDomicilio || '—')}</strong></div>
        <div><span>Condición IVA</span><strong>${esc(c.condicionIva || '—')}</strong></div>
        <div><span>Moneda</span><strong>${esc(c.moneda || 'ARS')}</strong></div>
    </section>

    <table class="cp-items">
        <thead><tr><th>Código</th><th>Descripción</th><th class="num">Cant.</th><th class="num">P. unitario</th><th class="num">Subtotal</th></tr></thead>
        <tbody>${filas}</tbody>
    </table>

    <dl class="totales cp-totales">
        <div><dt>Neto gravado</dt><dd>${moneda.format(c.importeNeto)}</dd></div>
        <div><dt>IVA 21%</dt><dd>${moneda.format(c.importeIva)}</dd></div>
        <div class="total"><dt>Total</dt><dd>${moneda.format(c.importeTotal)}</dd></div>
    </dl>

    <footer class="cp-pie">
        ${c.cae
        ? `CAE: ${esc(c.cae)} — Vto. CAE: ${fechaAR(c.caeFechaVencimiento)}`
        : `Comprobante sin CAE — documento no válido como factura fiscal.${c.motivoRechazo
            ? `<br>Motivo informado por ARCA: ${esc(c.motivoRechazo)}` : ''}`}
    </footer>`;
}
function cerrarComprobante() {
    $('modalComprobante').hidden = true;
    document.body.classList.remove('modal-abierto');
    comprobanteActual = null;
    focoPrevio?.focus();
}
async function abrirComprobante(id, imprimir = false) {
    focoPrevio = document.activeElement;
    const cont = $('comprobanteContenido');
    cont.innerHTML = '<p class="muted">Cargando comprobante…</p>';
    $('btnImprimir').disabled = true;
    $('modalComprobante').hidden = false;
    document.body.classList.add('modal-abierto');
    $('btnCerrarComprobante').focus();
    try {
        comprobanteActual = await service.obtenerComprobante(id);
        cont.innerHTML = htmlComprobante(comprobanteActual);
        $('tituloComprobante').textContent = `Factura N.º ${numeroComprobante(comprobanteActual)}`;
        $('btnImprimir').disabled = false;
        if (imprimir)
            imprimirComprobante();
    }
    catch (e) {
        cont.innerHTML = `<p class="vacio">No se pudo cargar el comprobante #${id}.</p>`;
        toast(msgError(e), 'error');
    }
}
// Usa el diálogo de impresión del navegador: ahí se elige la impresora o "Guardar como PDF".
// El CSS @media print deja visible sólo el comprobante.
function imprimirComprobante() {
    if (!comprobanteActual)
        return;
    const tituloOriginal = document.title;
    document.title = `Factura-${numeroComprobante(comprobanteActual)}`; // nombre sugerido del PDF
    window.addEventListener('afterprint', () => { document.title = tituloOriginal; }, { once: true });
    window.print();
}
// ---------- Alta de factura ----------
// Buscador de cliente (combobox): filtra por nombre o CUIT, sin tildes ni mayúsculas
const MAX_OPCIONES = 50;
let opcionesCliente = [];
let opcionActiva = -1;
const normalizar = (t) => t.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
function filtrarClientes(texto) {
    const tokens = normalizar(texto).split(/\s+/).filter(Boolean);
    if (tokens.length === 0)
        return clientes;
    return clientes.filter(c => {
        const base = normalizar(c.denominacion);
        const cuit = (c.cuit ?? '').replace(/\D/g, '');
        return tokens.every(t => base.includes(t) || (/\d/.test(t) && cuit.includes(t.replace(/\D/g, ''))));
    });
}
const etiquetaCliente = (c) => c.denominacion;
function abrirLista(abierta) {
    $('clienteLista').hidden = !abierta;
    $('clienteBuscar').setAttribute('aria-expanded', String(abierta));
    if (!abierta)
        opcionActiva = -1;
}
function renderListaClientes(texto) {
    const lista = $('clienteLista');
    const sel = Number($('clienteId').value);
    const coincidencias = filtrarClientes(texto);
    opcionesCliente = coincidencias.slice(0, MAX_OPCIONES);
    opcionActiva = opcionesCliente.length > 0 ? 0 : -1;
    if (clientes.length === 0) {
        lista.innerHTML = '<li class="combo-msg">No hay clientes cargados.</li>';
    }
    else if (opcionesCliente.length === 0) {
        lista.innerHTML = '<li class="combo-msg">Ningún cliente coincide con la búsqueda.</li>';
    }
    else {
        lista.innerHTML = opcionesCliente.map((c, i) => `<li class="combo-op${i === opcionActiva ? ' activa' : ''}" role="option" id="op-cliente-${c.id}" data-i="${i}" aria-selected="${c.id === sel}">
            <span class="combo-nombre">${esc(c.denominacion)}</span>
            ${c.cuit ? `<span class="combo-cuit">${esc(c.cuit)}</span>` : ''}
        </li>`).join('') + (coincidencias.length > MAX_OPCIONES
            ? `<li class="combo-msg">Mostrando ${MAX_OPCIONES} de ${coincidencias.length}. Seguí escribiendo para afinar.</li>` : '');
    }
    marcarActiva();
}
function marcarActiva() {
    const ops = $('clienteLista').querySelectorAll('.combo-op');
    ops.forEach((li, i) => li.classList.toggle('activa', i === opcionActiva));
    const activa = ops[opcionActiva];
    if (activa) {
        $('clienteBuscar').setAttribute('aria-activedescendant', activa.id);
        activa.scrollIntoView({ block: 'nearest' });
    }
    else {
        $('clienteBuscar').removeAttribute('aria-activedescendant');
    }
}
function elegirCliente(c) {
    $('clienteId').value = c ? String(c.id) : '';
    $('clienteBuscar').value = c ? etiquetaCliente(c) : '';
    abrirLista(false);
    mostrarResumenCliente();
}
// Al salir del campo sin elegir nada, vuelve a mostrar el cliente que estaba seleccionado
function restaurarTextoCliente() {
    const c = clientes.find(x => x.id === Number($('clienteId').value));
    $('clienteBuscar').value = c ? etiquetaCliente(c) : '';
}
function iniciarBuscadorCliente() {
    const input = $('clienteBuscar');
    const lista = $('clienteLista');
    input.addEventListener('focus', () => { input.select(); renderListaClientes(''); abrirLista(true); });
    input.addEventListener('click', () => { if (lista.hidden) {
        renderListaClientes('');
        abrirLista(true);
    } });
    input.addEventListener('input', () => {
        $('clienteId').value = ''; // al escribir se descarta la selección anterior
        mostrarResumenCliente();
        renderListaClientes(input.value);
        abrirLista(true);
    });
    input.addEventListener('keydown', e => {
        if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
            e.preventDefault();
            if (lista.hidden) {
                renderListaClientes('');
                abrirLista(true);
                return;
            }
            if (opcionesCliente.length === 0)
                return;
            const paso = e.key === 'ArrowDown' ? 1 : -1;
            opcionActiva = (opcionActiva + paso + opcionesCliente.length) % opcionesCliente.length;
            marcarActiva();
        }
        else if (e.key === 'Enter') {
            if (!lista.hidden) {
                e.preventDefault(); // que no envíe el formulario
                const c = opcionesCliente[opcionActiva];
                if (c)
                    elegirCliente(c);
            }
        }
        else if (e.key === 'Escape' && !lista.hidden) {
            e.preventDefault();
            abrirLista(false);
            restaurarTextoCliente();
        }
    });
    input.addEventListener('blur', () => { abrirLista(false); restaurarTextoCliente(); });
    // mousedown (y no click) para elegir antes de que el input pierda el foco
    lista.addEventListener('mousedown', e => {
        e.preventDefault();
        const li = e.target.closest('.combo-op');
        if (li)
            elegirCliente(opcionesCliente[Number(li.dataset.i)]);
    });
}
async function cargarClientes(seleccionarId) {
    const input = $('clienteBuscar');
    try {
        clientes = await service.obtenerClientes();
        input.disabled = false;
        input.placeholder = 'Buscar cliente por nombre o CUIT…';
        const elegido = seleccionarId ? clientes.find(c => c.id === seleccionarId) ?? null : null;
        if (elegido)
            elegirCliente(elegido);
        else
            restaurarTextoCliente();
        marcarApi(true);
    }
    catch (e) {
        clientes = [];
        input.disabled = true;
        input.placeholder = 'No se pudieron cargar los clientes';
        marcarApi(false);
        toast(msgError(e), 'error');
    }
    mostrarResumenCliente();
}
// Línea con los datos del cliente elegido, debajo del selector
function mostrarResumenCliente() {
    const el = $('resumenCliente');
    const id = Number($('clienteId').value);
    const c = clientes.find(x => x.id === id);
    if (!c) {
        el.hidden = true;
        el.innerHTML = '';
        return;
    }
    const lugar = [c.domicilio, c.localidad, c.provincia].filter(Boolean).join(', ');
    const domicilio = lugar && c.codigoPostal ? `${lugar} (CP ${c.codigoPostal})` : lugar;
    const partes = [
        ['CUIT', c.cuit], ['IVA', c.condicionIva], ['Email', c.email], ['Tel.', c.telefono], ['Domicilio', domicilio],
    ];
    const html = partes.filter(([, v]) => v).map(([k, v]) => `<span>${k}: <strong>${esc(v)}</strong></span>`).join('');
    el.innerHTML = html;
    el.hidden = !html;
}
// ---------- Nuevo cliente (ventana emergente) ----------
const PROVINCIAS = [
    'Buenos Aires', 'Ciudad Autónoma de Buenos Aires', 'Catamarca', 'Chaco', 'Chubut', 'Córdoba', 'Corrientes',
    'Entre Ríos', 'Formosa', 'Jujuy', 'La Pampa', 'La Rioja', 'Mendoza', 'Misiones', 'Neuquén', 'Río Negro',
    'Salta', 'San Juan', 'San Luis', 'Santa Cruz', 'Santa Fe', 'Santiago del Estero', 'Tierra del Fuego', 'Tucumán',
];
const CAMPOS_CLIENTE = [
    'cDenominacion', 'cCuit', 'cCondicionIva', 'cEmail', 'cTelefono',
    'cDomicilio', 'cLocalidad', 'cProvincia', 'cCodigoPostal', 'cObservaciones',
];
const valorCampo = (id) => $(id).value.trim();
function errorCampo(id, mensaje = '') {
    $(id).classList.toggle('invalido', mensaje !== '');
    const aviso = document.querySelector(`[data-error="${id}"]`);
    if (aviso)
        aviso.textContent = mensaje;
}
// 20123456789 -> 20-12345678-9 (mientras se escribe)
function formatearCuit(texto) {
    const d = texto.replace(/\D/g, '').slice(0, 11);
    if (d.length <= 2)
        return d;
    if (d.length <= 10)
        return `${d.slice(0, 2)}-${d.slice(2)}`;
    return `${d.slice(0, 2)}-${d.slice(2, 10)}-${d.slice(10)}`;
}
// Devuelve el id del primer campo inválido (o null si está todo bien)
function validarCliente() {
    let primero = null;
    const marcar = (id, msg) => { errorCampo(id, msg); if (msg && !primero)
        primero = id; };
    marcar('cDenominacion', valorCampo('cDenominacion') ? '' : 'La denominación es obligatoria.');
    const cuit = valorCampo('cCuit');
    marcar('cCuit', cuit && cuit.replace(/\D/g, '').length !== 11 ? 'El CUIT debe tener 11 dígitos.' : '');
    const email = valorCampo('cEmail');
    marcar('cEmail', email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) ? 'El email no es válido.' : '');
    const tel = valorCampo('cTelefono');
    const telValido = /^[0-9 +()\-]+$/.test(tel) && tel.replace(/\D/g, '').length >= 6;
    marcar('cTelefono', tel && !telValido ? 'Ingresá un teléfono válido.' : '');
    return primero;
}
function abrirDialogoCliente() {
    $('formCliente').reset();
    CAMPOS_CLIENTE.forEach(id => errorCampo(id));
    const dlg = $('dlgCliente');
    dlg.showModal();
    $('cDenominacion').focus();
    $('dlgCliente').querySelector('.modal-cuerpo').scrollTop = 0;
}
async function guardarCliente(e) {
    e.preventDefault();
    const invalido = validarCliente();
    if (invalido) {
        $(invalido).focus();
        return;
    }
    const dato = (id) => valorCampo(id) || undefined;
    const nuevo = {
        denominacion: valorCampo('cDenominacion'),
        cuit: dato('cCuit'),
        condicionIva: dato('cCondicionIva'),
        email: dato('cEmail'),
        telefono: dato('cTelefono'),
        domicilio: dato('cDomicilio'),
        localidad: dato('cLocalidad'),
        provincia: dato('cProvincia'),
        codigoPostal: dato('cCodigoPostal'),
        observaciones: dato('cObservaciones'),
    };
    const btn = $('btnGuardarCliente');
    btn.disabled = true;
    try {
        const c = await service.crearCliente(nuevo);
        await cargarClientes(c.id);
        $('dlgCliente').close();
        toast(`Cliente "${c.denominacion}" creado y seleccionado.`, 'exito');
    }
    catch (err) {
        toast(msgError(err), 'error'); // el diálogo queda abierto para no perder lo escrito
    }
    finally {
        btn.disabled = false;
    }
}
// ---------- Pestañas ----------
function mostrarVista(vista) {
    $('vistaNueva').hidden = vista !== 'nueva';
    $('vistaLista').hidden = vista !== 'lista';
    document.querySelectorAll('.tab').forEach(t => t.setAttribute('aria-selected', String(t.dataset.vista === vista)));
    if (vista === 'lista')
        cargarFacturas();
}
function renderItems() {
    $('sinItems').hidden = items.length > 0;
    $('tablaDetalles').hidden = items.length === 0;
    document.querySelector('#tablaDetalles tbody').innerHTML = items.map((it, i) => `<tr data-i="${i}">
        <td class="nombre-art" data-nombre></td>
        <td><input type="number" data-campo="cantidad" min="1" step="1" value="${it.cantidad}" required></td>
        <td><input type="number" data-campo="precioUnitario" min="0" step="0.01" value="${it.precioUnitario}" required></td>
        <td class="num" data-subtotal></td>
        <td class="num"><button type="button" class="btn sm danger" data-quitar title="Quitar ítem" aria-label="Quitar ítem">✕</button></td>
    </tr>`).join('');
    actualizarTotales();
}
function actualizarTotales() {
    let neto = 0;
    document.querySelectorAll('#tablaDetalles tbody tr').forEach(tr => {
        const it = items[Number(tr.dataset.i)];
        const sub = (it.cantidad || 0) * (it.precioUnitario || 0);
        neto += sub;
        tr.querySelector('[data-subtotal]').textContent = moneda.format(sub);
        const art = catalogo.find(a => a.id === it.articuloId);
        const celdaNombre = tr.querySelector('[data-nombre]');
        celdaNombre.textContent = art ? art.denominacion : '—';
        celdaNombre.classList.toggle('sin', !art);
    });
    $('totNeto').textContent = moneda.format(neto);
    $('totIva').textContent = moneda.format(neto * IVA);
    $('totTotal').textContent = moneda.format(neto * (1 + IVA));
    renderResumenCarrito(neto);
}
// ---------- Catálogo de artículos (ventana emergente tipo carrito) ----------
async function cargarCatalogo() {
    try {
        catalogo = await service.obtenerArticulos();
        marcarApi(true);
        return true;
    }
    catch (e) {
        catalogo = [];
        toast(msgError(e), 'error');
        return false;
    }
}
function renderResumenCarrito(neto) {
    const el = document.getElementById('resumenCarrito');
    if (!el)
        return;
    const unidades = items.reduce((a, it) => a + (it.cantidad || 0), 0);
    const subtotal = neto ?? items.reduce((a, it) => a + (it.cantidad || 0) * (it.precioUnitario || 0), 0);
    el.innerHTML = items.length === 0
        ? 'Carrito vacío'
        : `<strong>${items.length}</strong> artículo${items.length === 1 ? '' : 's'} · ${unidades} u. · Neto <strong>${moneda.format(subtotal)}</strong>`;
}
function totalFila(art, cantidad) {
    return `${cantidad} u. = ${moneda.format(art.precio * cantidad)}`;
}
function actualizarTotalFila(li, art, cantidad) {
    li.querySelector('[data-total]').textContent = totalFila(art, cantidad);
}
function renderCatalogo() {
    const q = $('buscarArticulo').value.trim().toLowerCase();
    const lista = catalogo.filter(a => !q || a.denominacion.toLowerCase().includes(q) || (a.codigo ?? '').toLowerCase().includes(q));
    if (lista.length === 0) {
        $('listaArticulos').innerHTML = `<li class="vacio" style="padding:24px 20px">${catalogo.length === 0 ? 'No hay artículos cargados.' : 'Ningún artículo coincide con la búsqueda.'}</li>`;
        return;
    }
    $('listaArticulos').innerHTML = lista.map(a => {
        const cant = cantidadesModal.get(a.id) ?? 1;
        const enCarrito = items.find(it => it.articuloId === a.id)?.cantidad ?? 0;
        return `<li class="art" data-art="${a.id}">
            <div class="art-info">
                <div class="art-nombre" title="${esc(a.denominacion)}">${esc(a.denominacion)}</div>
                <div class="art-cod">${esc(a.codigo)}${enCarrito ? ` · <span class="en-carrito">en carrito: ${enCarrito}</span>` : ''}</div>
            </div>
            <div class="art-precio">
                <div>${moneda.format(a.precio)}</div>
                <div class="art-total" data-total>${totalFila(a, cant)}</div>
            </div>
            <div class="stepper">
                <button type="button" class="btn sm" data-menos aria-label="Menos">−</button>
                <input type="number" data-cant min="1" step="1" value="${cant}" aria-label="Cantidad de ${esc(a.denominacion)}">
                <button type="button" class="btn sm" data-mas aria-label="Más">+</button>
            </div>
            <button type="button" class="btn sm primary" data-agregar>Agregar</button>
        </li>`;
    }).join('');
}
function agregarAlCarrito(art, cantidad) {
    const existente = items.find(it => it.articuloId === art.id);
    if (existente)
        existente.cantidad += cantidad;
    else
        items.push({ articuloId: art.id, cantidad, precioUnitario: art.precio });
    cantidadesModal.set(art.id, 1);
    renderItems();
    renderCatalogo();
    toast(`${cantidad} × ${art.denominacion} agregado al carrito.`, 'exito');
}
async function abrirCatalogo() {
    const dlg = $('dlgCatalogo');
    $('buscarArticulo').value = '';
    cantidadesModal.clear();
    dlg.showModal();
    if (catalogo.length === 0) {
        $('listaArticulos').innerHTML = '<li class="vacio" style="padding:24px 20px">Cargando artículos…</li>';
        await cargarCatalogo();
        actualizarTotales(); // ahora se pueden mostrar los nombres en la tabla
    }
    renderCatalogo();
    renderResumenCarrito();
    $('buscarArticulo').focus();
}
async function guardarFactura(e) {
    e.preventDefault();
    const clienteId = Number($('clienteId').value);
    if (!clienteId) {
        toast('Elegí un cliente de la lista.', 'error');
        $('clienteBuscar').focus();
        return;
    }
    if (items.length === 0)
        return toast('Agregá al menos un ítem.', 'error');
    const btn = $('btnGuardar');
    btn.disabled = true;
    try {
        const f = await service.crearFactura({
            clienteId,
            fechaEmision: `${new Date().toISOString().split('T')[0]}T00:00:00`,
            estado: 'EMITIDA',
            detalles: items,
        });
        toast(`Factura #${f.id} creada por ${moneda.format(f.importeTotal)}.`, 'exito');
        items = [];
        $('formFactura').reset();
        mostrarResumenCliente();
        renderItems();
        cargarFacturas();
    }
    catch (err) {
        toast(msgError(err), 'error');
    }
    finally {
        btn.disabled = false;
    }
}
// ---------- Eventos ----------
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.tab').forEach(t => t.addEventListener('click', () => mostrarVista(t.dataset.vista)));
    $('formFiltros').addEventListener('submit', e => { e.preventDefault(); cargarFacturas(); });
    $('btnLimpiar').addEventListener('click', () => { $('formFiltros').reset(); cargarFacturas(); });
    // N.º y cliente: en modo servidor se piden al backend (con retardo para no hacer una consulta por tecla)
    ['fId', 'fCliente'].forEach(id => $(id).addEventListener('input', () => {
        if (!modoServidor) {
            pagina = 1;
            renderFacturas();
            return;
        }
        clearTimeout(timerBusqueda);
        timerBusqueda = window.setTimeout(() => cargarFacturas(), 300);
    }));
    $('paginacion').addEventListener('click', e => {
        const btn = e.target.closest('[data-pag]');
        if (!btn || btn.disabled)
            return;
        pagina = Number(btn.dataset.pag);
        if (modoServidor)
            cargarFacturas(false);
        else
            renderFacturas();
    });
    // Nuevo cliente (ventana emergente). Solo se cierra con ✕, Cancelar o Esc: un clic suelto afuera no pierde lo escrito
    $('cProvincia').innerHTML = '<option value="">Sin especificar</option>' +
        PROVINCIAS.map(p => `<option>${esc(p)}</option>`).join('');
    $('btnNuevoCliente').addEventListener('click', abrirDialogoCliente);
    $('btnCerrarCliente').addEventListener('click', () => $('dlgCliente').close());
    $('btnCancelarCliente').addEventListener('click', () => $('dlgCliente').close());
    $('formCliente').addEventListener('submit', guardarCliente);
    $('cCuit').addEventListener('input', e => {
        const input = e.target;
        input.value = formatearCuit(input.value);
    });
    CAMPOS_CLIENTE.forEach(id => $(id).addEventListener('input', () => errorCampo(id))); // limpia el error al corregir
    iniciarBuscadorCliente();
    // Delegación: un solo listener para todos los botones "Anular"
    $('tablaFacturasBody').addEventListener('click', e => {
        const t = e.target;
        const anular = t.closest('[data-anular]');
        const ver = t.closest('[data-ver]');
        const pdf = t.closest('[data-imprimir]');
        if (anular)
            anularFactura(Number(anular.dataset.anular));
        else if (ver)
            abrirComprobante(Number(ver.dataset.ver));
        else if (pdf)
            abrirComprobante(Number(pdf.dataset.imprimir), true);
    });
    // Modal del comprobante
    $('btnImprimir').addEventListener('click', imprimirComprobante);
    $('btnCerrarComprobante').addEventListener('click', cerrarComprobante);
    $('modalComprobante').addEventListener('click', e => {
        if (e.target === e.currentTarget)
            cerrarComprobante(); // clic en el fondo oscuro
    });
    document.addEventListener('keydown', e => {
        if (e.key === 'Escape' && !$('modalComprobante').hidden)
            cerrarComprobante();
    });
    const tbodyItems = document.querySelector('#tablaDetalles tbody');
    tbodyItems.addEventListener('input', e => {
        const input = e.target;
        const tr = input.closest('tr');
        const campo = input.dataset.campo;
        if (!tr || !campo)
            return;
        items[Number(tr.dataset.i)][campo] = parseFloat(input.value) || 0;
        actualizarTotales(); // no re-renderiza las filas, así no se pierde el foco
    });
    tbodyItems.addEventListener('click', e => {
        const tr = e.target.closest('tr');
        if (tr && e.target.closest('[data-quitar]')) {
            items.splice(Number(tr.dataset.i), 1);
            renderItems();
        }
    });
    // Catálogo
    const dlg = $('dlgCatalogo');
    $('btnCatalogo').addEventListener('click', abrirCatalogo);
    $('btnCerrarCatalogo').addEventListener('click', () => dlg.close());
    $('btnListoCatalogo').addEventListener('click', () => dlg.close());
    dlg.addEventListener('click', e => { if (e.target === dlg)
        dlg.close(); }); // clic en el fondo oscuro
    $('buscarArticulo').addEventListener('input', renderCatalogo);
    const lista = $('listaArticulos');
    const articuloDe = (el) => {
        const li = el.closest('[data-art]');
        const art = li && catalogo.find(a => a.id === Number(li.dataset.art));
        return li && art ? { li, art } : null;
    };
    lista.addEventListener('click', e => {
        const t = e.target;
        const ctx = articuloDe(t);
        if (!ctx)
            return;
        const input = ctx.li.querySelector('[data-cant]');
        const actual = Math.max(1, Math.floor(Number(input.value)) || 1);
        const fijar = (n) => {
            input.value = String(n);
            cantidadesModal.set(ctx.art.id, n);
            actualizarTotalFila(ctx.li, ctx.art, n);
        };
        if (t.closest('[data-mas]'))
            fijar(actual + 1);
        else if (t.closest('[data-menos]'))
            fijar(Math.max(1, actual - 1));
        else if (t.closest('[data-agregar]'))
            agregarAlCarrito(ctx.art, actual);
    });
    lista.addEventListener('input', e => {
        const input = e.target;
        const ctx = articuloDe(input);
        if (ctx && input.matches('[data-cant]')) {
            const n = Math.max(1, Math.floor(Number(input.value)) || 1);
            cantidadesModal.set(ctx.art.id, n);
            actualizarTotalFila(ctx.li, ctx.art, n);
        }
    });
    $('formFactura').addEventListener('submit', guardarFactura);
    renderItems();
    cargarClientes();
    cargarCatalogo().then(() => actualizarTotales());
});
