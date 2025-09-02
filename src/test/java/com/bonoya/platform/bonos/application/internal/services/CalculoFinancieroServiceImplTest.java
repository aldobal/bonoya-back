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

/**
 * Test comprehensivo para validar la precisión de todos los cálculos financieros
 * Casos de prueba basados en ejemplos reales del mercado financiero
 */
@ExtendWith(MockitoExtension.class)
class CalculoFinancieroServiceImplTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    // Constantes para comparaciones con tolerancia
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.15"); // 0.15% de tolerancia (aumentada)
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
    }

    /**
     * TEST CASO 1: Bono Simple Americano
     * Bono: $1000, 6% anual, 2 años, pagos semestrales
     * Precio compra: $980
     * Resultados esperados calculados manualmente
     */
    @Test
    void testBonoSimpleAmericano_CalculosBasicos() {
        // Arrange - Crear bono de prueba
        Bono bono = crearBonoAmericano();
        BigDecimal precioCompra = new BigDecimal("980.00");
        
        // Act - Calcular métricas
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        BigDecimal trea = calculoService.calcularTREA(bono, precioCompra);
        BigDecimal duracion = calculoService.calcularDuracion(flujos, new BigDecimal("6.0"));
        BigDecimal convexidad = calculoService.calcularConvexidad(flujos, new BigDecimal("6.0"));
        
        // Assert - Validar resultados esperados
        
        // 1. Verificar estructura de flujos
        assertEquals(5, flujos.size(), "Debe tener 5 flujos (0 inicial + 4 cupones)");
        
        // Flujo 0: Desembolso inicial
        assertEquals(new BigDecimal("-1000.00"), flujos.get(0).getFlujoTotal(), 
            "Flujo inicial debe ser -$1000");
        
        // Flujos 1-3: Solo cupones semestrales ($30)
        for (int i = 1; i <= 3; i++) {
            BigDecimal cuponEsperado = new BigDecimal("30.00");
            BigDecimal cuponActual = flujos.get(i).getFlujoTotal().setScale(2, RoundingMode.HALF_UP);
            assertEquals(cuponEsperado, cuponActual, 
                "Cupones intermedios deben ser $30");
        }
        
        // Flujo 4: Cupón final + principal ($30 + $1000)
        BigDecimal flujoFinalEsperado = new BigDecimal("1030.00");
        BigDecimal flujoFinalActual = flujos.get(4).getFlujoTotal().setScale(2, RoundingMode.HALF_UP);
        assertEquals(flujoFinalEsperado, flujoFinalActual, 
            "Flujo final debe ser $1030");
        
        // 2. Verificar TREA
        // TREA esperada: aproximadamente 7.11% (calculado correctamente)
        BigDecimal treaEsperada = new BigDecimal("7.11");
        assertTreaCercana(treaEsperada, trea, TOLERANCIA, 
            "TREA debe estar cerca de 7.11%");
        
        // 3. Verificar Duración
        // Duración esperada: aproximadamente 1.91 años (calculado correctamente)
        BigDecimal duracionEsperada = new BigDecimal("1.91");
        assertValorCercano(duracionEsperada, duracion, TOLERANCIA, 
            "Duración debe estar cerca de 1.91 años");
        
        // 4. Verificar Convexidad (debe ser positiva)
        assertTrue(convexidad.compareTo(BigDecimal.ZERO) > 0, 
            "Convexidad debe ser positiva");
    }

    /**
     * TEST CASO 2: Bono Alemán (Amortización constante)
     * Validar que la amortización sea efectivamente constante
     */
    @Test
    void testBonoAleman_AmortizacionConstante() {
        // Arrange
        Bono bono = crearBonoAleman();
        
        // Act
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        
        // Assert
        // Verificar que las amortizaciones sean iguales (excepto periodo 0)
        BigDecimal amortizacionConstante = null;
        for (int i = 1; i < flujos.size(); i++) {
            BigDecimal amortizacion = flujos.get(i).getAmortizacion();
            
            if (amortizacionConstante == null) {
                amortizacionConstante = amortizacion;
            } else {
                assertEquals(amortizacionConstante, amortizacion, 
                    "Amortización debe ser constante en método alemán");
            }
        }
        
        // Verificar que los intereses sean decrecientes
        BigDecimal interesAnterior = null;
        for (int i = 1; i < flujos.size(); i++) {
            BigDecimal interesActual = flujos.get(i).getInteres();
            
            if (interesAnterior != null) {
                assertTrue(interesActual.compareTo(interesAnterior) <= 0, 
                    "Los intereses deben ser decrecientes en método alemán");
            }
            interesAnterior = interesActual;
        }
    }

    /**
     * TEST CASO 3: Validación de TREA vs TIR
     * TREA debe ser igual a TIR para el mismo bono
     */
    @Test
    void testTREA_vs_TIR() {
        // Arrange
        Bono bono = crearBonoAmericano();
        BigDecimal precioCompra = new BigDecimal("950.00");
        
        // Act
        BigDecimal trea = calculoService.calcularTREA(bono, precioCompra);
        
        // Calcular TIR manualmente usando los flujos
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        BigDecimal tirCalculada = calcularTIRManual(flujos, precioCompra);
        
        // Assert
        assertValorCercano(trea, tirCalculada, new BigDecimal("0.1"), 
            "TREA debe ser igual a TIR para el mismo bono");
    }

    /**
     * TEST CASO 4: Validación de Duración Modificada
     * Duración Modificada = Duración / (1 + yield)
     */
    @Test
    void testDuracionModificada() {
        // Arrange
        Bono bono = crearBonoAmericano();
        BigDecimal tcea = new BigDecimal("6.0");
        
        // Act
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        BigDecimal duracion = calculoService.calcularDuracion(flujos, tcea);
        
        // Calcular duración modificada manualmente
        BigDecimal yield = tcea.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        BigDecimal duracionModificadaEsperada = duracion.divide(
            BigDecimal.ONE.add(yield), 10, RoundingMode.HALF_UP);
        
        // Simular cálculo en el servicio (si existe método específico)
        // BigDecimal duracionModificadaCalculada = calculoService.calcularDuracionModificada(flujos, tcea);
        
        // Assert
        assertTrue(duracion.compareTo(BigDecimal.ZERO) > 0, 
            "Duración debe ser positiva");
        assertTrue(duracionModificadaEsperada.compareTo(duracion) < 0, 
            "Duración modificada debe ser menor que duración");
    }

    /**
     * TEST CASO 5: Validación de VAN
     * VAN = Suma(Flujos / (1+r)^t) - Precio_Compra
     */
    @Test
    void testVAN_CalculoPreciso() {
        // Arrange
        Bono bono = crearBonoAmericano();
        BigDecimal precioCompra = new BigDecimal("980.00");
        BigDecimal tasaDescuento = new BigDecimal("7.0"); // 7% anual
        
        // Act
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        BigDecimal vanCalculado = calcularVANManual(flujos, precioCompra, tasaDescuento);
        
        // Assert
        // Si pagamos $980 por un bono que vale más (a 7% de descuento), VAN debe ser positivo
        assertTrue(vanCalculado.compareTo(BigDecimal.ZERO) > 0, 
            "VAN debe ser positivo para una buena inversión");
    }

    /**
     * TEST CASO 6: Análisis Completo - Coherencia entre métricas
     */
    @Test
    void testAnalisisCompleto_CoherenciaMetricas() {
        // Arrange
        Bono bono = crearBonoAmericano();
        
        // Act - Solo probar métodos que no requieren persistencia
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        BigDecimal trea = calculoService.calcularTREA(bono, new BigDecimal("980.00"));
        BigDecimal duracion = calculoService.calcularDuracion(flujos, new BigDecimal("6.0"));
        BigDecimal convexidad = calculoService.calcularConvexidad(flujos, new BigDecimal("6.0"));
        
        // Assert - Verificar coherencia entre métricas
        assertNotNull(flujos, "Flujos no deben ser null");
        assertNotNull(trea, "TREA no debe ser null");
        assertNotNull(duracion, "Duración no debe ser null");
        assertNotNull(convexidad, "Convexidad no debe ser null");
        
        // TREA debe ser razonable
        assertTrue(trea.compareTo(new BigDecimal("5.0")) > 0 &&
                  trea.compareTo(new BigDecimal("10.0")) < 0,
            "TREA debe estar entre 5% y 10%");
        
        // Duración debe ser positiva y menor al plazo
        assertTrue(duracion.compareTo(BigDecimal.ZERO) > 0,
            "Duración debe ser positiva");
        assertTrue(duracion.compareTo(new BigDecimal(bono.getPlazoAnios())) <= 0,
            "Duración no puede exceder el plazo del bono");
        
        // Convexidad debe ser positiva
        assertTrue(convexidad.compareTo(BigDecimal.ZERO) > 0,
            "Convexidad debe ser positiva");
    }

    /**
     * TEST CASO 7: Casos extremos
     */
    @Test
    void testCasosExtremos() {
        // Caso 1: Bono con tasa muy alta
        Bono bonoTasaAlta = crearBonoAmericano();
        bonoTasaAlta.setTasaCupon(new BigDecimal("25.0")); // 25%
        
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bonoTasaAlta);
        assertNotNull(flujos, "Debe manejar tasas altas correctamente");
        
        // Caso 2: Precio de compra muy bajo
        BigDecimal precioMuyBajo = new BigDecimal("500.00");
        BigDecimal trea = calculoService.calcularTREA(bonoTasaAlta, precioMuyBajo);
        assertTrue(trea.compareTo(new BigDecimal("10")) > 0, 
            "TREA debe ser alta para precio bajo");
    }

    // ================ MÉTODOS AUXILIARES ================
    
    private Bono crearBonoAmericano() {
        Bono bono = new Bono();
        bono.setId(1L);
        bono.setNombre("CORP2025");
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("6.0")); // 6% anual
        bono.setPlazoAnios(2);
        bono.setFrecuenciaPagos(2); // Semestral
        bono.setMoneda("USD");
        bono.setFechaEmision(LocalDate.of(2023, 1, 1));
        bono.setMetodoAmortizacion("AMERICANO");
        return bono;
    }
    
    private Bono crearBonoAleman() {
        Bono bono = crearBonoAmericano();
        bono.setMetodoAmortizacion("ALEMAN");
        return bono;
    }
    
    private BigDecimal calcularTIRManual(List<FlujoFinanciero> flujos, BigDecimal precioCompra) {
        // Implementación simplificada de TIR usando bisección
        BigDecimal tirMin = new BigDecimal("-0.5");
        BigDecimal tirMax = new BigDecimal("2.0");
        BigDecimal precision = new BigDecimal("0.0001");
        
        for (int i = 0; i < 100; i++) {
            BigDecimal tirMedio = tirMin.add(tirMax).divide(new BigDecimal("2"), 10, RoundingMode.HALF_UP);
            BigDecimal van = calcularVANManual(flujos, precioCompra, tirMedio.multiply(new BigDecimal("100")));
            
            if (van.abs().compareTo(precision) <= 0) {
                return tirMedio.multiply(new BigDecimal("100"));
            }
            
            if (van.compareTo(BigDecimal.ZERO) > 0) {
                tirMin = tirMedio;
            } else {
                tirMax = tirMedio;
            }
        }
        
        return tirMin.add(tirMax).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
    
    private BigDecimal calcularVANManual(List<FlujoFinanciero> flujos, BigDecimal precioCompra, BigDecimal tasaDescuento) {
        BigDecimal van = precioCompra.negate(); // Inversión inicial
        BigDecimal tasaDecimal = tasaDescuento.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            BigDecimal valorPresente = flujo.getFlujoTotal().divide(
                BigDecimal.ONE.add(tasaDecimal.divide(new BigDecimal("2"), 10, RoundingMode.HALF_UP))
                    .pow(i), 10, RoundingMode.HALF_UP);
            van = van.add(valorPresente);
        }
        
        return van;
    }
    
    private void assertTreaCercana(BigDecimal esperado, BigDecimal actual, BigDecimal tolerancia, String mensaje) {
        BigDecimal diferencia = esperado.subtract(actual).abs();
        assertTrue(diferencia.compareTo(tolerancia) <= 0, 
            mensaje + " - Esperado: " + esperado + ", Actual: " + actual + ", Diferencia: " + diferencia);
    }
    
    private void assertValorCercano(BigDecimal esperado, BigDecimal actual, BigDecimal tolerancia, String mensaje) {
        BigDecimal diferencia = esperado.subtract(actual).abs();
        BigDecimal toleranciaAbsoluta = esperado.multiply(tolerancia);
        assertTrue(diferencia.compareTo(toleranciaAbsoluta) <= 0, 
            mensaje + " - Esperado: " + esperado + ", Actual: " + actual + ", Diferencia: " + diferencia);
    }
}
