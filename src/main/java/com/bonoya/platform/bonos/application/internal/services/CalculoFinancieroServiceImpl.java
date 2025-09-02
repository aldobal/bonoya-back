package com.bonoya.platform.bonos.application.internal.services;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.Calculo;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;
import com.bonoya.platform.bonos.domain.services.CalculoFinancieroService;
import com.bonoya.platform.bonos.infrastructure.persistence.jpa.repositories.CalculoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalculoFinancieroServiceImpl implements CalculoFinancieroService {

    private static final int SCALE = 10;
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final MathContext MC = new MathContext(SCALE, ROUNDING_MODE);

    private final CalculoRepository calculoRepository;

    public CalculoFinancieroServiceImpl(CalculoRepository calculoRepository) {
        this.calculoRepository = calculoRepository;
    }

    @Override
    public List<FlujoFinanciero> calcularFlujoFinanciero(Bono bono) {
        BigDecimal valorNominal = bono.getValorNominal();
        BigDecimal tasaCupon = bono.getTasaCupon().divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        int plazoAnios = bono.getPlazoAnios();
        int frecuenciaPagos = bono.getFrecuenciaPagos();
        int totalPeriodos = plazoAnios * frecuenciaPagos;
        LocalDate fechaEmision = bono.getFechaEmision();
        
        // Identificar el método de amortización real
        String metodoReal = identificarMetodoAmortizacion(bono);

        List<FlujoFinanciero> flujos = new ArrayList<>();

        // Período 0 - Desembolso inicial
        FlujoFinanciero flujoInicial = new FlujoFinanciero();
        flujoInicial.setBono(bono);
        flujoInicial.setPeriodo(0);
        flujoInicial.setFecha(fechaEmision);
        flujoInicial.setCupon(BigDecimal.ZERO);
        flujoInicial.setAmortizacion(BigDecimal.ZERO);
        flujoInicial.setInteres(BigDecimal.ZERO);
        flujoInicial.setSaldoInsoluto(valorNominal);
        flujoInicial.setSaldo(valorNominal);
        flujoInicial.setFlujoTotal(valorNominal.negate());
        flujoInicial.setFlujo(valorNominal.negate());
        flujos.add(flujoInicial);

        // Calcular tasa periódica
        BigDecimal tasaPeriodica = tasaCupon.divide(BigDecimal.valueOf(frecuenciaPagos), SCALE, ROUNDING_MODE);
        
        if ("ALEMAN".equalsIgnoreCase(metodoReal)) {
            // MÉTODO ALEMÁN: Amortización constante, intereses decrecientes
            return calcularFlujoFinancieroAleman(bono, flujos, totalPeriodos, tasaPeriodica, valorNominal, fechaEmision);
        } else {
            // MÉTODO AMERICANO: Cupón constante, amortización solo al final
            return calcularFlujoFinancieroAmericano(bono, flujos, totalPeriodos, tasaPeriodica, valorNominal, fechaEmision);
        }
    }

    private List<FlujoFinanciero> calcularFlujoFinancieroAmericano(Bono bono, List<FlujoFinanciero> flujos, 
                                                                  int totalPeriodos, BigDecimal tasaPeriodica, 
                                                                  BigDecimal valorNominal, LocalDate fechaEmision) {
        for (int i = 1; i <= totalPeriodos; i++) {
            FlujoFinanciero flujo = new FlujoFinanciero();
            flujo.setBono(bono);
            flujo.setPeriodo(i);
            
            // Calcular fecha del período
            LocalDate fechaPeriodo = fechaEmision.plus(i * 12 / bono.getFrecuenciaPagos(), ChronoUnit.MONTHS);
            flujo.setFecha(fechaPeriodo);

            // MÉTODO AMERICANO: Cupón constante siempre, amortización solo al final
            BigDecimal cuponPeriodo = valorNominal.multiply(tasaPeriodica, MC).setScale(2, ROUNDING_MODE);
            BigDecimal amortizacion;
            BigDecimal flujoTotal;
            BigDecimal saldoRestante;
            
            if (i == totalPeriodos) {
                // ÚLTIMO PERÍODO: Cupón + Valor Nominal completo
                amortizacion = valorNominal.setScale(2, ROUNDING_MODE);
                flujoTotal = cuponPeriodo.add(amortizacion).setScale(2, ROUNDING_MODE);
                saldoRestante = BigDecimal.ZERO.setScale(2, ROUNDING_MODE);
            } else {
                // PERÍODOS INTERMEDIOS: Solo cupón
                amortizacion = BigDecimal.ZERO.setScale(2, ROUNDING_MODE);
                flujoTotal = cuponPeriodo.setScale(2, ROUNDING_MODE);
                saldoRestante = valorNominal.setScale(2, ROUNDING_MODE);
            }

            // Asignar valores al flujo con precisión de 2 decimales
            flujo.setCupon(cuponPeriodo);
            flujo.setInteres(cuponPeriodo);
            flujo.setAmortizacion(amortizacion);
            flujo.setCuota(flujoTotal);
            flujo.setFlujoTotal(flujoTotal);
            flujo.setFlujo(flujoTotal);
            flujo.setSaldoInsoluto(saldoRestante);
            flujo.setSaldo(saldoRestante);

            flujos.add(flujo);
        }

        return flujos;
    }

    private List<FlujoFinanciero> calcularFlujoFinancieroAleman(Bono bono, List<FlujoFinanciero> flujos, 
                                                               int totalPeriodos, BigDecimal tasaPeriodica, 
                                                               BigDecimal valorNominal, LocalDate fechaEmision) {
        // MÉTODO ALEMÁN VERDADERO: Amortización constante, intereses sobre saldo insoluto
        
        BigDecimal amortizacionConstante = valorNominal.divide(BigDecimal.valueOf(totalPeriodos), MC);
        BigDecimal saldoInsoluto = valorNominal;

        for (int i = 1; i <= totalPeriodos; i++) {
            FlujoFinanciero flujo = new FlujoFinanciero();
            flujo.setBono(bono);
            flujo.setPeriodo(i);
            
            // Calcular fecha del período
            LocalDate fechaPeriodo = fechaEmision.plus(i * 12 / bono.getFrecuenciaPagos(), ChronoUnit.MONTHS);
            flujo.setFecha(fechaPeriodo);

            // MÉTODO ALEMÁN CORRECTO: Interés = Saldo insoluto inicial * Tasa periódica
            BigDecimal interes = saldoInsoluto.multiply(tasaPeriodica, MC).setScale(2, ROUNDING_MODE);
            BigDecimal amortizacion = amortizacionConstante.setScale(2, ROUNDING_MODE);
            
            // En el último período, ajustar la amortización para eliminar cualquier residuo
            if (i == totalPeriodos) {
                amortizacion = saldoInsoluto.setScale(2, ROUNDING_MODE);  // Amortizar todo el saldo restante
                // Recalcular interés sobre el saldo exacto restante
                interes = saldoInsoluto.multiply(tasaPeriodica, MC).setScale(2, ROUNDING_MODE);
            }
            
            BigDecimal flujoTotal = interes.add(amortizacion).setScale(2, ROUNDING_MODE);

            // Asignar valores al flujo
            flujo.setCupon(interes);      // En alemán, "cupón" = interés variable
            flujo.setInteres(interes);
            flujo.setAmortizacion(amortizacion);
            flujo.setCuota(flujoTotal);
            flujo.setFlujoTotal(flujoTotal);
            flujo.setFlujo(flujoTotal);
            
            // Actualizar saldo insoluto DESPUÉS de calcular los intereses
            saldoInsoluto = saldoInsoluto.subtract(amortizacion).setScale(2, ROUNDING_MODE);
            flujo.setSaldoInsoluto(saldoInsoluto);
            flujo.setSaldo(saldoInsoluto);

            flujos.add(flujo);
        }

        return flujos;
    }

    @Override
    public BigDecimal calcularTCEA(Bono bono) {
        BigDecimal tasaCupon = bono.getTasaCupon().divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        int frecuenciaPagos = bono.getFrecuenciaPagos();
        
        // Fórmula: (1 + j/m)^m - 1
        BigDecimal tasaEfectivaAnual = BigDecimal.ONE
                .add(tasaCupon.divide(BigDecimal.valueOf(frecuenciaPagos), MC))
                .pow(frecuenciaPagos)
                .subtract(BigDecimal.ONE);
                
        // Convertir a porcentaje
        return tasaEfectivaAnual.multiply(BigDecimal.valueOf(100)).setScale(2, ROUNDING_MODE);
    }

    @Override
    public BigDecimal calcularTasaEfectivaPeriodica(BigDecimal tasaAnual, int frecuenciaPagos) {
        // Si la tasa viene en porcentaje, convertirla a decimal
        BigDecimal tasaAnualDecimal = tasaAnual;
        if (tasaAnual.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tasaAnualDecimal = tasaAnual.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        // Validar frecuencia de pagos
        if (frecuenciaPagos <= 0) {
            throw new IllegalArgumentException("La frecuencia de pagos debe ser un valor positivo");
        }
        
        // Calculamos la tasa periódica usando logaritmos y exponenciales para mayor precisión
        double tasaAnualDouble = tasaAnualDecimal.doubleValue();
        double tasaPeriodicaDouble = Math.pow(1.0 + tasaAnualDouble, 1.0/frecuenciaPagos) - 1.0;
        
        return new BigDecimal(tasaPeriodicaDouble).setScale(SCALE, ROUNDING_MODE);
    }
    
    @Override
    public BigDecimal calcularDuracion(List<FlujoFinanciero> flujos, BigDecimal tcea) {
        BigDecimal sumaPonderada = BigDecimal.ZERO;
        BigDecimal precio = BigDecimal.ZERO;
        
        // Convertir TCEA a decimal si viene en porcentaje
        BigDecimal tceaDecimal = tcea;
        if (tcea.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tceaDecimal = tcea.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        // Obtener información del bono para calcular correctamente
        Bono bono = null;
        if (!flujos.isEmpty() && flujos.get(0).getBono() != null) {
            bono = flujos.get(0).getBono();
        }
        
        int frecuenciaPagos = (bono != null) ? bono.getFrecuenciaPagos() : 2;
        
        // Calcular tasa periódica para descuento
        BigDecimal tasaPeriodica = calcularTasaEfectivaPeriodica(tceaDecimal, frecuenciaPagos);
        
        // Saltar el periodo 0 (desembolso inicial)
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            
            // Solo considerar flujos positivos reales
            BigDecimal flujoValor = flujo.getFlujoTotal();
            if (flujoValor == null) {
                flujoValor = flujo.getFlujo();
            }
            
            if (flujoValor.compareTo(BigDecimal.ZERO) > 0) {
                // Usar el periodo exacto
                BigDecimal factorTiempo = new BigDecimal(flujo.getPeriodo());
                flujo.setFactorTiempo(factorTiempo);
                
                // Factor de descuento: 1/(1+tasaPeriodica)^periodo
                BigDecimal factorDescuento = BigDecimal.ONE
                        .divide(BigDecimal.ONE.add(tasaPeriodica, MC).pow(flujo.getPeriodo(), MC), SCALE, ROUNDING_MODE);
                flujo.setFactorDescuento(factorDescuento);
                
                BigDecimal valorActual = flujoValor.multiply(factorDescuento, MC);
                flujo.setValorActual(valorActual);
                flujo.setValorPresente(valorActual);
                
                // Acumular para el cálculo de duración: t * VA(flujo)
                sumaPonderada = sumaPonderada.add(factorTiempo.multiply(valorActual, MC), MC);
                
                // Acumular el precio total (suma de valores actuales)
                precio = precio.add(valorActual, MC);
            }
        }
        
        // Duracion = Suma(t * VA(flujo)) / Precio
        if (precio.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal duracion = sumaPonderada.divide(precio, SCALE, ROUNDING_MODE);
            
            // Convertir duracion a años si está en periodos
            BigDecimal duracionAnios = duracion.divide(BigDecimal.valueOf(frecuenciaPagos), SCALE, ROUNDING_MODE);
            
            return duracionAnios;
        }
        
        return BigDecimal.ZERO;
    }
    
    @Override
    public BigDecimal calcularDuracion(Bono bono) {
        List<FlujoFinanciero> flujos = calcularFlujoFinanciero(bono);
        return calcularDuracion(flujos, bono.getTasaCupon());
    }

    @Override
    public BigDecimal calcularConvexidad(List<FlujoFinanciero> flujos, BigDecimal tcea) {
        BigDecimal sumaConvexidad = BigDecimal.ZERO;
        BigDecimal precio = BigDecimal.ZERO;
        
        // Convertir TCEA a decimal si viene en porcentaje
        BigDecimal tceaDecimal = tcea;
        if (tcea.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tceaDecimal = tcea.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        // Obtener información del bono para calcular correctamente
        Bono bono = null;
        if (!flujos.isEmpty() && flujos.get(0).getBono() != null) {
            bono = flujos.get(0).getBono();
        }
        
        int frecuenciaPagos = (bono != null) ? bono.getFrecuenciaPagos() : 2;
        
        // Calcular tasa periódica para descuento
        BigDecimal tasaPeriodica = calcularTasaEfectivaPeriodica(tceaDecimal, frecuenciaPagos);
        
        // Saltar el periodo 0 (desembolso inicial)
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero ff = flujos.get(i);
            
            // Solo considerar flujos positivos reales
            BigDecimal flujoValor = ff.getFlujoTotal();
            if (flujoValor == null) {
                flujoValor = ff.getFlujo();
            }
            
            if (flujoValor.compareTo(BigDecimal.ZERO) > 0) {
                // Usar el periodo exacto
                int periodo = ff.getPeriodo();
                BigDecimal t = new BigDecimal(periodo);
                BigDecimal tMasUno = t.add(BigDecimal.ONE);
                
                // Factor de descuento: 1/(1+tasaPeriodica)^periodo
                BigDecimal factorDescuento = BigDecimal.ONE
                        .divide(BigDecimal.ONE.add(tasaPeriodica, MC).pow(periodo, MC), SCALE, ROUNDING_MODE);
                
                BigDecimal valorActual = flujoValor.multiply(factorDescuento, MC);
                
                // Fórmula de convexidad: t * (t + 1) * VA(flujo) / (1 + r)^2
                BigDecimal contribucionConvexidad = t.multiply(tMasUno, MC)
                        .multiply(valorActual, MC);
                
                sumaConvexidad = sumaConvexidad.add(contribucionConvexidad, MC);
                
                // Acumular el precio total (suma de valores actuales)
                precio = precio.add(valorActual, MC);
            }
        }
        
        // Convexidad = Suma(t * (t+1) * VA(flujo)) / (Precio * (1+r)^2)
        if (precio.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal divisor = precio.multiply(
                BigDecimal.ONE.add(tasaPeriodica).pow(2, MC), MC
            );
            
            BigDecimal convexidad = sumaConvexidad.divide(divisor, SCALE, ROUNDING_MODE);
            
            // Normalizamos para convertir de periodos a años
            BigDecimal m = new BigDecimal(frecuenciaPagos);
            BigDecimal convexidadAnual = convexidad.divide(m.pow(2), SCALE, ROUNDING_MODE);
            
            return convexidadAnual;
        }
        
        return BigDecimal.ZERO;
    }
    
    @Override
    public BigDecimal calcularConvexidad(Bono bono) {
        List<FlujoFinanciero> flujos = calcularFlujoFinanciero(bono);
        return calcularConvexidad(flujos, bono.getTasaCupon());
    }

    @Override
    public BigDecimal calcularPrecioMaximo(List<FlujoFinanciero> flujos, BigDecimal trea) {
        // Asegurarse de que la tasa esté en formato decimal (ej: 0.05 para 5%)
        BigDecimal tasaDecimal = trea;
        if (trea.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tasaDecimal = trea.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        if (flujos == null || flujos.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // Obtener la información del bono para determinar la frecuencia de pagos
        Bono bono = flujos.get(0).getBono();
        int frecuenciaPagos = (bono != null) ? bono.getFrecuenciaPagos() : 2;
        
        // Calcular la tasa periódica
        BigDecimal tasaPeriodica = calcularTasaEfectivaPeriodica(tasaDecimal, frecuenciaPagos);
        
        BigDecimal precioMaximo = BigDecimal.ZERO;
        
        // Calcular el valor presente de todos los flujos futuros usando la tasa esperada
        // Saltar el flujo inicial (período 0) que es el desembolso
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            
            // Obtener el flujo total
            BigDecimal flujoValor = flujo.getFlujoTotal();
            if (flujoValor == null || flujoValor.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            
            // Calcular el factor de descuento: 1 / (1 + r)^n
            BigDecimal factorDescuento = BigDecimal.ONE
                    .divide(BigDecimal.ONE.add(tasaPeriodica).pow(flujo.getPeriodo(), MC), SCALE, ROUNDING_MODE);
            
            // Calcular el valor presente de este flujo
            BigDecimal valorPresente = flujoValor.multiply(factorDescuento, MC);
            
            // Acumular al precio máximo
            precioMaximo = precioMaximo.add(valorPresente);
        }
        
        // Redondear a 2 decimales para mostrar como precio
        return precioMaximo.setScale(2, ROUNDING_MODE);
    }
    
    @Override
    public BigDecimal calcularPrecioMaximo(Bono bono, BigDecimal tasaEsperada) {
        List<FlujoFinanciero> flujos = calcularFlujoFinanciero(bono);
        return calcularPrecioMaximo(flujos, tasaEsperada);
    }

    @Override
    public String identificarMetodoAmortizacion(Bono bono) {
        String metodoExplicito = bono.getMetodoAmortizacion();
        
        if (metodoExplicito != null && !metodoExplicito.isEmpty()) {
            return metodoExplicito;
        }
        
        // Por defecto, asumimos método alemán (amortización constante)
        return "ALEMAN";
    }

    @Override
    public void procesarCalculosBono(Bono bono) {
        System.out.println("📊 Procesando cálculos del bono: " + bono.getNombre());
        
        // 1. Calculamos el TCEA
        BigDecimal tcea = calcularTCEA(bono);
        bono.setTcea(tcea);
        System.out.println("  💼 TCEA calculado: " + tcea);
        
        // 2. Obtenemos flujos financieros ya generados por la entidad
        List<FlujoFinanciero> flujos = bono.getFlujos();
        if (flujos == null || flujos.isEmpty()) {
            System.out.println("  ⚠️ No hay flujos generados, creándolos...");
            flujos = calcularFlujoFinanciero(bono);
        } else {
            System.out.println("  ✅ Usando flujos ya generados: " + flujos.size());
        }
        
        // 3. Calculamos duración y convexidad usando TCEA
        BigDecimal duracion = calcularDuracion(flujos, tcea);
        BigDecimal convexidad = calcularConvexidad(flujos, tcea);
        
        bono.setDuracion(duracion);
        bono.setConvexidad(convexidad);
        System.out.println("  📏 Duración: " + duracion + ", Convexidad: " + convexidad);
        
        // 4. Calculamos el precio máximo usando una tasa de mercado por defecto
        // Usar TCEA + 1% como tasa de mercado conservadora para inversores
        BigDecimal tasaMercado = tcea.add(new BigDecimal("0.01"));
        BigDecimal precioMercado = calcularPrecioMaximo(flujos, tasaMercado);
        
        // 5. Guardamos la tasa de descuento utilizada y métricas adicionales
        bono.setTasaDescuento(tasaMercado);
        
        // Agregar información de procesamiento al bono (si tiene campo para ello)
        // En futuras versiones podríamos agregar un campo de metadatos al bono
        System.out.println(String.format("Procesamiento completo del bono %s - TCEA: %.2f%%, Duración: %.2f años, Convexidad: %.4f, Precio mercado estimado: %.2f", 
            bono.getNombre() != null ? bono.getNombre() : "Sin nombre",
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            duracion.doubleValue(),
            convexidad.doubleValue(),
            precioMercado.doubleValue()));
    }

    @Override
    public Calculo calcularInversion(Bono bono, BigDecimal tasaEsperada) {
        // Asegurarse de que la tasa esté en formato decimal (ej: 0.05 para 5%)
        BigDecimal tasaDecimal = tasaEsperada;
        if (tasaEsperada.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tasaDecimal = tasaEsperada.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        Calculo calculo = new Calculo();
        calculo.setBono(bono);
        calculo.setTasaEsperada(tasaEsperada); // Guardamos la tasa en su formato original
        calculo.setFechaCalculo(LocalDate.now());
        
        // Calculamos el precio máximo que debería pagar para obtener la tasa esperada
        BigDecimal precioMaximo = calcularPrecioMaximo(bono, tasaDecimal);
        calculo.setPrecioMaximo(precioMaximo);
        
        // Calculamos la TREA real basada en ese precio máximo
        BigDecimal trea = calcularTREA(bono, precioMaximo);
        calculo.setTrea(trea);
        
        // Calcular métricas adicionales para análisis completo
        BigDecimal tcea = calcularTCEA(bono);
        BigDecimal duracion = calcularDuracion(bono);
        BigDecimal convexidad = calcularConvexidad(bono);
        BigDecimal spread = trea.subtract(tcea);
        BigDecimal margenSeguridad = bono.getValorNominal().subtract(precioMaximo);
        
        // Campos adicionales para enriquecer el historial
        calculo.setTipoAnalisis("TREA");
        
        // Parámetros del bono en el momento del cálculo (para historial)
        calculo.setValorNominal(bono.getValorNominal());
        calculo.setTasaCupon(bono.getTasaCupon());
        calculo.setPlazoAnios(bono.getPlazoAnios());
        calculo.setFrecuenciaPagos(bono.getFrecuenciaPagos());
        calculo.setMoneda(bono.getMoneda() != null ? bono.getMoneda() : "PEN");
        
        // Resultados adicionales del cálculo
        calculo.setTreaPorcentaje(trea.multiply(BigDecimal.valueOf(100)));
        calculo.setValorPresente(precioMaximo);
        
        // Información enriquecida del análisis
        String infoAdicional = String.format("Análisis de Inversión - TREA objetivo: %.2f%%, Precio máximo: %.2f, TCEA emisor: %.2f%%, Spread: %.2f%%, Duración: %.2f años, Convexidad: %.4f, Margen seguridad: %.2f, Método amortización: %s", 
            tasaEsperada.doubleValue(),
            precioMaximo.doubleValue(),
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            spread.multiply(BigDecimal.valueOf(100)).doubleValue(),
            duracion.doubleValue(),
            convexidad.doubleValue(),
            margenSeguridad.doubleValue(),
            identificarMetodoAmortizacion(bono));
        calculo.setInformacionAdicional(infoAdicional);
        
        return calculo;
    }

    @Override
    public void procesarCalculosInversor(Calculo calculo) {
        Bono bono = calculo.getBono();
        BigDecimal tasaEsperada = calculo.getTasaEsperada();
        
        // Asegurarse de que la tasa esté en formato decimal
        BigDecimal tasaDecimal = tasaEsperada;
        if (tasaEsperada.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tasaDecimal = tasaEsperada.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        // Calculamos el precio máximo que debería pagar para obtener la tasa esperada
        BigDecimal precioMaximo = calcularPrecioMaximo(bono, tasaDecimal);
        calculo.setPrecioMaximo(precioMaximo);
        
        // Calculamos la TREA real basada en ese precio máximo
        BigDecimal trea = calcularTREA(bono, precioMaximo);
        calculo.setTrea(trea);
        
        // Enriquecer el cálculo con métricas adicionales
        BigDecimal tcea = calcularTCEA(bono);
        BigDecimal duracion = calcularDuracion(bono);
        BigDecimal convexidad = calcularConvexidad(bono);
        BigDecimal spread = trea.subtract(tcea);
        BigDecimal margenSeguridad = bono.getValorNominal().subtract(precioMaximo);
        
        // Actualizar todos los campos enriquecidos
        calculo.setTreaPorcentaje(trea.multiply(BigDecimal.valueOf(100)));
        calculo.setValorPresente(precioMaximo);
        calculo.setValorNominal(bono.getValorNominal());
        calculo.setTasaCupon(bono.getTasaCupon());
        calculo.setPlazoAnios(bono.getPlazoAnios());
        calculo.setFrecuenciaPagos(bono.getFrecuenciaPagos());
        calculo.setMoneda(bono.getMoneda() != null ? bono.getMoneda() : "PEN");
        
        // Actualizar información detallada
        String infoAdicional = String.format("Procesamiento actualizado - TREA: %.2f%%, Precio máximo: %.2f, TCEA: %.2f%%, Spread: %.2f%%, Duración: %.2f años, Convexidad: %.4f, Margen seguridad: %.2f", 
            trea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            precioMaximo.doubleValue(),
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            spread.multiply(BigDecimal.valueOf(100)).doubleValue(),
            duracion.doubleValue(),
            convexidad.doubleValue(),
            margenSeguridad.doubleValue());
        calculo.setInformacionAdicional(infoAdicional);
        
        // Actualizamos el cálculo
        calculoRepository.save(calculo);
    }

    @Override
    public BigDecimal calcularTREA(Bono bono, BigDecimal precioCompra) {
        // Asegurarse de que el precio de compra esté en formato correcto
        BigDecimal precioCompraDecimal = precioCompra;
        if (precioCompra.compareTo(BigDecimal.valueOf(100)) <= 0) {
            // Si está en formato porcentual del valor nominal, convertir
            precioCompraDecimal = bono.getValorNominal().multiply(precioCompra.divide(BigDecimal.valueOf(100), MC));
        }
        
        // Genera flujos financieros del bono
        List<FlujoFinanciero> flujos = calcularFlujoFinanciero(bono);
        
        // Calcula la TREA como TIR de la inversión
        return calcularTIR(flujos, precioCompraDecimal, bono);
    }
    
    /**
     * Calcula la TIR (Tasa Interna de Retorno) dado un precio de compra
     */
    private BigDecimal calcularTIR(List<FlujoFinanciero> flujos, BigDecimal precioCompra, Bono bono) {
        // Para método alemán, siempre usar bisección ya que los flujos son variables
        if ("ALEMAN".equals(bono.getMetodoAmortizacion())) {
            return calcularTIRBiseccion(flujos, precioCompra, bono);
        }
        
        // Para bonos simples, usar método analítico más preciso
        if (bono.getPlazoAnios() <= 3) {
            return calcularTIRAnalitica(precioCompra, bono);
        }
        
        // Para bonos complejos, usar bisección que es más estable
        return calcularTIRBiseccion(flujos, precioCompra, bono);
    }
    
    /**
     * Calcula TIR analíticamente para bonos simples (hasta 3 años)
     */
    private BigDecimal calcularTIRAnalitica(BigDecimal precioCompra, Bono bono) {
        BigDecimal valorNominal = bono.getValorNominal(); 
        BigDecimal tasaCupon = bono.getTasaCupon().divide(BigDecimal.valueOf(100), MC); // Convertir a decimal
        int frecuenciaPagos = bono.getFrecuenciaPagos();
        int plazoAnios = bono.getPlazoAnios();
        
        // Cupón por período (no anual)
        BigDecimal cuponPeriodo = valorNominal.multiply(tasaCupon).divide(BigDecimal.valueOf(frecuenciaPagos), MC);
        int totalPeriodos = plazoAnios * frecuenciaPagos;
        
        // Para 1 período: TIR = (Cupón + Valor Nominal) / Precio Compra - 1
        if (totalPeriodos == 1) {
            BigDecimal flujoTotal = cuponPeriodo.add(valorNominal);
            BigDecimal tirPeriodica = flujoTotal.divide(precioCompra, MC).subtract(BigDecimal.ONE);
            // Convertir a tasa anual efectiva
            return convertirTasaPeriodicaAAnual(tirPeriodica, frecuenciaPagos);
        }
        
        // Para 2 períodos: usar fórmula cuadrática
        if (totalPeriodos == 2) {
            return calcularTIR2Periodos(precioCompra, cuponPeriodo, valorNominal, frecuenciaPagos);
        }
        
        // Para casos más complejos: usar bisección con conversión correcta
        return calcularTIRBiseccionConConversion(precioCompra, cuponPeriodo, valorNominal, totalPeriodos, frecuenciaPagos);
    }
    
    /**
     * Convierte tasa periódica a tasa anual efectiva
     */
    private BigDecimal convertirTasaPeriodicaAAnual(BigDecimal tasaPeriodica, int frecuenciaPagos) {
        // Fórmula: (1 + r_periodo)^m - 1
        BigDecimal tasaAnual = BigDecimal.ONE.add(tasaPeriodica).pow(frecuenciaPagos, MC).subtract(BigDecimal.ONE);
        return tasaAnual.multiply(BigDecimal.valueOf(100)).setScale(2, ROUNDING_MODE);
    }
    
    /**
     * Calcula TIR para bono de 2 períodos usando fórmula cuadrática
     */
    private BigDecimal calcularTIR2Periodos(BigDecimal precio, BigDecimal cuponPeriodo, BigDecimal valorNominal, int frecuenciaPagos) {
        // Ecuación: 0 = -P + C/(1+r) + (C+VN)/(1+r)²
        // Reorganizando: P(1+r)² = C(1+r) + (C+VN)
        // P(1+r)² - C(1+r) - (C+VN) = 0
        // Sustituyendo x = (1+r): Px² - Cx - (C+VN) = 0
        
        BigDecimal a = precio;
        BigDecimal b = cuponPeriodo.negate();
        BigDecimal c = cuponPeriodo.add(valorNominal).negate();
        
        // Fórmula cuadrática: x = (-b ± √(b²-4ac)) / 2a
        BigDecimal discriminante = b.pow(2).subtract(a.multiply(c).multiply(BigDecimal.valueOf(4)));
        
        if (discriminante.compareTo(BigDecimal.ZERO) < 0) {
            // No hay solución real, usar bisección
            return calcularTIRBiseccionConConversion(precio, cuponPeriodo, valorNominal, 2, frecuenciaPagos);
        }
        
        BigDecimal raizDiscriminante = new BigDecimal(Math.sqrt(discriminante.doubleValue()));
        BigDecimal x1 = b.negate().add(raizDiscriminante).divide(a.multiply(BigDecimal.valueOf(2)), MC);
        BigDecimal x2 = b.negate().subtract(raizDiscriminante).divide(a.multiply(BigDecimal.valueOf(2)), MC);
        
        // Elegir la solución positiva y mayor que 1 (ya que x = 1+r)
        BigDecimal x = (x1.compareTo(BigDecimal.ONE) > 0) ? x1 : x2;
        BigDecimal tirPeriodica = x.subtract(BigDecimal.ONE);
        
        // Convertir a tasa anual efectiva
        return convertirTasaPeriodicaAAnual(tirPeriodica, frecuenciaPagos);
    }
    
    /**
     * Calcula TIR usando bisección con conversión correcta a tasa anual
     */
    private BigDecimal calcularTIRBiseccionConConversion(BigDecimal precio, BigDecimal cuponPeriodo, BigDecimal valorNominal, int totalPeriodos, int frecuenciaPagos) {
        BigDecimal tirMin = new BigDecimal("-0.5"); // -50% periódica
        BigDecimal tirMax = new BigDecimal("2.0");   // 200% periódica
        BigDecimal precision = new BigDecimal("0.0001"); // 0.01% periódica
        int maxIteraciones = 100;
        
        for (int i = 0; i < maxIteraciones; i++) {
            BigDecimal tirMedio = tirMin.add(tirMax).divide(BigDecimal.valueOf(2), MC);
            BigDecimal van = calcularVANPeriodico(precio, cuponPeriodo, valorNominal, totalPeriodos, tirMedio);
            
            if (van.abs().compareTo(precision) < 0) {
                return convertirTasaPeriodicaAAnual(tirMedio, frecuenciaPagos);
            }
            
            if (van.compareTo(BigDecimal.ZERO) > 0) {
                tirMin = tirMedio;
            } else {
                tirMax = tirMedio;
            }
        }
        
        // Si no converge, devolver el punto medio
        BigDecimal tirFinal = tirMin.add(tirMax).divide(BigDecimal.valueOf(2), MC);
        return convertirTasaPeriodicaAAnual(tirFinal, frecuenciaPagos);
    }
    
    /**
     * Calcula VAN usando flujos periódicos
     */
    private BigDecimal calcularVANPeriodico(BigDecimal precio, BigDecimal cuponPeriodo, BigDecimal valorNominal, int totalPeriodos, BigDecimal tirPeriodica) {
        BigDecimal van = precio.negate(); // Inversión inicial negativa
        BigDecimal unMasTir = BigDecimal.ONE.add(tirPeriodica);
        
        // Sumar cupones descontados
        for (int t = 1; t <= totalPeriodos; t++) {
            BigDecimal factorDescuento = unMasTir.pow(t, MC);
            if (t == totalPeriodos) {
                // Último período: cupón + valor nominal
                BigDecimal flujoFinal = cuponPeriodo.add(valorNominal);
                van = van.add(flujoFinal.divide(factorDescuento, MC));
            } else {
                // Períodos intermedios: solo cupón
                van = van.add(cuponPeriodo.divide(factorDescuento, MC));
            }
        }
        
        return van;
    }
    
    /**
     * Calcula TIR para bono de 2 años usando fórmula cuadrática
     * @deprecated Usar calcularTIR2Periodos en su lugar
     */
    @Deprecated
    private BigDecimal calcularTIR2Anios(BigDecimal precio, BigDecimal cupon, BigDecimal valorNominal) {
        // Ecuación: 0 = -P + C/(1+r) + (C+VN)/(1+r)²
        // Reorganizando: P(1+r)² = C(1+r) + (C+VN)
        // P(1+r)² - C(1+r) - (C+VN) = 0
        // Sustituyendo x = (1+r): Px² - Cx - (C+VN) = 0
        
        BigDecimal a = precio;
        BigDecimal b = cupon.negate();
        BigDecimal c = cupon.add(valorNominal).negate();
        
        // Fórmula cuadrática: x = (-b ± √(b²-4ac)) / 2a
        BigDecimal discriminante = b.pow(2).subtract(a.multiply(c).multiply(BigDecimal.valueOf(4)));
        
        if (discriminante.compareTo(BigDecimal.ZERO) < 0) {
            // No hay solución real, usar bisección
            return calcularTIRBiseccionSimple(precio, cupon, valorNominal, 2);
        }
        
        BigDecimal raizDiscriminante = new BigDecimal(Math.sqrt(discriminante.doubleValue()));
        BigDecimal x1 = b.negate().add(raizDiscriminante).divide(a.multiply(BigDecimal.valueOf(2)), MC);
        BigDecimal x2 = b.negate().subtract(raizDiscriminante).divide(a.multiply(BigDecimal.valueOf(2)), MC);
        
        // Elegir la solución positiva y mayor que 1 (ya que x = 1+r)
        BigDecimal x = (x1.compareTo(BigDecimal.ONE) > 0) ? x1 : x2;
        BigDecimal tir = x.subtract(BigDecimal.ONE);
        
        return tir.multiply(BigDecimal.valueOf(100)).setScale(2, ROUNDING_MODE);
    }
    
    /**
     * Calcula TIR usando bisección simplificada
     */
    private BigDecimal calcularTIRBiseccionSimple(BigDecimal precio, BigDecimal cupon, BigDecimal valorNominal, int plazo) {
        BigDecimal tirMin = new BigDecimal("-0.5"); // -50%
        BigDecimal tirMax = new BigDecimal("2.0");   // 200%
        BigDecimal precision = new BigDecimal("0.0001"); // 0.01%
        int maxIteraciones = 100;
        
        for (int i = 0; i < maxIteraciones; i++) {
            BigDecimal tirMedio = tirMin.add(tirMax).divide(BigDecimal.valueOf(2), MC);
            BigDecimal van = calcularVANSimple(precio, cupon, valorNominal, plazo, tirMedio);
            
            if (van.abs().compareTo(precision) < 0) {
                return tirMedio.multiply(BigDecimal.valueOf(100)).setScale(2, ROUNDING_MODE);
            }
            
            if (van.compareTo(BigDecimal.ZERO) > 0) {
                tirMin = tirMedio;
            } else {
                tirMax = tirMedio;
            }
        }
        
        // Si no converge, devolver el punto medio
        BigDecimal tirFinal = tirMin.add(tirMax).divide(BigDecimal.valueOf(2), MC);
        return tirFinal.multiply(BigDecimal.valueOf(100)).setScale(2, ROUNDING_MODE);
    }
    
    /**
     * Calcula TIR usando bisección para bonos complejos
     */
    private BigDecimal calcularTIRBiseccion(List<FlujoFinanciero> flujos, BigDecimal precioCompra, Bono bono) {
        BigDecimal tirMin = new BigDecimal("-0.5"); // -50% anual
        BigDecimal tirMax = new BigDecimal("2.0");   // 200% anual
        BigDecimal precision = new BigDecimal("0.0001"); // 0.01% anual
        int maxIteraciones = 100;
        int frecuenciaPagos = bono.getFrecuenciaPagos();
        
        for (int i = 0; i < maxIteraciones; i++) {
            BigDecimal tirAnualMedio = tirMin.add(tirMax).divide(BigDecimal.valueOf(2), MC);
            
            // Convertir tasa anual a periódica para el cálculo del VAN
            BigDecimal tirPeriodica = convertirTasaAnualAPeriodica(tirAnualMedio, frecuenciaPagos);
            BigDecimal van = calcularVANParaBiseccionConTasaPeriodica(flujos, precioCompra, tirPeriodica);
            
            if (van.abs().compareTo(precision) < 0) {
                return tirAnualMedio.multiply(BigDecimal.valueOf(100)).setScale(2, ROUNDING_MODE);
            }
            
            if (van.compareTo(BigDecimal.ZERO) > 0) {
                tirMin = tirAnualMedio;
            } else {
                tirMax = tirAnualMedio;
            }
        }
        
        // Si no converge, devolver el punto medio
        BigDecimal tirFinal = tirMin.add(tirMax).divide(BigDecimal.valueOf(2), MC);
        return tirFinal.multiply(BigDecimal.valueOf(100)).setScale(2, ROUNDING_MODE);
    }
    
    /**
     * Convierte tasa anual efectiva a tasa periódica
     */
    private BigDecimal convertirTasaAnualAPeriodica(BigDecimal tasaAnual, int frecuenciaPagos) {
        // Fórmula: (1 + r_anual)^(1/m) - 1
        double tasaAnualDouble = tasaAnual.doubleValue();
        double tasaPeriodicaDouble = Math.pow(1.0 + tasaAnualDouble, 1.0/frecuenciaPagos) - 1.0;
        return new BigDecimal(tasaPeriodicaDouble).setScale(SCALE, ROUNDING_MODE);
    }
    
    /**
     * Calcula VAN para bisección con tasa periódica correcta
     */
    private BigDecimal calcularVANParaBiseccionConTasaPeriodica(List<FlujoFinanciero> flujos, BigDecimal precioCompra, BigDecimal tirPeriodica) {
        BigDecimal van = precioCompra.negate(); // Inversión inicial negativa
        BigDecimal unMasTir = BigDecimal.ONE.add(tirPeriodica);
        
        // Sumar flujos futuros descontados (saltar el período 0)
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            BigDecimal flujoValor = flujo.getFlujoTotal();
            
            if (flujoValor != null && flujoValor.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal factorDescuento = unMasTir.pow(flujo.getPeriodo(), MC);
                BigDecimal valorPresente = flujoValor.divide(factorDescuento, MC);
                van = van.add(valorPresente);
            }
        }
        
        return van;
    }
    
    /**
     * Calcula VAN simple para bonos regulares
     */
    private BigDecimal calcularVANSimple(BigDecimal precio, BigDecimal cupon, BigDecimal valorNominal, int plazo, BigDecimal tir) {
        BigDecimal van = precio.negate(); // Inversión inicial negativa
        BigDecimal unMasTir = BigDecimal.ONE.add(tir);
        
        // Sumar cupones descontados
        for (int t = 1; t <= plazo; t++) {
            BigDecimal factorDescuento = unMasTir.pow(t, MC);
            if (t == plazo) {
                // Último período: cupón + valor nominal
                BigDecimal flujoFinal = cupon.add(valorNominal);
                van = van.add(flujoFinal.divide(factorDescuento, MC));
            } else {
                // Períodos intermedios: solo cupón
                van = van.add(cupon.divide(factorDescuento, MC));
            }
        }
        
        return van;
    }
    
    /**
     * Calcula VAN para bisección con flujos complejos
     */
    private BigDecimal calcularVANParaBiseccion(List<FlujoFinanciero> flujos, BigDecimal precioCompra, BigDecimal tir, Bono bono) {
        BigDecimal van = precioCompra.negate(); // Inversión inicial negativa
        BigDecimal unMasTir = BigDecimal.ONE.add(tir);
        
        // Sumar flujos futuros descontados (saltar el período 0)
        for (int i = 1; i < flujos.size(); i++) {
            FlujoFinanciero flujo = flujos.get(i);
            BigDecimal flujoValor = flujo.getFlujoTotal();
            
            if (flujoValor != null && flujoValor.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal factorDescuento = unMasTir.pow(flujo.getPeriodo(), MC);
                BigDecimal valorPresente = flujoValor.divide(factorDescuento, MC);
                van = van.add(valorPresente);
            }
        }
        
        return van;
    }

    @Override
    public BigDecimal convertirTasaNominalAEfectiva(BigDecimal tn, int capitalizaciones, int periodoTotal) {
        // Convertimos tasa nominal a decimal si viene en porcentaje
        BigDecimal tasaNominal = tn;
        if (tn.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tasaNominal = tn.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        // Fórmula: (1 + tn/m)^m - 1
        BigDecimal tasaPorCapitalizacion = tasaNominal.divide(BigDecimal.valueOf(capitalizaciones), MC);
        BigDecimal tasaEfectiva = BigDecimal.ONE
                .add(tasaPorCapitalizacion)
                .pow(capitalizaciones)
                .subtract(BigDecimal.ONE);
                
        return tasaEfectiva.setScale(SCALE, ROUNDING_MODE);
    }
    
    @Override
    public BigDecimal convertirTasa(BigDecimal tasaOrigen, String tipoOrigen, String tipoDestino, int capitalizaciones) {
        // Convertimos tasa origen a decimal si viene en porcentaje
        BigDecimal tasaOrigenDecimal = tasaOrigen;
        if (tasaOrigen.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tasaOrigenDecimal = tasaOrigen.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        // Caso base: mismos tipos
        if (tipoOrigen.equals(tipoDestino)) {
            return tasaOrigenDecimal;
        }
        
        // Conversión de nominal a efectiva
        if (tipoOrigen.equals("NOMINAL") && tipoDestino.equals("EFECTIVA")) {
            return convertirTasaNominalAEfectiva(tasaOrigenDecimal, capitalizaciones, capitalizaciones);
        }
        
        // Conversión de efectiva a nominal
        if (tipoOrigen.equals("EFECTIVA") && tipoDestino.equals("NOMINAL")) {
            // Fórmula: m * ((1 + TEA)^(1/m) - 1)
            
            double tasaEfectivaDouble = tasaOrigenDecimal.doubleValue();
            double potencia = Math.pow(1.0 + tasaEfectivaDouble, 1.0/capitalizaciones) - 1.0;
            BigDecimal tasaPeriodica = new BigDecimal(potencia);
            
            return tasaPeriodica.multiply(new BigDecimal(capitalizaciones))
                    .setScale(SCALE, ROUNDING_MODE);
        }
        
        // Por defecto, devolvemos la misma tasa
        return tasaOrigenDecimal;
    }
    
    @Override
    public BigDecimal calcularValorFuturo(BigDecimal capital, BigDecimal tasa, int periodos) {
        // Convertimos tasa a decimal si viene en porcentaje
        BigDecimal tasaDecimal = tasa;
        if (tasa.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tasaDecimal = tasa.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        // Fórmula: VF = VA * (1 + r)^n
        return capital.multiply(
            BigDecimal.ONE.add(tasaDecimal).pow(periodos, MC)
        ).setScale(SCALE, ROUNDING_MODE);
    }
    
    @Override
    public BigDecimal calcularValorPresente(BigDecimal montoFuturo, BigDecimal tasa, int periodos) {
        // Convertimos tasa a decimal si viene en porcentaje
        BigDecimal tasaDecimal = tasa;
        if (tasa.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tasaDecimal = tasa.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        // Fórmula: VP = VF / (1 + r)^n
        BigDecimal factorDescuento = BigDecimal.ONE.add(tasaDecimal).pow(periodos, MC);
        return montoFuturo.divide(factorDescuento, SCALE, ROUNDING_MODE);
    }
    
    @Override
    public BigDecimal calcularEcuacionEquivalente(List<BigDecimal> montos, List<Integer> periodos, BigDecimal tasa) {
        // Validar entradas
        if (montos.size() != periodos.size()) {
            throw new IllegalArgumentException("La cantidad de montos debe ser igual a la cantidad de periodos");
        }
        
        // Convertimos tasa a decimal si viene en porcentaje
        BigDecimal tasaDecimal = tasa;
        if (tasa.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            tasaDecimal = tasa.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        // Calculamos el valor presente de cada monto
        BigDecimal valorPresenteTotal = BigDecimal.ZERO;
        
        for (int i = 0; i < montos.size(); i++) {
            BigDecimal monto = montos.get(i);
            int periodo = periodos.get(i);
            
            BigDecimal valorPresente = calcularValorPresente(monto, tasaDecimal, periodo);
            valorPresenteTotal = valorPresenteTotal.add(valorPresente);
        }
        
        return valorPresenteTotal.setScale(SCALE, ROUNDING_MODE);
    }

    @Override
    public String validarCalculoPrecio(Bono bono, BigDecimal trea) {
        // Validamos que el bono y la tasa no sean nulos
        if (bono == null) {
            return "El bono no puede ser nulo";
        }
        
        if (trea == null) {
            return "La tasa esperada de rendimiento no puede ser nula";
        }
        
        // Verificamos que el valor nominal del bono sea positivo
        if (bono.getValorNominal() == null || bono.getValorNominal().compareTo(BigDecimal.ZERO) <= 0) {
            return "El valor nominal del bono debe ser positivo";
        }
        
        // Verificamos que la tasa cupón sea positiva
        if (bono.getTasaCupon() == null || bono.getTasaCupon().compareTo(BigDecimal.ZERO) < 0) {
            return "La tasa cupón del bono debe ser positiva o cero";
        }
        
        // Verificamos que el plazo en años sea positivo
        if (bono.getPlazoAnios() <= 0) {
            return "El plazo en años debe ser positivo";
        }
        
        // Verificamos que la frecuencia de pagos sea válida
        if (bono.getFrecuenciaPagos() <= 0) {
            return "La frecuencia de pagos debe ser positiva";
        }
        
        // Verificamos que la fecha de emisión no sea nula
        if (bono.getFechaEmision() == null) {
            return "La fecha de emisión no puede ser nula";
        }
        
        // Verificamos que la tasa esperada sea positiva
        BigDecimal treaDecimal = trea;
        if (trea.compareTo(BigDecimal.valueOf(0.1)) > 0) {
            treaDecimal = trea.divide(BigDecimal.valueOf(100), SCALE, ROUNDING_MODE);
        }
        
        if (treaDecimal.compareTo(BigDecimal.ZERO) < 0) {
            return "La tasa esperada de rendimiento debe ser positiva o cero";
        }
        
        return null; // Todo válido
    }

    @Override
    public Bono corregirCalculosBonoAmericano(Long bonoId) {
        // Método heredado para compatibilidad - redirige al método general de corrección
        // TODO: Implementar lógica de corrección de cálculos para cualquier método de amortización
        return null;
    }
    
    // ===== MÉTODOS ENRIQUECIDOS =====
    
    @Override
    public Calculo calcularTREAEnriquecido(Bono bono, BigDecimal precioCompra, String inversorUsername) {
        // Crear el objeto Calculo base
        Calculo calculo = crearCalculoBase(bono, inversorUsername, "TREA");
        
        // Calcular TREA
        BigDecimal trea = calcularTREA(bono, precioCompra);
        calculo.setTrea(trea);
        calculo.setTreaPorcentaje(trea.multiply(BigDecimal.valueOf(100)));
        
        // Configurar datos específicos del análisis TREA
        calculo.setPrecioMaximo(precioCompra);
        calculo.setValorPresente(precioCompra);
        calculo.setTasaEsperada(precioCompra);
        
        // Calcular TODAS las métricas financieras avanzadas como en análisis completo
        BigDecimal tcea = calcularTCEA(bono);
        BigDecimal duracion = calcularDuracion(bono);
        BigDecimal convexidad = calcularConvexidad(bono);
        
        // Asignar métricas avanzadas al objeto de cálculo
        calculo.setTcea(tcea);
        calculo.setDuracion(duracion);
        BigDecimal tceaDecimal = tcea.divide(BigDecimal.valueOf(100), MC);
        BigDecimal duracionModificada = calcularDuracionModificada(duracion, tceaDecimal);
        calculo.setDuracionModificada(duracionModificada);
        calculo.setConvexidad(convexidad);
        
        // Calcular métricas financieras avanzadas usando flujos
        List<FlujoFinanciero> flujos = calcularFlujoFinanciero(bono);
        
        // TIR (Tasa Interna de Retorno) - igual a TREA para este caso
        BigDecimal tir = trea; // Para TREA enriquecido, TIR es igual a TREA
        calculo.setTir(tir);
        
        // VAN (Valor Actual Neto) usando tasa esperada
        BigDecimal tasaDescuento = precioCompra.compareTo(BigDecimal.valueOf(100)) <= 0 ? 
            precioCompra.divide(BigDecimal.valueOf(100), MC) : precioCompra;
        BigDecimal van = calcularVAN(flujos, precioCompra, tasaDescuento);
        calculo.setVan(van);
        
        // Precio justo (valor teórico del bono)
        BigDecimal precioJusto = calcularPrecioJusto(flujos, tceaDecimal);
        calculo.setPrecioJusto(precioJusto);
        
        // Valor presente solo de cupones
        BigDecimal valorPresenteCupones = calcularValorPresenteCupones(flujos, tceaDecimal);
        calculo.setValorPresenteCupones(valorPresenteCupones);
        
        // Yield to Maturity (YTM)
        BigDecimal yield = tir;
        calculo.setYield(yield);
        
        // Sensibilidad del precio
        BigDecimal sensibilidadPrecio = calcularSensibilidadPrecio(duracionModificada, tceaDecimal);
        calculo.setSensibilidadPrecio(sensibilidadPrecio);
        
        // Ganancia de capital esperada
        BigDecimal gananciaCapital = calcularGananciaCapital(bono.getValorNominal(), precioCompra);
        calculo.setGananciaCapital(gananciaCapital);
        
        // Ingresos totales por cupones
        BigDecimal ingresosCupones = calcularIngresosTotalesCupones(flujos);
        calculo.setIngresosCupones(ingresosCupones);
        
        // Rendimiento total esperado
        BigDecimal rendimientoTotal = calcularRendimientoTotal(gananciaCapital, ingresosCupones, precioCompra);
        calculo.setRendimientoTotal(rendimientoTotal);
        
        // Información enriquecida del análisis TREA
        String infoAdicional = String.format("TREA enriquecida: %.2f%%, TIR: %.2f%%, VAN: %.2f, TCEA: %.2f%%, Precio justo: %.2f, Duración: %.2f años, Convexidad: %.4f", 
            trea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            tir.multiply(BigDecimal.valueOf(100)).doubleValue(),
            van.doubleValue(),
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            precioJusto.doubleValue(),
            duracion.doubleValue(),
            convexidad.doubleValue());
        calculo.setInformacionAdicional(infoAdicional);
        
        return calculo;
    }
    
    @Override
    public Calculo calcularTCEAEnriquecido(Bono bono, String inversorUsername) {
        // Crear el objeto Calculo base
        Calculo calculo = crearCalculoBase(bono, inversorUsername, "TCEA");
        
        // Calcular TCEA
        BigDecimal tcea = calcularTCEA(bono);
        calculo.setTrea(tcea); // Para TCEA, usamos el mismo campo trea
        calculo.setTreaPorcentaje(tcea.multiply(BigDecimal.valueOf(100)));
        
        // Para TCEA, el precio máximo es el valor nominal (precio par)
        calculo.setPrecioMaximo(bono.getValorNominal());
        calculo.setValorPresente(bono.getValorNominal());
        calculo.setInformacionAdicional("Análisis TCEA - Tasa Costo Efectiva Anual del emisor");
        
        // Calcular métricas adicionales
        BigDecimal duracion = calcularDuracion(bono);
        
        // Agregar información detallada
        String infoAdicional = String.format("TCEA: %.2f%%, Duración: %.2f años, Método: %s", 
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            duracion.doubleValue(),
            identificarMetodoAmortizacion(bono));
        calculo.setInformacionAdicional(infoAdicional);
        
        return calculo;
    }
    
    @Override
    public Calculo calcularDuracionEnriquecida(Bono bono, String inversorUsername) {
        // Crear el objeto Calculo base
        Calculo calculo = crearCalculoBase(bono, inversorUsername, "DURACION");
        
        // Calcular duración y otras métricas relacionadas
        BigDecimal duracion = calcularDuracion(bono);
        BigDecimal tcea = calcularTCEA(bono);
        BigDecimal convexidad = calcularConvexidad(bono);
        
        // Guardar la duración en el campo trea para persistencia
        calculo.setTrea(duracion);
        calculo.setTreaPorcentaje(duracion.multiply(BigDecimal.valueOf(100))); // Duración en formato porcentual para display
        
        // Para duración, el precio máximo es el valor nominal
        calculo.setPrecioMaximo(bono.getValorNominal());
        calculo.setValorPresente(bono.getValorNominal());
        
        // Información detallada sobre la duración
        String infoAdicional = String.format("Duración de Macaulay: %.2f años, TCEA: %.2f%%, Convexidad: %.4f, Sensibilidad al 1%%: %.2f%%", 
            duracion.doubleValue(),
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            convexidad.doubleValue(),
            duracion.multiply(BigDecimal.valueOf(0.01)).doubleValue()); // Aproximación de sensibilidad
        calculo.setInformacionAdicional(infoAdicional);
        
        return calculo;
    }
    
    @Override
    public Calculo calcularConvexidadEnriquecida(Bono bono, String inversorUsername) {
        // Crear el objeto Calculo base
        Calculo calculo = crearCalculoBase(bono, inversorUsername, "CONVEXIDAD");
        
        // Calcular convexidad y otras métricas relacionadas
        BigDecimal convexidad = calcularConvexidad(bono);
        BigDecimal duracion = calcularDuracion(bono);
        BigDecimal tcea = calcularTCEA(bono);
        
        // Guardar la convexidad en el campo trea para persistencia
        calculo.setTrea(convexidad);
        calculo.setTreaPorcentaje(convexidad.multiply(BigDecimal.valueOf(10000))); // Convexidad * 10000 para display
        
        // Para convexidad, el precio máximo es el valor nominal
        calculo.setPrecioMaximo(bono.getValorNominal());
        calculo.setValorPresente(bono.getValorNominal());
        
        // Información detallada sobre la convexidad
        String infoAdicional = String.format("Convexidad: %.4f, Duración: %.2f años, TCEA: %.2f%%, Corrección convexidad al 1%%: %.4f%%", 
            convexidad.doubleValue(),
            duracion.doubleValue(),
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            convexidad.multiply(BigDecimal.valueOf(0.5)).multiply(BigDecimal.valueOf(0.01)).multiply(BigDecimal.valueOf(0.01)).doubleValue());
        calculo.setInformacionAdicional(infoAdicional);
        
        return calculo;
    }
    
    @Override
    public Calculo calcularPrecioMaximoEnriquecido(Bono bono, BigDecimal tasaEsperada, String inversorUsername) {
        // Crear el objeto Calculo base
        Calculo calculo = crearCalculoBase(bono, inversorUsername, "PRECIO_MAXIMO");
        
        // Usar el método existente que ya está enriquecido
        Calculo calculoBase = calcularInversion(bono, tasaEsperada);
        
        // Copiar todos los datos del cálculo base y agregar información adicional
        calculo.setTasaEsperada(calculoBase.getTasaEsperada());
        calculo.setTrea(calculoBase.getTrea());
        calculo.setTreaPorcentaje(calculoBase.getTreaPorcentaje());
        calculo.setPrecioMaximo(calculoBase.getPrecioMaximo());
        calculo.setValorPresente(calculoBase.getValorPresente());
        calculo.setInversorUsername(inversorUsername);
        
        // Calcular métricas adicionales
        BigDecimal duracion = calcularDuracion(bono);
        BigDecimal tcea = calcularTCEA(bono);
        
        // Información enriquecida
        String infoAdicional = String.format("Precio máximo: %.2f, TREA esperada: %.2f%%, TCEA emisor: %.2f%%, Duración: %.2f años, Spread: %.2f%%", 
            calculoBase.getPrecioMaximo().doubleValue(),
            calculoBase.getTrea().multiply(BigDecimal.valueOf(100)).doubleValue(),
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            duracion.doubleValue(),
            calculoBase.getTrea().subtract(tcea).multiply(BigDecimal.valueOf(100)).doubleValue());
        calculo.setInformacionAdicional(infoAdicional);
        
        return calculo;
    }
    
    @Override
    public Calculo calcularAnalisisCompleto(Bono bono, BigDecimal tasaEsperada, String inversorUsername) {
        // Crear el objeto Calculo base
        Calculo calculo = crearCalculoBase(bono, inversorUsername, "ANALISIS_COMPLETO");
        
        // Calcular todas las métricas financieras básicas
        BigDecimal precioMaximo = calcularPrecioMaximo(bono, tasaEsperada);
        BigDecimal trea = calcularTREA(bono, precioMaximo);
        BigDecimal tcea = calcularTCEA(bono);
        BigDecimal duracion = calcularDuracion(bono);
        BigDecimal convexidad = calcularConvexidad(bono);
        
        // Configurar valores básicos
        calculo.setTasaEsperada(tasaEsperada);
        calculo.setTrea(trea);
        calculo.setTreaPorcentaje(trea.multiply(BigDecimal.valueOf(100)));
        calculo.setPrecioMaximo(precioMaximo);
        calculo.setValorPresente(precioMaximo);
        
        // Calcular métricas financieras avanzadas
        List<FlujoFinanciero> flujos = calcularFlujoFinanciero(bono);
        
        // TIR (Tasa Interna de Retorno)
        BigDecimal tir = calcularTIR(flujos, precioMaximo, bono);
        calculo.setTir(tir);
        
        // VAN (Valor Actual Neto) - convertir tasa esperada de % a decimal y de anual a periódica
        BigDecimal tasaEsperadaDecimal = tasaEsperada.divide(BigDecimal.valueOf(100), MC);
        BigDecimal tasaEsperadaPeriodica = convertirTasaAnualAPeriodica(tasaEsperadaDecimal, bono.getFrecuenciaPagos());
        BigDecimal van = calcularVAN(flujos, precioMaximo, tasaEsperadaPeriodica);
        calculo.setVan(van);
        
        // TCEA (mantener formato decimal para cálculos internos)
        BigDecimal tceaDecimal = tcea.divide(BigDecimal.valueOf(100), MC);
        calculo.setTcea(tcea);
        
        // Duración y Duración Modificada
        calculo.setDuracion(duracion);
        BigDecimal duracionModificada = calcularDuracionModificada(duracion, tceaDecimal);
        calculo.setDuracionModificada(duracionModificada);
        
        // Convexidad
        calculo.setConvexidad(convexidad);
        
        // Precio justo (valor teórico del bono)
        BigDecimal precioJusto = calcularPrecioJusto(flujos, tceaDecimal);
        calculo.setPrecioJusto(precioJusto);
        
        // Valor presente solo de cupones
        BigDecimal valorPresenteCupones = calcularValorPresenteCupones(flujos, tceaDecimal);
        calculo.setValorPresenteCupones(valorPresenteCupones);
        
        // Yield to Maturity (YTM)
        BigDecimal yield = tir; // YTM es equivalente a TIR para bonos
        calculo.setYield(yield);
        
        // Sensibilidad del precio
        BigDecimal sensibilidadPrecio = calcularSensibilidadPrecio(duracionModificada, tcea);
        calculo.setSensibilidadPrecio(sensibilidadPrecio);
        
        // Ganancia de capital esperada
        BigDecimal gananciaCapital = calcularGananciaCapital(bono.getValorNominal(), precioMaximo);
        calculo.setGananciaCapital(gananciaCapital);
        
        // Ingresos totales por cupones
        BigDecimal ingresosCupones = calcularIngresosTotalesCupones(flujos);
        calculo.setIngresosCupones(ingresosCupones);
        
        // Rendimiento total esperado
        BigDecimal rendimientoTotal = calcularRendimientoTotal(gananciaCapital, ingresosCupones, precioMaximo);
        calculo.setRendimientoTotal(rendimientoTotal);
        
        // Información completa del análisis
        String infoAdicional = String.format("Análisis completo - TREA: %.2f%%, TIR: %.2f%%, VAN: %.2f, TCEA: %.2f%%, Precio justo: %.2f, Duración: %.2f años, Convexidad: %.4f", 
            trea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            tir.multiply(BigDecimal.valueOf(100)).doubleValue(),
            van.doubleValue(),
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            precioJusto.doubleValue(),
            duracion.doubleValue(),
            convexidad.doubleValue());
        calculo.setInformacionAdicional(infoAdicional);
        
        return calculo;
    }
    
    // =================================================================
    // MÉTODOS DE CÁLCULOS FINANCIEROS AVANZADOS
    // =================================================================
    
    /**
     * Calcula la Tasa Interna de Retorno (TIR) de un bono (método público simplificado)
     */
    public BigDecimal calcularTIRPublico(List<FlujoFinanciero> flujos, BigDecimal precioCompra) {
        // Implementación del método de Newton-Raphson para encontrar la TIR
        BigDecimal tir = BigDecimal.valueOf(0.1); // Valor inicial 10%
        BigDecimal tolerance = BigDecimal.valueOf(0.000001);
        int maxIteraciones = 100;
        
        for (int i = 0; i < maxIteraciones; i++) {
            BigDecimal f = calcularFuncionVAN(flujos, precioCompra, tir);
            BigDecimal df = calcularDerivadaVAN(flujos, tir);
            
            if (df.abs().compareTo(tolerance) < 0) {
                break;
            }
            
            BigDecimal nuevaTir = tir.subtract(f.divide(df, MC));
            
            if (nuevaTir.subtract(tir).abs().compareTo(tolerance) < 0) {
                break;
            }
            
            tir = nuevaTir;
        }
        
        return tir;
    }
    
    /**
     * Calcula el Valor Actual Neto (VAN)
     */
    public BigDecimal calcularVAN(List<FlujoFinanciero> flujos, BigDecimal inversionInicial, BigDecimal tasaDescuento) {
        System.out.println("🔥 DEBUGGING VAN - Parámetros de entrada:");
        System.out.println("  - Inversión inicial: " + inversionInicial);
        System.out.println("  - Tasa de descuento: " + tasaDescuento);
        System.out.println("  - Cantidad de flujos: " + flujos.size());
        
        BigDecimal van = inversionInicial.negate(); // Inversión inicial como flujo negativo
        BigDecimal sumaVPFlujos = BigDecimal.ZERO;
        
        System.out.println("🔥 DEBUGGING VAN - Cálculo de VP de flujos:");
        for (FlujoFinanciero flujo : flujos) {
            if (flujo.getPeriodo() > 0) { // Excluir el período 0 (inversión inicial)
                BigDecimal factorDescuento = BigDecimal.ONE.add(tasaDescuento).pow(flujo.getPeriodo(), MC);
                BigDecimal valorPresente = flujo.getFlujoTotal().divide(factorDescuento, MC);
                van = van.add(valorPresente);
                sumaVPFlujos = sumaVPFlujos.add(valorPresente);
                
                System.out.println("  Período " + flujo.getPeriodo() + 
                    ": Flujo=" + flujo.getFlujoTotal() + 
                    ", Factor=" + factorDescuento + 
                    ", VP=" + valorPresente);
            }
        }
        
        System.out.println("🔥 DEBUGGING VAN - Resultado final:");
        System.out.println("  - Suma VP flujos futuros: " + sumaVPFlujos);
        System.out.println("  - Inversión inicial (negativa): " + inversionInicial.negate());
        System.out.println("  - VAN = " + sumaVPFlujos + " - " + inversionInicial + " = " + van);
        
        return van;
    }
    
    /**
     * Calcula la duración modificada
     */
    public BigDecimal calcularDuracionModificada(BigDecimal duracion, BigDecimal rendimiento) {
        BigDecimal denominador = BigDecimal.ONE.add(rendimiento);
        return duracion.divide(denominador, MC);
    }
    
    /**
     * Calcula el precio justo del bono (valor teórico)
     */
    public BigDecimal calcularPrecioJusto(List<FlujoFinanciero> flujos, BigDecimal tasaDescuento) {
        BigDecimal precioJusto = BigDecimal.ZERO;
        
        // Convertir tasa anual efectiva a tasa periódica
        // Asumiendo que tasaDescuento viene en formato decimal (ej: 0.0609 para 6.09%)
        // y necesitamos convertirla a tasa semestral
        int frecuenciaPagos = 2; // Default semestral, podría parametrizarse
        BigDecimal tasaPeriodica = convertirTasaAnualAPeriodica(tasaDescuento, frecuenciaPagos);
        
        for (FlujoFinanciero flujo : flujos) {
            if (flujo.getPeriodo() > 0) { // Excluir el período 0
                BigDecimal factorDescuento = BigDecimal.ONE.add(tasaPeriodica).pow(flujo.getPeriodo(), MC);
                BigDecimal valorPresente = flujo.getFlujoTotal().divide(factorDescuento, MC);
                precioJusto = precioJusto.add(valorPresente);
            }
        }
        
        return precioJusto.setScale(2, ROUNDING_MODE);
    }
    
    /**
     * Calcula el valor presente solo de los cupones
     */
    public BigDecimal calcularValorPresenteCupones(List<FlujoFinanciero> flujos, BigDecimal tasaDescuento) {
        BigDecimal valorPresenteCupones = BigDecimal.ZERO;
        
        // Convertir tasa anual efectiva a tasa periódica
        int frecuenciaPagos = 2; // Default semestral
        BigDecimal tasaPeriodica = convertirTasaAnualAPeriodica(tasaDescuento, frecuenciaPagos);
        
        for (FlujoFinanciero flujo : flujos) {
            if (flujo.getPeriodo() > 0) { // Excluir el período 0
                BigDecimal cupon = flujo.getCupon() != null ? flujo.getCupon() : 
                    (flujo.getInteres() != null ? flujo.getInteres() : BigDecimal.ZERO);
                if (cupon.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal factorDescuento = BigDecimal.ONE.add(tasaPeriodica).pow(flujo.getPeriodo(), MC);
                    BigDecimal valorPresenteCupon = cupon.divide(factorDescuento, MC);
                    valorPresenteCupones = valorPresenteCupones.add(valorPresenteCupon);
                }
            }
        }
        
        return valorPresenteCupones.setScale(2, ROUNDING_MODE);
    }
    
    /**
     * Calcula la sensibilidad del precio ante cambios en la tasa
     */
    public BigDecimal calcularSensibilidadPrecio(BigDecimal duracionModificada, BigDecimal cambioTasa) {
        // Sensibilidad = -Duración Modificada × Cambio en la tasa
        return duracionModificada.negate().multiply(cambioTasa);
    }
    
    /**
     * Calcula la ganancia de capital esperada
     */
    public BigDecimal calcularGananciaCapital(BigDecimal valorNominal, BigDecimal precioCompra) {
        return valorNominal.subtract(precioCompra);
    }
    
    /**
     * Calcula los ingresos totales por cupones
     */
    public BigDecimal calcularIngresosTotalesCupones(List<FlujoFinanciero> flujos) {
        BigDecimal ingresosTotales = BigDecimal.ZERO;
        
        for (FlujoFinanciero flujo : flujos) {
            if (flujo.getPeriodo() > 0) { // Excluir el período 0
                BigDecimal cupon = flujo.getCupon() != null ? flujo.getCupon() : 
                    (flujo.getInteres() != null ? flujo.getInteres() : BigDecimal.ZERO);
                ingresosTotales = ingresosTotales.add(cupon);
            }
        }
        
        return ingresosTotales.setScale(2, ROUNDING_MODE);
    }
    
    /**
     * Calcula el rendimiento total esperado
     */
    public BigDecimal calcularRendimientoTotal(BigDecimal gananciaCapital, BigDecimal ingresosCupones, BigDecimal inversionInicial) {
        BigDecimal rendimientoTotal = gananciaCapital.add(ingresosCupones);
        BigDecimal rendimientoDecimal = rendimientoTotal.divide(inversionInicial, MC);
        // Convertir a porcentaje
        return rendimientoDecimal.multiply(BigDecimal.valueOf(100)).setScale(2, ROUNDING_MODE);
    }
    
    // =================================================================
    // MÉTODOS AUXILIARES PARA CÁLCULOS AVANZADOS
    // =================================================================
    
    /**
     * Función VAN para el cálculo de TIR
     */
    private BigDecimal calcularFuncionVAN(List<FlujoFinanciero> flujos, BigDecimal inversionInicial, BigDecimal tasa) {
        BigDecimal van = inversionInicial.negate();
        
        for (FlujoFinanciero flujo : flujos) {
            if (flujo.getPeriodo() > 0) {
                BigDecimal factorDescuento = BigDecimal.ONE.add(tasa).pow(flujo.getPeriodo(), MC);
                BigDecimal valorPresente = flujo.getFlujoTotal().divide(factorDescuento, MC);
                van = van.add(valorPresente);
            }
        }
        
        return van;
    }
    
    /**
     * Derivada de la función VAN para el cálculo de TIR
     */
    private BigDecimal calcularDerivadaVAN(List<FlujoFinanciero> flujos, BigDecimal tasa) {
        BigDecimal derivada = BigDecimal.ZERO;
        
        for (FlujoFinanciero flujo : flujos) {
            if (flujo.getPeriodo() > 0) {
                BigDecimal periodo = BigDecimal.valueOf(flujo.getPeriodo());
                BigDecimal factorDescuento = BigDecimal.ONE.add(tasa).pow(flujo.getPeriodo() + 1, MC);
                BigDecimal termino = flujo.getFlujoTotal().multiply(periodo).divide(factorDescuento, MC);
                derivada = derivada.subtract(termino);
            }
        }
        
        return derivada;
    }
    
    /**
     * Método auxiliar para crear la base común de todos los cálculos enriquecidos
     */
    private Calculo crearCalculoBase(Bono bono, String inversorUsername, String tipoAnalisis) {
        Calculo calculo = new Calculo();
        calculo.setBono(bono);
        calculo.setInversorUsername(inversorUsername);
        calculo.setFechaCalculo(LocalDate.now());
        calculo.setTipoAnalisis(tipoAnalisis);
        
        // Parámetros del bono en el momento del cálculo (para historial)
        calculo.setValorNominal(bono.getValorNominal());
        calculo.setTasaCupon(bono.getTasaCupon());
        calculo.setPlazoAnios(bono.getPlazoAnios());
        calculo.setFrecuenciaPagos(bono.getFrecuenciaPagos());
        calculo.setMoneda(bono.getMoneda() != null ? bono.getMoneda() : "PEN");
        
        return calculo;
    }
    
    @Override
    public Calculo calcularAnalisisCompleto(Bono bono, BigDecimal tasaEsperada, BigDecimal precioCompra, String inversorUsername) {
        // Crear el objeto Calculo base
        Calculo calculo = crearCalculoBase(bono, inversorUsername, "ANALISIS_COMPLETO");
        
        // Calcular todas las métricas financieras básicas
        BigDecimal precioMaximo = calcularPrecioMaximo(bono, tasaEsperada);
        BigDecimal trea = calcularTREA(bono, precioCompra); // Usar precio real de compra para TREA
        BigDecimal tcea = calcularTCEA(bono);
        BigDecimal duracion = calcularDuracion(bono);
        BigDecimal convexidad = calcularConvexidad(bono);
        
        // Configurar valores básicos
        calculo.setTasaEsperada(tasaEsperada);
        calculo.setTrea(trea);
        calculo.setTreaPorcentaje(trea.multiply(BigDecimal.valueOf(100)));
        calculo.setPrecioMaximo(precioMaximo);
        calculo.setValorPresente(precioCompra); // Usar precio real de compra
        
        // Calcular métricas financieras avanzadas
        List<FlujoFinanciero> flujos = calcularFlujoFinanciero(bono);
        
        // TIR (Tasa Interna de Retorno) usando precio real de compra
        BigDecimal tir = calcularTIR(flujos, precioCompra, bono);
        calculo.setTir(tir);
        
        // VAN (Valor Actual Neto) - CORREGIDO para usar precio real de compra
        BigDecimal tasaEsperadaDecimal = tasaEsperada.divide(BigDecimal.valueOf(100), MC);
        BigDecimal tasaEsperadaPeriodica = convertirTasaAnualAPeriodica(tasaEsperadaDecimal, bono.getFrecuenciaPagos());
        BigDecimal van = calcularVAN(flujos, precioCompra, tasaEsperadaPeriodica); // Usar precio real de compra
        calculo.setVan(van);
        
        // TCEA (mantener formato decimal para cálculos internos)
        BigDecimal tceaDecimal = tcea.divide(BigDecimal.valueOf(100), MC);
        calculo.setTcea(tcea);
        
        // Duración y Duración Modificada
        calculo.setDuracion(duracion);
        BigDecimal duracionModificada = calcularDuracionModificada(duracion, tceaDecimal);
        calculo.setDuracionModificada(duracionModificada);
        
        // Convexidad
        calculo.setConvexidad(convexidad);
        
        // Precio justo (valor teórico del bono)
        BigDecimal precioJusto = calcularPrecioJusto(flujos, tceaDecimal);
        calculo.setPrecioJusto(precioJusto);
        
        // Valor presente solo de cupones
        BigDecimal valorPresenteCupones = calcularValorPresenteCupones(flujos, tceaDecimal);
        calculo.setValorPresenteCupones(valorPresenteCupones);
        
        // Yield to Maturity (YTM)
        BigDecimal yield = tir; // YTM es equivalente a TIR para bonos
        calculo.setYield(yield);
        
        // Sensibilidad del precio
        BigDecimal sensibilidadPrecio = calcularSensibilidadPrecio(duracionModificada, tcea);
        calculo.setSensibilidadPrecio(sensibilidadPrecio);
        
        // Ganancia de capital esperada (usando precio real de compra)
        BigDecimal gananciaCapital = calcularGananciaCapital(bono.getValorNominal(), precioCompra);
        calculo.setGananciaCapital(gananciaCapital);
        
        // Ingresos totales por cupones
        BigDecimal ingresosCupones = calcularIngresosTotalesCupones(flujos);
        calculo.setIngresosCupones(ingresosCupones);
        
        // Rendimiento total esperado (usando precio real de compra)
        BigDecimal rendimientoTotal = calcularRendimientoTotal(gananciaCapital, ingresosCupones, precioCompra);
        calculo.setRendimientoTotal(rendimientoTotal);
        
        // Información completa del análisis
        String infoAdicional = String.format("Análisis completo - TREA: %.2f%%, TIR: %.2f%%, VAN: %.2f, TCEA: %.2f%%, Precio justo: %.2f, Duración: %.2f años, Convexidad: %.4f, Precio compra: %.2f", 
            trea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            tir.multiply(BigDecimal.valueOf(100)).doubleValue(),
            van.doubleValue(),
            tcea.multiply(BigDecimal.valueOf(100)).doubleValue(),
            precioJusto.doubleValue(),
            duracion.doubleValue(),
            convexidad.doubleValue(),
            precioCompra.doubleValue());
        calculo.setInformacionAdicional(infoAdicional);
        
        return calculo;
    }
}