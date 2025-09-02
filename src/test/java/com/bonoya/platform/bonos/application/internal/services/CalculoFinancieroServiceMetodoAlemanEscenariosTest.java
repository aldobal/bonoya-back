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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Tests adicionales de validación del método alemán con diferentes escenarios
 */
@ExtendWith(MockitoExtension.class)
class CalculoFinancieroServiceMetodoAlemanEscenariosTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
        lenient().when(calculoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    /**
     * Escenario 1: Bono trimestral de 2 años
     */
    @Test
    void testMetodoAlemanTrimestral() {
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("8.0"));  // 8% anual
        bono.setPlazoAnios(2);                      // 2 años
        bono.setFrecuenciaPagos(4);                 // Trimestral
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("ALEMAN");
        
        // Act
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        
        // Assert
        assertEquals(9, flujos.size(), "Debe haber 9 flujos (0 + 8 trimestres)");
        
        // Verificar amortización constante de $125 por trimestre
        BigDecimal amortizacionEsperada = new BigDecimal("125.00");
        for (int i = 1; i < flujos.size() - 1; i++) {
            assertEquals(amortizacionEsperada, flujos.get(i).getAmortizacion(),
                "Amortización trimestral debe ser $125");
        }
        
        // Verificar intereses decrecientes (2% trimestral)
        // Primer trimestre: $1000 * 2% = $20
        // Segundo trimestre: $875 * 2% = $17.50
        BigDecimal primerInteres = new BigDecimal("20.00");
        assertEquals(primerInteres, flujos.get(1).getInteres(),
            "Primer interés trimestral debe ser $20");
        
        // Los intereses deben ser decrecientes
        for (int i = 2; i < flujos.size(); i++) {
            assertTrue(flujos.get(i).getInteres().compareTo(flujos.get(i-1).getInteres()) <= 0,
                "Intereses deben ser decrecientes en método alemán");
        }
        
        System.out.println("✅ Método alemán trimestral validado correctamente");
    }

    /**
     * Escenario 2: Bono anual de 5 años
     */
    @Test
    void testMetodoAlemanAnual() {
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("5000.00"));
        bono.setTasaCupon(new BigDecimal("5.0"));   // 5% anual
        bono.setPlazoAnios(5);                       // 5 años
        bono.setFrecuenciaPagos(1);                  // Anual
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("ALEMAN");
        
        // Act
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        
        // Assert
        assertEquals(6, flujos.size(), "Debe haber 6 flujos (0 + 5 años)");
        
        // Verificar amortización constante de $1000 por año
        BigDecimal amortizacionEsperada = new BigDecimal("1000.00");
        for (int i = 1; i < flujos.size() - 1; i++) {
            assertEquals(amortizacionEsperada, flujos.get(i).getAmortizacion(),
                "Amortización anual debe ser $1000");
        }
        
        // Verificar cálculo manual de intereses
        // Año 1: $5000 * 5% = $250, Flujo = $250 + $1000 = $1250
        // Año 2: $4000 * 5% = $200, Flujo = $200 + $1000 = $1200
        // Año 3: $3000 * 5% = $150, Flujo = $150 + $1000 = $1150
        // Año 4: $2000 * 5% = $100, Flujo = $100 + $1000 = $1100
        // Año 5: $1000 * 5% = $50,  Flujo = $50  + $1000 = $1050
        
        BigDecimal[] interesesEsperados = {
            new BigDecimal("250.00"),
            new BigDecimal("200.00"),
            new BigDecimal("150.00"),
            new BigDecimal("100.00"),
            new BigDecimal("50.00")
        };
        
        for (int i = 0; i < interesesEsperados.length; i++) {
            assertEquals(interesesEsperados[i], flujos.get(i + 1).getInteres(),
                "Interés del año " + (i + 1) + " debe ser $" + interesesEsperados[i]);
        }
        
        System.out.println("✅ Método alemán anual validado correctamente");
    }

    /**
     * Escenario 3: Comparación directa alemán vs americano con cálculo manual
     */
    @Test
    void testComparacionDirectaAlemanAmericano() {
        // Arrange
        Bono bonoComun = new Bono();
        bonoComun.setValorNominal(new BigDecimal("1000.00"));
        bonoComun.setTasaCupon(new BigDecimal("12.0"));  // 12% anual (fácil de calcular)
        bonoComun.setPlazoAnios(2);                       // 2 años
        bonoComun.setFrecuenciaPagos(1);                  // Anual
        bonoComun.setFechaEmision(LocalDate.now());
        
        Bono bonoAmericano = new Bono();
        bonoAmericano.setValorNominal(bonoComun.getValorNominal());
        bonoAmericano.setTasaCupon(bonoComun.getTasaCupon());
        bonoAmericano.setPlazoAnios(bonoComun.getPlazoAnios());
        bonoAmericano.setFrecuenciaPagos(bonoComun.getFrecuenciaPagos());
        bonoAmericano.setFechaEmision(bonoComun.getFechaEmision());
        bonoAmericano.setMetodoAmortizacion("AMERICANO");
        
        Bono bonoAleman = new Bono();
        bonoAleman.setValorNominal(bonoComun.getValorNominal());
        bonoAleman.setTasaCupon(bonoComun.getTasaCupon());
        bonoAleman.setPlazoAnios(bonoComun.getPlazoAnios());
        bonoAleman.setFrecuenciaPagos(bonoComun.getFrecuenciaPagos());
        bonoAleman.setFechaEmision(bonoComun.getFechaEmision());
        bonoAleman.setMetodoAmortizacion("ALEMAN");
        
        // Act
        List<FlujoFinanciero> flujosAmericano = calculoService.calcularFlujoFinanciero(bonoAmericano);
        List<FlujoFinanciero> flujosAleman = calculoService.calcularFlujoFinanciero(bonoAleman);
        
        // CÁLCULO MANUAL ESPERADO:
        
        // AMERICANO: 
        // Año 1: $120 (solo interés)
        // Año 2: $1120 (interés + principal)
        // Total intereses: $240
        // Total flujos: $1240
        
        // ALEMÁN:
        // Año 1: $1000 * 12% + $500 = $120 + $500 = $620
        // Año 2: $500 * 12% + $500 = $60 + $500 = $560
        // Total intereses: $180
        // Total flujos: $1180
        
        // Assert - Validar resultados
        System.out.println("=== COMPARACIÓN DIRECTA ALEMÁN VS AMERICANO ===");
        
        // Americano
        assertEquals(new BigDecimal("120.00"), flujosAmericano.get(1).getFlujoTotal(),
            "Primer flujo americano debe ser $120");
        assertEquals(new BigDecimal("1120.00"), flujosAmericano.get(2).getFlujoTotal(),
            "Segundo flujo americano debe ser $1120");
        
        BigDecimal totalAmericano = flujosAmericano.stream()
            .skip(1).map(FlujoFinanciero::getFlujoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("1240.00"), totalAmericano,
            "Total americano debe ser $1240");
        
        // Alemán
        assertEquals(new BigDecimal("620.00"), flujosAleman.get(1).getFlujoTotal(),
            "Primer flujo alemán debe ser $620");
        assertEquals(new BigDecimal("560.00"), flujosAleman.get(2).getFlujoTotal(),
            "Segundo flujo alemán debe ser $560");
        
        BigDecimal totalAleman = flujosAleman.stream()
            .skip(1).map(FlujoFinanciero::getFlujoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("1180.00"), totalAleman,
            "Total alemán debe ser $1180");
        
        // Diferencia
        BigDecimal diferencia = totalAmericano.subtract(totalAleman);
        assertEquals(new BigDecimal("60.00"), diferencia,
            "Diferencia debe ser exactamente $60");
        
        System.out.println("Americano: $" + totalAmericano + ", Alemán: $" + totalAleman + 
                          ", Diferencia: $" + diferencia);
        System.out.println("✅ Comparación directa validada correctamente");
    }
}
