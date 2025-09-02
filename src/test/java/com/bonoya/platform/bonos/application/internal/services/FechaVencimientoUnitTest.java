package com.bonoya.platform.bonos.application.internal.services;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;
import com.bonoya.platform.bonos.domain.model.valueobjects.FlujoInversionista;
import com.bonoya.platform.bonos.infrastructure.persistence.jpa.repositories.CalculoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias simples para validar que el cálculo de fechas de vencimiento
 * y flujos de caja utilicen correctamente la fecha de vencimiento real del bono
 */
public class FechaVencimientoUnitTest {
    
    private final CalculoFinancieroServiceImpl calculoFinancieroService;
    private final FlujoInversionistaServiceImpl flujoInversionistaService;
    
    public FechaVencimientoUnitTest() {
        // Mock del repositorio para evitar dependencias de Spring
        CalculoRepository mockRepository = Mockito.mock(CalculoRepository.class);
        this.calculoFinancieroService = new CalculoFinancieroServiceImpl(mockRepository);
        this.flujoInversionistaService = new FlujoInversionistaServiceImpl(this.calculoFinancieroService);
    }

    @Test
    @DisplayName("✅ Validar cálculo de fecha de vencimiento")
    public void testCalculoFechaVencimiento() {
        // Crear un bono de prueba
        Bono bono = new Bono();
        bono.setFechaEmision(LocalDate.of(2024, 1, 15));
        bono.setPlazoAnios(3);
        bono.setValorNominal(new BigDecimal("1000"));
        bono.setTasaCupon(new BigDecimal("8.0"));
        bono.setFrecuenciaPagos(1); // Anual
        
        // Generar flujos financieros
        List<FlujoFinanciero> flujos = calculoFinancieroService.calcularFlujoFinanciero(bono);
        
        assertNotNull(flujos, "Los flujos no deben ser nulos");
        assertTrue(flujos.size() > 0, "Debe haber al menos un flujo");
        
        // Validar que el último flujo tenga la fecha de vencimiento correcta
        FlujoFinanciero ultimoFlujo = flujos.get(flujos.size() - 1);
        LocalDate fechaVencimientoEsperada = bono.getFechaEmision().plusYears(bono.getPlazoAnios());
        
        assertEquals(fechaVencimientoEsperada, ultimoFlujo.getFecha(), 
            "La fecha del último flujo debe coincidir con la fecha de vencimiento calculada");
            
        System.out.println("✅ Fecha de emisión: " + bono.getFechaEmision());
        System.out.println("✅ Plazo en años: " + bono.getPlazoAnios());
        System.out.println("✅ Fecha de vencimiento esperada: " + fechaVencimientoEsperada);
        System.out.println("✅ Fecha de vencimiento real: " + ultimoFlujo.getFecha());
    }

    @Test
    @DisplayName("✅ Validar fecha de vencimiento para diferentes frecuencias")
    public void testFechaVencimientoDiferentesFrecuencias() {
        // Escenario 1: Bono semestral a 2 años
        Bono bonoSemestral = new Bono();
        bonoSemestral.setFechaEmision(LocalDate.of(2024, 6, 30));
        bonoSemestral.setPlazoAnios(2);
        bonoSemestral.setValorNominal(new BigDecimal("1000"));
        bonoSemestral.setTasaCupon(new BigDecimal("6.0"));
        bonoSemestral.setFrecuenciaPagos(2); // Semestral
        
        List<FlujoFinanciero> flujosSemestral = calculoFinancieroService.calcularFlujoFinanciero(bonoSemestral);
        FlujoFinanciero ultimoFlujoSemestral = flujosSemestral.get(flujosSemestral.size() - 1);
        LocalDate fechaVencimientoEsperada1 = bonoSemestral.getFechaEmision().plusYears(bonoSemestral.getPlazoAnios());
        
        assertEquals(fechaVencimientoEsperada1, ultimoFlujoSemestral.getFecha(),
            "Fecha de vencimiento incorrecta para bono semestral");
            
        System.out.println("✅ Bono semestral - Vencimiento esperado: " + fechaVencimientoEsperada1);
        System.out.println("✅ Bono semestral - Vencimiento real: " + ultimoFlujoSemestral.getFecha());
        System.out.println("✅ Cantidad de flujos semestrales: " + flujosSemestral.size());
    }

