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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Validación MINUCIOSA del método alemán de amortización
 * Comparación paso a paso con cálculos manuales
 */
@ExtendWith(MockitoExtension.class)
class CalculoFinancieroServiceMetodoAlemanValidacionTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
        lenient().when(calculoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    /**
     * VALIDACIÓN PASO A PASO DEL MÉTODO ALEMÁN
     * Caso: $1000, 6% anual, 3 años, semestral
     */
    @Test
    void validacionMetodoAlemanPasoAPaso() {
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("6.0"));
        bono.setPlazoAnios(3);
        bono.setFrecuenciaPagos(2);
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("ALEMAN");
        
        // Act
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        
        // CÁLCULO MANUAL PASO A PASO
        System.out.println("=== VALIDACIÓN MÉTODO ALEMÁN ===");
        System.out.println("Parámetros: $1000, 6% anual, 3 años, semestral");
        
        BigDecimal valorNominal = new BigDecimal("1000.00");
        BigDecimal tasaAnual = new BigDecimal("0.06");
        BigDecimal tasaSemestral = tasaAnual.divide(new BigDecimal("2"), 10, RoundingMode.HALF_UP); // 3% semestral
        int totalPeriodos = 6;
        
        // Amortización constante = Principal / Total períodos
        BigDecimal amortizacionConstante = valorNominal.divide(new BigDecimal(totalPeriodos), 10, RoundingMode.HALF_UP);
        
        System.out.println("Tasa semestral: " + tasaSemestral.multiply(new BigDecimal("100")) + "%");
        System.out.println("Amortización constante: $" + amortizacionConstante.setScale(2, RoundingMode.HALF_UP));
        System.out.println();
        
        System.out.println("--- CÁLCULO MANUAL MÉTODO ALEMÁN ---");
        BigDecimal saldoInsoluto = valorNominal;
        BigDecimal sumaInteresesManual = BigDecimal.ZERO;
        BigDecimal sumaAmortizacionManual = BigDecimal.ZERO;
        BigDecimal sumaFlujosManual = BigDecimal.ZERO;
        
        for (int i = 1; i <= totalPeriodos; i++) {
            // Método alemán: Interés = Saldo insoluto inicial * Tasa periódica
            BigDecimal interesManual = saldoInsoluto.multiply(tasaSemestral, new java.math.MathContext(10));
            BigDecimal amortizacionManual = amortizacionConstante;
            
            // En último período, ajustar amortización para cerrar saldo
            if (i == totalPeriodos) {
                amortizacionManual = saldoInsoluto;
            }
            
            BigDecimal flujoTotalManual = interesManual.add(amortizacionManual);
            
            System.out.printf("Período %d: Saldo inicial=$%.2f, Interés=$%.2f, Amortización=$%.2f, Flujo=$%.2f%n",
                i, saldoInsoluto.doubleValue(), interesManual.doubleValue(), 
                amortizacionManual.doubleValue(), flujoTotalManual.doubleValue());
            
            // Actualizar saldo insoluto
            saldoInsoluto = saldoInsoluto.subtract(amortizacionManual);
            
            // Acumular totales
            sumaInteresesManual = sumaInteresesManual.add(interesManual);
            sumaAmortizacionManual = sumaAmortizacionManual.add(amortizacionManual);
            sumaFlujosManual = sumaFlujosManual.add(flujoTotalManual);
        }
        
        System.out.println();
        System.out.println("TOTALES MANUALES:");
        System.out.println("- Suma intereses: $" + sumaInteresesManual.setScale(2, RoundingMode.HALF_UP));
        System.out.println("- Suma amortización: $" + sumaAmortizacionManual.setScale(2, RoundingMode.HALF_UP));
        System.out.println("- Suma flujos: $" + sumaFlujosManual.setScale(2, RoundingMode.HALF_UP));
        System.out.println();
        
        // COMPARAR CON RESULTADO DEL SISTEMA
        System.out.println("--- RESULTADO DEL SISTEMA ---");
        BigDecimal sumaInteresesSistema = BigDecimal.ZERO;
        BigDecimal sumaAmortizacionSistema = BigDecimal.ZERO;
        BigDecimal sumaFlujosSistema = BigDecimal.ZERO;
        
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            System.out.printf("Período %d: Saldo=$%.2f, Interés=$%.2f, Amortización=$%.2f, Flujo=$%.2f%n",
                flujo.getPeriodo(), flujo.getSaldoInsoluto().doubleValue(), 
                flujo.getInteres().doubleValue(), flujo.getAmortizacion().doubleValue(),
                flujo.getFlujoTotal().doubleValue());
            
            sumaInteresesSistema = sumaInteresesSistema.add(flujo.getInteres());
            sumaAmortizacionSistema = sumaAmortizacionSistema.add(flujo.getAmortizacion());
            sumaFlujosSistema = sumaFlujosSistema.add(flujo.getFlujoTotal());
        }
        
        System.out.println();
        System.out.println("TOTALES SISTEMA:");
        System.out.println("- Suma intereses: $" + sumaInteresesSistema.setScale(2, RoundingMode.HALF_UP));
        System.out.println("- Suma amortización: $" + sumaAmortizacionSistema.setScale(2, RoundingMode.HALF_UP));
        System.out.println("- Suma flujos: $" + sumaFlujosSistema.setScale(2, RoundingMode.HALF_UP));
        System.out.println();
        
        // VALIDACIONES
        System.out.println("--- VALIDACIONES ---");
        
        // 1. Verificar que hay 7 flujos (periodo 0 + 6 períodos)
        assertEquals(7, flujos.size(), "Debe haber 7 flujos (0 + 6 períodos)");
        
        // 2. Verificar que las amortizaciones son constantes (excepto la última)
        BigDecimal amortizacionEsperada = valorNominal.divide(new BigDecimal(6), 2, RoundingMode.HALF_UP);
        for (int i = 1; i < flujos.size() - 1; i++) {
            BigDecimal amortizacionActual = flujos.get(i).getAmortizacion().setScale(2, RoundingMode.HALF_UP);
            assertEquals(amortizacionEsperada, amortizacionActual, 
                "Amortización debe ser constante: $" + amortizacionEsperada);
        }
        System.out.println("✅ Amortizaciones constantes verificadas");
        
        // 3. Verificar que los intereses son DECRECIENTES
        for (int i = 2; i < flujos.size(); i++) {
            BigDecimal interesAnterior = flujos.get(i-1).getInteres();
            BigDecimal interesActual = flujos.get(i).getInteres();
            assertTrue(interesActual.compareTo(interesAnterior) <= 0,
                "Intereses deben ser decrecientes: Período " + (i-1) + "=$" + interesAnterior + 
                " >= Período " + i + "=$" + interesActual);
        }
        System.out.println("✅ Intereses decrecientes verificados");
        
        // 4. Verificar que los flujos totales son DECRECIENTES
        for (int i = 2; i < flujos.size(); i++) {
            BigDecimal flujoAnterior = flujos.get(i-1).getFlujoTotal();
            BigDecimal flujoActual = flujos.get(i).getFlujoTotal();
            assertTrue(flujoActual.compareTo(flujoAnterior) <= 0,
                "Flujos deben ser decrecientes: Período " + (i-1) + "=$" + flujoAnterior + 
                " >= Período " + i + "=$" + flujoActual);
        }
        System.out.println("✅ Flujos decrecientes verificados");
        
        // 5. Verificar que el saldo insoluto decrece linealmente
        BigDecimal decrementoEsperado = amortizacionEsperada;
        BigDecimal saldoEsperado = valorNominal;
        for (int i = 1; i < flujos.size(); i++) {
            if (i < flujos.size() - 1) {
                saldoEsperado = saldoEsperado.subtract(decrementoEsperado);
                BigDecimal saldoActual = flujos.get(i).getSaldoInsoluto().setScale(2, RoundingMode.HALF_UP);
                assertEquals(saldoEsperado.setScale(2, RoundingMode.HALF_UP), saldoActual,
                    "Saldo insoluto debe decrecer linealmente");
            }
        }
        System.out.println("✅ Saldo insoluto decreciente verificado");
        
        // 6. Verificar que el último saldo es cero
        BigDecimal saldoFinal = flujos.get(flujos.size() - 1).getSaldoInsoluto();
        assertEquals(BigDecimal.ZERO.setScale(2), saldoFinal.setScale(2),
            "Saldo final debe ser cero");
        System.out.println("✅ Saldo final cero verificado");
        
        // 7. COMPARACIÓN CON CÁLCULO MANUAL
        BigDecimal tolerancia = new BigDecimal("0.01"); // $0.01 de tolerancia
        
        assertTrue(Math.abs(sumaInteresesManual.subtract(sumaInteresesSistema).doubleValue()) < tolerancia.doubleValue(),
            "Suma de intereses debe coincidir con cálculo manual: Manual=$" + sumaInteresesManual + 
            ", Sistema=$" + sumaInteresesSistema);
        System.out.println("✅ Suma de intereses coincide con cálculo manual");
        
        assertTrue(Math.abs(sumaAmortizacionManual.subtract(sumaAmortizacionSistema).doubleValue()) < tolerancia.doubleValue(),
            "Suma de amortización debe coincidir: Manual=$" + sumaAmortizacionManual + 
            ", Sistema=$" + sumaAmortizacionSistema);
        System.out.println("✅ Suma de amortización coincide");
        
        assertTrue(Math.abs(sumaFlujosManual.subtract(sumaFlujosSistema).doubleValue()) < tolerancia.doubleValue(),
            "Suma de flujos debe coincidir: Manual=$" + sumaFlujosManual + 
            ", Sistema=$" + sumaFlujosSistema);
        System.out.println("✅ Suma de flujos coincide");
        
        System.out.println();
        System.out.println("🎉 MÉTODO ALEMÁN VALIDADO CORRECTAMENTE");
    }
}
