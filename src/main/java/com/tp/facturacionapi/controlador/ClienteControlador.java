package com.tp.facturacionapi.controlador;

import com.tp.facturacionapi.modelo.Cliente;
import com.tp.facturacionapi.repositorio.ClienteRepositorio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
@CrossOrigin(origins = "*")
public class ClienteControlador {

    @Autowired
    private ClienteRepositorio clienteRepositorio;

    @GetMapping
    public List<Cliente> listarClientes() {
        return clienteRepositorio.findAll();
    }

    @PostMapping
    public Cliente guardarCliente(@RequestBody Cliente cliente) {
        if (cliente.getDenominacion() == null || cliente.getDenominacion().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La denominación del cliente es obligatoria.");
        }
        cliente.setDenominacion(cliente.getDenominacion().trim());
        return clienteRepositorio.save(cliente);
    }
}
