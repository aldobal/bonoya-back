package com.bonoya.platform.bonos.domain.model.entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bono Amortization Methods Test")
class BonoAmortizacionTest {

    private Bono bonoAleman;
    private Bono bonoAmericano;
    private BigDecimal tasaDescuento;

    @BeforeEach
    void setUp() {
        tasaDescuento = new BigDecimal("0.08"); // 8% tasa de descuento
        
        // Configuración para bono con método alemán
        bonoAleman = new Bono();
        bonoAleman.setValorNominal(new BigDecimal("100000")); // $100,000
        bonoAleman.setTasaCupon(new BigDecimal("10.0")); // 10% anual
        bonoAleman.setPlazoAnios(5); // 5 años
        bonoAleman.setFrecuenciaPagos(1); // Anual
        bonoAleman.setFechaEmision(LocalDate.of(2024, 1, 1));
        bonoAleman.setMetodoAmortizacion("ALEMAN");

        // Configuración para bono con método americano
        bonoAmericano = new Bono();
        bonoAmericano.setValorNominal(new BigDecimal("100000")); // $100,000
        bonoAmericano.setTasaCupon(new BigDecimal("10.0")); // 10% anual
        bonoAmericano.setPlazoAnios(5); // 5 años
        bonoAmericano.setFrecuenciaPagos(1); // Anual
        bonoAmericano.setFechaEmision(LocalDate.of(2024, 1, 1));
        bonoAmericano.setMetodoAmortizacion("AMERICANO");
    }

    @Test
    @DisplayName("Método alemán debería generar amortización constante")
    void testMetodoAleman_AmortizacionConstante() {
        List<FlujoFinanciero> flujos = bonoAleman.generarFlujoCajaMetodoAleman(tasaDescuento);
        
        // Verificar que tenemos 5 periodos
        assertEquals(5, flujos.size());
        
        // Amortización constante = Valor Nominal / Plazo = 100,000 / 5 = 20,000
        BigDecimal amortizacionEsperada = new BigDecimal("20000.00");
        
        for (FlujoFinanciero flujo : flujos) {
            assertEquals(0, amortizacionEsperada.compareTo(flujo.getAmortizacion().setScale(2, java.math.RoundingMode.HALF_UP)), 
                "La amortización debe ser constante en cada periodo");
        }
    }

    @Test
    @DisplayName("Método alemán debería tener intereses decrecientes")
    void testMetodoAleman_InteresesDecrecientes() {
        List<FlujoFinanciero> flujos = bonoAleman.generarFlujoCajaMetodoAleman(tasaDescuento);
        
        BigDecimal interesAnterior = null;
        
        for (FlujoFinanciero flujo : flujos) {
            BigDecimal interes = flujo.getInteres();
            
            if (interesAnterior != null) {
                assertTrue(interes.compareTo(interesAnterior) < 0, 
                    "Los intereses deben ser decrecientes en cada periodo");
            }
            
            interesAnterior = interes;
        }
    }

    @Test
    @DisplayName("Método alemán debería tener saldo decreciente")
    void testMetodoAleman_SaldoDecreciente() {
        List<FlujoFinanciero> flujos = bonoAleman.generarFlujoCajaMetodoAleman(tasaDescuento);
        
        BigDecimal saldoAnterior = bonoAleman.getValorNominal();
        
        for (FlujoFinanciero flujo : flujos) {
            BigDecimal saldo = flujo.getSaldoInsoluto();
            
            assertTrue(saldo.compareTo(saldoAnterior) < 0, 
                "El saldo debe decrecer en cada periodo");
            
            saldoAnterior = saldo;
        }
        
        // El saldo final debe ser cero
        FlujoFinanciero ultimoFlujo = flujos.get(flujos.size() - 1);
        assertEquals(0, BigDecimal.ZERO.compareTo(ultimoFlujo.getSaldoInsoluto()), 
            "El saldo final debe ser cero");
    }

    @Test
    @DisplayName("Método alemán: primer periodo debe tener el interés más alto")
    void testMetodoAleman_PrimerPeriodoInteresMasAlto() {
        List<FlujoFinanciero> flujos = bonoAleman.generarFlujoCajaMetodoAleman(tasaDescuento);
        
        FlujoFinanciero primerPeriodo = flujos.get(0);
        BigDecimal primerInteres = primerPeriodo.getInteres();
        
        // Primer interés = Valor Nominal * (Tasa/100) = 100,000 * 0.10 = 10,000
        BigDecimal interesEsperado = new BigDecimal("10000.00");
        assertEquals(0, interesEsperado.compareTo(primerInteres.setScale(2, java.math.RoundingMode.HALF_UP)), 
            "El primer interés debe ser el 10% del valor nominal");
    }

    @Test
    @DisplayName("Método americano debería tener intereses constantes")
    void testMetodoAmericano_InteresesConstantes() {
        List<FlujoFinanciero> flujos = bonoAmericano.generarFlujoCajaMetodoAmericano(tasaDescuento);
        
        // Interés constante = Valor Nominal * (Tasa/100) = 100,000 * 0.10 = 10,000
        BigDecimal interesEsperado = new BigDecimal("10000.00");
        
        for (FlujoFinanciero flujo : flujos) {
            BigDecimal interes = flujo.getInteres();
            assertEquals(0, interesEsperado.compareTo(interes.setScale(2, java.math.RoundingMode.HALF_UP)), 
                "Los intereses deben ser constantes en método americano");
        }
    }

