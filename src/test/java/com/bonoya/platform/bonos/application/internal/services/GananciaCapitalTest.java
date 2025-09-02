package com.bonoya.platform.bonos.application.internal.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

/**
 * Test específico para validar el cálculo de Ganancia Capital
 * con diferentes precios de compra
 */
public class GananciaCapitalTest {

    private final CalculoFinancieroServiceImpl calculoService = new CalculoFinancieroServiceImpl(null);

    @Test
    @DisplayName("Ganancia Capital = 0 cuando precio compra = valor nominal")
    void testGananciaCapitalCeroConPrecioIgualANominal() {
        // Arrange
        BigDecimal valorNominal = new BigDecimal("1850.00");
        BigDecimal precioCompra = new BigDecimal("1850.00");
        
        // Act
        BigDecimal gananciaCapital = calculoService.calcularGananciaCapital(valorNominal, precioCompra);
        
        // Assert
        assertEquals(0, gananciaCapital.compareTo(BigDecimal.ZERO), 
            "Ganancia Capital debe ser 0 cuando precio de compra = valor nominal");
        
        System.out.println("🔍 Test 1 - Precio = Valor Nominal:");
        System.out.println("   Valor Nominal: " + valorNominal);
        System.out.println("   Precio Compra: " + precioCompra);
        System.out.println("   Ganancia Capital: " + gananciaCapital);
        System.out.println("   ✅ CORRECTO: Ganancia Capital = 0\n");
    }

    @Test
    @DisplayName("Ganancia Capital positiva cuando precio compra < valor nominal")
    void testGananciaCapitalPositivaConPrecioMenor() {
        // Arrange
        BigDecimal valorNominal = new BigDecimal("1850.00");
        BigDecimal precioCompra = new BigDecimal("1800.00");
        BigDecimal gananciaEsperada = new BigDecimal("50.00");
        
        // Act
        BigDecimal gananciaCapital = calculoService.calcularGananciaCapital(valorNominal, precioCompra);
        
        // Assert
        assertEquals(0, gananciaCapital.compareTo(gananciaEsperada), 
            "Ganancia Capital debe ser positiva cuando precio < valor nominal");
        
        System.out.println("🔍 Test 2 - Precio Menor al Valor Nominal:");
        System.out.println("   Valor Nominal: " + valorNominal);
        System.out.println("   Precio Compra: " + precioCompra);
        System.out.println("   Ganancia Capital: " + gananciaCapital);
        System.out.println("   ✅ CORRECTO: Ganancia Capital = " + gananciaEsperada + "\n");
    }

    @Test
    @DisplayName("Ganancia Capital negativa cuando precio compra > valor nominal")
    void testGananciaCapitalNegativaConPrecioMayor() {
        // Arrange
        BigDecimal valorNominal = new BigDecimal("1850.00");
        BigDecimal precioCompra = new BigDecimal("2000.00");
        BigDecimal gananciaEsperada = new BigDecimal("-150.00");
        
        // Act
        BigDecimal gananciaCapital = calculoService.calcularGananciaCapital(valorNominal, precioCompra);
        
        // Assert
        assertEquals(0, gananciaCapital.compareTo(gananciaEsperada), 
            "Ganancia Capital debe ser negativa cuando precio > valor nominal");
        
        System.out.println("🔍 Test 3 - Precio Mayor al Valor Nominal:");
        System.out.println("   Valor Nominal: " + valorNominal);
        System.out.println("   Precio Compra: " + precioCompra);
        System.out.println("   Ganancia Capital: " + gananciaCapital);
        System.out.println("   ✅ CORRECTO: Ganancia Capital = " + gananciaEsperada + " (pérdida)\n");
    }

    @Test
    @DisplayName("Escenarios múltiples para demostrar la fórmula")
    void testEscenariosMultiples() {
        BigDecimal valorNominal = new BigDecimal("1850.00");
        
        // Casos de prueba
        BigDecimal[] precios = {
            new BigDecimal("1700.00"), // Descuento grande
            new BigDecimal("1825.00"), // Descuento pequeño
            new BigDecimal("1850.00"), // Par
            new BigDecimal("1875.00"), // Prima pequeña
            new BigDecimal("2000.00")  // Prima grande
        };
        
        System.out.println("🎯 DEMOSTRACIÓN: Ganancia Capital = Valor Nominal - Precio Compra");
        System.out.println("   Valor Nominal del Bono: " + valorNominal);
        System.out.println("   ================================================");
        
        for (BigDecimal precio : precios) {
            BigDecimal ganancia = calculoService.calcularGananciaCapital(valorNominal, precio);
            String tipo = ganancia.compareTo(BigDecimal.ZERO) > 0 ? "GANANCIA" : 
                         ganancia.compareTo(BigDecimal.ZERO) < 0 ? "PÉRDIDA" : "NEUTRO";
            
            System.out.printf("   Precio Compra: %8s -> Ganancia Capital: %8s (%s)%n", 
                precio, ganancia, tipo);
        }
        System.out.println("   ================================================\n");
    }
}
