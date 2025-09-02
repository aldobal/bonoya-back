package com.bonoya.platform.bonos.infrastructure.persistence.jpa.repositories;

import com.bonoya.platform.bonos.domain.model.entities.FlujoInversionistaDetalle;
import com.bonoya.platform.bonos.domain.model.entities.SimulacionInversionista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlujoInversionistaDetalleRepository extends JpaRepository<FlujoInversionistaDetalle, Long> {
    
    /**
     * Encuentra flujos por simulación ordenados por período
     */
    List<FlujoInversionistaDetalle> findBySimulacionOrderByPeriodo(SimulacionInversionista simulacion);
    
    /**
     * Encuentra flujos por simulación ID
     */
    List<FlujoInversionistaDetalle> findBySimulacion_IdOrderByPeriodo(Long simulacionId);
    
    /**
     * Encuentra solo flujos positivos (recibidos)
     */
    @Query("SELECT f FROM FlujoInversionistaDetalle f WHERE f.simulacion = :simulacion AND f.flujoNeto > 0 ORDER BY f.periodo")
    List<FlujoInversionistaDetalle> findPositiveFlowsBySimulation(@Param("simulacion") SimulacionInversionista simulacion);
    
    /**
     * Encuentra el período de recuperación
     */
    @Query("SELECT MIN(f.periodo) FROM FlujoInversionistaDetalle f WHERE f.simulacion = :simulacion AND f.saldoAcumulado >= 0 AND f.periodo > 0")
    Integer findRecoveryPeriod(@Param("simulacion") SimulacionInversionista simulacion);
    
    /**
     * Elimina flujos por simulación
     */
    void deleteBySimulacion(SimulacionInversionista simulacion);
    
    /**
     * Cuenta flujos por simulación
     */
    long countBySimulacion(SimulacionInversionista simulacion);
}
