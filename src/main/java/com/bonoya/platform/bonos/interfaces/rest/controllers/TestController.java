package com.bonoya.platform.bonos.interfaces.rest.controllers;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;
import com.bonoya.platform.bonos.domain.model.valueobjects.PlazoGracia;
import com.bonoya.platform.bonos.domain.model.valueobjects.TasaInteres;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/metodo-aleman")
    public ResponseEntity<Map<String, Object>> testMetodoAleman() {
        System.out.println("🧪 Test del método alemán iniciado");
        
        try {
            // Crear un bono de prueba
            Bono bono = new Bono();
            bono.setNombre("Bono Test Alemán");
            bono.setValorNominal(BigDecimal.valueOf(1000));
            bono.setTasaCupon(BigDecimal.valueOf(8)); // 8%
            bono.setPlazoAnios(2);
            bono.setFrecuenciaPagos(2); // Semestral
            bono.setFechaEmision(LocalDate.now());
            bono.setMetodoAmortizacion("ALEMAN");
            
            // Establecer objetos de valor
            bono.setPlazoGracia(PlazoGracia.sinPlazoGracia());
            bono.setMoneda("USD");
            bono.setTasaInteres(new TasaInteres(
                BigDecimal.valueOf(8),
                TasaInteres.TipoTasa.EFECTIVA,
                2
            ));
            
            System.out.println("  📋 Bono configurado: VN=" + bono.getValorNominal() + 
                              ", TC=" + bono.getTasaCupon() + 
                              ", Plazo=" + bono.getPlazoAnios() + " años" +
                              ", Freq=" + bono.getFrecuenciaPagos() +
                              ", Método=" + bono.getMetodoAmortizacion());
            
            // Generar flujo de caja
            BigDecimal tasaDescuento = BigDecimal.valueOf(0.08); // 8%
            List<FlujoFinanciero> flujos = bono.generarFlujoCaja(tasaDescuento);
            
            // Preparar respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("bonoTest", Map.of(
                "nombre", bono.getNombre(),
                "valorNominal", bono.getValorNominal(),
                "tasaCupon", bono.getTasaCupon(),
                "plazoAnios", bono.getPlazoAnios(),
                "frecuenciaPagos", bono.getFrecuenciaPagos(),
                "metodoAmortizacion", bono.getMetodoAmortizacion()
            ));
            response.put("flujosGenerados", flujos.size());
            response.put("flujos", flujos);
            response.put("success", true);
            response.put("message", "Método alemán ejecutado correctamente");
            
            System.out.println("✅ Test completado exitosamente - Flujos: " + flujos.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ Error en test: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("stackTrace", e.getStackTrace());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
