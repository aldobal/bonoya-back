package com.bonoya.platform.bonos.application.internal.services;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;
import com.bonoya.platform.bonos.domain.model.valueobjects.FlujoInversionista;
import com.bonoya.platform.bonos.domain.services.CalculoFinancieroService;
import com.bonoya.platform.bonos.domain.services.FlujoInversionistaService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación del servicio de dominio para calcular flujos del inversionista.
 */
@Service
public class FlujoInversionistaServiceImpl implements FlujoInversionistaService {

    private static final MathContext MC = new MathContext(10, RoundingMode.HALF_UP);
    
    private final CalculoFinancieroService calculoFinancieroService;
    
    public FlujoInversionistaServiceImpl(CalculoFinancieroService calculoFinancieroService) {
        this.calculoFinancieroService = calculoFinancieroService;
    }

    @Override
    public List<FlujoInversionista> calcularFlujoInversionista(Bono bono, BigDecimal precioCompra) {
        System.out.println("🔄 Calculando flujo del inversionista - Bono: " + bono.getNombre() + 
                          ", Precio compra: " + precioCompra);
        
        // Obtener el flujo financiero del bono
        List<FlujoFinanciero> flujoOriginal = calculoFinancieroService.calcularFlujoFinanciero(bono);
        
        // Crear la lista de flujos del inversionista
        List<FlujoInversionista> flujoInversionista = new ArrayList<>();
        BigDecimal saldoAcumulado = precioCompra.negate(); // Empezamos con la inversión negativa
        
        // Período 0: Inversión inicial
        FlujoInversionista inversionInicial = FlujoInversionista.inversionInicial(precioCompra, LocalDate.now());
        flujoInversionista.add(inversionInicial);
        
        System.out.println("  📤 Período 0 (Inversión): " + precioCompra.negate());
        
        // Procesar flujos futuros del bono
        for (FlujoFinanciero flujoOriginalPeriodo : flujoOriginal) {
            if (flujoOriginalPeriodo.getPeriodo() != null && flujoOriginalPeriodo.getPeriodo() > 0) {
                BigDecimal cupon = flujoOriginalPeriodo.getCupon() != null ? 
                    flujoOriginalPeriodo.getCupon() : BigDecimal.ZERO;
                BigDecimal principal = flujoOriginalPeriodo.getAmortizacion() != null ? 
                    flujoOriginalPeriodo.getAmortizacion() : BigDecimal.ZERO;
                BigDecimal flujoTotalPeriodo = flujoOriginalPeriodo.getFlujoTotal() != null ? 
                    flujoOriginalPeriodo.getFlujoTotal() : BigDecimal.ZERO;
                
                // Actualizar saldo acumulado
                saldoAcumulado = saldoAcumulado.add(flujoTotalPeriodo);
                
                // Determinar descripción
                String descripcion = flujoOriginalPeriodo.getPeriodo() == flujoOriginal.size() ? 
                    "Pago final + valor nominal" : 
                    "Cupón período " + flujoOriginalPeriodo.getPeriodo();
                
                // Crear flujo del inversionista
                FlujoInversionista flujo = FlujoInversionista.flujoRecibido(
                    flujoOriginalPeriodo.getPeriodo(),
                    flujoOriginalPeriodo.getFecha(),
                    cupon,
                    principal,
                    saldoAcumulado,
                    descripcion
                );
                
                flujoInversionista.add(flujo);
                
                System.out.println("  📥 Período " + flujoOriginalPeriodo.getPeriodo() + 
                                  ": Flujo=" + flujoTotalPeriodo + ", Saldo=" + saldoAcumulado);
            }
        }
        
        System.out.println("✅ Flujo del inversionista calculado: " + flujoInversionista.size() + " períodos");
        return flujoInversionista;
    }

    @Override
    public MetricasInversionista calcularMetricas(List<FlujoInversionista> flujoInversionista, BigDecimal precioCompra) {
        if (flujoInversionista.isEmpty()) {
            return new MetricasInversionista(BigDecimal.ZERO, BigDecimal.ZERO, 0, 
                                           BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        // Ganancia neta: saldo final
        FlujoInversionista ultimoFlujo = flujoInversionista.get(flujoInversionista.size() - 1);
        BigDecimal gananciaNeta = ultimoFlujo.getSaldoAcumulado();
        
        // Rendimiento total: (ganancia / inversión) * 100
        BigDecimal rendimientoTotal = BigDecimal.ZERO;
        if (precioCompra.compareTo(BigDecimal.ZERO) > 0) {
            rendimientoTotal = gananciaNeta.divide(precioCompra, MC).multiply(BigDecimal.valueOf(100));
        }
        
        // Período de recuperación: primer período donde el saldo es positivo
        Integer periodoRecuperacion = 0;
        for (FlujoInversionista flujo : flujoInversionista) {
            if (flujo.haRecuperadoInversion() && flujo.getPeriodo() > 0) {
                periodoRecuperacion = flujo.getPeriodo();
                break;
            }
        }
        
        // Total de cupones y principal
        BigDecimal totalCupones = flujoInversionista.stream()
            .filter(f -> f.getPeriodo() > 0)
            .map(FlujoInversionista::getCupon)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        BigDecimal totalPrincipal = flujoInversionista.stream()
            .filter(f -> f.getPeriodo() > 0)
            .map(FlujoInversionista::getPrincipal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        System.out.println("📊 Métricas calculadas - Ganancia: " + gananciaNeta + 
                          ", ROI: " + rendimientoTotal + "%, Recuperación: período " + periodoRecuperacion);
        
        return new MetricasInversionista(gananciaNeta, rendimientoTotal, periodoRecuperacion, 
                                       totalCupones, totalPrincipal);
    }
}
