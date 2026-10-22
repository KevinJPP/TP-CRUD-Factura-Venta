package modelo;

import com.tp.modelo.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.time.LocalDateTime;

public class Main {

    public static void main(String[] args) {

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FacturacionPU");
        EntityManager em = emf.createEntityManager();

        try {

            em.getTransaction().begin();


            Usuario usuario = new Usuario();
            usuario.setUsuario("admin");
            usuario.setClave("1234");
            usuario.setNombre("Carlos");
            usuario.setApellido("Gómez");
            em.persist(usuario);

            PuntoVenta puntoVenta = new PuntoVenta();
            puntoVenta.setNumero(1);
            puntoVenta.setDescripcion("Sucursal Central");
            puntoVenta.setFechaAlta(LocalDateTime.now());
            puntoVenta.setFechaModificacion(LocalDateTime.now());
            puntoVenta.setUsuarioCarga(usuario);
            puntoVenta.setUsuarioModificacion(usuario);
            em.persist(puntoVenta);

            Articulo articulo = new Articulo();
            articulo.setCodigo("ART-100");
            articulo.setDenominacion("Monitor 24 pulgadas");
            articulo.setFechaAlta(LocalDateTime.now());
            articulo.setFechaModificacion(LocalDateTime.now());
            articulo.setUsuarioCarga(usuario);
            articulo.setUsuarioModificacion(usuario);
            em.persist(articulo);

            ListaPrecio listaPrecio = new ListaPrecio();
            listaPrecio.setCodigo("LP-01");
            listaPrecio.setDenominacion("General Contado");
            listaPrecio.setFechaAlta(LocalDateTime.now());
            listaPrecio.setFechaModificacion(LocalDateTime.now());
            listaPrecio.setUsuarioCarga(usuario);
            listaPrecio.setUsuarioModificacion(usuario);
            em.persist(listaPrecio);

            ListaPrecioArticulo lpArticulo = new ListaPrecioArticulo();
            lpArticulo.setArticulo(articulo);
            lpArticulo.setListaPrecio(listaPrecio);
            lpArticulo.setPrecioVenta(250000.0);
            lpArticulo.setFechaAlta(LocalDateTime.now());
            lpArticulo.setFechaModificacion(LocalDateTime.now());
            lpArticulo.setUsuarioCarga(usuario);
            lpArticulo.setUsuarioModificacion(usuario);
            em.persist(lpArticulo);


            FacturaVenta factura = new FacturaVenta();
            factura.setNumero(5001L);
            factura.setFechaEmision(LocalDateTime.now());
            factura.setPuntoVenta(puntoVenta);
            factura.setEstado("EMITIDA");
            factura.setImporteTotal(500000.0);
            factura.setFechaAlta(LocalDateTime.now());
            factura.setFechaModificacion(LocalDateTime.now());
            factura.setUsuarioCarga(usuario);
            factura.setUsuarioModificacion(usuario);


            FacturaVentaDetalle detalle1 = new FacturaVentaDetalle();
            detalle1.setListaPrecioArticulo(lpArticulo);
            detalle1.setCantidad(2);
            detalle1.setPrecioUnitario(250000.0);
            detalle1.setImporteSubtotal(500000.0);
            detalle1.setDescripcion("Venta de 2 Monitores");

            factura.addDetalle(detalle1);


            em.persist(factura);


            em.getTransaction().commit();
            System.out.println(">>> ¡TP Ejecutado correctamente! Factura e ítems guardados en cascada.");

        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }
}