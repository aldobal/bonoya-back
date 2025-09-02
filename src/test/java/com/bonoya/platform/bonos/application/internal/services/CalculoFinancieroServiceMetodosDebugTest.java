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
import static org.mockito.Mockito.lenient;

/**
 * Test de debug para comparar métodos de amortización
 */
@ExtendWith(MockitoExtension.class)
class CalculoFinancieroServiceMetodosDebugTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
        lenient().when(calculoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void debugComparacionMetodosAmortizacion() {
        // Arrange
        Bono bonoAmericano = crearBonoBase();
        bonoAmericano.setMetodoAmortizacion("AMERICANO");
        
        Bono bonoAleman = crearBonoBase();
        bonoAleman.setMetodoAmortizacion("ALEMAN");
        
        // Act
        List<FlujoFinanciero> flujosAmericano = calculoService.calcularFlujoFinanciero(bonoAmericano);
        List<FlujoFinanciero> flujosAleman = calculoService.calcularFlujoFinanciero(bonoAleman);
        
        // Debug output
        System.out.println("=== DEBUG COMPARACIÓN MÉTODOS AMORTIZACIÓN ===");
        System.out.println("Bono: $1000, 6%, 3 años, semestral");
        System.out.println();
        
        // Método Americano
        System.out.println("--- MÉTODO AMERICANO ---");
        BigDecimal sumaAmericano = BigDecimal.ZERO;
        BigDecimal interesesAmericano = BigDecimal.ZERO;
        BigDecimal amortizacionAmericano = BigDecimal.ZERO;
        
        for (int i = 1; i < flujosAmericano.size(); i++) {
            FlujoFinanciero flujo = flujosAmericano.get(i);
            System.out.println("Período " + flujo.getPeriodo() + 
                             ": Flujo=" + flujo.getFlujoTotal() + 
                             ", Cupón=" + flujo.getCupon() +
                             ", Amortización=" + flujo.getAmortizacion() +
                             ", Saldo=" + flujo.getSaldoInsoluto());
            sumaAmericano = sumaAmericano.add(flujo.getFlujoTotal());
            interesesAmericano = interesesAmericano.add(flujo.getInteres());
            amortizacionAmericano = amortizacionAmericano.add(flujo.getAmortizacion());
        }
        
        System.out.println("TOTALES AMERICANO:");
        System.out.println("- Suma flujos: " + sumaAmericano);
        System.out.println("- Total intereses: " + interesesAmericano);  
        System.out.println("- Total amortización: " + amortizacionAmericano);
        System.out.println();
        
        // Método Alemán
        System.out.println("--- MÉTODO ALEMÁN ---");
        BigDecimal sumaAleman = BigDecimal.ZERO;
        BigDecimal interesesAleman = BigDecimal.ZERO;
        BigDecimal amortizacionAleman = BigDecimal.ZERO;
        
        for (int i = 1; i < flujosAleman.size(); i++) {
            FlujoFinanciero flujo = flujosAleman.get(i);
            System.out.println("Período " + flujo.getPeriodo() + 
                             ": Flujo=" + flujo.getFlujoTotal() + 
                             ", Cupón=" + flujo.getCupon() +
                             ", Amortización=" + flujo.getAmortizacion() +
                             ", Saldo=" + flujo.getSaldoInsoluto());
            sumaAleman = sumaAleman.add(flujo.getFlujoTotal());
            interesesAleman = interesesAleman.add(flujo.getInteres());
            amortizacionAleman = amortizacionAleman.add(flujo.getAmortizacion());
        }
        
        System.out.println("TOTALES ALEMÁN:");
        System.out.println("- Suma flujos: " + sumaAleman);
        System.out.println("- Total intereses: " + interesesAleman);
        System.out.println("- Total amortización: " + amortizacionAleman);
        System.out.println();
        
        // Comparación
        System.out.println("--- COMPARACIÓN ---");
        System.out.println("Diferencia en suma flujos: " + sumaAmericano.subtract(sumaAleman));
        System.out.println("Diferencia en intereses: " + interesesAmericano.subtract(interesesAleman));
        System.out.println("Diferencia en amortizaciones: " + amortizacionAmericano.subtract(amortizacionAleman));
        
        // Cálculo manual esperado
        System.out.println("\n--- CÁLCULO MANUAL ESPERADO ---");
        BigDecimal valorNominal = new BigDecimal("1000.00");
        BigDecimal tasaAnual = new BigDecimal("0.06");
        int años = 3;
        
        // Intereses totales esperados: 6% anual * 3 años = 18% * $1000 = $180
        BigDecimal interesesEsperados = valorNominal.multiply(tasaAnual).multiply(new BigDecimal(años));
        System.out.println("Intereses totales esperados (aproximado): " + interesesEsperados);
        System.out.println("Principal: " + valorNominal);
        System.out.println("Suma total esperada: " + valorNominal.add(interesesEsperados));
    }
    
    private Bono crearBonoBase() {
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("6.0"));
        bono.setPlazoAnios(3);
        bono.setFrecuenciaPagos(2);
        bono.setFechaEmision(LocalDate.now());
        return bono;
    }
}
