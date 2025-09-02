package com.bonoya.platform.bonos.domain.model.entities;

import com.bonoya.platform.bonos.domain.model.valueobjects.ConfiguracionCalculo;
import com.bonoya.platform.bonos.domain.model.valueobjects.DuracionConvexidad;
import com.bonoya.platform.bonos.domain.model.valueobjects.Moneda;
import com.bonoya.platform.bonos.domain.model.valueobjects.PlazoGracia;
import com.bonoya.platform.bonos.domain.model.valueobjects.PrecioMercado;
import com.bonoya.platform.bonos.domain.model.valueobjects.Rendimiento;
import com.bonoya.platform.bonos.domain.model.valueobjects.TasaInteres;
import com.bonoya.platform.shared.domain.model.entities.AuditableModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad principal que representa un Bono Corporativo.
 * Contiene atributos principales del bono para su persistencia en base de datos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "bonos")
public class Bono extends AuditableModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;

    @Column(precision = 19, scale = 4)
    private BigDecimal valorNominal;

    @Column(precision = 19, scale = 6)
    private BigDecimal tasaCupon;

    private int plazoAnios;
    private int frecuenciaPagos;
    private String moneda;
    private LocalDate fechaEmision;
    private int plazosGraciaTotal;
    private int plazosGraciaParcial;

    @Column(precision = 19, scale = 6)
    private BigDecimal tcea;

    @Column(precision = 19, scale = 6)
    private BigDecimal duracion;

    @Column(precision = 19, scale = 6)
    private BigDecimal convexidad;

    @Column(precision = 19, scale = 6)
    private BigDecimal tasaDescuento;

    private String metodoAmortizacion = "ALEMAN";

    private String emisorUsername;

    @Transient
    private List<FlujoFinanciero> flujos = new ArrayList<>();

    @OneToMany(mappedBy = "bono", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Calculo> calculos = new ArrayList<>();

    // Constantes para cálculos
    private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

    /**
     * Métodos adicionales para la lógica de negocio
     */
    @Transient
    private PlazoGracia plazoGracia;

    @Transient
    private TasaInteres tasaInteres;

    @Transient
    private ConfiguracionCalculo configuracion;

    public PlazoGracia getPlazoGracia() {
        if (plazoGracia == null) {
            // Priorizamos el plazo de gracia total sobre el parcial
            if (plazosGraciaTotal > 0) {
                plazoGracia = PlazoGracia.plazoGraciaTotal(plazosGraciaTotal);
            } else if (plazosGraciaParcial > 0) {
                plazoGracia = PlazoGracia.plazoGraciaParcial(plazosGraciaParcial);
            } else {
                plazoGracia = PlazoGracia.sinPlazoGracia();
            }
        }
        return plazoGracia;
    }

    public void setPlazoGracia(PlazoGracia plazoGracia) {
        this.plazoGracia = plazoGracia;
        if (plazoGracia != null) {
            // Limpiamos ambos valores primero
            this.plazosGraciaTotal = 0;
            this.plazosGraciaParcial = 0;

            // Establecemos solo el tipo que corresponda
            if (plazoGracia.esPlazoGraciaTotal()) {
                this.plazosGraciaTotal = plazoGracia.getPeriodos();
            } else if (plazoGracia.esPlazoGraciaParcial()) {
                this.plazosGraciaParcial = plazoGracia.getPeriodos();
            }
        }
    }

    public Moneda getMonedaObj() {
        return new Moneda(this.moneda, this.moneda, this.moneda.substring(0, 1));
    }

    public void setMonedaObj(Moneda moneda) {
        if (moneda != null) {
            this.moneda = moneda.getCodigo();
        }
    }

    public TasaInteres getTasaInteres() {
        if (tasaInteres == null) {
            tasaInteres = new TasaInteres(
                    tasaCupon,
                    TasaInteres.TipoTasa.EFECTIVA,
                    frecuenciaPagos
            );
        }
        return tasaInteres;
    }

    public void setTasaInteres(TasaInteres tasaInteres) {
        this.tasaInteres = tasaInteres;
        if (tasaInteres != null) {
            this.tasaCupon = tasaInteres.getValor();
            this.frecuenciaPagos = tasaInteres.getFrecuenciaCapitalizacion();
        }
    }

    public ConfiguracionCalculo getConfiguracion() {
        if (configuracion == null) {
            configuracion = ConfiguracionCalculo.configuracionEstandar(getMonedaObj());
        }
        return configuracion;
    }

    public void setConfiguracion(ConfiguracionCalculo configuracion) {
        this.configuracion = configuracion;
    }

    public String getEmisorUsername() {
        return emisorUsername;
    }

    public void setEmisor(Object emisor) {
        if (emisor != null) {
            try {
                // Intenta obtener el username mediante reflexión o cast
                this.emisorUsername = emisor.toString();
            } catch (Exception e) {
                this.emisorUsername = "unknown";
            }
        }
    }

    /**
     * Método de utilidad para convertir BigDecimal a double
     */
    public double getValorNominalAsDouble() {
        return valorNominal != null ? valorNominal.doubleValue() : 0.0;
    }

    /**
     * Método de utilidad para convertir BigDecimal a double
     */
    public double getTasaCuponAsDouble() {
        return tasaCupon != null ? tasaCupon.doubleValue() : 0.0;
    }

    /**
     * Método de utilidad para convertir double a BigDecimal
     */
    public void setValorNominalFromDouble(double value) {
        this.valorNominal = BigDecimal.valueOf(value);
    }

    /**
     * Método de utilidad para convertir double a BigDecimal
     */
    public void setTasaCuponFromDouble(double value) {
        this.tasaCupon = BigDecimal.valueOf(value);
    }

    /**
     * Método de utilidad para convertir double a BigDecimal
     */
    public void setTceaFromDouble(double value) {
        this.tcea = BigDecimal.valueOf(value);
    }

    /**
     * Método de utilidad para convertir double a BigDecimal
     */
    public void setDuracionFromDouble(double value) {
        this.duracion = BigDecimal.valueOf(value);
    }

    /**
     * Método de utilidad para convertir double a BigDecimal
     */
    public void setConvexidadFromDouble(double value) {
        this.convexidad = BigDecimal.valueOf(value);
    }

    /**
     * Método de utilidad para convertir double a BigDecimal
     */
    public void setTasaDescuentoFromDouble(double value) {
        this.tasaDescuento = BigDecimal.valueOf(value);
    }

    /**
     * Genera el flujo de caja para el bono usando el método americano.
     * @param tasaDescuento Tasa de descuento para calcular valores actuales
     * @return Lista de flujos de caja
     */
    public List<FlujoFinanciero> generarFlujoCajaMetodoAmericano(BigDecimal tasaDescuento) {
        // Validamos la tasa de descuento
        if (tasaDescuento == null) {
            tasaDescuento = BigDecimal.valueOf(0.08); // Valor por defecto 8%
        }

        // Guardamos la tasa para futuros cálculos
        this.tasaDescuento = tasaDescuento;

        // Calculamos el número total de períodos
        int periodosTotales = this.plazoAnios * this.frecuenciaPagos;

        // Preparamos la tasa por período (convertimos porcentaje a decimal)
        BigDecimal tasaCuponDecimal = this.tasaCupon.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal tasaCuponPorPeriodo = tasaCuponDecimal.divide(BigDecimal.valueOf(frecuenciaPagos), 10, RoundingMode.HALF_UP);

        // Calculamos la tasa de descuento por período
        BigDecimal tasaDescuentoPorPeriodo = tasaDescuento.divide(BigDecimal.valueOf(frecuenciaPagos), 10, RoundingMode.HALF_UP);

        // Inicializamos la lista de flujos
        List<FlujoFinanciero> flujos = new ArrayList<>();

        // Valor nominal como saldo inicial
        BigDecimal saldoInicial = this.valorNominal;

        // Calculamos la fecha de inicio (fecha de emisión)
        LocalDate fechaInicio = this.fechaEmision;

        // Inicializamos el valor presente total
        BigDecimal valorPresenteTotal = BigDecimal.ZERO;

        // Para cada período generamos un flujo
        for (int periodo = 1; periodo <= periodosTotales; periodo++) {
            FlujoFinanciero flujo = new FlujoFinanciero();
            flujo.setPeriodo(periodo);

            // Calculamos la fecha del flujo
            LocalDate fechaFlujo = calcularFechaFlujo(fechaInicio, periodo);
            flujo.setFecha(fechaFlujo);

            // Establecemos el bono asociado al flujo
            flujo.setBono(this);

            // Calculamos el interés para este período
            BigDecimal interes = saldoInicial.multiply(tasaCuponPorPeriodo);

            // Por defecto no hay amortización excepto en el último período
            BigDecimal amortizacion = BigDecimal.ZERO;

            // En el método americano, sólo se amortiza el capital en el último período
            if (periodo == periodosTotales) {
                amortizacion = saldoInicial;
            }

            // Aplicamos reglas de períodos de gracia
            if (periodo <= plazosGraciaTotal) {
                // En período de gracia total, no se paga nada
                flujo.setCupon(BigDecimal.ZERO);
                flujo.setAmortizacion(BigDecimal.ZERO);
                flujo.setInteres(BigDecimal.ZERO);

                // El interés se capitaliza
                saldoInicial = saldoInicial.add(interes);
            } else if (periodo <= plazosGraciaTotal + plazosGraciaParcial) {
                // En período de gracia parcial, sólo se pagan intereses
                flujo.setCupon(interes);
                flujo.setAmortizacion(BigDecimal.ZERO);
                flujo.setInteres(interes);
            } else {
                // Período normal
                flujo.setCupon(interes);
                flujo.setAmortizacion(amortizacion);
                flujo.setInteres(interes);
            }

            // Calculamos el flujo total
            BigDecimal flujoTotal = flujo.getCupon().add(flujo.getAmortizacion());
            flujo.setFlujoTotal(flujoTotal);

            // Actualizamos el saldo insoluto
            BigDecimal nuevoSaldo = saldoInicial.subtract(flujo.getAmortizacion());
            flujo.setSaldoInsoluto(nuevoSaldo);
            saldoInicial = nuevoSaldo;

            // Actualizamos todos los campos derivados para mantener consistencia
            flujo.actualizarCamposDerivados();

            // Calculamos el valor presente de este flujo
            BigDecimal factorDescuento = BigDecimal.ONE.add(tasaDescuentoPorPeriodo).pow(periodo, MC);
            BigDecimal valorPresente = flujoTotal.divide(factorDescuento, 10, RoundingMode.HALF_UP);
            flujo.setValorPresente(valorPresente);

            // Acumulamos el valor presente total
            valorPresenteTotal = valorPresenteTotal.add(valorPresente);

            // Agregamos el flujo a la lista
            flujos.add(flujo);
        }

        // Guardamos los flujos generados
        this.flujos = flujos;

        return flujos;
    }

    /**
     * Genera el flujo de caja para el bono usando el método alemán.
     * En el método alemán:
     * - La amortización es constante en cada período
     * - Los intereses se calculan sobre el saldo pendiente (decrecientes)
     * - La cuota total decrece a lo largo del tiempo
     * 
     * @param tasaDescuento Tasa de descuento para calcular valores actuales
     * @return Lista de flujos de caja
     */
    public List<FlujoFinanciero> generarFlujoCajaMetodoAleman(BigDecimal tasaDescuento) {
        System.out.println("🔧 Iniciando método alemán");
        
        // Validamos la tasa de descuento
        if (tasaDescuento == null) {
            tasaDescuento = BigDecimal.valueOf(0.08); // Valor por defecto 8%
        }

        // Guardamos la tasa para futuros cálculos
        this.tasaDescuento = tasaDescuento;

        // Calculamos el número total de períodos
        int periodosTotales = this.plazoAnios * this.frecuenciaPagos;
        System.out.println("  📋 Períodos totales: " + periodosTotales);

        // Preparamos la tasa de cupón por período (convertimos porcentaje a decimal)
        BigDecimal tasaCuponDecimal = this.tasaCupon.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal tasaCuponPorPeriodo = tasaCuponDecimal.divide(BigDecimal.valueOf(frecuenciaPagos), 10, RoundingMode.HALF_UP);
        System.out.println("  💰 Tasa cupón por período: " + tasaCuponPorPeriodo);

        // Calculamos la tasa de descuento por período
        BigDecimal tasaDescuentoPorPeriodo = tasaDescuento.divide(BigDecimal.valueOf(frecuenciaPagos), 10, RoundingMode.HALF_UP);

        // Inicializamos la lista de flujos
        List<FlujoFinanciero> flujos = new ArrayList<>();

        // Valor nominal como saldo inicial
        BigDecimal saldoInsoluto = this.valorNominal;

        // Calculamos la amortización constante (método alemán)
        BigDecimal amortizacionConstante = this.valorNominal.divide(BigDecimal.valueOf(periodosTotales), 10, RoundingMode.HALF_UP);
        System.out.println("  🏗️ Amortización constante: " + amortizacionConstante);

        // Calculamos la fecha de inicio (fecha de emisión)
        LocalDate fechaInicio = this.fechaEmision;

        // Inicializamos el valor presente total
        BigDecimal valorPresenteTotal = BigDecimal.ZERO;

        // Para cada período generamos un flujo
        for (int periodo = 1; periodo <= periodosTotales; periodo++) {
            FlujoFinanciero flujo = new FlujoFinanciero();
            flujo.setPeriodo(periodo);

            // Calculamos la fecha del flujo
            LocalDate fechaFlujo = calcularFechaFlujo(fechaInicio, periodo);
            flujo.setFecha(fechaFlujo);

            // Establecemos el bono asociado al flujo
            flujo.setBono(this);

            // MÉTODO ALEMÁN: Los intereses se calculan sobre el saldo pendiente
            BigDecimal interes = saldoInsoluto.multiply(tasaCuponPorPeriodo);

            // La amortización es constante en todos los períodos
            BigDecimal amortizacion = amortizacionConstante;

            // Manejo de plazos de gracia
            PlazoGracia plazoGracia = getPlazoGracia();
            if (plazoGracia.getTipo() == PlazoGracia.TipoPlazoGracia.TOTAL && periodo <= plazosGraciaTotal) {
                // En período de gracia total, no se paga nada y se capitaliza
                flujo.setCupon(BigDecimal.ZERO);
                flujo.setAmortizacion(BigDecimal.ZERO);
                flujo.setInteres(BigDecimal.ZERO);
                // Los intereses se capitalizan (se suman al saldo)
                saldoInsoluto = saldoInsoluto.add(interes);
            } else if (plazoGracia.getTipo() == PlazoGracia.TipoPlazoGracia.PARCIAL && 
                      periodo <= plazosGraciaTotal + plazosGraciaParcial) {
                // En período de gracia parcial, solo se pagan intereses
                flujo.setCupon(interes);
                flujo.setAmortizacion(BigDecimal.ZERO);
                flujo.setInteres(interes);
                // El saldo no cambia
            } else {
                // Período normal: pago de intereses y amortización
                flujo.setCupon(interes);
                flujo.setAmortizacion(amortizacion);
                flujo.setInteres(interes);
                // Actualizamos el saldo insoluto
                saldoInsoluto = saldoInsoluto.subtract(amortizacion);
            }

            // Calculamos el flujo total
            BigDecimal flujoTotal = flujo.getCupon().add(flujo.getAmortizacion());
            flujo.setFlujoTotal(flujoTotal);

            // Establecemos el saldo insoluto después del pago
            flujo.setSaldoInsoluto(saldoInsoluto);

            // Actualizamos todos los campos derivados para mantener consistencia
            flujo.actualizarCamposDerivados();

            // Calculamos el valor presente de este flujo
            BigDecimal factorDescuento = BigDecimal.ONE.add(tasaDescuentoPorPeriodo).pow(periodo, MC);
            BigDecimal valorPresente = flujoTotal.divide(factorDescuento, 10, RoundingMode.HALF_UP);
            flujo.setValorPresente(valorPresente);

            // Acumulamos el valor presente total
            valorPresenteTotal = valorPresenteTotal.add(valorPresente);

            // Agregamos el flujo a la lista
            flujos.add(flujo);
        }

        // Guardamos los flujos generados
        this.flujos = flujos;
        
        System.out.println("✅ Método alemán completado - Flujos generados: " + flujos.size());

        return flujos;
    }

    /**
     * Genera el flujo de caja para el bono usando el método de amortización configurado.
     * 
     * @param tasaDescuento Tasa de descuento para calcular valores actuales
     * @return Lista de flujos de caja
     */
    public List<FlujoFinanciero> generarFlujoCaja(BigDecimal tasaDescuento) {
        System.out.println("🎯 Generando flujo de caja - Bono: " + this.nombre + 
                          ", Método: " + this.metodoAmortizacion);
        
        if ("ALEMAN".equalsIgnoreCase(this.metodoAmortizacion)) {
            System.out.println("  ✅ Llamando a método alemán");
            return generarFlujoCajaMetodoAleman(tasaDescuento);
        } else {
            System.out.println("  ✅ Llamando a método americano (default)");
            // Por defecto usar método americano
            return generarFlujoCajaMetodoAmericano(tasaDescuento);
        }
    }

    /**
     * Calcula la fecha de un flujo basado en la fecha de emisión y el período.
     *
     * @param fechaEmision Fecha de emisión del bono
     * @param periodo Número de período
     * @return Fecha del flujo
     */
    private LocalDate calcularFechaFlujo(LocalDate fechaEmision, int periodo) {
        int mesesPorPeriodo = 12 / frecuenciaPagos;
        return fechaEmision.plusMonths((long) periodo * mesesPorPeriodo);
    }

    /**
     * Calcula las métricas de duración y convexidad del bono.
     *
     * @param tasaMercado Tasa de mercado para el descuento
     * @return Objeto con las métricas calculadas
     */
    public DuracionConvexidad calcularMetricas(BigDecimal tasaMercado) {
        // Generamos flujos si no existen
        if (flujos.isEmpty()) {
            generarFlujoCaja(tasaMercado);
        }

        // Preparamos variables para el cálculo
        BigDecimal precioActual = calcularPrecio(tasaMercado);
        BigDecimal tasaPorPeriodo = tasaMercado.divide(BigDecimal.valueOf(frecuenciaPagos), 10, RoundingMode.HALF_UP);

        // Variables para acumular cálculos
        BigDecimal sumaDuracion = BigDecimal.ZERO;
        BigDecimal sumaConvexidad = BigDecimal.ZERO;

        // Para cada flujo calculamos su contribución a la duración y convexidad
        for (FlujoFinanciero flujo : flujos) {
            BigDecimal flujoTotal = flujo.getFlujoTotal();
            int periodo = flujo.getPeriodo();

            // Factor de descuento
            BigDecimal factorDescuento = BigDecimal.ONE.add(tasaPorPeriodo).pow(periodo, MC);

            // Valor presente del flujo
            BigDecimal valorPresente = flujoTotal.divide(factorDescuento, 10, RoundingMode.HALF_UP);

            // Contribución a la duración (ponderada por tiempo)
            BigDecimal contribucionDuracion = valorPresente.multiply(BigDecimal.valueOf(periodo));
            sumaDuracion = sumaDuracion.add(contribucionDuracion);

            // Contribución a la convexidad
            BigDecimal t = BigDecimal.valueOf(periodo);
            BigDecimal tMasUno = t.add(BigDecimal.ONE);
            BigDecimal contribucionConvexidad = valorPresente.multiply(t).multiply(tMasUno);
            sumaConvexidad = sumaConvexidad.add(contribucionConvexidad);
        }

        // Calculamos la duración de Macaulay (en períodos)
        BigDecimal duracionPeriodos = sumaDuracion.divide(precioActual, 10, RoundingMode.HALF_UP);

        // Convertimos a años
        BigDecimal duracionAnios = duracionPeriodos.divide(BigDecimal.valueOf(frecuenciaPagos), 10, RoundingMode.HALF_UP);

        // Calculamos la duración modificada
        BigDecimal duracionModificada = duracionPeriodos.divide(BigDecimal.ONE.add(tasaPorPeriodo), 10, RoundingMode.HALF_UP);

        // Convertimos la duración modificada a años
        BigDecimal duracionModificadaAnios = duracionModificada.divide(BigDecimal.valueOf(frecuenciaPagos), 10, RoundingMode.HALF_UP);

        // Calculamos la convexidad
        BigDecimal convexidad = sumaConvexidad.divide(
                precioActual.multiply(BigDecimal.valueOf(Math.pow(1 + tasaPorPeriodo.doubleValue(), 2))),
                10, RoundingMode.HALF_UP);

        // Normalizamos la convexidad
        BigDecimal convexidadNormalizada = convexidad.divide(
                BigDecimal.valueOf(Math.pow(frecuenciaPagos, 2)),
                10, RoundingMode.HALF_UP);

        // Guardamos la duración y convexidad en el bono
        this.duracion = duracionAnios;
        this.convexidad = convexidadNormalizada;

        return new DuracionConvexidad(duracionAnios, duracionModificadaAnios, convexidadNormalizada, tasaMercado);
    }

    /**
     * Calcula el precio del bono dado una tasa de rendimiento.
     *
     * @param tasaRendimiento Tasa de rendimiento para el descuento
     * @return Precio calculado
     */
    public BigDecimal calcularPrecio(BigDecimal tasaRendimiento) {
        // Generamos flujos si no existen
        if (flujos.isEmpty()) {
            generarFlujoCaja(tasaRendimiento);
        }

        // Sumamos los valores presentes de todos los flujos
        BigDecimal precio = BigDecimal.ZERO;
        for (FlujoFinanciero flujo : flujos) {
            precio = precio.add(flujo.getValorPresente());
        }

        return precio;
    }

    /**
     * Calcula la TCEA (Tasa de Coste Efectivo Anual) desde la perspectiva del emisor.
     *
     * @param costosEmision Costos de emisión del bono
     * @return Objeto con la TCEA calculada
     */
    public Rendimiento calcularTCEA(BigDecimal costosEmision) {
        // GENERAR FLUJOS antes de calcular TIR (FIX PRINCIPAL)
        if (flujos.isEmpty()) {
            generarFlujoCaja(
                tasaDescuento != null ? tasaDescuento : BigDecimal.valueOf(0.08)
            );
        }
        
        // El emisor recibe el valor nominal menos los costos de emisión
        BigDecimal importeRecibido = valorNominal.subtract(costosEmision);

        // Calcula la tasa interna de retorno (TIR) con método numérico
        BigDecimal tir = calcularTIR(importeRecibido);

        // La TCEA es la TIR
        this.tcea = tir;

        return new Rendimiento(tir, importeRecibido);
    }

    /**
     * Calcula la TREA (Tasa de Rendimiento Efectivo Anual) desde la perspectiva del inversor.
     *
     * @param precioCompra Precio de compra del bono
     * @return Objeto con la TREA calculada
     */
    public Rendimiento calcularTREA(BigDecimal precioCompra) {
        // GENERAR FLUJOS antes de calcular TIR (consistencia con TCEA)
        if (flujos.isEmpty()) {
            generarFlujoCaja(
                tasaDescuento != null ? tasaDescuento : BigDecimal.valueOf(0.08)
            );
        }
        
        // Calcula la tasa interna de retorno (TIR) con método numérico
        BigDecimal tir = calcularTIR(precioCompra);

        return new Rendimiento(tir, precioCompra);
    }

    /**
     * Calcula el precio máximo que el mercado estaría dispuesto a pagar por el bono.
     *
     * @param tasaMercado Tasa de rendimiento requerida por el mercado
     * @return Objeto con el precio máximo calculado
     */
    public PrecioMercado calcularPrecioMercado(BigDecimal tasaMercado) {
        // El precio es el valor presente de los flujos futuros
        BigDecimal precio = calcularPrecio(tasaMercado);

        // Calculamos el precio como porcentaje del valor nominal
        BigDecimal precioPorcentaje = precio.multiply(BigDecimal.valueOf(100))
                .divide(valorNominal, 4, RoundingMode.HALF_UP);

        return new PrecioMercado(precio, tasaMercado, valorNominal, precioPorcentaje);
    }

    /**
     * Calcula la Tasa Interna de Retorno (TIR) usando el método de Newton-Raphson.
     *
     * @param importeInicial Importe inicial (precio de compra o valor recibido)
     * @return TIR calculada
     */
    private BigDecimal calcularTIR(BigDecimal importeInicial) {
        // VALIDACIÓN: Verificar que existan flujos
        if (flujos.isEmpty()) {
            throw new IllegalStateException("No hay flujos generados para calcular TIR. Se debe generar el flujo de caja primero.");
        }
        
        // Valores para el método numérico
        BigDecimal estimacionTIR = BigDecimal.valueOf(0.10); // Estimación inicial 10%
        BigDecimal precision = BigDecimal.valueOf(0.0000001); // Precisión deseada
        int maxIteraciones = 100;

        for (int i = 0; i < maxIteraciones; i++) {
            BigDecimal valorActualNeto = calcularVAN(estimacionTIR, importeInicial);

            // Si el VAN es suficientemente cercano a cero, hemos encontrado la TIR
            if (valorActualNeto.abs().compareTo(precision) < 0) {
                break;
            }

            // Calculamos la derivada del VAN respecto a la tasa
            BigDecimal derivadaVAN = calcularDerivadaVAN(estimacionTIR, importeInicial);

            // PROTECCIÓN contra división por cero según mejores prácticas
            if (derivadaVAN.abs().compareTo(BigDecimal.valueOf(0.00000001)) < 0) {
                // La derivada es muy pequeña, usar método de bisección como respaldo
                return calcularTIRPorBiseccion(importeInicial, BigDecimal.ZERO, BigDecimal.ONE);
            }

            // Método de Newton-Raphson: siguiente estimación = actual - f(x)/f'(x)
            BigDecimal siguienteEstimacion = estimacionTIR.subtract(
                    valorActualNeto.divide(derivadaVAN, 10, RoundingMode.HALF_UP)
            );

            // Actualizamos la estimación
            estimacionTIR = siguienteEstimacion;
        }

        return estimacionTIR;
    }

    /**
     * Método de bisección como respaldo cuando Newton-Raphson falla.
     * 
     * @param importeInicial Importe inicial
     * @param tasaMin Tasa mínima
     * @param tasaMax Tasa máxima
     * @return TIR calculada por bisección
     */
    private BigDecimal calcularTIRPorBiseccion(BigDecimal importeInicial, 
                                              BigDecimal tasaMin, 
                                              BigDecimal tasaMax) {
        BigDecimal precision = BigDecimal.valueOf(0.0000001);
        int maxIteraciones = 100;
        
        for (int i = 0; i < maxIteraciones; i++) {
            BigDecimal tasaMedio = tasaMin.add(tasaMax).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
            BigDecimal vanMedio = calcularVAN(tasaMedio, importeInicial);
            
            if (vanMedio.abs().compareTo(precision) < 0) {
                return tasaMedio;
            }
            
            BigDecimal vanMin = calcularVAN(tasaMin, importeInicial);
            
            if (vanMedio.multiply(vanMin).compareTo(BigDecimal.ZERO) < 0) {
                tasaMax = tasaMedio;
            } else {
                tasaMin = tasaMedio;
            }
        }
        
        return tasaMin.add(tasaMax).divide(BigDecimal.valueOf(2), 10, RoundingMode.HALF_UP);
    }

    /**
     * Calcula el Valor Actual Neto (VAN) para una tasa dada.
     *
     * @param tasa Tasa de descuento
     * @param importeInicial Importe inicial (negativo para inversión)
     * @return VAN calculado
     */
    private BigDecimal calcularVAN(BigDecimal tasa, BigDecimal importeInicial) {
        BigDecimal van = importeInicial.negate(); // El importe inicial es una salida (negativo)

        // Tasa por período
        BigDecimal tasaPorPeriodo = tasa.divide(BigDecimal.valueOf(frecuenciaPagos), 10, RoundingMode.HALF_UP);

        for (FlujoFinanciero flujo : flujos) {
            BigDecimal flujoTotal = flujo.getFlujoTotal();
            int periodo = flujo.getPeriodo();

            // Factor de descuento
            BigDecimal factorDescuento = BigDecimal.ONE.add(tasaPorPeriodo).pow(periodo, MC);

            // Valor presente del flujo
            BigDecimal valorPresente = flujoTotal.divide(factorDescuento, 10, RoundingMode.HALF_UP);

            // Acumulamos al VAN
            van = van.add(valorPresente);
        }

        return van;
    }

    /**
     * Calcula la derivada del VAN respecto a la tasa.
     *
     * @param tasa Tasa de descuento
     * @param importeInicial Importe inicial
     * @return Derivada del VAN
     */
    private BigDecimal calcularDerivadaVAN(BigDecimal tasa, BigDecimal importeInicial) {
        BigDecimal derivada = BigDecimal.ZERO;

        // Tasa por período
        BigDecimal tasaPorPeriodo = tasa.divide(BigDecimal.valueOf(frecuenciaPagos), 10, RoundingMode.HALF_UP);

        for (FlujoFinanciero flujo : flujos) {
            BigDecimal flujoTotal = flujo.getFlujoTotal();
            int periodo = flujo.getPeriodo();

            // Factor de descuento
            BigDecimal factorDescuento = BigDecimal.ONE.add(tasaPorPeriodo).pow(periodo + 1, MC);

            // Contribución a la derivada: -t * CF_t / (1+r)^(t+1)
            BigDecimal contribucion = BigDecimal.valueOf(periodo)
                    .multiply(flujoTotal)
                    .divide(factorDescuento, 10, RoundingMode.HALF_UP)
                    .negate();

            // Acumulamos a la derivada
            derivada = derivada.add(contribucion);
        }

        return derivada;
    }

    @Override
    public String toString() {
        return "Bono " + nombre + " [" + id + "] - " +
                moneda + " " + valorNominal + ", " +
                (tasaCupon != null ? tasaCupon.multiply(BigDecimal.valueOf(100)) : "0") + "%, " +
                plazoAnios + " años";
    }
}
