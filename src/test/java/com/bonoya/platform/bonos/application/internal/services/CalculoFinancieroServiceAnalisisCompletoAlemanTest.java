package com.bonoya.platform.bonos.application.internal.services;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.Calculo;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;
import com.bonoya.platform.bonos.infrastructure.persistence.jpa.repositories.CalculoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Validación EXHAUSTIVA del Análisis Financiero Completo para bonos con método alemán
 * Verifica la correctitud de TREA, TCEA, TIR, VAN, duración, convexidad, precio justo, etc.
 */
@ExtendWith(MockitoExtension.class)
class CalculoFinancieroServiceAnalisisCompletoAlemanTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
        lenient().when(calculoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    /**
     * VALIDACIÓN COMPLETA: Análisis financiero completo para bono alemán
     * Caso: $1000, 6% anual, 3 años, semestral, tasa esperada 5%
     */
    @Test
    void validacionAnalisisCompletoMetodoAleman() {
        System.out.println("=== VALIDACIÓN ANÁLISIS COMPLETO - MÉTODO ALEMÁN ===");
        
        // Arrange
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("6.0"));     // 6% anual
        bono.setPlazoAnios(3);                        // 3 años
        bono.setFrecuenciaPagos(2);                   // Semestral
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("ALEMAN");
        
        BigDecimal tasaEsperada = new BigDecimal("5.0");  // 5% anual esperada
        
        System.out.println("Parámetros del bono:");
        System.out.println("- Valor nominal: $" + bono.getValorNominal());
        System.out.println("- Tasa cupón: " + bono.getTasaCupon() + "% anual");
        System.out.println("- Plazo: " + bono.getPlazoAnios() + " años");
        System.out.println("- Frecuencia: " + bono.getFrecuenciaPagos() + " pagos/año");
        System.out.println("- Método: " + bono.getMetodoAmortizacion());
        System.out.println("- Tasa esperada: " + tasaEsperada + "%");
        System.out.println();
        
        // Act - Calcular análisis completo
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, "test-user");
        
        // También calcular flujos para verificación manual
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        
        // VALIDACIÓN PASO A PASO
        System.out.println("--- RESULTADOS DEL ANÁLISIS COMPLETO ---");
        
        // 1. VALIDAR FLUJOS FINANCIEROS
        System.out.println("1. FLUJOS FINANCIEROS:");
        BigDecimal sumaTotalFlujos = BigDecimal.ZERO;
        BigDecimal sumaIntereses = BigDecimal.ZERO;
        BigDecimal sumaAmortizacion = BigDecimal.ZERO;
        
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            System.out.printf("   Período %d: Flujo=$%.2f, Interés=$%.2f, Amortización=$%.2f, Saldo=$%.2f%n",
                flujo.getPeriodo(), flujo.getFlujoTotal().doubleValue(), 
                flujo.getInteres().doubleValue(), flujo.getAmortizacion().doubleValue(),
                flujo.getSaldoInsoluto().doubleValue());
            
            sumaTotalFlujos = sumaTotalFlujos.add(flujo.getFlujoTotal());
            sumaIntereses = sumaIntereses.add(flujo.getInteres());
            sumaAmortizacion = sumaAmortizacion.add(flujo.getAmortizacion());
        }
        System.out.println("   TOTALES: Flujos=$" + sumaTotalFlujos.setScale(2, RoundingMode.HALF_UP) + 
                          ", Intereses=$" + sumaIntereses.setScale(2, RoundingMode.HALF_UP) + 
                          ", Amortización=$" + sumaAmortizacion.setScale(2, RoundingMode.HALF_UP));
        System.out.println();
        
        // 2. VALIDAR PRECIO MÁXIMO/VALOR PRESENTE
        System.out.println("2. PRECIO MÁXIMO/VALOR PRESENTE:");
        System.out.println("   Precio máximo calculado: $" + resultado.getPrecioMaximo());
        System.out.println("   Valor presente: $" + resultado.getValorPresente());
        
        // Calcular precio máximo manualmente (VPN de flujos a tasa esperada)
        BigDecimal precioMaximoManual = calcularVPNManual(flujos, tasaEsperada);
        System.out.println("   Precio máximo manual (VPN): $" + precioMaximoManual);
        
        // Validar que coincidan
        assertTrue(Math.abs(resultado.getPrecioMaximo().subtract(precioMaximoManual).doubleValue()) < 5.0,
            "Precio máximo debe coincidir con VPN manual");
        System.out.println("   ✅ Precio máximo correcto");
        System.out.println();
        
        // 3. VALIDAR TREA
        System.out.println("3. TREA (Tasa de Rendimiento Efectiva Anual):");
        System.out.println("   TREA calculada: " + resultado.getTrea() + "%");
        
        // TREA debe ser el rendimiento del bono comprando al precio máximo
        BigDecimal treaManuales = calcularTREAManual(flujos, resultado.getPrecioMaximo());
        System.out.println("   TREA manual: " + treaManuales + "%");
        
        assertTrue(Math.abs(resultado.getTrea().subtract(treaManuales).doubleValue()) < 0.1,
            "TREA debe coincidir con cálculo manual");
        System.out.println("   ✅ TREA correcta");
        System.out.println();
        
        // 4. VALIDAR TIR
        System.out.println("4. TIR (Tasa Interna de Retorno):");
        System.out.println("   TIR calculada: " + resultado.getTir() + "%");
        
        // TIR debe ser igual a TREA para el mismo precio
        assertTrue(Math.abs(resultado.getTir().subtract(resultado.getTrea()).doubleValue()) < 0.1,
            "TIR debe ser igual a TREA para el mismo precio");
        System.out.println("   ✅ TIR = TREA (correcto)");
        System.out.println();
        
        // 5. VALIDAR VAN
        System.out.println("5. VAN (Valor Actual Neto):");
        System.out.println("   VAN calculado: $" + resultado.getVan());
        
        // VAN manual: VPN de flujos - precio máximo
        BigDecimal vanManual = precioMaximoManual.subtract(resultado.getPrecioMaximo());
        System.out.println("   VAN manual: $" + vanManual);
        
        // VAN debe ser cercano a cero cuando se usa la tasa esperada y precio máximo
        assertTrue(Math.abs(resultado.getVan().doubleValue()) < 1.0,
            "VAN debe ser cercano a cero para precio máximo a tasa esperada");
        System.out.println("   ✅ VAN correcto (cercano a cero)");
        System.out.println();
        
        // 6. VALIDAR TCEA
        System.out.println("6. TCEA (Tasa de Costo Efectiva Anual):");
        System.out.println("   TCEA calculada: " + resultado.getTcea() + "%");
        
        // TCEA debe ser la tasa efectiva del cupón
        BigDecimal tceaManual = calcularTCEAManual(bono);
        System.out.println("   TCEA manual: " + tceaManual + "%");
        
        assertTrue(Math.abs(resultado.getTcea().subtract(tceaManual).doubleValue()) < 0.01,
            "TCEA debe coincidir con cálculo manual");
        System.out.println("   ✅ TCEA correcta");
        System.out.println();
        
        // 7. VALIDAR DURACIÓN
        System.out.println("7. DURACIÓN:");
        System.out.println("   Duración calculada: " + resultado.getDuracion() + " años");
        System.out.println("   Duración modificada: " + resultado.getDuracionModificada() + " años");
        
        // Duración debe ser positiva y menor al plazo
        assertTrue(resultado.getDuracion().compareTo(BigDecimal.ZERO) > 0,
            "Duración debe ser positiva");
        assertTrue(resultado.getDuracion().compareTo(new BigDecimal(bono.getPlazoAnios())) <= 0,
            "Duración no puede exceder el plazo");
        
        // Duración modificada debe ser menor que duración
        assertTrue(resultado.getDuracionModificada().compareTo(resultado.getDuracion()) < 0,
            "Duración modificada debe ser menor que duración");
        System.out.println("   ✅ Duración correcta");
        System.out.println();
        
        // 8. VALIDAR CONVEXIDAD
        System.out.println("8. CONVEXIDAD:");
        System.out.println("   Convexidad: " + resultado.getConvexidad());
        
        assertTrue(resultado.getConvexidad().compareTo(BigDecimal.ZERO) > 0,
            "Convexidad debe ser positiva");
        System.out.println("   ✅ Convexidad positiva");
        System.out.println();
        
        // 9. VALIDAR PRECIO JUSTO
        System.out.println("9. PRECIO JUSTO:");
        System.out.println("   Precio justo: $" + resultado.getPrecioJusto());
        
        // Precio justo debe ser el VPN de flujos a TCEA
        BigDecimal precioJustoManual = calcularVPNManual(flujos, resultado.getTcea());
        System.out.println("   Precio justo manual (VPN a TCEA): $" + precioJustoManual);
        
        assertTrue(Math.abs(resultado.getPrecioJusto().subtract(precioJustoManual).doubleValue()) < 1.0,
            "Precio justo debe coincidir con VPN a TCEA");
        System.out.println("   ✅ Precio justo correcto");
        System.out.println();
        
        // 10. VALIDAR VALOR PRESENTE DE CUPONES
        System.out.println("10. VALOR PRESENTE DE CUPONES:");
        System.out.println("    VP cupones: $" + resultado.getValorPresenteCupones());
        
        // VP cupones manual: suma de intereses descontados
        BigDecimal vpCuponesManual = calcularVPCuponesManual(flujos, resultado.getTcea());
        System.out.println("    VP cupones manual: $" + vpCuponesManual);
        
        assertTrue(Math.abs(resultado.getValorPresenteCupones().subtract(vpCuponesManual).doubleValue()) < 1.0,
            "VP cupones debe coincidir con cálculo manual");
        System.out.println("    ✅ VP cupones correcto");
        System.out.println();
        
        // 11. VALIDAR INGRESOS TOTALES POR CUPONES
        System.out.println("11. INGRESOS TOTALES POR CUPONES:");
        System.out.println("    Ingresos cupones: $" + resultado.getIngresosCupones());
        System.out.println("    Suma intereses (manual): $" + sumaIntereses);
        
        assertTrue(Math.abs(resultado.getIngresosCupones().subtract(sumaIntereses).doubleValue()) < 0.01,
            "Ingresos cupones debe ser igual a suma de intereses");
        System.out.println("    ✅ Ingresos cupones correctos");
        System.out.println();
        
        // 12. VALIDAR GANANCIA DE CAPITAL
        System.out.println("12. GANANCIA DE CAPITAL:");
        System.out.println("    Ganancia capital: $" + resultado.getGananciaCapital());
        
        BigDecimal gananciaCap = bono.getValorNominal().subtract(resultado.getPrecioMaximo());
        System.out.println("    Ganancia manual (VN - Precio): $" + gananciaCap);
        
        assertTrue(Math.abs(resultado.getGananciaCapital().subtract(gananciaCap).doubleValue()) < 0.01,
            "Ganancia capital debe ser VN - Precio");
        System.out.println("    ✅ Ganancia capital correcta");
        System.out.println();
        
        // 13. VALIDAR RENDIMIENTO TOTAL
        System.out.println("13. RENDIMIENTO TOTAL:");
        System.out.println("    Rendimiento total: " + resultado.getRendimientoTotal() + "%");
        
        BigDecimal rendimientoManual = resultado.getGananciaCapital()
            .add(resultado.getIngresosCupones())
            .divide(resultado.getPrecioMaximo(), MathContext.DECIMAL128)
            .multiply(new BigDecimal("100"));
        System.out.println("    Rendimiento manual: " + rendimientoManual.setScale(2, RoundingMode.HALF_UP) + "%");
        
        assertTrue(Math.abs(resultado.getRendimientoTotal().subtract(rendimientoManual).doubleValue()) < 1.0,
            "Rendimiento total debe coincidir con cálculo manual");
        System.out.println("    ✅ Rendimiento total correcto");
        System.out.println();
        
        // VALIDACIONES ESPECÍFICAS DEL MÉTODO ALEMÁN
        System.out.println("--- VALIDACIONES ESPECÍFICAS MÉTODO ALEMÁN ---");
        
        // 1. Los flujos deben ser decrecientes
        for (int i = 2; i < flujos.size(); i++) {
            assertTrue(flujos.get(i).getFlujoTotal().compareTo(flujos.get(i-1).getFlujoTotal()) <= 0,
                "Flujos deben ser decrecientes en método alemán");
        }
        System.out.println("✅ Flujos decrecientes verificados");
        
        // 2. Amortización debe ser constante (excepto última)
        BigDecimal amortizacionConstante = flujos.get(1).getAmortizacion();
        for (int i = 2; i < flujos.size() - 1; i++) {
            assertEquals(amortizacionConstante.setScale(2), flujos.get(i).getAmortizacion().setScale(2),
                "Amortización debe ser constante");
        }
        System.out.println("✅ Amortización constante verificada");
        
        // 3. Saldo final debe ser cero
        assertEquals(BigDecimal.ZERO.setScale(2), 
                    flujos.get(flujos.size()-1).getSaldoInsoluto().setScale(2),
                    "Saldo final debe ser cero");
        System.out.println("✅ Saldo final cero verificado");
        
        System.out.println();
        System.out.println("🎉 ANÁLISIS COMPLETO MÉTODO ALEMÁN VALIDADO CORRECTAMENTE");
        System.out.println("   Todas las métricas financieras son coherentes y precisas.");
    }
    
    // MÉTODOS AUXILIARES PARA VALIDACIÓN MANUAL
    
    private BigDecimal calcularVPNManual(List<FlujoFinanciero> flujos, BigDecimal tasa) {
        BigDecimal vpn = BigDecimal.ZERO;
        BigDecimal tasaDecimal = tasa.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        
        // Convertir tasa anual efectiva a tasa periódica (semestral)
        BigDecimal tasaPeriodica = convertirTasaAnualAPeriodica(tasaDecimal, 2);
        
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            BigDecimal factor = BigDecimal.ONE.add(tasaPeriodica)
                .pow(flujo.getPeriodo(), new MathContext(10));
            BigDecimal vp = flujo.getFlujoTotal().divide(factor, new MathContext(10));
            vpn = vpn.add(vp);
        }
        
        return vpn.setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calcularTREAManual(List<FlujoFinanciero> flujos, BigDecimal precio) {
        // Implementación usando bisección con conversión correcta
        BigDecimal tirMin = new BigDecimal("0.001");
        BigDecimal tirMax = new BigDecimal("0.50");
        BigDecimal precision = new BigDecimal("0.0001");
        
        for (int i = 0; i < 100; i++) {
            BigDecimal tirAnualMedio = tirMin.add(tirMax).divide(new BigDecimal("2"), 10, RoundingMode.HALF_UP);
            
            // Convertir tasa anual a tasa periódica (semestral)
            BigDecimal tirSemestral = convertirTasaAnualAPeriodica(tirAnualMedio, 2);
            
            // Calcular VPN con tasa semestral
            BigDecimal vpn = calcularVPNConTasaPeriodica(flujos, tirSemestral);
            BigDecimal van = vpn.subtract(precio);
            
            if (van.abs().compareTo(precision) <= 0) {
                return tirAnualMedio.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            }
            
            if (van.compareTo(BigDecimal.ZERO) > 0) {
                tirMin = tirAnualMedio;
            } else {
                tirMax = tirAnualMedio;
            }
        }
        
        return tirMin.add(tirMax).divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
    
    private BigDecimal convertirTasaAnualAPeriodica(BigDecimal tasaAnual, int frecuencia) {
        // (1 + r_anual)^(1/m) - 1
        double tasaAnualDouble = tasaAnual.doubleValue();
        double tasaPeriodicaDouble = Math.pow(1.0 + tasaAnualDouble, 1.0/frecuencia) - 1.0;
        return new BigDecimal(tasaPeriodicaDouble).setScale(10, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calcularVPNConTasaPeriodica(List<FlujoFinanciero> flujos, BigDecimal tasaPeriodica) {
        BigDecimal vpn = BigDecimal.ZERO;
        BigDecimal unMasTasa = BigDecimal.ONE.add(tasaPeriodica);
        
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            BigDecimal factor = unMasTasa.pow(flujo.getPeriodo(), new MathContext(10));
            BigDecimal vp = flujo.getFlujoTotal().divide(factor, new MathContext(10));
            vpn = vpn.add(vp);
        }
        
        return vpn.setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calcularTCEAManual(Bono bono) {
        BigDecimal tasaNominal = bono.getTasaCupon().divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        int frecuencia = bono.getFrecuenciaPagos();
        
        // TCEA = (1 + TN/m)^m - 1
        BigDecimal tcea = BigDecimal.ONE
            .add(tasaNominal.divide(new BigDecimal(frecuencia), 10, RoundingMode.HALF_UP))
            .pow(frecuencia, new MathContext(10))
            .subtract(BigDecimal.ONE);
            
        return tcea.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calcularVPCuponesManual(List<FlujoFinanciero> flujos, BigDecimal tcea) {
        BigDecimal vpCupones = BigDecimal.ZERO;
        BigDecimal tceaDecimal = tcea.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
        
        // Convertir TCEA a tasa periódica (semestral)
        BigDecimal tasaPeriodica = convertirTasaAnualAPeriodica(tceaDecimal, 2);
        
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            BigDecimal factor = BigDecimal.ONE.add(tasaPeriodica)
                .pow(flujo.getPeriodo(), new MathContext(10));
            BigDecimal vpCupon = flujo.getInteres().divide(factor, new MathContext(10));
            vpCupones = vpCupones.add(vpCupon);
        }
        
        return vpCupones.setScale(2, RoundingMode.HALF_UP);
    }
}
