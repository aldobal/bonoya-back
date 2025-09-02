package com.bonoya.platform.bonos.application.internal.services;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.Calculo;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * VALIDACIÓN MATEMÁTICA ESPECÍFICA del Análisis Completo
 * Verifica la precisión numérica y consistencia de los cálculos financieros
 */
@ExtendWith(MockitoExtension.class)
class AnalisisCompletoMatematicaTest {

    @Mock
    private CalculoRepository calculoRepository;
    
    private CalculoFinancieroServiceImpl calculoService;
    
    // Constantes para validación matemática precisas
    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    private static final BigDecimal TOLERANCIA_ESTRICTA = new BigDecimal("0.01"); // 1 centavo
    private static final BigDecimal TOLERANCIA_PORCENTAJE = new BigDecimal("0.01"); // 0.01%
    
    @BeforeEach
    void setUp() {
        calculoService = new CalculoFinancieroServiceImpl(calculoRepository);
        lenient().when(calculoRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    /**
     * VALIDACIÓN MATEMÁTICA: Bono Bullet Simple 
     * Caso conocido con cálculos manuales verificables
     * Parámetros: $1000, 5% anual, 1 año, anual - Precio $980
     */
    @Test
    void testPrecisionMatematica_BonoBulletSimple() {
        System.out.println("🧮 VALIDACIÓN MATEMÁTICA - BONO BULLET SIMPLE");
        System.out.println("=" .repeat(60));
        
        // CONFIGURAR BONO SIMPLE PARA CÁLCULO MANUAL
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("5.0"));        // 5% anual
        bono.setPlazoAnios(1);                           // 1 año
        bono.setFrecuenciaPagos(1);                      // Anual
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal tasaEsperada = new BigDecimal("6.0");    // 6% esperada
        BigDecimal precioCompra = new BigDecimal("980.00");  // Paga $980
        
        System.out.println("📋 PARÁMETROS SIMPLES:");
        System.out.println("   Valor nominal: $1000, Cupón: 5% anual, Plazo: 1 año");
        System.out.println("   Precio compra: $980, Tasa esperada: 6%");
        System.out.println();
        
        // EJECUTAR ANÁLISIS
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, precioCompra, "test-matematica");
        
        // CÁLCULOS MANUALES ESPERADOS
        System.out.println("🧮 CÁLCULOS MANUALES ESPERADOS:");
        
        // 1. TIR: (50 + 1000) / 980 - 1 = 1050/980 - 1 = 7.14%
        BigDecimal tirEsperada = new BigDecimal("1050").divide(new BigDecimal("980"), MC).subtract(BigDecimal.ONE).multiply(new BigDecimal("100"));
        System.out.println("   TIR esperada: " + tirEsperada + "%");
        
        // 2. VAN: (1050 / 1.06) - 980 = 990.57 - 980 = 10.57
        BigDecimal vanEsperado = new BigDecimal("1050").divide(new BigDecimal("1.06"), MC).subtract(new BigDecimal("980"));
        System.out.println("   VAN esperado: $" + vanEsperado);
        
        // 3. Precio máximo para 6%: 1050 / 1.06 = 990.57
        BigDecimal precioMaximoEsperado = new BigDecimal("1050").divide(new BigDecimal("1.06"), MC);
        System.out.println("   Precio máximo esperado: $" + precioMaximoEsperado);
        
        System.out.println();
        System.out.println("📊 RESULTADOS CALCULADOS:");
        System.out.println("   TIR calculada: " + resultado.getTir() + "%");
        System.out.println("   VAN calculado: $" + resultado.getVan());
        System.out.println("   Precio máximo calculado: $" + resultado.getPrecioMaximo());
        System.out.println();
        
        // VALIDACIONES ESTRICTAS
        System.out.println("✅ VALIDACIONES MATEMÁTICAS:");
        
        // Validar TIR (tolerancia de 0.01%)
        BigDecimal diferenciaTIR = resultado.getTir().subtract(tirEsperada).abs();
        assertTrue(diferenciaTIR.compareTo(TOLERANCIA_PORCENTAJE) <= 0,
                  String.format("TIR debe ser %.2f%% ± 0.01%%, calculada: %.2f%%", 
                               tirEsperada.doubleValue(), resultado.getTir().doubleValue()));
        System.out.println("   ✓ TIR dentro de tolerancia: " + diferenciaTIR + "% de diferencia");
        
        // Validar VAN (tolerancia de $0.01)
        BigDecimal diferenciaVAN = resultado.getVan().subtract(vanEsperado).abs();
        assertTrue(diferenciaVAN.compareTo(TOLERANCIA_ESTRICTA) <= 0,
                  String.format("VAN debe ser $%.2f ± $0.01, calculado: $%.2f", 
                               vanEsperado.doubleValue(), resultado.getVan().doubleValue()));
        System.out.println("   ✓ VAN dentro de tolerancia: $" + diferenciaVAN + " de diferencia");
        
        // Validar Precio Máximo (tolerancia de $0.01)
        BigDecimal diferenciaPrecio = resultado.getPrecioMaximo().subtract(precioMaximoEsperado).abs();
        assertTrue(diferenciaPrecio.compareTo(TOLERANCIA_ESTRICTA) <= 0,
                  String.format("Precio máximo debe ser $%.2f ± $0.01, calculado: $%.2f", 
                               precioMaximoEsperado.doubleValue(), resultado.getPrecioMaximo().doubleValue()));
        System.out.println("   ✓ Precio máximo dentro de tolerancia: $" + diferenciaPrecio + " de diferencia");
        
        System.out.println();
        System.out.println("🎯 PRECISIÓN MATEMÁTICA VERIFICADA CORRECTAMENTE");
        System.out.println("=" .repeat(60));
    }

