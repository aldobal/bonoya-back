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
 * Tests de validación con casos reales del mercado financiero
 * Para verificar que los cálculos coincidan con ejemplos conocidos
 */
@ExtendWith(MockitoExtension.class)
class CalculoFinancieroServiceValidacionTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
        // Usar lenient para evitar errores de stubbing innecesario
        lenient().when(calculoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    /**
     * CASO REAL 1: Bono del Tesoro Americano típico
     * Bono: $1000, 5% anual, 10 años, cupones semestrales
     * Validación contra calculadoras financieras estándar
     */
    @Test
    void testBonoTesoroAmericano_ValidacionCalculadoraFinanciera() {
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
        BigDecimal duracion = calculoService.calcularDuracion(flujos, new BigDecimal("5.0"));
        
        // Assert
        // Para un bono a la par, TREA debe ser muy cercana a la tasa cupón
        assertTrue(Math.abs(trea.doubleValue() - 5.0) < 0.1, 
            "TREA debe ser ~5% para bono a la par: " + trea);
        
        // Duración de bono 10 años al 5% debe ser ~7.7 años
        assertTrue(duracion.doubleValue() > 7.0 && duracion.doubleValue() < 8.5, 
            "Duración debe estar entre 7-8.5 años: " + duracion);
        
        // Debe haber 21 flujos (periodo 0 + 20 cupones)
        assertEquals(21, flujos.size(), "Debe tener 21 flujos para 10 años semestrales");
        
        // Todos los cupones intermedios deben ser $25 (5% anual / 2)
        for (int i = 1; i < 20; i++) {
            assertEquals(new BigDecimal("25.00"), flujos.get(i).getFlujoTotal(),
                "Cupones intermedios deben ser $25");
        }
        
        // Último flujo debe ser $1025 (cupón + principal)
        assertEquals(new BigDecimal("1025.00"), flujos.get(20).getFlujoTotal(),
            "Último flujo debe ser $1025");
    }

    /**
     * CASO REAL 2: Bono con descuento significativo
     * Verificar que TREA refleje correctamente el mayor rendimiento
     */
    @Test
    void testBonoConDescuento_RendimientoSuperior() {
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
        
        // Assert
        // TREA debe ser mayor que la tasa cupón debido al descuento
        assertTrue(trea.doubleValue() > 4.5, 
            "TREA debe ser > 4.5% para bono con descuento: " + trea);
        
        // Debe ser menor que 8% (límite razonable)
        assertTrue(trea.doubleValue() < 8.0, 
            "TREA debe ser < 8% para este descuento: " + trea);
    }

    /**
     * CASO REAL 3: Bono con premium
     * Verificar que TREA sea menor que tasa cupón
     */
    @Test
    void testBonoConPremium_RendimientoInferior() {
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("7.0"));
        bono.setPlazoAnios(3);
        bono.setFrecuenciaPagos(2);
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal precioPremium = new BigDecimal("1100.00"); // 10% premium
        
        // Act
        BigDecimal trea = calculoService.calcularTREA(bono, precioPremium);
        
        // Assert
        // TREA debe ser menor que la tasa cupón debido al premium
        assertTrue(trea.doubleValue() < 7.0, 
            "TREA debe ser < 7% para bono con premium: " + trea);
        
        // Debe ser mayor que 3% (límite inferior razonable para un bono con 10% premium)
        assertTrue(trea.doubleValue() > 3.0, 
            "TREA debe ser > 3% para este premium: " + trea);
    }

    /**
     * CASO REAL 4: Comparación método Alemán vs Americano
     * NOTA: El método alemán da MENOS intereses totales porque se amortiza principal antes
     */
    @Test
    void testComparacionMetodosAmortizacion() {
        // Arrange
        Bono bonoAmericano = crearBonoBase();
        bonoAmericano.setMetodoAmortizacion("AMERICANO");
        
        Bono bonoAleman = crearBonoBase();
        bonoAleman.setMetodoAmortizacion("ALEMAN");
        
        // Act
        List<FlujoFinanciero> flujosAmericano = calculoService.calcularFlujoFinanciero(bonoAmericano);
        List<FlujoFinanciero> flujosAleman = calculoService.calcularFlujoFinanciero(bonoAleman);
        
        // Assert
        assertEquals(flujosAmericano.size(), flujosAleman.size(), 
            "Ambos métodos deben tener el mismo número de flujos");
        
        // Calcular totales
        BigDecimal sumaAmericano = flujosAmericano.stream()
            .skip(1) // Saltar periodo 0
            .map(FlujoFinanciero::getFlujoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal sumaAleman = flujosAleman.stream()
            .skip(1) // Saltar periodo 0
            .map(FlujoFinanciero::getFlujoTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal interesesAmericano = flujosAmericano.stream()
            .skip(1)
            .map(FlujoFinanciero::getInteres)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal interesesAleman = flujosAleman.stream()
            .skip(1)
            .map(FlujoFinanciero::getInteres)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // VALIDACIONES CORRECTAS FINANCIERAMENTE:
        
        // 1. El método alemán debe dar MENOS intereses totales (porque se amortiza antes)
        assertTrue(interesesAleman.compareTo(interesesAmericano) < 0,
            "Método alemán debe generar menos intereses: Alemán=$" + interesesAleman + 
            ", Americano=$" + interesesAmericano);
        
        // 2. El método alemán debe dar MENOS flujos totales
        assertTrue(sumaAleman.compareTo(sumaAmericano) < 0,
            "Método alemán debe dar menor suma total: Alemán=$" + sumaAleman + 
            ", Americano=$" + sumaAmericano);
        
        // 3. La diferencia debe estar en un rango razonable (aproximadamente $75 para este ejemplo)
        BigDecimal diferencia = sumaAmericano.subtract(sumaAleman);
        assertTrue(diferencia.doubleValue() > 50.0 && diferencia.doubleValue() < 100.0,
            "Diferencia debe estar entre $50-$100: $" + diferencia);
        
        // Verificar patrones específicos
        // Americano: flujos constantes hasta el final
        for (int i = 1; i < flujosAmericano.size() - 1; i++) {
            assertEquals(flujosAmericano.get(i).getFlujoTotal(), 
                        flujosAmericano.get(1).getFlujoTotal(),
                "Flujos intermedios deben ser iguales en método americano");
        }
        
        // Alemán: flujos decrecientes
        for (int i = 2; i < flujosAleman.size(); i++) {
            assertTrue(flujosAleman.get(i).getFlujoTotal()
                      .compareTo(flujosAleman.get(i-1).getFlujoTotal()) <= 0,
                "Flujos deben ser decrecientes en método alemán");
        }
    }

    /**
     * CASO REAL 5: Validación de edge cases
     */
    @Test
    void testEdgeCases() {
        // Caso 1: Bono a 1 año
        Bono bono1Anio = new Bono();
        bono1Anio.setValorNominal(new BigDecimal("1000.00"));
        bono1Anio.setTasaCupon(new BigDecimal("5.0"));
        bono1Anio.setPlazoAnios(1);
        bono1Anio.setFrecuenciaPagos(1);
        bono1Anio.setFechaEmision(LocalDate.now());
        bono1Anio.setMetodoAmortizacion("AMERICANO");
        
        List<FlujoFinanciero> flujos1Anio = calculoService.calcularFlujoFinanciero(bono1Anio);
        
        assertEquals(2, flujos1Anio.size(), "Bono 1 año debe tener 2 flujos");
        assertEquals(new BigDecimal("1050.00"), flujos1Anio.get(1).getFlujoTotal(),
            "Flujo único debe ser $1050");
        
        // Caso 2: Bono con tasa 0%
        Bono bonoSinCupon = new Bono();
        bonoSinCupon.setValorNominal(new BigDecimal("1000.00"));
        bonoSinCupon.setTasaCupon(new BigDecimal("0.0"));
        bonoSinCupon.setPlazoAnios(2);
        bonoSinCupon.setFrecuenciaPagos(1);
        bonoSinCupon.setFechaEmision(LocalDate.now());
        bonoSinCupon.setMetodoAmortizacion("AMERICANO");
        
        List<FlujoFinanciero> flujosSinCupon = calculoService.calcularFlujoFinanciero(bonoSinCupon);
        
        // Solo debe haber pagos al final
        assertEquals(BigDecimal.ZERO.setScale(2), flujosSinCupon.get(1).getFlujoTotal().setScale(2),
            "Flujo intermedio debe ser 0 para bono sin cupón");
        assertEquals(new BigDecimal("1000.00"), flujosSinCupon.get(2).getFlujoTotal().setScale(2),
            "Flujo final debe ser solo el principal");
    }

    /**
     * CASO REAL 6: Validación de precisión numérica
     */
    @Test
    void testPrecisionNumerica() {
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("3.75")); // Tasa con decimales
        bono.setPlazoAnios(7);
        bono.setFrecuenciaPagos(4); // Trimestral
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal precio = new BigDecimal("987.50"); // Precio con decimales
        
        // Act
        BigDecimal trea = calculoService.calcularTREA(bono, precio);
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        
        // Assert
        assertNotNull(trea, "TREA no debe ser null");
        assertTrue(trea.scale() >= 2, "TREA debe tener al menos 2 decimales");
        
        // Verificar que los flujos tengan precisión adecuada
        for (FlujoFinanciero flujo : flujos) {
            if (flujo.getFlujoTotal() != null) {
                assertTrue(flujo.getFlujoTotal().scale() >= 2, 
                    "Flujos deben tener al menos 2 decimales");
            }
        }
    }

    // Método auxiliar
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
