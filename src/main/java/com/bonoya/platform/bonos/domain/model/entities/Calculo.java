package com.bonoya.platform.bonos.domain.model.entities;

import com.bonoya.platform.shared.domain.model.entities.AuditableModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "calculos")
public class Calculo extends AuditableModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "bono_id")
    private Bono bono;
    
    private String inversorUsername;
    
    @Column(precision = 19, scale = 6)
    private BigDecimal tasaEsperada;
    
    @Column(precision = 19, scale = 6)
    private BigDecimal trea;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal precioMaximo;
    
    private LocalDate fechaCalculo;
    
    // Información adicional sobre el cálculo (como tipo de valoración)
    @Column(name = "informacion_adicional", length = 255)
    private String informacionAdicional;
    
    // Campos adicionales para enriquecer el historial
    @Column(name = "tipo_analisis", length = 50)
    private String tipoAnalisis = "TREA";
    
    // Parámetros del bono en el momento del cálculo (para historial)
    @Column(precision = 19, scale = 6)
    private BigDecimal valorNominal;
    
    @Column(precision = 19, scale = 6) 
    private BigDecimal tasaCupon;
    
    private Integer plazoAnios;
    
    private Integer frecuenciaPagos;
    
    @Column(length = 10)
    private String moneda;
    
    // Resultados adicionales del cálculo
    @Column(precision = 19, scale = 6)
    private BigDecimal treaPorcentaje;
    
    @Column(precision = 19, scale = 4)
    private BigDecimal valorPresente;
    
    // Cálculos financieros avanzados
    @Column(precision = 19, scale = 6)
    private BigDecimal tir; // Tasa Interna de Retorno
    
    @Column(precision = 19, scale = 4)
    private BigDecimal van; // Valor Actual Neto
    
    @Column(precision = 19, scale = 6)
    private BigDecimal tcea; // Tasa de Costo Efectivo Anual
    
    @Column(precision = 19, scale = 4)
    private BigDecimal duracion; // Duración de Macaulay
    
    @Column(precision = 19, scale = 4)
    private BigDecimal duracionModificada; // Duración Modificada
    
    @Column(precision = 19, scale = 4)
    private BigDecimal convexidad; // Convexidad del bono
    
    @Column(precision = 19, scale = 4)
    private BigDecimal precioJusto; // Precio justo calculado
    
    @Column(precision = 19, scale = 4)
    private BigDecimal valorPresenteCupones; // Valor presente solo de cupones
    
    @Column(precision = 19, scale = 4)
    private BigDecimal yield; // Rendimiento al vencimiento (YTM)
    
    @Column(precision = 19, scale = 6)
    private BigDecimal sensibilidadPrecio; // Sensibilidad del precio
    
    @Column(precision = 19, scale = 4)
    private BigDecimal gananciaCapital; // Ganancia de capital esperada
    
    @Column(precision = 19, scale = 4)
    private BigDecimal ingresosCupones; // Ingresos totales por cupones
    
    @Column(precision = 19, scale = 4)
    private BigDecimal rendimientoTotal; // Rendimiento total esperado
    
    public Long getBonoId() {
        return bono != null ? bono.getId() : null;
    }
    
    public String getBonoNombre() {
        return bono != null ? bono.getNombre() : null;
    }
    
    /**
     * Métodos de utilidad para convertir entre BigDecimal y double
     */
    public double getTasaEsperadaAsDouble() {
        return tasaEsperada != null ? tasaEsperada.doubleValue() : 0.0;
    }
    
    public double getTreaAsDouble() {
        return trea != null ? trea.doubleValue() : 0.0;
    }
    
    public double getPrecioMaximoAsDouble() {
        return precioMaximo != null ? precioMaximo.doubleValue() : 0.0;
    }
    
    public void setTasaEsperadaFromDouble(double value) {
        this.tasaEsperada = BigDecimal.valueOf(value);
    }
    
    public void setTreaFromDouble(double value) {
        this.trea = BigDecimal.valueOf(value);
    }
    
    public void setPrecioMaximoFromDouble(double value) {
        this.precioMaximo = BigDecimal.valueOf(value);
    }
    
    // Getters y setters para los nuevos campos
    public String getTipoAnalisis() {
        return tipoAnalisis;
    }
    
    public void setTipoAnalisis(String tipoAnalisis) {
        this.tipoAnalisis = tipoAnalisis;
    }
    
    public BigDecimal getValorNominal() {
        return valorNominal;
    }
    
    public void setValorNominal(BigDecimal valorNominal) {
        this.valorNominal = valorNominal;
    }
    
    public BigDecimal getTasaCupon() {
        return tasaCupon;
    }
    
    public void setTasaCupon(BigDecimal tasaCupon) {
        this.tasaCupon = tasaCupon;
    }
    
    public Integer getPlazoAnios() {
        return plazoAnios;
    }
    
    public void setPlazoAnios(Integer plazoAnios) {
        this.plazoAnios = plazoAnios;
    }
    
    public Integer getFrecuenciaPagos() {
        return frecuenciaPagos;
    }
    
    public void setFrecuenciaPagos(Integer frecuenciaPagos) {
        this.frecuenciaPagos = frecuenciaPagos;
    }
    
    public String getMoneda() {
        return moneda;
    }
    
    public void setMoneda(String moneda) {
        this.moneda = moneda;
    }
    
    public BigDecimal getTreaPorcentaje() {
        return treaPorcentaje;
    }
    
    public void setTreaPorcentaje(BigDecimal treaPorcentaje) {
        this.treaPorcentaje = treaPorcentaje;
    }
    
    public BigDecimal getValorPresente() {
        return valorPresente;
    }
    
    public void setValorPresente(BigDecimal valorPresente) {
        this.valorPresente = valorPresente;
    }
    
    // Getters y setters para cálculos financieros avanzados
    public BigDecimal getTir() {
        return tir;
    }
    
    public void setTir(BigDecimal tir) {
        this.tir = tir;
    }
    
    public BigDecimal getVan() {
        return van;
    }
    
    public void setVan(BigDecimal van) {
        this.van = van;
    }
    
    public BigDecimal getTcea() {
        return tcea;
    }
    
    public void setTcea(BigDecimal tcea) {
        this.tcea = tcea;
    }
    
    public BigDecimal getDuracion() {
        return duracion;
    }
    
    public void setDuracion(BigDecimal duracion) {
        this.duracion = duracion;
    }
    
    public BigDecimal getDuracionModificada() {
        return duracionModificada;
    }
    
    public void setDuracionModificada(BigDecimal duracionModificada) {
        this.duracionModificada = duracionModificada;
    }
    
    public BigDecimal getConvexidad() {
        return convexidad;
    }
    
    public void setConvexidad(BigDecimal convexidad) {
        this.convexidad = convexidad;
    }
    
    public BigDecimal getPrecioJusto() {
        return precioJusto;
    }
    
    public void setPrecioJusto(BigDecimal precioJusto) {
        this.precioJusto = precioJusto;
    }
    
    public BigDecimal getValorPresenteCupones() {
        return valorPresenteCupones;
    }
    
    public void setValorPresenteCupones(BigDecimal valorPresenteCupones) {
        this.valorPresenteCupones = valorPresenteCupones;
    }
    
    public BigDecimal getYield() {
        return yield;
    }
    
    public void setYield(BigDecimal yield) {
        this.yield = yield;
    }
    
    public BigDecimal getSensibilidadPrecio() {
        return sensibilidadPrecio;
    }
    
    public void setSensibilidadPrecio(BigDecimal sensibilidadPrecio) {
        this.sensibilidadPrecio = sensibilidadPrecio;
    }
    
    public BigDecimal getGananciaCapital() {
        return gananciaCapital;
    }
    
    public void setGananciaCapital(BigDecimal gananciaCapital) {
        this.gananciaCapital = gananciaCapital;
    }
    
    public BigDecimal getIngresosCupones() {
        return ingresosCupones;
    }
    
    public void setIngresosCupones(BigDecimal ingresosCupones) {
        this.ingresosCupones = ingresosCupones;
    }
    
    public BigDecimal getRendimientoTotal() {
        return rendimientoTotal;
    }
    
    public void setRendimientoTotal(BigDecimal rendimientoTotal) {
        this.rendimientoTotal = rendimientoTotal;
    }
}