package com.bonoya.platform.bonos.infrastructure.persistence.jpa.repositories;

import com.bonoya.platform.bonos.domain.model.entities.SimulacionInversionista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SimulacionInversionistaRepository extends JpaRepository<SimulacionInversionista, Long> {
    
    /**
     * Encuentra simulaciones por username del inversor
     */
    List<SimulacionInversionista> findByInversorUsernameOrderByFechaSimulacionDesc(String inversorUsername);
    
    /**
     * Encuentra simulaciones por bono
     */
    List<SimulacionInversionista> findByBono_IdOrderByFechaSimulacionDesc(Long bonoId);
    
    /**
     * Encuentra simulaciones por inversor y bono
     */
    List<SimulacionInversionista> findByInversorUsernameAndBono_IdOrderByFechaSimulacionDesc(
            String inversorUsername, Long bonoId);
    
    /**
     * Encuentra simulaciones por precio de compra en un rango
     */
    @Query("SELECT s FROM SimulacionInversionista s WHERE s.precioCompra BETWEEN :precioMin AND :precioMax ORDER BY s.fechaSimulacion DESC")
    List<SimulacionInversionista> findByPrecioCompraBetween(
            @Param("precioMin") BigDecimal precioMin, 
            @Param("precioMax") BigDecimal precioMax);
    
    /**
     * Encuentra simulaciones recientes (últimos N días)
     */
    @Query("SELECT s FROM SimulacionInversionista s WHERE s.fechaSimulacion >= :fechaDesde ORDER BY s.fechaSimulacion DESC")
    List<SimulacionInversionista> findRecentSimulations(@Param("fechaDesde") LocalDate fechaDesde);
    
    /**
     * Encuentra las mejores simulaciones por rendimiento
     */
    @Query("SELECT s FROM SimulacionInversionista s WHERE s.rendimientoTotal >= :rendimientoMinimo ORDER BY s.rendimientoTotal DESC")
    List<SimulacionInversionista> findByRendimientoTotalGreaterThanEqual(@Param("rendimientoMinimo") BigDecimal rendimientoMinimo);
    
    /**
     * Cuenta simulaciones por inversor
     */
    long countByInversorUsername(String inversorUsername);
    
    /**
     * Elimina simulaciones antiguas (más de X días)
     */
    @Query("DELETE FROM SimulacionInversionista s WHERE s.fechaSimulacion < :fechaLimite")
    void deleteOldSimulations(@Param("fechaLimite") LocalDate fechaLimite);
}
