package com.bonoya.platform.bonos.interfaces.rest.controllers;

import com.bonoya.platform.bonos.application.services.BonoService;
import com.bonoya.platform.bonos.application.services.CalculoService;
import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.Calculo;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;
import com.bonoya.platform.bonos.domain.services.CalculoFinancieroService;
import com.bonoya.platform.bonos.domain.services.FlujoInversionistaService;
import com.bonoya.platform.bonos.domain.model.valueobjects.FlujoInversionista;
import com.bonoya.platform.bonos.interfaces.rest.resources.*;
import com.bonoya.platform.bonos.interfaces.rest.transform.BonoResourceFromEntityAssembler;
import com.bonoya.platform.bonos.interfaces.rest.transform.CalculoResourceFromEntityAssembler;
import com.bonoya.platform.bonos.interfaces.rest.transform.FlujoFinancieroResourceFromEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/v1/inversor", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Inversor", description = "Endpoints para análisis de bonos por inversores")

public class InversorBonoController {

    private final BonoService bonoService;
    private final CalculoService calculoService;
    private final CalculoFinancieroService calculoFinancieroService;
    private final FlujoInversionistaService flujoInversionistaService;

    @Autowired
    public InversorBonoController(BonoService bonoService, CalculoService calculoService,
                                  CalculoFinancieroService calculoFinancieroService,
                                  FlujoInversionistaService flujoInversionistaService) {
        this.bonoService = bonoService;
        this.calculoService = calculoService;
        this.calculoFinancieroService = calculoFinancieroService;
        this.flujoInversionistaService = flujoInversionistaService;
    }

