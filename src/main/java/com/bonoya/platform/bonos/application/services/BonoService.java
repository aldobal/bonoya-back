package com.bonoya.platform.bonos.application.services;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;
import com.bonoya.platform.bonos.infrastructure.persistence.jpa.repositories.BonoRepository;
import com.bonoya.platform.bonos.infrastructure.persistence.jpa.repositories.FlujoFinancieroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class BonoService {

    private final BonoRepository bonoRepository;
    private final FlujoFinancieroRepository flujoFinancieroRepository;

    @Autowired
    public BonoService(BonoRepository bonoRepository, FlujoFinancieroRepository flujoFinancieroRepository) {
        this.bonoRepository = bonoRepository;
        this.flujoFinancieroRepository = flujoFinancieroRepository;
    }

    @Transactional
    public Bono crearBono(Bono bono) {
        System.out.println("🏭 Creando bono: " + bono.getNombre() + ", Método: " + bono.getMetodoAmortizacion());
        
        validarBono(bono);
        
        // Guardar el bono primero
        Bono bonoGuardado = bonoRepository.save(bono);
        System.out.println("  💾 Bono guardado con ID: " + bonoGuardado.getId());
        
        // Generar flujo de caja usando la tasa de descuento del bono
        BigDecimal tasaDescuento = bono.getTasaDescuento() != null ? bono.getTasaDescuento() : BigDecimal.valueOf(0.08);
        System.out.println("  📊 Generando flujo de caja con tasa descuento: " + tasaDescuento);
        
        List<FlujoFinanciero> flujos = bonoGuardado.generarFlujoCaja(tasaDescuento);
        System.out.println("  💰 Flujos generados: " + flujos.size());
        
        // Persistir los flujos en la base de datos
        if (!flujos.isEmpty()) {
            flujoFinancieroRepository.saveAll(flujos);
            System.out.println("  ✅ Flujos persistidos en BD");
        }
        
        return bonoGuardado;
    }

    public Optional<Bono> obtenerBonoPorId(Long id) {
        return bonoRepository.findById(id);
    }

    public List<Bono> obtenerTodosLosBonos() {
        return bonoRepository.findAll();
    }

    public List<Bono> obtenerBonosPorEmisor(String emisorUsername) {
        return bonoRepository.findByEmisorUsername(emisorUsername);
    }

    public List<Bono> obtenerBonosPorMoneda(String moneda) {
        return bonoRepository.findByMoneda(moneda);
    }

    public List<Bono> obtenerBonosPorRangoTasa(double tasaMinima, double tasaMaxima) {
        BigDecimal minTasa = BigDecimal.valueOf(tasaMinima);
        
        if (tasaMaxima == Double.MAX_VALUE) {
            // Si no se especificó tasa máxima, buscar bonos con tasa mayor o igual a la mínima
            return bonoRepository.findByTasaCuponGreaterThanEqual(minTasa);
        } else {
            BigDecimal maxTasa = BigDecimal.valueOf(tasaMaxima);
            return bonoRepository.findByTasaCuponBetween(minTasa, maxTasa);
        }
    }

    @Transactional
    public Bono actualizarBono(Long id, Bono bono) {
        System.out.println("🔄 Actualizando bono ID: " + id + ", Método: " + bono.getMetodoAmortizacion());
        
        validarBono(bono);
        return bonoRepository.findById(id)
                .map(existingBono -> {
                    bono.setId(id);
                    
                    // Eliminar flujos antiguos
                    flujoFinancieroRepository.deleteByBono(existingBono);
                    System.out.println("  🗑️ Flujos antiguos eliminados");
                    
                    // Guardar bono actualizado
                    Bono bonoActualizado = bonoRepository.save(bono);
                    
                    // Regenerar flujo de caja
                    BigDecimal tasaDescuento = bono.getTasaDescuento() != null ? bono.getTasaDescuento() : BigDecimal.valueOf(0.08);
                    System.out.println("  📊 Regenerando flujo de caja con tasa descuento: " + tasaDescuento);
                    
                    List<FlujoFinanciero> nuevos = bonoActualizado.generarFlujoCaja(tasaDescuento);
                    System.out.println("  💰 Nuevos flujos generados: " + nuevos.size());
                    
                    // Persistir nuevos flujos
                    if (!nuevos.isEmpty()) {
                        flujoFinancieroRepository.saveAll(nuevos);
                        System.out.println("  ✅ Nuevos flujos persistidos en BD");
                    }
                    
                    return bonoActualizado;
                })
                .orElseThrow(() -> new IllegalArgumentException("Bono no encontrado"));
    }

    public void eliminarBono(Long id) {
        bonoRepository.deleteById(id);
    }

    public void validarBono(Bono bono) {
        // Implementación básica
        if (bono.getValorNominal() == null || bono.getValorNominal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El valor nominal debe ser positivo");
        }
        
        if (bono.getTasaCupon() == null) {
            throw new IllegalArgumentException("La tasa cupón no puede ser nula");
        }
        
        if (bono.getPlazoAnios() <= 0) {
            throw new IllegalArgumentException("El plazo en años debe ser positivo");
        }
        
        if (bono.getFrecuenciaPagos() <= 0) {
            throw new IllegalArgumentException("La frecuencia de pagos debe ser positiva");
        }
        
        if (bono.getFechaEmision() == null) {
            throw new IllegalArgumentException("La fecha de emisión no puede ser nula");
        }
    }

    public List<FlujoFinanciero> obtenerFlujoFinancieroBono(Long id) {
        return bonoRepository.findById(id)
                .map(flujoFinancieroRepository::findByBonoOrderByPeriodo)
                .orElse(null);
    }
}

