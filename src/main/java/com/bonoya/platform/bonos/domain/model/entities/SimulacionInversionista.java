package com.bonoya.platform.bonos.domain.model.entities;

import com.bonoya.platform.shared.domain.model.entities.AuditableModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que almacena simulaciones de inversión específicas del inversionista.
 * Permite mantener historial de diferentes escenarios de inversión.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "simulaciones_inversionista")
public class SimulacionInversionista extends AuditableModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "bono_id", nullable = false)
    private Bono bono;
    
    @Column(name = "inversor_username", nullable = false)
    private String inversorUsername;
    
    @Column(name = "precio_compra", precision = 19, scale = 4, nullable = false)
    private BigDecimal precioCompra;
    
    @Column(name = "fecha_simulacion", nullable = false)
    private LocalDate fechaSimulacion;
    
    @Column(name = "descripcion", length = 500)
    private String descripcion;
    
    // Métricas calculadas
    @Column(name = "ganancia_neta", precision = 19, scale = 4)
    private BigDecimal gananciaNeta;
    
    @Column(name = "rendimiento_total", precision = 19, scale = 6)
    private BigDecimal rendimientoTotal;
    
    @Column(name = "periodo_recuperacion")
    private Integer periodoRecuperacion;
    
    @Column(name = "total_cupones", precision = 19, scale = 4)
    private BigDecimal totalCupones;
    
    @Column(name = "total_principal", precision = 19, scale = 4)
    private BigDecimal totalPrincipal;
    
    // Relación con los flujos detallados
    @OneToMany(mappedBy = "simulacion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<FlujoInversionistaDetalle> flujos = new ArrayList<>();
    
    // Constructores de utilidad
    public SimulacionInversionista(Bono bono, String inversorUsername, BigDecimal precioCompra, String descripcion) {
        this.bono = bono;
        this.inversorUsername = inversorUsername;
        this.precioCompra = precioCompra;
        this.descripcion = descripcion;
        this.fechaSimulacion = LocalDate.now();
    }
    
    // Métodos de utilidad
    public void agregarFlujo(FlujoInversionistaDetalle flujo) {
        flujos.add(flujo);
        flujo.setSimulacion(this);
    }
    
    public void calcularMetricas() {
        if (flujos.isEmpty()) return;
        
        // Calcular ganancia neta (último saldo acumulado)
        this.gananciaNeta = flujos.get(flujos.size() - 1).getSaldoAcumulado();
        
        // Calcular rendimiento total
        if (precioCompra.compareTo(BigDecimal.ZERO) > 0) {
            this.rendimientoTotal = gananciaNeta.divide(precioCompra, 6, java.math.RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        
        // Calcular período de recuperación
        this.periodoRecuperacion = flujos.stream()
                .filter(f -> f.getSaldoAcumulado().compareTo(BigDecimal.ZERO) >= 0 && f.getPeriodo() > 0)
                .mapToInt(FlujoInversionistaDetalle::getPeriodo)
                .min()
                .orElse(0);
        
        // Calcular totales
        this.totalCupones = flujos.stream()
                .filter(f -> f.getPeriodo() > 0)
                .map(FlujoInversionistaDetalle::getCupon)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        this.totalPrincipal = flujos.stream()
                .filter(f -> f.getPeriodo() > 0)
                .map(FlujoInversionistaDetalle::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    @Override
    public String toString() {
        return String.format("SimulacionInversionista{id=%d, bono=%s, inversor=%s, precioCompra=%s, gananciaNeta=%s, rendimiento=%s%%}", 
                           id, bono != null ? bono.getNombre() : "null", inversorUsername, 
                           precioCompra, gananciaNeta, rendimientoTotal);
    }
}