    /**
     * VALIDACIÓN: Consistencia entre TIR y VAN
     * Verifica que cuando VAN=0, la TIR debe igualar la tasa de descuento
     */
    @Test
    void testConsistencia_TIR_VAN() {
        System.out.println("🔬 VALIDACIÓN CONSISTENCIA TIR-VAN");
        System.out.println("=" .repeat(60));
        
        // CONFIGURAR BONO
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("4.0"));        // 4% anual
        bono.setPlazoAnios(2);                           // 2 años
        bono.setFrecuenciaPagos(1);                      // Anual
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal tasaEsperada = new BigDecimal("5.0");    // 5% esperada
        
        // USAR EL PRECIO MÁXIMO COMO PRECIO DE COMPRA (VAN debería ser ~0)
        Calculo resultadoPrecioMaximo = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, "test-precio-max");
        BigDecimal precioMaximo = resultadoPrecioMaximo.getPrecioMaximo();
        
        // AHORA ANALIZAR CON ESE PRECIO EXACTO
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, precioMaximo, "test-consistencia");
        
        System.out.println("📋 PARÁMETROS:");
        System.out.println("   Bono: $1000, 4% anual, 2 años");
        System.out.println("   Tasa esperada: " + tasaEsperada + "%");
        System.out.println("   Precio de compra (=precio máximo): $" + precioMaximo);
        System.out.println();
        
        System.out.println("📊 RESULTADOS:");
        System.out.println("   VAN: $" + resultado.getVan());
        System.out.println("   TIR: " + resultado.getTir() + "%");
        System.out.println("   Tasa esperada: " + tasaEsperada + "%");
        System.out.println();
        
        // VALIDACIONES DE CONSISTENCIA
        System.out.println("✅ VALIDANDO CONSISTENCIA:");
        
        // VAN debe ser muy cercano a cero (tolerancia $1.00)
        assertTrue(resultado.getVan().abs().compareTo(BigDecimal.ONE) <= 0,
                  "VAN debe ser cercano a cero cuando precio = precio máximo");
        System.out.println("   ✓ VAN cercano a cero: $" + resultado.getVan());
        
        // TIR debe ser muy cercana a la tasa esperada (tolerancia 0.1%)
        BigDecimal diferenciaTasa = resultado.getTir().subtract(tasaEsperada).abs();
        assertTrue(diferenciaTasa.compareTo(new BigDecimal("0.1")) <= 0,
                  "TIR debe ser cercana a tasa esperada cuando precio = precio máximo");
        System.out.println("   ✓ TIR cercana a tasa esperada: " + diferenciaTasa + "% de diferencia");
        
        System.out.println();
        System.out.println("🎯 CONSISTENCIA TIR-VAN VERIFICADA");
        System.out.println("=" .repeat(60));
    }

    /**
     * VALIDACIÓN: Sensibilidad de Duración
     * Verifica que la duración modificada predice correctamente cambios de precio
     */
    @Test
    void testValidacion_SensibilidadDuracion() {
        System.out.println("📐 VALIDACIÓN SENSIBILIDAD DE DURACIÓN");
        System.out.println("=" .repeat(60));
        
        // CONFIGURAR BONO
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("5.0"));        // 5% anual
        bono.setPlazoAnios(3);                           // 3 años
        bono.setFrecuenciaPagos(1);                      // Anual
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        // CALCULAR PRECIO PARA DIFERENTES TASAS
        BigDecimal tasa1 = new BigDecimal("4.0");    // 4%
        BigDecimal tasa2 = new BigDecimal("6.0");    // 6%
        
        Calculo resultado1 = calculoService.calcularAnalisisCompleto(bono, tasa1, "test-duracion-1");
        Calculo resultado2 = calculoService.calcularAnalisisCompleto(bono, tasa2, "test-duracion-2");
        
        BigDecimal precio1 = resultado1.getPrecioMaximo();
        BigDecimal precio2 = resultado2.getPrecioMaximo();
        BigDecimal duracionModificada = resultado1.getDuracionModificada();
        
        System.out.println("📋 ANÁLISIS DE SENSIBILIDAD:");
        System.out.println("   Precio a 4%: $" + precio1);
        System.out.println("   Precio a 6%: $" + precio2);
        System.out.println("   Duración modificada: " + duracionModificada + " años");
        System.out.println();
        
        // CALCULAR CAMBIO REAL DE PRECIO
        BigDecimal cambioTasa = tasa2.subtract(tasa1).divide(new BigDecimal("100"), MC); // 2% = 0.02
        BigDecimal cambioRelativoPrecio = precio2.subtract(precio1).divide(precio1, MC); // (P2-P1)/P1
        
        // CAMBIO PREDICHO POR DURACIÓN: -DurMod × ΔY
        BigDecimal cambioPredichoDuracion = duracionModificada.negate().multiply(cambioTasa);
        
        System.out.println("📊 COMPARACIÓN SENSIBILIDAD:");
        System.out.println("   Cambio tasa: " + cambioTasa.multiply(new BigDecimal("100")) + "%");
        System.out.println("   Cambio real precio: " + cambioRelativoPrecio.multiply(new BigDecimal("100")) + "%");
        System.out.println("   Cambio predicho duración: " + cambioPredichoDuracion.multiply(new BigDecimal("100")) + "%");
        System.out.println();
        
        // VALIDAR QUE LA PREDICCIÓN ES RAZONABLEMENTE PRECISA (tolerancia 1%)
        BigDecimal diferenciaPrediction = cambioRelativoPrecio.subtract(cambioPredichoDuracion).abs();
        System.out.println("✅ VALIDANDO PRECISIÓN DURACIÓN:");
        System.out.println("   Diferencia en predicción: " + diferenciaPrediction.multiply(new BigDecimal("100")) + "%");
        
        assertTrue(diferenciaPrediction.compareTo(new BigDecimal("0.01")) <= 0,
                  "Duración modificada debe predecir cambios de precio con precisión razonable");
        System.out.println("   ✓ Duración modificada predice correctamente la sensibilidad");
        
        System.out.println();
        System.out.println("🎯 SENSIBILIDAD DE DURACIÓN VERIFICADA");
        System.out.println("=" .repeat(60));
    }

    /**
     * VALIDACIÓN: Convexidad del Bono
     * Verifica que la convexidad sea positiva y esté en rango lógico
     */
    @Test
    void testValidacion_Convexidad() {
        System.out.println("📐 VALIDACIÓN CONVEXIDAD");
        System.out.println("=" .repeat(60));
        
        // CONFIGURAR BONO CON ALTA CONVEXIDAD
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("3.0"));        // Cupón bajo = alta convexidad
        bono.setPlazoAnios(10);                          // Plazo largo = alta convexidad  
        bono.setFrecuenciaPagos(1);                      // Anual
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal tasaEsperada = new BigDecimal("5.0");
        
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, "test-convexidad");
        
        System.out.println("📋 PARÁMETROS (Alto Convexidad):");
        System.out.println("   Bono: $1000, 3% anual, 10 años");
        System.out.println("   Tasa esperada: " + tasaEsperada + "%");
        System.out.println();
        
        System.out.println("📊 MÉTRICAS DE CONVEXIDAD:");
        System.out.println("   Duración: " + resultado.getDuracion() + " años");
        System.out.println("   Convexidad: " + resultado.getConvexidad());
        System.out.println("   Duración modificada: " + resultado.getDuracionModificada());
        System.out.println();
        
        // VALIDACIONES DE CONVEXIDAD
        System.out.println("✅ VALIDANDO CONVEXIDAD:");
        
        // Convexidad debe ser positiva
        assertTrue(resultado.getConvexidad().compareTo(BigDecimal.ZERO) > 0,
                  "Convexidad debe ser positiva");
        System.out.println("   ✓ Convexidad positiva: " + resultado.getConvexidad());
        
        // Para bonos de largo plazo, convexidad debe ser significativa (> duración)
        assertTrue(resultado.getConvexidad().compareTo(resultado.getDuracion()) > 0,
                  "Para bonos largos, convexidad debe ser mayor que duración");
        System.out.println("   ✓ Convexidad > Duración para bono largo plazo");
        
        // Duración debe estar en rango lógico (< plazo del bono)
        assertTrue(resultado.getDuracion().compareTo(new BigDecimal(bono.getPlazoAnios())) < 0,
                  "Duración debe ser menor que plazo del bono");
        System.out.println("   ✓ Duración < Plazo del bono");
        
        // Para bono de 10 años con cupón bajo, duración debe ser relativamente alta (>7 años)
        assertTrue(resultado.getDuracion().compareTo(new BigDecimal("7.0")) > 0,
                  "Bono largo con cupón bajo debe tener duración alta");
        System.out.println("   ✓ Duración alta para bono largo con cupón bajo");
        
        System.out.println();
        System.out.println("🎯 CONVEXIDAD VALIDADA CORRECTAMENTE");
        System.out.println("=" .repeat(60));
    }

    /**
     * VALIDACIÓN: Rendimiento Total vs Componentes
     * Verifica que rendimiento total = (ganancia capital + cupones) / inversión
     */
    @Test
    void testValidacion_RendimientoTotal() {
        System.out.println("💰 VALIDACIÓN RENDIMIENTO TOTAL");
        System.out.println("=" .repeat(60));
        
        // CONFIGURAR BONO
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1000.00"));
        bono.setTasaCupon(new BigDecimal("6.0"));        // 6% anual
        bono.setPlazoAnios(2);                           // 2 años
        bono.setFrecuenciaPagos(2);                      // Semestral
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal tasaEsperada = new BigDecimal("7.0");
        BigDecimal precioCompra = new BigDecimal("970.00");
        
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, precioCompra, "test-rendimiento");
        
        System.out.println("📋 PARÁMETROS:");
        System.out.println("   Bono: $1000, 6% semestral, 2 años");
        System.out.println("   Precio compra: $" + precioCompra);
        System.out.println();
        
        System.out.println("📊 COMPONENTES DEL RENDIMIENTO:");
        System.out.println("   Ganancia capital: $" + resultado.getGananciaCapital());
        System.out.println("   Ingresos cupones: $" + resultado.getIngresosCupones());
        System.out.println("   Rendimiento total: " + resultado.getRendimientoTotal() + "%");
        System.out.println();
        
        // CALCULAR RENDIMIENTO TOTAL MANUALMENTE
        BigDecimal rendimientoManual = resultado.getGananciaCapital()
                .add(resultado.getIngresosCupones())
                .divide(precioCompra, MC)
                .multiply(new BigDecimal("100"));
        
        System.out.println("🧮 CÁLCULO MANUAL:");
        System.out.println("   Rendimiento = (" + resultado.getGananciaCapital() + " + " + 
                          resultado.getIngresosCupones() + ") / " + precioCompra + " × 100");
        System.out.println("   Rendimiento manual: " + rendimientoManual + "%");
        System.out.println();
        
        // VALIDAR CONSISTENCIA
        BigDecimal diferencia = resultado.getRendimientoTotal().subtract(rendimientoManual).abs();
        
        System.out.println("✅ VALIDANDO CONSISTENCIA:");
        System.out.println("   Diferencia: " + diferencia + "%");
        
        assertTrue(diferencia.compareTo(TOLERANCIA_PORCENTAJE) <= 0,
                  "Rendimiento total debe coincidir con cálculo manual");
        System.out.println("   ✓ Rendimiento total consistente con componentes");
        
        // Validar que ganancia capital = VN - precio compra
        BigDecimal gananciaEsperada = bono.getValorNominal().subtract(precioCompra);
        assertEquals(gananciaEsperada.setScale(2, RoundingMode.HALF_UP), 
                    resultado.getGananciaCapital().setScale(2, RoundingMode.HALF_UP),
                    "Ganancia capital debe ser VN - Precio Compra");
        System.out.println("   ✓ Ganancia capital calculada correctamente");
        
        // Para bono semestral de 2 años, debe haber 4 cupones
        BigDecimal cuponSemestral = bono.getValorNominal()
                .multiply(bono.getTasaCupon().divide(new BigDecimal("100"), MC))
                .divide(new BigDecimal("2"), MC);
        BigDecimal ingresosCuponesEsperados = cuponSemestral.multiply(new BigDecimal("4"));
        
        BigDecimal diferenciaCupones = resultado.getIngresosCupones().subtract(ingresosCuponesEsperados).abs();
        assertTrue(diferenciaCupones.compareTo(TOLERANCIA_ESTRICTA) <= 0,
                  "Ingresos por cupones deben ser correctos");
        System.out.println("   ✓ Ingresos por cupones calculados correctamente");
        
        System.out.println();
        System.out.println("🎯 RENDIMIENTO TOTAL VALIDADO CORRECTAMENTE");
        System.out.println("=" .repeat(60));
    }

    /**
     * VALIDACIÓN: Caso Real del Usuario
     * Bono: S/ 1,850, 10.5% cupón, 2 años, compra a S/ 500, tasa esperada 10.5%
     * Los resultados obtenidos (TREA 200%, TIR 200%) son extremadamente altos y probablemente incorrectos
     */
    @Test
    void testValidacion_CasoRealUsuario() {
        System.out.println("🔍 VALIDACIÓN CASO REAL DEL USUARIO");
        System.out.println("=" .repeat(60));
        
        // CONFIGURAR BONO EXACTO DEL USUARIO
        Bono bono = new Bono();
        bono.setValorNominal(new BigDecimal("1850.00"));   // S/ 1,850
        bono.setTasaCupon(new BigDecimal("10.5"));         // 10.5% anual
        bono.setPlazoAnios(2);                             // 2 años
        bono.setFrecuenciaPagos(2);                        // Semestral (asumido)
        bono.setFechaEmision(LocalDate.now());
        bono.setMetodoAmortizacion("AMERICANO");
        
        BigDecimal tasaEsperada = new BigDecimal("10.5");     // 10.5% esperada
        BigDecimal precioCompra = new BigDecimal("500.00");   // Compra a S/ 500
        
        System.out.println("📋 PARÁMETROS DEL CASO REAL:");
        System.out.println("   Valor nominal: S/ 1,850.00");
        System.out.println("   Tasa cupón: 10.5% anual");
        System.out.println("   Plazo: 2 años");
        System.out.println("   Precio compra: S/ 500.00");
        System.out.println("   Tasa esperada: 10.5%");
        System.out.println();
        
        // EJECUTAR ANÁLISIS
        Calculo resultado = calculoService.calcularAnalisisCompleto(bono, tasaEsperada, precioCompra, "test-caso-real");
        
        // MOSTRAR RESULTADOS OBTENIDOS
        System.out.println("📊 RESULTADOS OBTENIDOS:");
        System.out.println("   TREA: " + resultado.getTrea() + "%");
        System.out.println("   TIR: " + resultado.getTir() + "%");
        System.out.println("   VAN: S/ " + resultado.getVan());
        System.out.println("   TCEA: " + resultado.getTcea() + "%");
        System.out.println("   Precio máximo: S/ " + resultado.getPrecioMaximo());
        System.out.println("   Ganancia capital: S/ " + resultado.getGananciaCapital());
        System.out.println("   Ingresos cupones: S/ " + resultado.getIngresosCupones());
        System.out.println("   Rendimiento total: " + resultado.getRendimientoTotal() + "%");
        System.out.println();
        
        // CÁLCULOS MANUALES PARA VALIDACIÓN
        System.out.println("🧮 CÁLCULOS MANUALES ESPERADOS:");
        
        // Para bono semestral: cupón semestral = 1850 × 10.5% / 2 = 97.125
        BigDecimal cuponSemestral = bono.getValorNominal()
                .multiply(bono.getTasaCupon().divide(new BigDecimal("100"), MC))
                .divide(new BigDecimal("2"), MC);
        System.out.println("   Cupón semestral: S/ " + cuponSemestral);
        
        // Flujos de caja esperados:
        // Período 1: S/ 97.125
        // Período 2: S/ 97.125  
        // Período 3: S/ 97.125
        // Período 4: S/ 97.125 + S/ 1,850 = S/ 1,947.125
        System.out.println("   Flujos esperados:");
        System.out.println("     Período 1: S/ " + cuponSemestral);
        System.out.println("     Período 2: S/ " + cuponSemestral);
        System.out.println("     Período 3: S/ " + cuponSemestral);
        System.out.println("     Período 4: S/ " + cuponSemestral.add(bono.getValorNominal()));
        
        // TIR esperada: resolver (500 = 97.125/(1+r) + 97.125/(1+r)² + 97.125/(1+r)³ + 1947.125/(1+r)⁴)
        // Usando aproximación: TIR ≈ 45-50% semestral ≈ 100-125% anual efectiva
        System.out.println("   TIR esperada (aproximada): 100-125% anual");
        System.out.println("   Razón: precio muy bajo (500) vs flujos altos (1947 total)");
        System.out.println();
        
        // VALIDACIONES DE LÓGICA
        System.out.println("🔍 VALIDACIONES DE LÓGICA:");
        
        // 1. Ganancia capital debe ser 1850 - 500 = 1350
        BigDecimal gananciaCapitalEsperada = new BigDecimal("1350.00");
        assertEquals(gananciaCapitalEsperada.setScale(2, RoundingMode.HALF_UP), 
                    resultado.getGananciaCapital().setScale(2, RoundingMode.HALF_UP),
                    "Ganancia capital debe ser 1850 - 500 = 1350");
        System.out.println("   ✓ Ganancia capital correcta: S/ " + resultado.getGananciaCapital());
        
        // 2. Ingresos cupones deben ser 4 × 97.125 = 388.50
        BigDecimal ingresosCuponesEsperados = cuponSemestral.multiply(new BigDecimal("4"));
        BigDecimal diferenciaCupones = resultado.getIngresosCupones().subtract(ingresosCuponesEsperados).abs();
        assertTrue(diferenciaCupones.compareTo(new BigDecimal("1.00")) <= 0,
                  "Ingresos cupones deben ser aproximadamente 4 × 97.125 = 388.50");
        System.out.println("   ✓ Ingresos cupones razonables: S/ " + resultado.getIngresosCupones());
        
        // 3. VAN debe ser positivo y muy alto (ya que tasa esperada = 10.5% pero TIR real es mucho mayor)
        assertTrue(resultado.getVan().compareTo(new BigDecimal("1000")) > 0,
                  "VAN debe ser muy positivo dado el precio tan bajo");
        System.out.println("   ✓ VAN muy positivo: S/ " + resultado.getVan());
        
        // 4. TIR debe estar en rango 80-150% (muy alta pero no 200%)
        assertTrue(resultado.getTir().compareTo(new BigDecimal("80")) >= 0 && 
                  resultado.getTir().compareTo(new BigDecimal("150")) <= 0,
                  "TIR debe estar en rango 80-150% para este caso extremo");
        System.out.println("   ✓ TIR en rango esperado: " + resultado.getTir() + "%");
        
        // 5. TREA debe ser similar a TIR
        BigDecimal diferenciaTREA_TIR = resultado.getTrea().subtract(resultado.getTir()).abs();
        assertTrue(diferenciaTREA_TIR.compareTo(new BigDecimal("5.0")) <= 0,
                  "TREA y TIR deben ser similares");
        System.out.println("   ✓ TREA similar a TIR: diferencia " + diferenciaTREA_TIR + "%");
        
        System.out.println();
        System.out.println("💡 INTERPRETACIÓN DEL CASO:");
        System.out.println("   - Comprar a S/ 500 un bono que vale S/ 1,850 es una GRAN OPORTUNIDAD");
        System.out.println("   - TIR muy alta (100%+) es CORRECTA para este caso extremo");
        System.out.println("   - Ganancia capital de S/ 1,350 + cupones S/ 388 = S/ 1,738 total");
        System.out.println("   - Rendimiento total: (1738/500) × 100 = 347.6%");
        System.out.println("   - Este es un caso de inversión extremadamente atractiva");
        System.out.println();
        System.out.println("🎯 CASO REAL VALIDADO - RESULTADOS SON CORRECTOS PARA ESTE ESCENARIO EXTREMO");
        System.out.println("=" .repeat(60));
    }
}
