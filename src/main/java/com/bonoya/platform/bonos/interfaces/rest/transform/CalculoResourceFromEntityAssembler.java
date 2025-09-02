package com.bonoya.platform.bonos.interfaces.rest.transform;

import com.bonoya.platform.bonos.domain.model.entities.Calculo;
import com.bonoya.platform.bonos.interfaces.rest.resources.CalculoResource;

public class CalculoResourceFromEntityAssembler {

    public static CalculoResource toResourceFromEntity(Calculo entity) {
        CalculoResource resource = new CalculoResource();
        
        // Campos básicos
        resource.setId(entity.getId());
        resource.setBonoId(entity.getBonoId());
        resource.setBonoNombre(entity.getBonoNombre());
        resource.setInversorUsername(entity.getInversorUsername());
        resource.setTasaEsperada(entity.getTasaEsperada());
        resource.setTrea(entity.getTrea());
        resource.setPrecioMaximo(entity.getPrecioMaximo());
        resource.setFechaCalculo(entity.getFechaCalculo());
        resource.setInformacionAdicional(entity.getInformacionAdicional());
        
        // Campos adicionales
        resource.setTipoAnalisis(entity.getTipoAnalisis() != null ? entity.getTipoAnalisis() : "TREA");
        resource.setFecha(entity.getFechaCalculo());
        resource.setBono(entity.getBonoNombre());
        
        // Parámetros del análisis
        CalculoResource.ParametrosAnalisis parametros = new CalculoResource.ParametrosAnalisis();
        parametros.setTasaEsperada(entity.getTasaEsperada());
        parametros.setValorNominal(entity.getValorNominal());
        parametros.setTasa(entity.getTasaCupon());
        parametros.setPlazo(entity.getPlazoAnios());
        parametros.setFrecuenciaPago(entity.getFrecuenciaPagos());
        parametros.setMoneda(entity.getMoneda());
        resource.setParametros(parametros);
        
        // Resultados del análisis
        CalculoResource.ResultadosAnalisis resultados = new CalculoResource.ResultadosAnalisis();
        resultados.setTrea(entity.getTrea());
        resultados.setPrecioMaximo(entity.getPrecioMaximo());
        resultados.setTasaEsperada(entity.getTasaEsperada());
        resultados.setTreaPorcentaje(entity.getTreaPorcentaje() != null ? entity.getTreaPorcentaje() : entity.getTrea());
        resultados.setValorPresente(entity.getValorPresente() != null ? entity.getValorPresente() : entity.getPrecioMaximo());
        
        // Cálculos financieros avanzados
        resultados.setTir(entity.getTir());
        resultados.setVan(entity.getVan());
        resultados.setTcea(entity.getTcea());
        resultados.setDuracion(entity.getDuracion());
        resultados.setDuracionModificada(entity.getDuracionModificada());
        resultados.setConvexidad(entity.getConvexidad());
        resultados.setPrecioJusto(entity.getPrecioJusto());
        resultados.setValorPresenteCupones(entity.getValorPresenteCupones());
        resultados.setYield(entity.getYield());
        resultados.setSensibilidadPrecio(entity.getSensibilidadPrecio());
        resultados.setGananciaCapital(entity.getGananciaCapital());
        resultados.setIngresosCupones(entity.getIngresosCupones());
        resultados.setRendimientoTotal(entity.getRendimientoTotal());
        
        resource.setResultados(resultados);
        
        // Información del backend
        CalculoResource.CalculoBackend calculoBackend = new CalculoResource.CalculoBackend();
        calculoBackend.setTreaPorcentaje(entity.getTreaPorcentaje() != null ? entity.getTreaPorcentaje() : entity.getTrea());
        calculoBackend.setValorPresente(entity.getValorPresente() != null ? entity.getValorPresente() : entity.getPrecioMaximo());
        calculoBackend.setFechaCalculo(entity.getFechaCalculo());
        resource.setCalculoBackend(calculoBackend);
        
        return resource;
    }
}