    @GetMapping("/bonos/catalogo")
    @Operation(summary = "Obtener catálogo completo de bonos disponibles")
    public ResponseEntity<List<BonoResource>> obtenerCatalogoBonos() {
        List<Bono> bonos = bonoService.obtenerTodosLosBonos();
        List<BonoResource> resources = bonos.stream()
                .map(BonoResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/bonos/catalogo/{id}")
    @Operation(summary = "Obtener detalles de un bono específico")
    public ResponseEntity<BonoResource> obtenerBonoPorId(@PathVariable Long id) {
        return bonoService.obtenerBonoPorId(id)
                .map(BonoResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/bonos/catalogo/moneda/{moneda}")
    @Operation(summary = "Filtrar bonos por tipo de moneda")
    public ResponseEntity<List<BonoResource>> obtenerBonosPorMoneda(
            @Parameter(description = "Código de moneda (ej: USD, PEN)") @PathVariable String moneda) {
        List<Bono> bonos = bonoService.obtenerBonosPorMoneda(moneda);
        List<BonoResource> resources = bonos.stream()
                .map(BonoResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/bonos/catalogo/tasa")
    @Operation(summary = "Filtrar bonos por rango de tasa cupón")
    public ResponseEntity<List<BonoResource>> obtenerBonosPorRangoTasa(
            @Parameter(description = "Tasa mínima (ej: 5.0)") @RequestParam double tasaMinima,
            @Parameter(description = "Tasa máxima (opcional)") @RequestParam(required = false) Double tasaMaxima) {
        
        double maxTasa = tasaMaxima != null ? tasaMaxima : Double.MAX_VALUE;
        List<Bono> bonos = bonoService.obtenerBonosPorRangoTasa(tasaMinima, maxTasa);
        List<BonoResource> resources = bonos.stream()
                .map(BonoResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/bonos/{id}/flujo")
    @Operation(summary = "Obtener el flujo financiero de un bono")
    public ResponseEntity<List<FlujoFinancieroResource>> obtenerFlujoFinanciero(@PathVariable Long id) {
        return bonoService.obtenerBonoPorId(id)
                .map(bono -> {
                    List<FlujoFinanciero> flujos = bonoService.obtenerFlujoFinancieroBono(id);
                    
                    // Si no hay flujos en la base de datos, los generamos automáticamente
                    if (flujos == null || flujos.isEmpty()) {
                        flujos = calculoFinancieroService.calcularFlujoFinanciero(bono);
                    }
                    
                    List<FlujoFinancieroResource> resources = flujos.stream()
                            .map(FlujoFinancieroResourceFromEntityAssembler::toResourceFromEntity)
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(resources);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/calculos")
    @Operation(summary = "Realizar cálculo de inversión (TREA y precio máximo)")
    public ResponseEntity<CalculoResource> calcularInversion(@RequestBody CreateCalculoResource resource) {
        String username = obtenerUsernameAutenticado();
        Bono bono = bonoService.obtenerBonoPorId(resource.getBonoId())
                .orElseThrow(() -> new IllegalArgumentException("Bono no encontrado"));
        
        // Calcular inversión directamente con la tasa en porcentaje
        Calculo calculo = calculoFinancieroService.calcularInversion(bono, resource.getTasaEsperada());
        calculo.setInversorUsername(username);
        
        Calculo calculoGuardado = calculoService.guardarCalculo(calculo);
        return new ResponseEntity<>(CalculoResourceFromEntityAssembler.toResourceFromEntity(calculoGuardado), HttpStatus.CREATED);
    }

    @GetMapping("/calculos")
    @Operation(summary = "Obtener todos mis cálculos de inversión")
    public ResponseEntity<List<CalculoResource>> obtenerMisCalculos() {
        String username = obtenerUsernameAutenticado();
        List<Calculo> calculos = calculoService.obtenerCalculosPorInversor(username);
        List<CalculoResource> resources = calculos.stream()
                .map(CalculoResourceFromEntityAssembler::toResourceFromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(resources);
    }

    @GetMapping("/calculos/{id}")
    @Operation(summary = "Obtener detalle de un cálculo específico")
    public ResponseEntity<CalculoResource> obtenerCalculoPorId(@PathVariable Long id) {
        String username = obtenerUsernameAutenticado();
        return calculoService.obtenerCalculoPorId(id)
                .filter(calculo -> calculo.getInversorUsername().equals(username))
                .map(CalculoResourceFromEntityAssembler::toResourceFromEntity)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/calculos/{id}")
    @Operation(summary = "Eliminar un cálculo")
    public ResponseEntity<Void> eliminarCalculo(@PathVariable Long id) {
        String username = obtenerUsernameAutenticado();
        return calculoService.obtenerCalculoPorId(id)
                .filter(calculo -> calculo.getInversorUsername().equals(username))
                .map(calculo -> {
                    calculoService.eliminarCalculo(id);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== MÉTODOS ENRIQUECIDOS ====================
    
    @PostMapping("/calculos/trea-enriquecido")
    @Operation(summary = "Calcular TREA enriquecida con análisis completo")
    public ResponseEntity<CalculoResource> calcularTREAEnriquecido(@RequestBody TREAEnriquecidoRequest request) {
        String username = obtenerUsernameAutenticado();
        
        Bono bono = bonoService.obtenerBonoPorId(request.getBonoId())
                .orElseThrow(() -> new IllegalArgumentException("Bono no encontrado"));
        
        Calculo resultado = calculoFinancieroService.calcularTREAEnriquecido(bono, request.getPrecioCompra(), username);
        Calculo calculoGuardado = calculoService.guardarCalculo(resultado);
        
        return ResponseEntity.ok(CalculoResourceFromEntityAssembler.toResourceFromEntity(calculoGuardado));
    }
    
    @PostMapping("/calculos/tcea-enriquecido")
    @Operation(summary = "Calcular TCEA enriquecida")
    public ResponseEntity<CalculoResource> calcularTCEAEnriquecido(@RequestBody BonoAnalisisRequest request) {
        String username = obtenerUsernameAutenticado();
        
        Bono bono = bonoService.obtenerBonoPorId(request.getBonoId())
                .orElseThrow(() -> new IllegalArgumentException("Bono no encontrado"));
        
        Calculo resultado = calculoFinancieroService.calcularTCEAEnriquecido(bono, username);
        Calculo calculoGuardado = calculoService.guardarCalculo(resultado);
        
        return ResponseEntity.ok(CalculoResourceFromEntityAssembler.toResourceFromEntity(calculoGuardado));
    }
    
    @PostMapping("/calculos/duracion-enriquecida")
    @Operation(summary = "Calcular duración enriquecida")
    public ResponseEntity<CalculoResource> calcularDuracionEnriquecida(@RequestBody BonoAnalisisRequest request) {
        String username = obtenerUsernameAutenticado();
        
        Bono bono = bonoService.obtenerBonoPorId(request.getBonoId())
                .orElseThrow(() -> new IllegalArgumentException("Bono no encontrado"));
        
        Calculo resultado = calculoFinancieroService.calcularDuracionEnriquecida(bono, username);
        Calculo calculoGuardado = calculoService.guardarCalculo(resultado);
        
        return ResponseEntity.ok(CalculoResourceFromEntityAssembler.toResourceFromEntity(calculoGuardado));
    }
    
    @PostMapping("/calculos/convexidad-enriquecida")
    @Operation(summary = "Calcular convexidad enriquecida")
    public ResponseEntity<CalculoResource> calcularConvexidadEnriquecida(@RequestBody BonoAnalisisRequest request) {
        String username = obtenerUsernameAutenticado();
        
        Bono bono = bonoService.obtenerBonoPorId(request.getBonoId())
                .orElseThrow(() -> new IllegalArgumentException("Bono no encontrado"));
        
        Calculo resultado = calculoFinancieroService.calcularConvexidadEnriquecida(bono, username);
        Calculo calculoGuardado = calculoService.guardarCalculo(resultado);
        
        return ResponseEntity.ok(CalculoResourceFromEntityAssembler.toResourceFromEntity(calculoGuardado));
    }
    
    @PostMapping("/calculos/precio-maximo-enriquecido")
    @Operation(summary = "Calcular precio máximo enriquecido")
    public ResponseEntity<CalculoResource> calcularPrecioMaximoEnriquecido(@RequestBody PrecioMaximoEnriquecidoRequest request) {
        String username = obtenerUsernameAutenticado();
        
        Bono bono = bonoService.obtenerBonoPorId(request.getBonoId())
                .orElseThrow(() -> new IllegalArgumentException("Bono no encontrado"));
        
        Calculo resultado = calculoFinancieroService.calcularPrecioMaximoEnriquecido(bono, request.getTasaEsperada(), username);
        Calculo calculoGuardado = calculoService.guardarCalculo(resultado);
        
        return ResponseEntity.ok(CalculoResourceFromEntityAssembler.toResourceFromEntity(calculoGuardado));
    }
    
    @PostMapping("/calculos/analisis-completo")
    @Operation(summary = "Análisis completo enriquecido")
    public ResponseEntity<CalculoResource> calcularAnalisisCompleto(@RequestBody AnalisisCompletoRequest request) {
        String username = obtenerUsernameAutenticado();
        
        Bono bono = bonoService.obtenerBonoPorId(request.getBonoId())
                .orElseThrow(() -> new IllegalArgumentException("Bono no encontrado"));
        
        // Usar el nuevo método que incluye precio de compra para cálculo correcto del VAN
        Calculo resultado;
        if (request.getPrecioCompra() != null) {
            resultado = calculoFinancieroService.calcularAnalisisCompleto(bono, request.getTasaEsperada(), request.getPrecioCompra(), username);
        } else {
            // Fallback al método anterior si no se proporciona precio de compra
            resultado = calculoFinancieroService.calcularAnalisisCompleto(bono, request.getTasaEsperada(), username);
        }
        
        Calculo calculoGuardado = calculoService.guardarCalculo(resultado);
        
        return ResponseEntity.ok(CalculoResourceFromEntityAssembler.toResourceFromEntity(calculoGuardado));
    }
    
    @PostMapping("/calculos/calculo-enriquecido-independiente")
    @Operation(summary = "Cálculo enriquecido independiente sin bono específico")
    public ResponseEntity<CalculoResource> calcularEnriquecidoIndependiente(@RequestBody CalculoIndependienteRequest request) {
        String username = obtenerUsernameAutenticado();
        
        // Crear un bono temporal para los cálculos
        Bono bonoTemporal = new Bono();
        bonoTemporal.setNombre("Cálculo Independiente");
        bonoTemporal.setDescripcion("Cálculo independiente generado desde calculadora");
        bonoTemporal.setValorNominal(request.getValorNominal());
        bonoTemporal.setTasaCupon(request.getTasaCupon());
        bonoTemporal.setPlazoAnios(request.getPlazoAnios());
        bonoTemporal.setFrecuenciaPagos(request.getFrecuenciaPagos() != null ? request.getFrecuenciaPagos() : 2);
        bonoTemporal.setFechaEmision(java.time.LocalDate.now());
        bonoTemporal.setMetodoAmortizacion("AMERICANO");
        bonoTemporal.setMoneda("PEN");
        
        // Calcular usando el precio de compra para TREA
        Calculo resultado = calculoFinancieroService.calcularTREAEnriquecido(bonoTemporal, request.getPrecioCompra(), username);
        resultado.setBono(null); // No guardar referencia al bono temporal
        
        Calculo calculoGuardado = calculoService.guardarCalculo(resultado);
        
        return ResponseEntity.ok(CalculoResourceFromEntityAssembler.toResourceFromEntity(calculoGuardado));
    }
    
    @PostMapping("/bonos/{bonoId}/flujo-inversionista")
    @Operation(summary = "Obtener flujo de caja específico del inversionista")
    public ResponseEntity<FlujoInversionistaResponse> obtenerFlujoInversionista(
            @PathVariable @Parameter(description = "ID del bono") Long bonoId,
            @RequestBody FlujoInversionistaRequest request) {
        
        System.out.println("🔍 Solicitud de flujo del inversionista - Bono ID: " + bonoId + 
                          ", Precio compra: " + request.getPrecioCompra());
        
        Bono bono = bonoService.obtenerBonoPorId(bonoId).orElse(null);
        if (bono == null) {
            return ResponseEntity.notFound().build();
        }
        
        // Calcular flujo del inversionista
        List<FlujoInversionista> flujos = flujoInversionistaService.calcularFlujoInversionista(bono, request.getPrecioCompra());
        
        // Calcular métricas
        FlujoInversionistaService.MetricasInversionista metricas = 
            flujoInversionistaService.calcularMetricas(flujos, request.getPrecioCompra());
        
        // Crear respuesta
        FlujoInversionistaResponse response = new FlujoInversionistaResponse();
        response.setBonoId(bonoId);
        response.setBonoNombre(bono.getNombre());
        response.setPrecioCompra(request.getPrecioCompra());
        response.setFlujos(flujos.stream()
            .map(this::convertirFlujoAResource)
            .collect(Collectors.toList()));
        
        // Agregar métricas
        response.setGananciaNeta(metricas.getGananciaNeta());
        response.setRendimientoTotal(metricas.getRendimientoTotal());
        response.setPeriodoRecuperacion(metricas.getPeriodoRecuperacion());
        response.setTotalCupones(metricas.getTotalCupones());
        response.setTotalPrincipal(metricas.getTotalPrincipal());
        
        System.out.println("✅ Flujo del inversionista calculado exitosamente - " + flujos.size() + " períodos");
        
        return ResponseEntity.ok(response);
    }
    
    private FlujoInversionistaResource convertirFlujoAResource(FlujoInversionista flujo) {
        FlujoInversionistaResource resource = new FlujoInversionistaResource();
        resource.setPeriodo(flujo.getPeriodo());
        resource.setFecha(flujo.getFecha());
        resource.setCupon(flujo.getCupon());
        resource.setPrincipal(flujo.getPrincipal());
        resource.setFlujoTotal(flujo.getFlujoTotal());
        resource.setFlujoNeto(flujo.getFlujoNeto());
        resource.setSaldo(flujo.getSaldoAcumulado());
        resource.setDescripcion(flujo.getDescripcion());
        resource.setEsInversionInicial(flujo.isEsInversionInicial());
        return resource;
    }

    private String obtenerUsernameAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}