package com.bonoya.platform.bonos.interfaces.rest.resources;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CalculoIndependienteRequest {
    private BigDecimal precioCompra;
    private BigDecimal valorNominal;
    private BigDecimal tasaCupon;
    private Integer plazoAnios;
    private Integer frecuenciaPagos;
}
