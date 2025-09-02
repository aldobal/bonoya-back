package com.bonoya.platform.bonos.domain.services;

import com.bonoya.platform.bonos.domain.model.entities.Bono;
import com.bonoya.platform.bonos.domain.model.entities.Calculo;
import com.bonoya.platform.bonos.domain.model.entities.FlujoFinanciero;

import java.math.BigDecimal;
import java.util.List;

public interface CalculoFinancieroService {

    // Calcula el cronograma de pagos según el método de amortización configurado
    List<FlujoFinanciero> calcularFlujoFinanciero(Bono bono);

    // Calcula la TCEA del bono
    BigDecimal calcularTCEA(Bono bono);

    // Calcula la TREA para un inversor basada en el precio de compra
    BigDecimal calcularTREA(Bono bono, BigDecimal precioCompra);

    // Calcula la duración de Macaulay
    BigDecimal calcularDuracion(Bono bono);

    // Calcula la convexidad
    BigDecimal calcularConvexidad(Bono bono);

    // Calcula el precio máximo que un inversor estaría dispuesto a pagar
    BigDecimal calcularPrecioMaximo(Bono bono, BigDecimal tasaEsperada);

    // Procesa todos los cálculos para un bono
    void procesarCalculosBono(Bono bono);

    // Procesa los cálculos para un inversor
    void procesarCalculosInversor(Calculo calculo);

    BigDecimal calcularTasaEfectivaPeriodica(BigDecimal tasaAnual, int frecuenciaPagos);

    BigDecimal calcularDuracion(List<FlujoFinanciero> flujos, BigDecimal tcea);

    BigDecimal calcularConvexidad(List<FlujoFinanciero> flujos, BigDecimal tcea);

    BigDecimal calcularPrecioMaximo(List<FlujoFinanciero> flujos, BigDecimal trea);

    // Calcula la inversión para un bono con una tasa esperada
    Calculo calcularInversion(Bono bono, BigDecimal tasaEsperada);
    
    // Sobrecarga que acepta tasaEsperada como double
    default Calculo calcularInversion(Bono bono, double tasaEsperada) {
        return calcularInversion(bono, BigDecimal.valueOf(tasaEsperada).divide(BigDecimal.valueOf(100)));
    }
    
    // Convierte una tasa nominal a efectiva
    BigDecimal convertirTasaNominalAEfectiva(BigDecimal tn, int capitalizaciones, int periodoTotal);
    
    // Convierte entre diferentes tipos de tasas
    BigDecimal convertirTasa(BigDecimal tasaOrigen, String tipoOrigen, String tipoDestino, int capitalizaciones);
    
    // Calcula el valor futuro de un capital
    BigDecimal calcularValorFuturo(BigDecimal capital, BigDecimal tasa, int periodos);
    
    // Calcula el valor presente de un capital futuro
    BigDecimal calcularValorPresente(BigDecimal capitalFuturo, BigDecimal tasa, int periodos);
    
    // Calcula el resultado de una ecuación de valor equivalente
    BigDecimal calcularEcuacionEquivalente(List<BigDecimal> montos, List<Integer> periodos, BigDecimal tasa);
    
    // Método para validar el cálculo del precio máximo (depuración)
    String validarCalculoPrecio(Bono bono, BigDecimal trea);
    
    // Identifica el método de amortización real del bono (considerando períodos de gracia)
    String identificarMetodoAmortizacion(Bono bono);

    // Corrige cálculos de bonos americanos existentes
    Bono corregirCalculosBonoAmericano(Long bonoId);
    
    // Métodos enriquecidos que devuelven estructuras Calculo completas
    
    // Calcula TREA enriquecido con todos los datos del historial
    Calculo calcularTREAEnriquecido(Bono bono, BigDecimal precioCompra, String inversorUsername);
    
    // Calcula TCEA enriquecido con todos los datos del historial
    Calculo calcularTCEAEnriquecido(Bono bono, String inversorUsername);
    
    // Calcula duración enriquecida con todos los datos del historial
    Calculo calcularDuracionEnriquecida(Bono bono, String inversorUsername);
    
    // Calcula convexidad enriquecida con todos los datos del historial
    Calculo calcularConvexidadEnriquecida(Bono bono, String inversorUsername);
    
    // Calcula precio máximo enriquecido con todos los datos del historial
    Calculo calcularPrecioMaximoEnriquecido(Bono bono, BigDecimal tasaEsperada, String inversorUsername);
    
    // Calcula análisis completo con múltiples métricas
    Calculo calcularAnalisisCompleto(Bono bono, BigDecimal tasaEsperada, String inversorUsername);
    
    // Calcula análisis completo con precio de compra específico para VAN correcto
    Calculo calcularAnalisisCompleto(Bono bono, BigDecimal tasaEsperada, BigDecimal precioCompra, String inversorUsername);
}