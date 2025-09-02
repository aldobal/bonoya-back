package com.bonoya.platform.bonos.interfaces.rest.resources;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CalculoResource {
    private Long id;
    private Long bonoId;
    private String bonoNombre;
    private String inversorUsername;
    private BigDecimal tasaEsperada;
    private BigDecimal trea;
    private BigDecimal precioMaximo;
    private LocalDate fechaCalculo;
    private String informacionAdicional;
    
    // Campos adicionales para el historial enriquecido
    private String tipoAnalisis;
    private LocalDate fecha; // Alias para fechaCalculo
    private String bono; // Alias para bonoNombre
    
    // Parámetros del análisis
    private ParametrosAnalisis parametros;
    
    // Resultados del análisis  
    private ResultadosAnalisis resultados;
    
    // Información del backend específica
    private CalculoBackend calculoBackend;
    
    @Getter
    @Setter
    public static class ParametrosAnalisis {
        private BigDecimal tasaEsperada;
        private BigDecimal valorNominal;
        private BigDecimal tasa; // tasa cupón
        private Integer plazo;
        private Integer frecuenciaPago;
        private String moneda;
    }
    
    @Getter
    @Setter
    public static class ResultadosAnalisis {
        private BigDecimal trea;
        private BigDecimal precioMaximo;
        private BigDecimal tasaEsperada;
        private BigDecimal treaPorcentaje;
        private BigDecimal valorPresente;
        
        // Cálculos financieros avanzados
        private BigDecimal tir; // Tasa Interna de Retorno
        private BigDecimal van; // Valor Actual Neto
        private BigDecimal tcea; // Tasa de Costo Efectivo Anual
        private BigDecimal duracion; // Duración de Macaulay
        private BigDecimal duracionModificada; // Duración Modificada
        private BigDecimal convexidad; // Convexidad del bono
        private BigDecimal precioJusto; // Precio justo calculado
        private BigDecimal valorPresenteCupones; // Valor presente solo de cupones
        private BigDecimal yield; // Rendimiento al vencimiento (YTM)
        private BigDecimal sensibilidadPrecio; // Sensibilidad del precio
        private BigDecimal gananciaCapital; // Ganancia de capital esperada
        private BigDecimal ingresosCupones; // Ingresos totales por cupones
        private BigDecimal rendimientoTotal; // Rendimiento total esperado
    }
    
    @Getter
    @Setter
    public static class CalculoBackend {
        private BigDecimal treaPorcentaje;
        private BigDecimal valorPresente;
        private LocalDate fechaCalculo;
    }
}