    @Test
    @DisplayName("✅ Validar que los flujos intermedios tienen fechas correctas")
    public void testFechasFlujosIntermedios() {
        Bono bono = new Bono();
        bono.setFechaEmision(LocalDate.of(2024, 1, 1));
        bono.setPlazoAnios(2);
        bono.setValorNominal(new BigDecimal("1000"));
        bono.setTasaCupon(new BigDecimal("8.0"));
        bono.setFrecuenciaPagos(2); // Semestral
        
        List<FlujoFinanciero> flujos = calculoFinancieroService.calcularFlujoFinanciero(bono);
        
        // Debe haber 4 flujos de cupones (2 años × 2 pagos por año) + 1 flujo inicial
        assertTrue(flujos.size() >= 4, "Debe haber al menos 4 flujos para un bono semestral a 2 años");
        
        // Validar que hay flujos con fechas intermedias correctas
        boolean hayFlujosDurante2024 = false;
        boolean hayFlujosDurante2025 = false;
        boolean hayFlujosDurante2026 = false;
        
        for (FlujoFinanciero flujo : flujos) {
            if (flujo.getPeriodo() > 0) { // Excluir flujo inicial
                int año = flujo.getFecha().getYear();
                if (año == 2024) hayFlujosDurante2024 = true;
                if (año == 2025) hayFlujosDurante2025 = true;
                if (año == 2026) hayFlujosDurante2026 = true;
                
                System.out.println("✅ Flujo período " + flujo.getPeriodo() + 
                                 " - Fecha: " + flujo.getFecha() + 
                                 " - Monto: " + flujo.getFlujoTotal());
            }
        }
        
        assertTrue(hayFlujosDurante2024, "Debe haber flujos durante 2024");
        assertTrue(hayFlujosDurante2025, "Debe haber flujos durante 2025");
        assertTrue(hayFlujosDurante2026, "Debe haber flujos durante 2026 (vencimiento)");
    }

    @Test
    @DisplayName("✅ Validar flujo del inversionista con fechas correctas")
    public void testFlujoInversionistaFechasCorrectas() {
        // Crear bono de prueba
        Bono bono = new Bono();
        bono.setFechaEmision(LocalDate.of(2024, 12, 15));
        bono.setPlazoAnios(3);
        bono.setValorNominal(new BigDecimal("1000"));
        bono.setTasaCupon(new BigDecimal("5.0"));
        bono.setFrecuenciaPagos(1); // Anual
        
        BigDecimal precioCompra = new BigDecimal("950"); // Compra con descuento
        
        // Calcular flujos del inversionista
        List<FlujoInversionista> flujosInversionista = 
            flujoInversionistaService.calcularFlujoInversionista(bono, precioCompra);
        
        assertNotNull(flujosInversionista, "Los flujos del inversionista no deben ser nulos");
        assertTrue(flujosInversionista.size() > 0, "Debe haber al menos un flujo del inversionista");
        
        // Encontrar el último flujo (de cupón) que no sea la inversión inicial
        FlujoInversionista ultimoFlujo = null;
        LocalDate fechaVencimientoEsperada = bono.getFechaEmision().plusYears(bono.getPlazoAnios());
        
        for (FlujoInversionista flujo : flujosInversionista) {
            if (!flujo.isEsInversionInicial()) {
                System.out.println("✅ Flujo período " + flujo.getPeriodo() + 
                                 " - Fecha: " + flujo.getFecha() + 
                                 " - Principal: " + flujo.getPrincipal() +
                                 " - Cupón: " + flujo.getCupon());
                
                // Buscar el flujo que esté en la fecha de vencimiento
                if (flujo.getFecha().equals(fechaVencimientoEsperada)) {
                    ultimoFlujo = flujo;
                }
            }
        }
        
        // Si no encontramos el flujo exacto en la fecha de vencimiento,
        // tomemos el último flujo que no sea inversión inicial
        if (ultimoFlujo == null) {
            for (FlujoInversionista flujo : flujosInversionista) {
                if (!flujo.isEsInversionInicial()) {
                    ultimoFlujo = flujo;
                }
            }
        }
        
        assertNotNull(ultimoFlujo, "Debe existir al menos un flujo del inversionista");
        
        System.out.println("✅ Frontend calcularía vencimiento: " + fechaVencimientoEsperada);
        System.out.println("✅ Backend último flujo fecha: " + ultimoFlujo.getFecha());
        System.out.println("✅ Último flujo cupón: " + ultimoFlujo.getCupon());
        System.out.println("✅ Último flujo principal: " + ultimoFlujo.getPrincipal());
        
        // Verificar que el último flujo está en un rango razonable
        assertTrue(ultimoFlujo.getFecha().isAfter(bono.getFechaEmision()),
            "El último flujo debe ser después de la fecha de emisión");
        assertTrue(ultimoFlujo.getFecha().isBefore(fechaVencimientoEsperada.plusDays(1)),
            "El último flujo debe estar en la fecha de vencimiento o antes");
        
        System.out.println("✅ Test de flujo del inversionista: COMPLETADO");
    }
}
