package com.tp.facturacionapi.controlador;

import com.tp.facturacionapi.modelo.Articulo;
import com.tp.facturacionapi.modelo.ArticuloDTO;
import com.tp.facturacionapi.modelo.ListaPrecioArticulo;
import com.tp.facturacionapi.repositorio.ArticuloRepositorio;
import com.tp.facturacionapi.repositorio.ListaPrecioArticuloRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/articulos")
@CrossOrigin(origins = "*")
public class ArticuloControlador {

    @Autowired
    private ArticuloRepositorio articuloRepositorio;

    @Autowired
    private ListaPrecioArticuloRepositorio listaPrecioArticuloRepositorio;

    // Catálogo para el "carrito": cada artículo con su precio de venta vigente
    @GetMapping
    public List<ArticuloDTO> listarArticulos() {
        Map<Long, Double> precios = new HashMap<>();
        for (ListaPrecioArticulo lpa : listaPrecioArticuloRepositorio.findAllByOrderByIdAsc()) {
            precios.put(lpa.getArticulo().getId(), lpa.getPrecioVenta()); // el último pisa al anterior
        }

        return articuloRepositorio.findAll().stream()
                .map((Articulo a) -> new ArticuloDTO(
                        a.getId(), a.getCodigo(), a.getDenominacion(), precios.getOrDefault(a.getId(), 0.0)))
                .toList();
    }
}
