package com.bonoya.platform.bonos.interfaces.rest.resources;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TREAEnriquecidoRequest {
    private Long bonoId;
    private BigDecimal precioCompra;
}
