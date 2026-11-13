package com.tp.facturacionapi;

import com.tp.facturacionapi.modelo.Articulo;
import com.tp.facturacionapi.modelo.ListaPrecio;
import com.tp.facturacionapi.modelo.ListaPrecioArticulo;
import com.tp.facturacionapi.repositorio.ArticuloRepositorio;
import com.tp.facturacionapi.repositorio.ListaPrecioArticuloRepositorio;
import com.tp.facturacionapi.repositorio.ListaPrecioRepositorio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Carga un catálogo tipo supermercado al arrancar. Es seguro correrlo muchas veces:
 *  - crea solo los artículos cuyo código todavía no existe,
 *  - asigna precio solo a los artículos que no tienen ninguno (no pisa precios ya cargados).
 */
@Configuration
public class DatosIniciales {

    // {codigo, denominacion, precio}
    private static final Object[][] CATALOGO = {
            // Almacén
            {"ALM-001", "Fideos tirabuzón 500 g", 1250.0},
            {"ALM-002", "Fideos spaghetti 500 g", 1180.0},
            {"ALM-003", "Fideos moñito 500 g", 1190.0},
            {"ALM-004", "Arroz largo fino 1 kg", 1890.0},
            {"ALM-005", "Arroz integral 1 kg", 2350.0},
            {"ALM-006", "Harina 000 1 kg", 980.0},
            {"ALM-007", "Harina de maíz (polenta) 500 g", 890.0},
            {"ALM-008", "Azúcar 1 kg", 1320.0},
            {"ALM-009", "Sal fina 500 g", 720.0},
            {"ALM-010", "Aceite de girasol 900 ml", 2650.0},
            {"ALM-011", "Aceite de oliva 500 ml", 7900.0},
            {"ALM-012", "Puré de tomate 520 g", 980.0},
            {"ALM-013", "Salsa de tomate 340 g", 850.0},
            {"ALM-014", "Lentejas 400 g", 1450.0},
            {"ALM-015", "Garbanzos 400 g", 1380.0},
            {"ALM-016", "Atún en aceite 170 g", 2900.0},
            {"ALM-017", "Mayonesa 475 g", 2300.0},
            {"ALM-018", "Vinagre de alcohol 500 ml", 780.0},
            // Desayuno y dulces
            {"DES-001", "Yerba mate 1 kg", 4890.0},
            {"DES-002", "Café molido 250 g", 5200.0},
            {"DES-003", "Té en saquitos x25", 1600.0},
            {"DES-004", "Mermelada de durazno 454 g", 2100.0},
            {"DES-005", "Dulce de leche 400 g", 2750.0},
            {"DES-006", "Galletitas de agua", 1050.0},
            {"DES-007", "Galletitas dulces 300 g", 1500.0},
            {"DES-008", "Cereales 300 g", 3400.0},
            // Lácteos y fiambres
            {"LAC-001", "Leche entera 1 L", 1450.0},
            {"LAC-002", "Leche descremada 1 L", 1450.0},
            {"LAC-003", "Yogur bebible 1 L", 2100.0},
            {"LAC-004", "Queso cremoso 1 kg", 8900.0},
            {"LAC-005", "Manteca 200 g", 2600.0},
            {"LAC-006", "Huevos docena", 3600.0},
            // Bebidas
            {"BEB-001", "Agua mineral 2 L", 1100.0},
            {"BEB-002", "Agua con gas 500 ml", 750.0},
            {"BEB-003", "Gaseosa cola 2,25 L", 2400.0},
            {"BEB-004", "Gaseosa lima-limón 2,25 L", 2300.0},
            {"BEB-005", "Jugo en polvo naranja", 350.0},
            {"BEB-006", "Cerveza rubia lata 473 ml", 1500.0},
            {"BEB-007", "Vino tinto 750 ml", 4200.0},
            // Limpieza e higiene
            {"LIM-001", "Detergente 750 ml", 2200.0},
            {"LIM-002", "Lavandina 1 L", 1000.0},
            {"LIM-003", "Jabón en polvo 800 g", 3900.0},
            {"LIM-004", "Papel higiénico x4", 2800.0},
            {"LIM-005", "Rollo de cocina x3", 2500.0},
            {"LIM-006", "Jabón de tocador x3", 2100.0},
            {"LIM-007", "Shampoo 400 ml", 3700.0},
            {"LIM-008", "Pasta dental 90 g", 1800.0},
            // Artículo que ya estaba cargado en la base sin precio
            {"ART-100", "Monitor 24 pulgadas", 185000.0},
    };

    @Bean
    CommandLineRunner cargarArticulos(ArticuloRepositorio articulos,
                                      ListaPrecioRepositorio listas,
                                      ListaPrecioArticuloRepositorio precios) {
        return args -> {
            ListaPrecio lista = listas.findAll().stream().findFirst().orElseGet(() -> {
                ListaPrecio nueva = new ListaPrecio();
                nueva.setCodigo("LP-GENERAL");
                nueva.setDenominacion("Lista general");
                return listas.save(nueva);
            });

            Map<String, Articulo> porCodigo = new HashMap<>();
            articulos.findAll().forEach(a -> porCodigo.putIfAbsent(a.getCodigo(), a));

            Set<Long> conPrecio = new HashSet<>();
            precios.findAll().forEach(p -> conPrecio.add(p.getArticulo().getId()));

            for (Object[] fila : CATALOGO) {
                String codigo = (String) fila[0];
                Articulo a = porCodigo.get(codigo);
                if (a == null) {
                    a = new Articulo();
                    a.setCodigo(codigo);
                    a.setDenominacion((String) fila[1]);
                    a = articulos.save(a);
                }
                if (!conPrecio.contains(a.getId())) {
                    ListaPrecioArticulo lpa = new ListaPrecioArticulo();
                    lpa.setListaPrecio(lista);
                    lpa.setArticulo(a);
                    lpa.setPrecioVenta((Double) fila[2]);
                    precios.save(lpa);
                }
            }
        };
    }
}
