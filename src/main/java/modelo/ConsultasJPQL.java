package modelo;

import com.tp.modelo.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class ConsultasJPQL {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {
            System.out.println("=== EJECUTANDO CONSULTAS JPQL ===\n");

            // NIVEL 1: Consultas Básicas y Proyecciones


            // 1. Consulta de Entidades Completas
            List<FacturaVenta> facturas = em.createQuery(
                            "SELECT f FROM FacturaVenta f", FacturaVenta.class)
                    .getResultList();

            // 2. Proyección de Atributos Específicos
            List<Object[]> datosFacturas = em.createQuery(
                            "SELECT f.numero, f.fechaEmision, f.importeTotal FROM FacturaVenta f", Object[].class)
                    .getResultList();

            // 3. Filtrado por Igualdad (WHERE)
            List<Articulo> articulosRubro = em.createQuery(
                            "SELECT a FROM Articulo a WHERE a.rubro.denominacion = :denominacion", Articulo.class)
                    .setParameter("denominacion", "Electrónica")
                    .getResultList();

            // 4. Filtrado por Rango de Fechas (BETWEEN)
            List<FacturaVenta> facturasEntreFechas = em.createQuery(
                            "SELECT f FROM FacturaVenta f WHERE f.fechaEmision BETWEEN :fechaInicio AND :fechaFin", FacturaVenta.class)
                    .setParameter("fechaInicio", LocalDateTime.now().minusDays(30))
                    .setParameter("fechaFin", LocalDateTime.now())
                    .getResultList();


            // NIVEL 2: Condicionales Combinados, Operadores de Texto y Agregaciones Básicas

            // 5. Condicionales Complejos y Verificación de Nulos (AND, OR, IS NULL)
            List<FacturaVenta> facturasEmitidasAltas = em.createQuery(
                            "SELECT f FROM FacturaVenta f WHERE f.estado = 'EMITIDA' AND f.importeTotal > 10000 AND f.fechaAnulacion IS NULL", FacturaVenta.class)
                    .getResultList();

            // 6. Búsqueda por Patrón de Texto (LIKE y LOWER)
            List<FacturaVenta> facturasFiltro = em.createQuery(
                            "SELECT f FROM FacturaVenta f WHERE LOWER(f.observaciones) LIKE :texto", FacturaVenta.class)
                    .setParameter("texto", "%venta%")
                    .getResultList();

            // 7. Valores Distintos y Ordenamiento (DISTINCT y ORDER BY)
            List<String> estadosDistintos = em.createQuery(
                            "SELECT DISTINCT f.estado FROM FacturaVenta f ORDER BY f.estado ASC", String.class)
                    .getResultList();

            // 8. Funciones de Agregación Simples (COUNT, SUM, AVG)
            Object[] estadisticas = em.createQuery(
                            "SELECT COUNT(f), SUM(f.importeTotal), AVG(f.importeTotal) FROM FacturaVenta f", Object[].class)
                    .getSingleResult();

            // 9. Operador de Inclusión (IN)
            List<PuntoVenta> puntosVentaIn = em.createQuery(
                            "SELECT p FROM PuntoVenta p WHERE p.numero IN :numeros", PuntoVenta.class)
                    .setParameter("numeros", Arrays.asList(1, 2, 5))
                    .getResultList();


            // NIVEL 3: Navegación de Entidades, JOINs y Subconsultas Simples

            // 10. Navegación Implícita por Relaciones (Path Expressions)
            List<FacturaVenta> facturasPorUsuario = em.createQuery(
                            "SELECT f FROM FacturaVenta f WHERE f.usuarioCarga.usuario = :username", FacturaVenta.class)
                    .setParameter("username", "admin")
                    .getResultList();

            // 11. Cláusula INNER JOIN Explícita
            List<FacturaVentaDetalle> detallesPuntoVenta = em.createQuery(
                            "SELECT d FROM FacturaVentaDetalle d INNER JOIN d.factura f WHERE f.puntoVenta.numero = :numPuntoVenta", FacturaVentaDetalle.class)
                    .setParameter("numPuntoVenta", 1)
                    .getResultList();

            // 12. Cláusula LEFT JOIN (Inclusión de Nulos)
            List<Object[]> articulosYMarcas = em.createQuery(
                            "SELECT a.denominacion, m.denominacion FROM Articulo a LEFT JOIN a.marca m", Object[].class)
                    .getResultList();

            // 13. Navegación Multinivel con JOINs Combinados
            List<FacturaVenta> facturasPorMarca = em.createQuery(
                            "SELECT DISTINCT f FROM FacturaVenta f JOIN f.detalles d JOIN d.listaPrecioArticulo lpa JOIN lpa.articulo a JOIN a.marca m WHERE m.denominacion = :nombreMarca", FacturaVenta.class)
                    .setParameter("nombreMarca", "Samsung")
                    .getResultList();

            // 14. Subconsulta en Cláusula WHERE
            List<FacturaVenta> facturasSobrePromedio = em.createQuery(
                            "SELECT f FROM FacturaVenta f WHERE f.importeTotal > (SELECT AVG(f2.importeTotal) FROM FacturaVenta f2)", FacturaVenta.class)
                    .getResultList();

            // 15. Filtrado por Estado
            List<FacturaVenta> facturasPorEstado = em.createQuery(
                            "SELECT f FROM FacturaVenta f WHERE f.estado = :estado", FacturaVenta.class)
                    .setParameter("estado", "EMITIDA")
                    .getResultList();


            // -------------------------------------------------------------
            // NIVEL 4: Agrupamiento (GROUP BY) y Filtros de Grupo (HAVING)
            // -------------------------------------------------------------

            // 16. Agrupamiento Básico (GROUP BY)
            List<Object[]> resumenPuntoVenta = em.createQuery(
                            "SELECT f.puntoVenta.descripcion, COUNT(f), SUM(f.importeTotal) FROM FacturaVenta f GROUP BY f.puntoVenta.descripcion", Object[].class)
                    .getResultList();

            // 17. Agrupamiento con Condicional de Grupo (HAVING)
            List<String> usuariosProductivos = em.createQuery(
                            "SELECT u.usuario FROM FacturaVenta f JOIN f.usuarioCarga u GROUP BY u.usuario HAVING COUNT(f) > 5", String.class)
                    .getResultList();

            // 18. Agrupamiento y Agregación sobre Entidades Relacionadas
            List<Object[]> ventasPorMarca = em.createQuery(
                            "SELECT m.denominacion, SUM(d.cantidad), SUM(d.importeSubtotal) FROM FacturaVentaDetalle d JOIN d.listaPrecioArticulo lpa JOIN lpa.articulo a JOIN a.marca m GROUP BY m.denominacion", Object[].class)
                    .getResultList();

            // 19. Agrupamiento por Punto de Venta
            List<Object[]> ventasPorPuntoVenta = em.createQuery(
                            "SELECT p.descripcion, SUM(f.importeTotal) FROM FacturaVenta f JOIN f.puntoVenta p GROUP BY p.descripcion", Object[].class)
                    .getResultList();


            // -------------------------------------------------------------
            // NIVEL 5: Subconsultas Correlacionadas, EXISTS, NOT EXISTS y CASE WHEN
            // -------------------------------------------------------------

            // 20. Subconsulta Correlacionada con EXISTS
            List<Marca> marcasFacturadas = em.createQuery(
                            "SELECT m FROM Marca m WHERE EXISTS (SELECT d FROM FacturaVentaDetalle d WHERE d.listaPrecioArticulo.articulo.marca = m)", Marca.class)
                    .getResultList();

            // 21. Subconsulta Correlacionada con NOT EXISTS
            List<Articulo> articulosSinVender = em.createQuery(
                            "SELECT a FROM Articulo a WHERE NOT EXISTS (SELECT d FROM FacturaVentaDetalle d WHERE d.listaPrecioArticulo.articulo = a)", Articulo.class)
                    .getResultList();

            // 22. Proyección Condicional (CASE WHEN)
            List<Object[]> categoriasFacturas = em.createQuery(
                            "SELECT f.numero, f.importeTotal, " +
                                    "CASE WHEN f.importeTotal > 50000 THEN 'ALTO VALOR' " +
                                    "     WHEN f.importeTotal BETWEEN 10000 AND 50000 THEN 'MEDIO VALOR' " +
                                    "     ELSE 'BAJO VALOR' END " +
                                    "FROM FacturaVenta f ORDER BY f.importeTotal DESC", Object[].class)
                    .getResultList();

            System.out.println(">>> ¡Las 22 consultas JPQL fueron compiladas y ejecutadas exitosamente!");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }
}
