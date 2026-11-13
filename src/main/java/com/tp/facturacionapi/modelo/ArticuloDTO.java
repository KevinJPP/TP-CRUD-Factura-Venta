package com.tp.facturacionapi.modelo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ArticuloDTO {
    private Long id;
    private String codigo;
    private String denominacion;
    private double precio;
}