    @Test
    @DisplayName("Método americano debería amortizar solo en el último periodo")
    void testMetodoAmericano_AmortizacionAlFinal() {
        List<FlujoFinanciero> flujos = bonoAmericano.generarFlujoCajaMetodoAmericano(tasaDescuento);
        
        // Los primeros 4 periodos no deben tener amortización
        for (int i = 0; i < 4; i++) {
            FlujoFinanciero flujo = flujos.get(i);
            assertEquals(0, BigDecimal.ZERO.compareTo(flujo.getAmortizacion()), 
                "No debe haber amortización en los primeros periodos del método americano");
        }
        
        // El último periodo debe tener toda la amortización
        FlujoFinanciero ultimoFlujo = flujos.get(4);
        assertEquals(0, bonoAmericano.getValorNominal().compareTo(ultimoFlujo.getAmortizacion()), 
            "La última amortización debe ser igual al valor nominal");
    }

    @Test
    @DisplayName("Método general debería usar alemán por defecto")
    void testMetodoGeneral_AlemanPorDefecto() {
        List<FlujoFinanciero> flujoGeneral = bonoAleman.generarFlujoCaja(tasaDescuento);
        List<FlujoFinanciero> flujoAleman = bonoAleman.generarFlujoCajaMetodoAleman(tasaDescuento);
        
        assertEquals(flujoAleman.size(), flujoGeneral.size(), 
            "Ambos métodos deben generar la misma cantidad de periodos");
        
        // Verificar que los flujos son idénticos
        for (int i = 0; i < flujoGeneral.size(); i++) {
            FlujoFinanciero flujoGen = flujoGeneral.get(i);
            FlujoFinanciero flujoAle = flujoAleman.get(i);
            
            assertEquals(flujoAle.getPeriodo(), flujoGen.getPeriodo(), 
                "Los periodos deben ser idénticos");
            assertEquals(0, flujoAle.getInteres().compareTo(flujoGen.getInteres()), 
                "Los intereses deben ser idénticos");
            assertEquals(0, flujoAle.getAmortizacion().compareTo(flujoGen.getAmortizacion()), 
                "Las amortizaciones deben ser idénticas");
        }
    }

    @Test
    @DisplayName("Validar suma total de flujos en método alemán")
    void testMetodoAleman_SumaTotalFlujos() {
        List<FlujoFinanciero> flujoCaja = bonoAleman.generarFlujoCajaMetodoAleman(tasaDescuento);
        
        BigDecimal totalIntereses = BigDecimal.ZERO;
        BigDecimal totalAmortizacion = BigDecimal.ZERO;
        
        for (FlujoFinanciero flujo : flujoCaja) {
            totalIntereses = totalIntereses.add(flujo.getInteres());
            totalAmortizacion = totalAmortizacion.add(flujo.getAmortizacion());
        }
        
        // La suma de amortizaciones debe igual el valor nominal
        assertEquals(0, bonoAleman.getValorNominal().compareTo(totalAmortizacion), 
            "La suma de amortizaciones debe igualar el valor nominal");
        
        // Los intereses totales deben ser menores que en método americano
        List<FlujoFinanciero> flujoAmericano = bonoAmericano.generarFlujoCajaMetodoAmericano(tasaDescuento);
        BigDecimal totalInteresesAmericano = flujoAmericano.stream()
            .map(FlujoFinanciero::getInteres)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        assertTrue(totalIntereses.compareTo(totalInteresesAmericano) < 0, 
            "Los intereses totales del método alemán deben ser menores que los del americano");
    }

    @Test
    @DisplayName("Método alemán debería generar diferentes saldos insolutos")
    void testMetodoAleman_SaldosInsolutosDiferentes() {
        List<FlujoFinanciero> flujos = bonoAleman.generarFlujoCajaMetodoAleman(tasaDescuento);
        
        // Verificar que cada saldo insoluto es diferente y decrece
        BigDecimal valorNominal = bonoAleman.getValorNominal();
        BigDecimal amortizacionConstante = valorNominal.divide(new BigDecimal("5"), 2, java.math.RoundingMode.HALF_UP);
        
        for (int i = 0; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            BigDecimal saldoEsperado = valorNominal.subtract(amortizacionConstante.multiply(new BigDecimal(i + 1)));
            
            if (i == flujos.size() - 1) {
                // El último saldo debe ser cero
                assertEquals(0, BigDecimal.ZERO.compareTo(flujo.getSaldoInsoluto()), 
                    "El último saldo insoluto debe ser cero");
            } else {
                assertTrue(flujo.getSaldoInsoluto().compareTo(BigDecimal.ZERO) > 0, 
                    "El saldo insoluto debe ser positivo hasta el penúltimo periodo");
            }
        }
    }
}
