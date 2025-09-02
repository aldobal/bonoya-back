package com.bonoya.platform.bonos.interfaces.rest.controllers;

import com.bonoya.platform.bonos.application.services.BonoService;
import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;
import com.bonoya.platform.bonos.domain.services.CalculoFinancieroService;
import com.bonoya.platform.bonos.interfaces.rest.resources.BonoResource;
import com.bonoya.platform.bonos.interfaces.rest.resources.CreateBonoResource;
import com.bonoya.platform.bonos.interfaces.rest.transform.BonoResourceFromEntityAssembler;
import com.bonoya.platform.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/emisor/bonos", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Emisor Bonos", description = "Endpoints para manejo de bonos por emisores")
public class EmisorBonoController {

    private final BonoService bonoService;
    private final UserRepository userRepository;
    private final CalculoFinancieroService calculoFinancieroService;

    @Autowired
    public EmisorBonoController(BonoService bonoService, UserRepository userRepository, CalculoFinancieroService calculoFinancieroService) {
        this.bonoService = bonoService;
        this.userRepository = userRepository;
        this.calculoFinancieroService = calculoFinancieroService;
    }

    @GetMapping
    public ResponseEntity<List<BonoResource>> obtenerMisBonos() {
        String username = obtenerUsernameAutenticado();
        List<Bono> bonos = bonoService.obtenerBonosPorEmisor(username);
        List<BonoResource> resources = bonos.stream()
                .map(BonoResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BonoResource> obtenerBonoPorId(@PathVariable Long id) {
        String username = obtenerUsernameAutenticado();
        return bonoService.obtenerBonoPorId(id)
                .filter(bono -> bono.getEmisorUsername().equals(username))
                .map(BonoResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BonoResource> crearBono(@RequestBody CreateBonoResource resource) {
        String username = obtenerUsernameAutenticado();
        System.out.println("🎯 Creando bono desde controller - Método: " + resource.getMetodoAmortizacion());

        Bono bono = BonoResourceFromEntityAssembler.toEntityFromCreateResource(resource);
        bono.setEmisorUsername(username);

        // El método crearBono ya maneja la generación y persistencia de flujos
        Bono bonoCreado = bonoService.crearBono(bono);
        
        // Procesar cálculos adicionales (TCEA, duración, convexidad)
        calculoFinancieroService.procesarCalculosBono(bonoCreado);

        BonoResource bonoResource = BonoResourceFromEntityAssembler.toResourceFromEntity(bonoCreado);
        return new ResponseEntity<>(bonoResource, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BonoResource> actualizarBono(@PathVariable Long id, @RequestBody CreateBonoResource resource) {
        String username = obtenerUsernameAutenticado();

        return bonoService.obtenerBonoPorId(id)
                .filter(bono -> bono.getEmisorUsername().equals(username))
                .map(bono -> {
                    Bono bonoActualizado = BonoResourceFromEntityAssembler.toEntityFromCreateResource(resource);
                    bonoActualizado.setId(id);
                    bonoActualizado.setEmisorUsername(username);

                    Bono resultado = bonoService.actualizarBono(id, bonoActualizado);
                    calculoFinancieroService.procesarCalculosBono(resultado);
                    resultado = bonoService.actualizarBono(id, resultado);

                    return ResponseEntity.ok(BonoResourceFromEntityAssembler.toResourceFromEntity(resultado));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarBono(@PathVariable Long id) {
        String username = obtenerUsernameAutenticado();

        return bonoService.obtenerBonoPorId(id)
                .filter(bono -> bono.getEmisorUsername().equals(username))
                .map(bono -> {
                    bonoService.eliminarBono(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/flujo")
    public ResponseEntity<List<java.util.Map<String, Object>>> obtenerFlujoFinanciero(@PathVariable Long id) {
        String username = obtenerUsernameAutenticado();
        System.out.println("📊 Obteniendo flujo para bono ID: " + id);

        return bonoService.obtenerBonoPorId(id)
                .filter(bono -> bono.getEmisorUsername().equals(username))
                .map(bono -> {
                    // Primero intentar obtener flujos de la base de datos
                    List<FlujoFinanciero> flujos = bonoService.obtenerFlujoFinancieroBono(id);
                    
                    if (flujos == null || flujos.isEmpty()) {
                        System.out.println("  ⚠️ No hay flujos en BD, regenerando...");
                        flujos = bono.getFlujos();
                        
                        if (flujos == null || flujos.isEmpty()) {
                            System.out.println("  🔧 Generando flujos desde cero...");
                            BigDecimal tasaDescuento = bono.getTasaDescuento() != null ? 
                                bono.getTasaDescuento() : BigDecimal.valueOf(0.08);
                            flujos = bono.generarFlujoCaja(tasaDescuento);
                        }
                    } else {
                        System.out.println("  ✅ Flujos obtenidos de BD: " + flujos.size());
                    }
                    
                    // Devolver solo los flujos limpios, sin la información completa del bono
                    var flujosLimpios = flujos.stream()
                        .map(flujo -> {
                            Map<String, Object> flujoMap = new java.util.HashMap<>();
                            flujoMap.put("periodo", flujo.getPeriodo());
                            flujoMap.put("fecha", flujo.getFecha());
                            flujoMap.put("interes", flujo.getInteres());
                            flujoMap.put("amortizacion", flujo.getAmortizacion());
                            flujoMap.put("cuota", flujo.getFlujoTotal());
                            flujoMap.put("saldoInsoluto", flujo.getSaldoInsoluto());
                            flujoMap.put("valorPresente", flujo.getValorPresente());
                            return flujoMap;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    
                    return ResponseEntity.ok(flujosLimpios);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private String obtenerUsernameAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}