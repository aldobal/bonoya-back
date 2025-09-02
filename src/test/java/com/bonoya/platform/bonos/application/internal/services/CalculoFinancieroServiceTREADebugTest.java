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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test de debug específico para verificar los cálculos de TREA
 */
@ExtendWith(MockitoExtension.class)
class CalculoFinancieroServiceTREADebugTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
        when(calculoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void debugTREABonoTesoroAmericano() {
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("5.0"));
        bono.setPlazoAnios(10);
        bono.setFrecuenciaPagos(2);
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal precioMercado = new BigDecimal("1000.00"); // A la par
        
        // Act
        BigDecimal trea = calculoService.calcularTREA(bono, precioMercado);
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        
        // Debug output
        System.out.println("=== DEBUG TREA BONO TESORO AMERICANO ===");
        System.out.println("Valor Nominal: " + bono.getValorNominal());
        System.out.println("Tasa Cupón: " + bono.getTasaCupon() + "%");
        System.out.println("Plazo: " + bono.getPlazoAnios() + " años");
        System.out.println("Frecuencia: " + bono.getFrecuenciaPagos() + " pagos/año");
        System.out.println("Precio Compra: " + precioMercado);
        System.out.println("TREA Calculada: " + trea + "%");
        System.out.println("Total de flujos: " + flujos.size());
        
        // Mostrar primeros 5 flujos
        System.out.println("\nPrimeros 5 flujos:");
        for (int i = 0; i < Math.min(5, flujos.size()); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            System.out.println("Período " + flujo.getPeriodo() + 
                             ": Flujo=" + flujo.getFlujoTotal() + 
                             ", Cupón=" + flujo.getCupon() +
                             ", Amortización=" + flujo.getAmortizacion());
        }
        
        // Mostrar último flujo
        if (flujos.size() > 5) {
            FlujoFinanciero ultimoFlujo = flujos.get(flujos.size() - 1);
            System.out.println("Último flujo (período " + ultimoFlujo.getPeriodo() + 
                             "): Flujo=" + ultimoFlujo.getFlujoTotal() + 
                             ", Cupón=" + ultimoFlujo.getCupon() +
                             ", Amortización=" + ultimoFlujo.getAmortizacion());
        }
        
        // Verificar cálculo manual para bono semestral
        System.out.println("\n=== VERIFICACIÓN MANUAL ===");
        BigDecimal cuponSemestral = new BigDecimal("25.00"); // 5% anual / 2 = 2.5% semestral sobre $1000
        BigDecimal tasaAnualEquivalente = new BigDecimal("1.025").pow(2).subtract(BigDecimal.ONE);
        System.out.println("Cupón semestral esperado: " + cuponSemestral);
        System.out.println("Tasa anual equivalente a 2.5% semestral: " + 
                         tasaAnualEquivalente.multiply(new BigDecimal("100")) + "%");
        
        // Para un bono a la par, la TREA debe ser muy cercana a la tasa cupón efectiva
        System.out.println("TREA esperada para bono a la par: ~5.06%");
        System.out.println("Diferencia: " + Math.abs(trea.doubleValue() - 5.06) + "%");
    }

    @Test
    void debugTREABonoDescuento() {
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("4.0"));
        bono.setPlazoAnios(5);
        bono.setFrecuenciaPagos(2);
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal precioDescuento = new BigDecimal("900.00"); // 10% descuento
        
        // Act
        BigDecimal trea = calculoService.calcularTREA(bono, precioDescuento);
        
        // Debug output
        System.out.println("\n=== DEBUG TREA BONO CON DESCUENTO ===");
        System.out.println("Valor Nominal: " + bono.getValorNominal());
        System.out.println("Tasa Cupón: " + bono.getTasaCupon() + "%");
        System.out.println("Plazo: " + bono.getPlazoAnios() + " años");
        System.out.println("Precio Compra: " + precioDescuento + " (10% descuento)");
        System.out.println("TREA Calculada: " + trea + "%");
        
        // Para un bono con descuento, la TREA debe ser mayor que la tasa cupón
        System.out.println("TREA esperada: > 4% (debido al descuento)");
        System.out.println("¿TREA > Tasa Cupón?: " + (trea.doubleValue() > 4.0));
    }
}
