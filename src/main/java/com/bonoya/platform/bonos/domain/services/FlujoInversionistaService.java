package com.bonoya.platform.bonos.domain.services;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.valueobjects.FlujoInversionista;

import java.math.BigDecimal;
import java.util.List;

/**
 * Servicio de dominio para calcular el flujo de caja específico del inversionista.
 * Transforma el flujo genérico del bono en un flujo desde la perspectiva del inversionista.
 */
public interface FlujoInversionistaService {

    /**
     * Calcula el flujo de caja específico del inversionista basado en su precio de compra real.
     * 
     * @param bono El bono de inversión
     * @param precioCompra El precio real que pagará el inversionista
     * @return Lista de flujos desde la perspectiva del inversionista
     */
    List<FlujoInversionista> calcularFlujoInversionista(Bono bono, BigDecimal precioCompra);

    /**
     * Calcula métricas específicas del flujo del inversionista.
     * 
     * @param flujoInversionista Lista de flujos del inversionista
     * @param precioCompra Precio de compra inicial
     * @return Métricas calculadas
     */
    MetricasInversionista calcularMetricas(List<FlujoInversionista> flujoInversionista, BigDecimal precioCompra);

    /**
     * Clase interna para las métricas del inversionista
     */
    class MetricasInversionista {
        private final BigDecimal gananciaNeta;
        private final BigDecimal rendimientoTotal;
        private final Integer periodoRecuperacion;
        private final BigDecimal totalCupones;
        private final BigDecimal totalPrincipal;

        public MetricasInversionista(BigDecimal gananciaNeta, BigDecimal rendimientoTotal, 
                                   Integer periodoRecuperacion, BigDecimal totalCupones, 
                                   BigDecimal totalPrincipal) {
            this.gananciaNeta = gananciaNeta;
            this.rendimientoTotal = rendimientoTotal;
            this.periodoRecuperacion = periodoRecuperacion;
            this.totalCupones = totalCupones;
            this.totalPrincipal = totalPrincipal;
        }

        // Getters
        public BigDecimal getGananciaNeta() { return gananciaNeta; }
        public BigDecimal getRendimientoTotal() { return rendimientoTotal; }
        public Integer getPeriodoRecuperacion() { return periodoRecuperacion; }
        public BigDecimal getTotalCupones() { return totalCupones; }
        public BigDecimal getTotalPrincipal() { return totalPrincipal; }
    }
}
