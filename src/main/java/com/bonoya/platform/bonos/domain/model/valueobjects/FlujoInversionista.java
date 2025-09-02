package com.bonoya.platform.bonos.domain.model.valueobjects;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value Object que representa un flujo de caja desde la perspectiva del inversionista.
 * Incluye información específica sobre la inversión y el saldo neto acumulado.
 */
public class FlujoInversionista {
    
    private final Integer periodo;
    private final LocalDate fecha;
    private final BigDecimal cupon;
    private final BigDecimal principal;
    private final BigDecimal flujoTotal;
    private final BigDecimal flujoNeto;      // Flujo neto recibido (sin inversión inicial)
    private final BigDecimal saldoAcumulado; // Saldo neto acumulado del inversionista
    private final String descripcion;
    private final boolean esInversionInicial;
    
    public FlujoInversionista(Integer periodo, LocalDate fecha, BigDecimal cupon, 
                             BigDecimal principal, BigDecimal flujoTotal, BigDecimal flujoNeto,
                             BigDecimal saldoAcumulado, String descripcion, boolean esInversionInicial) {
        this.periodo = periodo;
        this.fecha = fecha;
        this.cupon = cupon != null ? cupon : BigDecimal.ZERO;
        this.principal = principal != null ? principal : BigDecimal.ZERO;
        this.flujoTotal = flujoTotal != null ? flujoTotal : BigDecimal.ZERO;
        this.flujoNeto = flujoNeto != null ? flujoNeto : BigDecimal.ZERO;
        this.saldoAcumulado = saldoAcumulado != null ? saldoAcumulado : BigDecimal.ZERO;
        this.descripcion = descripcion;
        this.esInversionInicial = esInversionInicial;
    }
    
    // Factory method para inversión inicial
    public static FlujoInversionista inversionInicial(BigDecimal precioCompra, LocalDate fecha) {
        return new FlujoInversionista(
            0, 
            fecha, 
            BigDecimal.ZERO, 
            BigDecimal.ZERO, 
            precioCompra.negate(), 
            precioCompra.negate(),
            precioCompra.negate(), 
            "Inversión inicial", 
            true
        );
    }
    
    // Factory method para flujo de cupón/principal
    public static FlujoInversionista flujoRecibido(Integer periodo, LocalDate fecha, 
                                                  BigDecimal cupon, BigDecimal principal,
                                                  BigDecimal saldoAcumulado, String descripcion) {
        BigDecimal flujoTotal = cupon.add(principal);
        return new FlujoInversionista(
            periodo, 
            fecha, 
            cupon, 
            principal, 
            flujoTotal, 
            flujoTotal, // Flujo neto es positivo (recibido)
            saldoAcumulado, 
            descripcion, 
            false
        );
    }
    
    // Getters
    public Integer getPeriodo() { return periodo; }
    public LocalDate getFecha() { return fecha; }
    public BigDecimal getCupon() { return cupon; }
    public BigDecimal getPrincipal() { return principal; }
    public BigDecimal getFlujoTotal() { return flujoTotal; }
    public BigDecimal getFlujoNeto() { return flujoNeto; }
    public BigDecimal getSaldoAcumulado() { return saldoAcumulado; }
    public String getDescripcion() { return descripcion; }
    public boolean isEsInversionInicial() { return esInversionInicial; }
    
    // Métodos de utilidad
    public boolean esPositivo() {
        return flujoNeto.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean esPeriodoFinal(int totalPeriodos) {
        return periodo != null && periodo == totalPeriodos;
    }
    
    public boolean haRecuperadoInversion() {
        return saldoAcumulado.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    @Override
    public String toString() {
        return String.format("FlujoInversionista{periodo=%d, fecha=%s, flujoTotal=%s, saldoAcumulado=%s, descripcion='%s'}", 
                           periodo, fecha, flujoTotal, saldoAcumulado, descripcion);
    }
}
