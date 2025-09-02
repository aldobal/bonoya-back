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
 * PRUEBAS EXHAUSTIVAS del Análisis Completo de Bonos
 * Verifica la correctitud matemática de todos los cálculos financieros
 */
@ExtendWith(MockitoExtension.class)
class AnalisisCompletoBonoValidacionTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    // Constantes para validación matemática
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.01"); // 1 centavo de tolerancia
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
        lenient().when(calculoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    /**
     * CASO 1: Bono Americano Simple - Verificación matemática completa
     * Parámetros: $1000, 6% anual, 2 años, semestral
     * Escenario: Inversionista quiere 8% y está dispuesto a pagar $950
     */
    @Test
    void testAnalisisCompleto_BonoAmericano_CasoReal() {
        System.out.println("🔬 ANÁLISIS COMPLETO - BONO AMERICANO CASO REAL");
        System.out.println("=" .repeat(60));
        
        // CONFIGURACIÓN DEL BONO
        Bono bono = crearBonoAmericano();
        BigDecimal tasaEsperada = new BigDecimal("8.0");    // Quiere 8% anual
        BigDecimal precioCompra = new BigDecimal("950.00");  // Dispuesto a pagar $950
        String username = "test-inversor";
        
        imprimirParametrosBono(bono, tasaEsperada, precioCompra);
        
        // EJECUTAR ANÁLISIS COMPLETO
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, precioCompra, username);
        
        // VALIDACIONES EXHAUSTIVAS
        validarParametrosBasicos(resultado, bono, tasaEsperada, precioCompra, username);
        validarCalculosFinancieros(resultado, bono, tasaEsperada, precioCompra);
        validarMetricasRiesgo(resultado, bono);
        validarConsistenciaMatematica(resultado, bono, precioCompra);
        
        System.out.println("✅ TODAS LAS VALIDACIONES PASARON CORRECTAMENTE");
        System.out.println("=" .repeat(60));
    }

    /**
     * CASO 2: Bono Alemán - Verificación con amortización variable
     * Parámetros: $1000, 5% anual, 3 años, semestral
     * Escenario: Inversionista quiere 6% y pagará $980
     */
    @Test
    void testAnalisisCompleto_BonoAleman_CasoComplejo() {
        System.out.println("🔬 ANÁLISIS COMPLETO - BONO ALEMÁN CASO COMPLEJO");
        System.out.println("=" .repeat(60));
        
        // CONFIGURACIÓN DEL BONO
        Bono bono = crearBonoAleman();
        BigDecimal tasaEsperada = new BigDecimal("6.0");     // Quiere 6% anual
        BigDecimal precioCompra = new BigDecimal("980.00");   // Pagará $980
        String username = "test-inversor-aleman";
        
        imprimirParametrosBono(bono, tasaEsperada, precioCompra);
        
        // EJECUTAR ANÁLISIS COMPLETO
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, precioCompra, username);
        
        // VALIDACIONES EXHAUSTIVAS
        validarParametrosBasicos(resultado, bono, tasaEsperada, precioCompra, username);
        validarCalculosFinancieros(resultado, bono, tasaEsperada, precioCompra);
        validarMetricasRiesgo(resultado, bono);
        validarConsistenciaMatematica(resultado, bono, precioCompra);
        
        // VALIDACIONES ESPECÍFICAS PARA MÉTODO ALEMÁN
        validarFlujosBondoAleman(bono);
        
        System.out.println("✅ TODAS LAS VALIDACIONES PARA BONO ALEMÁN PASARON");
        System.out.println("=" .repeat(60));
    }

    /**
     * CASO 3: Escenario de VAN Negativo - Precio muy alto
     * Verifica que el análisis detecte una mala inversión
     */
    @Test
    void testAnalisisCompleto_EscenarioVANNegativo() {
        System.out.println("🔬 ANÁLISIS COMPLETO - ESCENARIO VAN NEGATIVO");
        System.out.println("=" .repeat(60));
        
        // CONFIGURACIÓN DEL BONO
        Bono bono = crearBonoAmericano();
        BigDecimal tasaEsperada = new BigDecimal("8.0");      // Quiere 8% anual
        BigDecimal precioCompra = new BigDecimal("1100.00");  // PRECIO MUY ALTO
        String username = "test-van-negativo";
        
        imprimirParametrosBono(bono, tasaEsperada, precioCompra);
        
        // EJECUTAR ANÁLISIS COMPLETO
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, precioCompra, username);
        
        // VALIDAR QUE EL VAN ES NEGATIVO (mala inversión)
        System.out.println("📊 VALIDANDO ESCENARIO VAN NEGATIVO:");
        System.out.println("   VAN calculado: $" + resultado.getVan());
        System.out.println("   TIR calculada: " + resultado.getTir() + "%");
        System.out.println("   Precio máximo recomendado: $" + resultado.getPrecioMaximo());
        
        assertTrue(resultado.getVan().compareTo(BigDecimal.ZERO) < 0, 
                  "El VAN debe ser negativo cuando el precio es muy alto");
        assertTrue(resultado.getTir().compareTo(tasaEsperada) < 0, 
                  "La TIR debe ser menor que la tasa esperada");
        assertTrue(resultado.getPrecioMaximo().compareTo(precioCompra) < 0, 
                  "El precio máximo debe ser menor que el precio de compra");
        
        System.out.println("✅ ESCENARIO VAN NEGATIVO VALIDADO CORRECTAMENTE");
        System.out.println("=" .repeat(60));
    }

    /**
     * CASO 4: Análisis sin precio de compra (método fallback)
     */
    @Test
    void testAnalisisCompleto_SinPrecioCompra() {
        System.out.println("🔬 ANÁLISIS COMPLETO - SIN PRECIO DE COMPRA");
        System.out.println("=" .repeat(60));
        
        // CONFIGURACIÓN DEL BONO
        Bono bono = crearBonoAmericano();
        BigDecimal tasaEsperada = new BigDecimal("7.0");
        String username = "test-sin-precio";
        
        System.out.println("📋 PARÁMETROS:");
        System.out.println("   Valor nominal: $" + bono.getValorNominal());
        System.out.println("   Tasa cupón: " + bono.getTasaCupon() + "%");
        System.out.println("   Tasa esperada: " + tasaEsperada + "%");
        System.out.println("   Precio de compra: NO ESPECIFICADO");
        System.out.println();
        
        // EJECUTAR ANÁLISIS COMPLETO (método fallback)
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, username);
        
        // VALIDAR QUE USA EL PRECIO MÁXIMO COMO PRECIO DE COMPRA
        System.out.println("📊 RESULTADOS (Método Fallback):");
        System.out.println("   Precio máximo calculado: $" + resultado.getPrecioMaximo());
        System.out.println("   Valor presente (precio usado): $" + resultado.getValorPresente());
        System.out.println("   TREA: " + resultado.getTrea() + "%");
        System.out.println("   VAN: $" + resultado.getVan());
        
        // En el método fallback, valor presente debe igual precio máximo
        assertEquals(resultado.getPrecioMaximo(), resultado.getValorPresente(),
                    "En método fallback, valor presente debe igualar precio máximo");
        
        // VAN debe ser aproximadamente cero (usa precio óptimo)
        assertTrue(Math.abs(resultado.getVan().doubleValue()) < 1.0,
                  "VAN debe ser cercano a cero cuando se usa precio máximo");
        
        System.out.println("✅ MÉTODO FALLBACK VALIDADO CORRECTAMENTE");
        System.out.println("=" .repeat(60));
    }

    // =================================================================
    // MÉTODOS AUXILIARES DE VALIDACIÓN
    // =================================================================

    private void validarParametrosBasicos(Calculo resultado, Bono bono, BigDecimal tasaEsperada, 
                                        BigDecimal precioCompra, String username) {
        System.out.println("🔍 VALIDANDO PARÁMETROS BÁSICOS:");
        
        assertNotNull(resultado, "El resultado del análisis no debe ser nulo");
        assertEquals(bono, resultado.getBono(), "El bono debe coincidir");
        assertEquals(tasaEsperada, resultado.getTasaEsperada(), "La tasa esperada debe coincidir");
        assertEquals(precioCompra, resultado.getValorPresente(), "El precio de compra debe coincidir");
        assertEquals(username, resultado.getInversorUsername(), "El username debe coincidir");
        assertEquals("ANALISIS_COMPLETO", resultado.getTipoAnalisis(), "El tipo debe ser ANALISIS_COMPLETO");
        assertEquals(LocalDate.now(), resultado.getFechaCalculo(), "La fecha debe ser hoy");
        
        System.out.println("   ✓ Parámetros básicos correctos");
    }

    private void validarCalculosFinancieros(Calculo resultado, Bono bono, BigDecimal tasaEsperada, 
                                          BigDecimal precioCompra) {
        System.out.println("🔍 VALIDANDO CÁLCULOS FINANCIEROS:");
        
        // Validar que todos los campos están poblados
        assertNotNull(resultado.getTrea(), "TREA no debe ser nulo");
        assertNotNull(resultado.getTir(), "TIR no debe ser nulo");
        assertNotNull(resultado.getVan(), "VAN no debe ser nulo");
        assertNotNull(resultado.getTcea(), "TCEA no debe ser nulo");
        assertNotNull(resultado.getPrecioMaximo(), "Precio máximo no debe ser nulo");
        assertNotNull(resultado.getPrecioJusto(), "Precio justo no debe ser nulo");
        assertNotNull(resultado.getDuracion(), "Duración no debe ser nulo");
        assertNotNull(resultado.getConvexidad(), "Convexidad no debe ser nulo");
        
        // Validar rangos lógicos
        assertTrue(resultado.getTrea().compareTo(BigDecimal.ZERO) > 0, "TREA debe ser positiva");
        assertTrue(resultado.getTir().compareTo(BigDecimal.ZERO) > 0, "TIR debe ser positiva");
        assertTrue(resultado.getTcea().compareTo(BigDecimal.ZERO) > 0, "TCEA debe ser positiva");
        assertTrue(resultado.getPrecioMaximo().compareTo(BigDecimal.ZERO) > 0, "Precio máximo debe ser positivo");
        assertTrue(resultado.getDuracion().compareTo(BigDecimal.ZERO) > 0, "Duración debe ser positiva");
        assertTrue(resultado.getConvexidad().compareTo(BigDecimal.ZERO) > 0, "Convexidad debe ser positiva");
        
        // Validar que duración está en rango lógico (menor que plazo del bono)
        assertTrue(resultado.getDuracion().compareTo(new BigDecimal(bono.getPlazoAnios())) <= 0,
                  "Duración debe ser menor o igual al plazo del bono");
        
        System.out.println("   📈 TREA: " + resultado.getTrea() + "%");
        System.out.println("   📈 TIR: " + resultado.getTir() + "%");
        System.out.println("   💰 VAN: $" + resultado.getVan());
        System.out.println("   📊 TCEA: " + resultado.getTcea() + "%");
        System.out.println("   💵 Precio máximo: $" + resultado.getPrecioMaximo());
        System.out.println("   💎 Precio justo: $" + resultado.getPrecioJusto());
        System.out.println("   ⏱️ Duración: " + resultado.getDuracion() + " años");
        System.out.println("   📐 Convexidad: " + resultado.getConvexidad());
        System.out.println("   ✓ Cálculos financieros correctos");
    }

    private void validarMetricasRiesgo(Calculo resultado, Bono bono) {
        System.out.println("🔍 VALIDANDO MÉTRICAS DE RIESGO:");
        
        assertNotNull(resultado.getDuracionModificada(), "Duración modificada no debe ser nula");
        assertNotNull(resultado.getSensibilidadPrecio(), "Sensibilidad del precio no debe ser nula");
        
        // Duración modificada debe ser menor que duración normal
        assertTrue(resultado.getDuracionModificada().compareTo(resultado.getDuracion()) < 0,
                  "Duración modificada debe ser menor que duración normal");
        
        // Sensibilidad debe ser negativa (precio baja cuando tasas suben)
        assertTrue(resultado.getSensibilidadPrecio().compareTo(BigDecimal.ZERO) < 0,
                  "Sensibilidad del precio debe ser negativa");
        
        System.out.println("   ⚖️ Duración modificada: " + resultado.getDuracionModificada());
        System.out.println("   📉 Sensibilidad precio: " + resultado.getSensibilidadPrecio());
        System.out.println("   ✓ Métricas de riesgo correctas");
    }

    private void validarConsistenciaMatematica(Calculo resultado, Bono bono, BigDecimal precioCompra) {
        System.out.println("🔍 VALIDANDO CONSISTENCIA MATEMÁTICA:");
        
        // Validar relación entre ganancia de capital y precios
        BigDecimal gananciaCalculada = bono.getValorNominal().subtract(precioCompra);
        assertEquals(gananciaCalculada.setScale(2, RoundingMode.HALF_UP), 
                    resultado.getGananciaCapital().setScale(2, RoundingMode.HALF_UP),
                    "Ganancia de capital debe ser VN - Precio Compra");
        
        // Validar que rendimiento total es consistente
        assertNotNull(resultado.getRendimientoTotal(), "Rendimiento total no debe ser nulo");
        assertNotNull(resultado.getIngresosCupones(), "Ingresos por cupones no debe ser nulo");
        
        // El rendimiento total debe incluir ganancia de capital + cupones
        BigDecimal rendimientoEsperado = resultado.getGananciaCapital()
                .add(resultado.getIngresosCupones())
                .divide(precioCompra, MC)
                .multiply(new BigDecimal("100"));
                
        BigDecimal diferencia = resultado.getRendimientoTotal().subtract(rendimientoEsperado).abs();
        assertTrue(diferencia.compareTo(TOLERANCIA) < 0,
                  "Rendimiento total debe ser consistente con ganancia + cupones");
        
        System.out.println("   💰 Ganancia capital: $" + resultado.getGananciaCapital());
        System.out.println("   💵 Ingresos cupones: $" + resultado.getIngresosCupones());
        System.out.println("   📊 Rendimiento total: " + resultado.getRendimientoTotal() + "%");
        System.out.println("   ✓ Consistencia matemática verificada");
    }

    private void validarFlujosBondoAleman(Bono bono) {
        System.out.println("🔍 VALIDANDO FLUJOS ESPECÍFICOS BONO ALEMÁN:");
        
        List<FlujoFinanciero> flujos = calculoService.calcularFlujoFinanciero(bono);
        
        // Verificar que los flujos están correctamente calculados
        assertNotNull(flujos, "Los flujos financieros no deben ser nulos");
        assertTrue(flujos.size() > bono.getPlazoAnios() * bono.getFrecuenciaPagos(),
                  "Debe haber flujos para todos los períodos + período 0");
        
        // Verificar que la suma de amortizaciones = valor nominal
        BigDecimal sumaAmortizaciones = BigDecimal.ZERO;
        for (int i = 1; i < flujos.size(); i++) {
            sumaAmortizaciones = sumaAmortizaciones.add(flujos.get(i).getAmortizacion());
        }
        
        BigDecimal diferencia = sumaAmortizaciones.subtract(bono.getValorNominal()).abs();
        assertTrue(diferencia.compareTo(TOLERANCIA) < 0,
                  "Suma de amortizaciones debe igualar valor nominal");
        
        System.out.println("   📊 Total períodos: " + (flujos.size() - 1));
        System.out.println("   💰 Suma amortizaciones: $" + sumaAmortizaciones);
        System.out.println("   ✓ Flujos del bono alemán correctos");
    }

    // =================================================================
    // MÉTODOS AUXILIARES DE CREACIÓN
    // =================================================================

    private Bono crearBonoAmericano() {
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("6.0"));        // 6% anual
        bono.setPlazoAnios(2);                           // 2 años
        bono.setFrecuenciaPagos(2);                      // Semestral
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        return bono;
    }

    private Bono crearBonoAleman() {
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("5.0"));        // 5% anual
        bono.setPlazoAnios(3);                           // 3 años
        bono.setFrecuenciaPagos(2);                      // Semestral
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("ALEMAN");
        return bono;
    }

    private void imprimirParametrosBono(Bono bono, BigDecimal tasaEsperada, BigDecimal precioCompra) {
        System.out.println("📋 PARÁMETROS DEL ANÁLISIS:");
        System.out.println("   Valor nominal: $" + bono.getValorNominal());
        System.out.println("   Tasa cupón: " + bono.getTasaCupon() + "% anual");
        System.out.println("   Plazo: " + bono.getPlazoAnios() + " años");
        System.out.println("   Frecuencia: " + bono.getFrecuenciaPagos() + " pagos/año");
        System.out.println("   Método: " + bono.getMetodoAmortizacion());
        System.out.println("   Tasa esperada: " + tasaEsperada + "% anual");
        System.out.println("   Precio de compra: $" + precioCompra);
        System.out.println();
    }
}
