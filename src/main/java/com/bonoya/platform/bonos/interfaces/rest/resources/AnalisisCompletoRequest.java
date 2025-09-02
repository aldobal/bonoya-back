package com.bonoya.platform.bonos.interfaces.rest.resources;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AnalisisCompletoRequest {
    private Long bonoId;
    private BigDecimal tasaEsperada;
    private BigDecimal precioCompra; // Precio que el inversionista está dispuesto a pagar
}
