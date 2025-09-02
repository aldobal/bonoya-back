package com.bonoya.platform.bonos.domain.model.entities;

import com.bonoya.platform.shared.domain.model.entities.AuditableModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entidad que almacena el detalle de cada flujo de una simulación del inversionista.
 * Complementa a SimulacionInversionista con información período por período.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "flujos_inversionista_detalle")
public class FlujoInversionistaDetalle extends AuditableModel {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "simulacion_id", nullable = false)
    private SimulacionInversionista simulacion;
    
    @Column(name = "periodo", nullable = false)
    private Integer periodo;
    
    @Column(name = "fecha")
    private LocalDate fecha;
    
    @Column(name = "cupon", precision = 19, scale = 4)
    private BigDecimal cupon = BigDecimal.ZERO;
    
    @Column(name = "principal", precision = 19, scale = 4)
    private BigDecimal principal = BigDecimal.ZERO;
    
    @Column(name = "flujo_total", precision = 19, scale = 4)
    private BigDecimal flujoTotal = BigDecimal.ZERO;
    
    @Column(name = "flujo_neto", precision = 19, scale = 4)
    private BigDecimal flujoNeto = BigDecimal.ZERO;
    
    @Column(name = "saldo_acumulado", precision = 19, scale = 4)
    private BigDecimal saldoAcumulado = BigDecimal.ZERO;
    
    @Column(name = "descripcion", length = 255)
    private String descripcion;
    
    @Column(name = "es_inversion_inicial")
    private boolean esInversionInicial = false;
    
    // Constructor de utilidad
    public FlujoInversionistaDetalle(SimulacionInversionista simulacion, Integer periodo, LocalDate fecha,
                                   BigDecimal cupon, BigDecimal principal, BigDecimal flujoTotal,
                                   BigDecimal flujoNeto, BigDecimal saldoAcumulado, String descripcion,
                                   boolean esInversionInicial) {
        this.simulacion = simulacion;
        this.periodo = periodo;
        this.fecha = fecha;
        this.cupon = cupon != null ? cupon : BigDecimal.ZERO;
        this.principal = principal != null ? principal : BigDecimal.ZERO;
        this.flujoTotal = flujoTotal != null ? flujoTotal : BigDecimal.ZERO;
        this.flujoNeto = flujoNeto != null ? flujoNeto : BigDecimal.ZERO;
        this.saldoAcumulado = saldoAcumulado != null ? saldoAcumulado : BigDecimal.ZERO;
        this.descripcion = descripcion;
        this.esInversionInicial = esInversionInicial;
    }
    
    // Métodos de utilidad
    public boolean esPositivo() {
        return flujoNeto.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean haRecuperadoInversion() {
        return saldoAcumulado.compareTo(BigDecimal.ZERO) >= 0;
    }
    
    public boolean esPeriodoFinal(int totalPeriodos) {
        return periodo != null && periodo == totalPeriodos;
    }
    
    @Override
    public String toString() {
        return String.format("FlujoInversionistaDetalle{periodo=%d, fecha=%s, flujoTotal=%s, saldoAcumulado=%s, descripcion='%s'}", 
                           periodo, fecha, flujoTotal, saldoAcumulado, descripcion);
    }
}
