package com.bonoya.platform.bonos.interfaces.rest.resources;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class FlujoInversionistaResource {
    private Integer periodo;
    private LocalDate fecha;
    private BigDecimal cupon;
    private BigDecimal principal;
    private BigDecimal flujoTotal;
    private BigDecimal flujoNeto;
    private BigDecimal saldo;
    private String descripcion;
    private boolean esInversionInicial;
}
