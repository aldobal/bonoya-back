package com.bonoya.platform.bonos.interfaces.rest.resources;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class FlujoInversionistaResponse {
    private Long bonoId;
    private String bonoNombre;
    private BigDecimal precioCompra;
    private List<FlujoInversionistaResource> flujos;
    
    // Métricas del inversionista
    private BigDecimal gananciaNeta;
    private BigDecimal rendimientoTotal;
    private Integer periodoRecuperacion;
    private BigDecimal totalCupones;
    private BigDecimal totalPrincipal;
}
