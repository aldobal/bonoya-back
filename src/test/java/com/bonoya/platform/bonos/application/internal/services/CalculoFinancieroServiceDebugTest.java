package com.bonoya.platform.bonos.application.internal.services;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;
import com.bonoya.platform.bonos.infrastructure.persistence.jpa.repositories.CalculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test para depurar y verificar cálculos financieros paso a paso
 */
@ExtendWith(MockitoExtension.class)
class CalculoFinancieroServiceDebugTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
    }

    /**
     * Test para verificar cálculo TREA paso a paso
     * Caso: Bono $1000, 6% anual, 2 años, pagos semestrales, precio $980
     */
    @Test
    void debugTREA_CalculoDetallado() {
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("6.0")); // 6% anual
        bono.setPlazoAnios(2);
        bono.setFrecuenciaPagos(2); // Semestral
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal precioCompra = new BigDecimal("980.00");
        
        // Act
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        BigDecimal treaCalculada = calculoService.calcularTREA(bono, precioCompra);
        
        // Debug - Imprimir flujos
        System.out.println("=== DEBUG FLUJOS ===");
        for (int i = 0; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            System.out.printf("Período %d: Fecha=%s, Flujo=%.2f, Cupón=%.2f, Amortización=%.2f%n", 
                flujo.getPeriodo(), 
                flujo.getFecha(),
                flujo.getFlujoTotal() != null ? flujo.getFlujoTotal().doubleValue() : 0.0,
                flujo.getCupon() != null ? flujo.getCupon().doubleValue() : 0.0,
                flujo.getAmortizacion() != null ? flujo.getAmortizacion().doubleValue() : 0.0);
        }
        
        // Debug - Cálculo manual de TREA
        System.out.println("\n=== CÁLCULO MANUAL TREA ===");
        
        // TREA = ((Flujos Recibidos / Precio Compra)^(1/años)) - 1
        BigDecimal totalFlujos = BigDecimal.ZERO;
        for (int i = 1; i < flujos.size(); i++) { // Saltar periodo 0
            totalFlujos = totalFlujos.add(flujos.get(i).getFlujoTotal());
        }
        
        System.out.printf("Precio compra: %.2f%n", precioCompra.doubleValue());
        System.out.printf("Total flujos recibidos: %.2f%n", totalFlujos.doubleValue());
        
        // Cálculo manual simple
        double ratio = totalFlujos.doubleValue() / precioCompra.doubleValue();
        double anios = bono.getPlazoAnios();
        double treaManual = Math.pow(ratio, 1.0/anios) - 1.0;
        
        System.out.printf("Ratio (Total/Precio): %.6f%n", ratio);
        System.out.printf("Años: %.1f%n", anios);
        System.out.printf("TREA manual: %.2f%%   %n", treaManual * 100);
        System.out.printf("TREA calculada: %.2f%%   %n", treaCalculada.doubleValue());
        
        // Verificar que los flujos sean correctos
        assertEquals(5, flujos.size(), "Debe tener 5 flujos");
        
        // Verificar flujo inicial (periodo 0)
        assertEquals(new BigDecimal("-1000.00"), flujos.get(0).getFlujoTotal(), 
            "Flujo inicial debe ser -$1000");
        
        // Verificar cupones semestrales ($30 cada uno)
        BigDecimal cuponEsperado = new BigDecimal("30.00");
        for (int i = 1; i <= 3; i++) {
            assertEquals(cuponEsperado.setScale(2), 
                        flujos.get(i).getFlujoTotal().setScale(2),
                        "Cupón " + i + " debe ser $30");
        }
        
        // Verificar flujo final
        assertEquals(new BigDecimal("1030.00"), 
                    flujos.get(4).getFlujoTotal().setScale(2),
                    "Flujo final debe ser $1030");
        
        // Verificar que TREA sea razonable (entre 5% y 10%)
        assertTrue(treaCalculada.doubleValue() >= 5.0 && treaCalculada.doubleValue() <= 10.0,
            "TREA debe estar entre 5% y 10%: " + treaCalculada);
        
        // La TREA manual y calculada deberían ser similares
        double diferencia = Math.abs(treaManual * 100 - treaCalculada.doubleValue());
        assertTrue(diferencia < 0.5, 
            String.format("Diferencia entre TREA manual (%.2f) y calculada (%.2f) debe ser < 0.5%%", 
                treaManual * 100, treaCalculada.doubleValue()));
    }

    /**
     * Test para verificar TIR usando método de bisección
     */
    @Test
    void debugTIR_MetodoBiseccion() {
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("6.0"));
        bono.setPlazoAnios(2);
        bono.setFrecuenciaPagos(2);
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal precioCompra = new BigDecimal("980.00");
        
        // Act
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        
        // Calcular TIR manualmente usando bisección
        double tirManual = calcularTIRBiseccionManual(flujos, precioCompra);
        
        System.out.println("\n=== DEBUG TIR ===");
        System.out.printf("TIR manual (bisección): %.4f%%   %n", tirManual);
        
        // Verificar con función de VAN
        double van = calcularVAN(flujos, precioCompra, tirManual / 100.0);
        System.out.printf("VAN con TIR manual: %.6f (debe ser ~0)%n", van);
        
        assertTrue(Math.abs(van) < 0.01, "VAN debe ser ~0 cuando se usa la TIR correcta");
        assertTrue(tirManual > 6.0 && tirManual < 10.0, "TIR debe estar entre 6% y 10%");
    }

    // Método auxiliar para calcular TIR usando bisección
    private double calcularTIRBiseccionManual(List<FlujoFinanciero> flujos, BigDecimal precioCompra) {
        double tirMin = -0.5; // -50%
        double tirMax = 2.0;   // 200%
        double precision = 0.0001; // 0.01%
        
        for (int i = 0; i < 100; i++) {
            double tirMedio = (tirMin + tirMax) / 2.0;
            double van = calcularVAN(flujos, precioCompra, tirMedio);
            
            if (Math.abs(van) <= precision) {
                return tirMedio * 100; // Convertir a porcentaje
            }
            
            if (van > 0) {
                tirMin = tirMedio;
            } else {
                tirMax = tirMedio;
            }
        }
        
        return (tirMin + tirMax) / 2.0 * 100;
    }
    
    // Método auxiliar para calcular VAN
    private double calcularVAN(List<FlujoFinanciero> flujos, BigDecimal precioCompra, double tasa) {
        double van = -precioCompra.doubleValue(); // Inversión inicial
        
        for (int i = 1; i < flujos.size(); i++) {
            double flujo = flujos.get(i).getFlujoTotal().doubleValue();
            double periodo = i * 0.5; // Períodos semestrales a años
            double valorPresente = flujo / Math.pow(1 + tasa, periodo);
            van += valorPresente;
        }
        
        return van;
    }
}
