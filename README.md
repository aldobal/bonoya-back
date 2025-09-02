# 🏛️ BonoYa Platform - Sistema de Gestión de Bonos Corporativos

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.java.net/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.6-brightgreen)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-blue)](https://www.docker.com/)
[![JUnit](https://img.shields.io/badge/Tests-JUnit%205-green)](https://junit.org/junit5/)

## 📖 Descripción del Proyecto

**BonoYa Platform** es una plataforma web empresarial diseñada para la **gestión integral de bonos corporativos**. El sistema permite a empresas emisoras crear y gestionar bonos, mientras que los inversores pueden analizarlos mediante sofisticados cálculos financieros.

### 🎯 ¿Qué Problema Soluciona?

- **Falta de transparencia** en el mercado de bonos corporativos
- **Cálculos financieros complejos** que requieren herramientas especializadas
- **Gestión manual** de portafolios de bonos
- **Análisis de riesgo** poco accesible para inversores

### 🚀 ¿Para Qué Sirve?

#### Para Emisores de Bonos:
- ✅ Crear y gestionar bonos corporativos
- ✅ Configurar términos financieros (tasa cupón, plazos, amortización)
- ✅ Generar flujos de caja automáticamente
- ✅ Monitorear el rendimiento de sus emisiones

#### Para Inversores:
- 📊 Analizar bonos disponibles en el mercado
- 💰 Calcular métricas financieras avanzadas (TIR, VAN, TREA, etc.)
- 📈 Evaluar riesgo mediante duración y convexidad
- 🎯 Determinar precios justos y máximos de compra

## 🏗️ Arquitectura y Tecnologías

### Stack Tecnológico

#### Backend
- **Java 17** - Lenguaje de programación principal
- **Spring Boot 3.2.6** - Framework de aplicación
- **Spring Data JPA** - Persistencia de datos
- **Spring Security** - Autenticación y autorización
- **JWT (jjwt 0.12.3)** - Tokens de seguridad
- **PostgreSQL 15** - Base de datos principal
- **H2** - Base de datos en memoria para testing
- **Lombok** - Reducción de código boilerplate
- **Flyway** - Migración de base de datos

#### Documentación y Testing
- **SpringDoc OpenAPI 2.5.0** - Documentación API automática
- **JUnit 5** - Framework de testing
- **Maven** - Gestión de dependencias

#### DevOps
- **Docker & Docker Compose** - Contenedorización
- **Jenkins** - CI/CD Pipeline

### 🎯 Arquitectura Clean Architecture / DDD

El proyecto implementa **Domain-Driven Design (DDD)** con **Clean Architecture**, organizando el código en capas bien definidas:

```
src/main/java/com/bonoya/platform/
├── shared/                    # Código compartido
│   ├── domain/               # Entidades base, Value Objects
│   ├── infrastructure/       # Configuraciones compartidas
│   └── interfaces/          # Middleware, manejo de errores
├── iam/                      # Identity & Access Management
│   ├── application/         # Casos de uso, servicios de aplicación
│   ├── domain/              # Entidades, Value Objects, Servicios de dominio
│   ├── infrastructure/      # Persistencia, seguridad
│   └── interfaces/          # Controllers REST, DTOs
├── profiles/                 # Gestión de perfiles de usuario
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── interfaces/
└── bonos/                    # Dominio principal - Gestión de Bonos
    ├── application/         # Servicios de aplicación
    │   └── services/        # BonoApplicationService, CalculoService
    ├── domain/              # Lógica de negocio
    │   ├── model/
    │   │   ├── entities/    # Bono, Calculo, FlujoFinanciero
    │   │   └── valueobjects/ # Moneda, TasaInteres, PlazoGracia
    │   └── services/        # Servicios de dominio
    ├── infrastructure/      # Persistencia JPA
    │   └── persistence/
    └── interfaces/          # Controllers REST
        └── rest/
```

### 🧩 Principios de Clean Code Aplicados

#### 1. **Separación de Responsabilidades**
- **Controllers**: Solo manejan HTTP requests/responses
- **Application Services**: Orquestan casos de uso
- **Domain Services**: Contienen lógica de negocio pura
- **Repositories**: Abstracción de persistencia

#### 2. **Dependency Inversion**
```java
@Service
public class BonoApplicationService {
    private final IBonoService bonoService; // Depende de abstracción
    
    public BonoApplicationService(IBonoService bonoService) {
        this.bonoService = bonoService;
    }
}
```

#### 3. **Single Responsibility Principle**
Cada clase tiene una responsabilidad específica:
- `Bono` - Entidad de dominio con lógica financiera
- `BonoService` - Operaciones CRUD
- `CalculoFinancieroService` - Cálculos financieros complejos

#### 4. **Value Objects para Inmutabilidad**
```java
public class Moneda {
    private final String codigo;
    private final String nombre;
    // Inmutable, con validaciones en constructor
}
```

#### 5. **Naming Conventions**
- Nombres descriptivos en español (dominio financiero local)
- Métodos que expresan intención: `calcularTIRPorBiseccion()`
- Clases que representan conceptos del negocio

## 🧪 Testing y Calidad

### Estrategia de Testing

#### 1. **Pruebas Unitarias** (JUnit 5)
```java
@DisplayName("Bono Amortization Methods Test")
class BonoAmortizacionTest {
    
    @Test
    @DisplayName("Debe calcular amortización constante en método alemán")
    void testMetodoAleman_AmortizacionConstante() {
        // Given, When, Then pattern
    }
}
```

#### 2. **Pruebas de Integración**
- Validación de flujos completos
- Testing con base de datos H2 en memoria
- Configuración específica en `application-test.properties`

#### 3. **Cobertura de Testing**
✅ **Cálculos Financieros Validados:**
- Método Alemán de amortización
- Método Americano de amortización
- Cálculo de TIR, VAN, TREA
- Duración y Convexidad
- Flujos de caja con plazos de gracia

### Validación Matemática

El proyecto incluye **validación exhaustiva** de cálculos financieros:

```markdown
## ✅ MÉTRICAS FINANCIERAS VALIDADAS
- TREA (Tasa de Rendimiento Efectiva Anual) ✓
- TIR (Tasa Interna de Retorno) ✓  
- VAN (Valor Actual Neto) ✓
- TCEA (Tasa de Costo Efectiva Anual) ✓
- Duración de Macaulay ✓
- Duración Modificada ✓
- Convexidad ✓
- Sensibilidad de Precio ✓
```

## 🔒 Seguridad

### Implementación de Seguridad

#### 1. **Autenticación JWT**
```java
@Configuration
@EnableMethodSecurity
public class WebSecurityConfiguration {
    // Configuración de filtros de seguridad
    // JWT token validation
    // CORS configuration
}
```

#### 2. **Autorización por Roles**
- **ROLE_EMISOR**: Puede crear y gestionar bonos
- **ROLE_INVERSOR**: Puede analizar bonos existentes
- **ROLE_ADMIN**: Acceso administrativo completo

#### 3. **Encriptación de Contraseñas**
- BCrypt hashing para passwords
- Salt automático para mayor seguridad

#### 4. **Validación de Entrada**
- Bean Validation en DTOs
- Sanitización de datos
- Manejo centralizado de excepciones

## 📊 Funcionalidades Financieras

### Cálculos Financieros Avanzados

#### 1. **Métodos de Amortización**
```java
// Método Alemán - Amortización constante
public List<FlujoFinanciero> generarFlujoCajaMetodoAleman(BigDecimal tasaDescuento)

// Método Americano - Pago al vencimiento  
public List<FlujoFinanciero> generarFlujoCajaMetodoAmericano(BigDecimal tasaDescuento)
```

#### 2. **Métricas de Rendimiento**
- **TIR** por método de bisección
- **VAN** con flujos descontados
- **TREA** anualizada
- **Precio Justo** basado en valor presente

#### 3. **Análisis de Riesgo**
- **Duración de Macaulay** - Sensibilidad a tasas
- **Duración Modificada** - Volatilidad del precio
- **Convexidad** - Curvatura precio-rendimiento

#### 4. **Gestión de Plazos de Gracia**
```java
public enum TipoPlazoGracia {
    TOTAL,    // No se paga nada, se capitaliza
    PARCIAL,  // Solo se pagan intereses
    SIN_GRACIA // Pago normal desde el inicio
}
```

## 🌐 API REST

### Endpoints Principales

#### Emisores de Bonos
```http
POST   /api/v1/emisor/bonos           # Crear bono
GET    /api/v1/emisor/bonos           # Mis bonos
GET    /api/v1/emisor/bonos/{id}      # Detalle de bono
PUT    /api/v1/emisor/bonos/{id}      # Actualizar bono
DELETE /api/v1/emisor/bonos/{id}      # Eliminar bono
```

#### Inversores
```http
GET    /api/v1/inversor/bonos/catalogo               # Catálogo de bonos
POST   /api/v1/inversor/bonos/{id}/analizar          # Analizar bono
GET    /api/v1/inversor/bonos/{id}/flujo-caja        # Flujo de caja
GET    /api/v1/inversor/calculos/historial           # Historial de análisis
```

#### Cálculos Financieros
```http
POST   /api/bonos/{bonoId}/calculos/flujo-caja       # Generar flujo de caja
POST   /api/bonos/{bonoId}/calculos/rendimiento      # Calcular rendimiento
POST   /api/bonos/{bonoId}/calculos/duracion         # Duración y convexidad
POST   /api/bonos/{bonoId}/calculos/precio-mercado   # Análisis de precio
```

#### Autenticación
```http
POST   /api/v1/authentication/sign-up    # Registro
POST   /api/v1/authentication/sign-in    # Login
POST   /api/v1/authentication/refresh    # Renovar token
```

## 🐳 Deployment con Docker

### Configuración Docker

#### docker-compose.yaml
```yaml
services:
  db:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: bonoya_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: B@ldeon18
    ports:
      - "5432:5432"

  backend:
    build: .
    depends_on:
      - db
    networks:
      - bonofacil-network
```

### Comandos de Deployment

```bash
# Desarrollo
docker-compose up -d

# Producción
docker-compose -f compose.prod.yaml up -d

# Tests de integración
./test-integration.sh
```

## 🚀 Getting Started

### Prerrequisitos
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 15 (opcional, incluido en Docker)

### Instalación

1. **Clonar el repositorio**
```bash
git clone https://github.com/CodAress/bonoya-backend.git
cd bonoya-backend
```

2. **Configurar base de datos**
```bash
# Iniciar PostgreSQL con Docker
docker-compose up -d db
```

3. **Configurar variables de entorno**
```bash
# application-dev.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/bonoya_db
spring.datasource.username=postgres
spring.datasource.password=B@ldeon18
```

4. **Ejecutar la aplicación**
```bash
# Con Maven
./mvnw spring-boot:run

# Con Docker completo
docker-compose up -d
```

5. **Acceder a la documentación**
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/v3/api-docs`

### Ejecutar Tests

```bash
# Tests unitarios
./mvnw test

# Tests de integración
./mvnw verify

# Tests específicos
./mvnw test -Dtest=BonoAmortizacionTest
```

## 📁 Estructura de Base de Datos

### Entidades Principales

#### bonos
```sql
- id (BIGINT, PK)
- nombre (VARCHAR)
- valor_nominal (DECIMAL)
- tasa_cupon (DECIMAL)
- plazo_anios (INTEGER)
- frecuencia_pagos (INTEGER)
- fecha_emision (DATE)
- metodo_amortizacion (VARCHAR)
- emisor_username (VARCHAR)
- created_at, updated_at (TIMESTAMP)
```

#### calculos
```sql
- id (BIGINT, PK)
- bono_id (BIGINT, FK)
- inversor_username (VARCHAR)
- tasa_esperada (DECIMAL)
- trea (DECIMAL)
- precio_maximo (DECIMAL)
- tir (DECIMAL)
- van (DECIMAL)
- duracion (DECIMAL)
- convexidad (DECIMAL)
- fecha_calculo (DATE)
```

### Migraciones Flyway

```
src/main/resources/db/migration/
├── V1_1__update_default_amortization_method.sql
├── V1_2__add_calculos_enriched_fields.sql
└── V1_3__add_column_length_constraints.sql
```

## 🤝 Buenas Prácticas Implementadas

### 1. **Domain-Driven Design**
- Bounded Contexts bien definidos
- Value Objects inmutables
- Aggregate Roots con lógica de negocio
- Servicios de dominio para lógica compleja

### 2. **SOLID Principles**
- **S**ingle Responsibility: Cada clase una responsabilidad
- **O**pen/Closed: Extensible mediante interfaces
- **L**iskov Substitution: Herencia bien implementada
- **I**nterface Segregation: Interfaces específicas
- **D**ependency Inversion: Depender de abstracciones

### 3. **Clean Architecture**
- Capas independientes
- Dependencias hacia adentro
- Casos de uso como servicios de aplicación
- Separación clara entre framework y dominio

### 4. **Security Best Practices**
- JWT stateless authentication
- Role-based authorization
- Input validation y sanitization
- Encrypted passwords con BCrypt

### 5. **Testing Strategies**
- Test-Driven Development (TDD)
- Pruebas unitarias con coverage alto
- Integration tests con H2
- Naming descriptivo en tests

## 📈 Métricas y Performance

### Optimizaciones Implementadas

1. **Lazy Loading** en relaciones JPA
2. **Connection Pooling** para base de datos
3. **Caching** de cálculos costosos
4. **Pagination** en endpoints de listado
5. **Compression** HTTP habilitada

### Validación de Precisión Matemática

```java
// Precisión garantizada en cálculos financieros
private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

// Tolerancia para comparaciones
BigDecimal precision = BigDecimal.valueOf(0.0000001);
```

## 🎯 Roadmap Futuro

### Funcionalidades Planificadas
- [ ] **Dashboard de inversiones** con gráficos
- [ ] **Notificaciones** de vencimientos
- [ ] **API de mercado** para precios en tiempo real
- [ ] **Análisis de portfolio** completo
- [ ] **Machine Learning** para predicción de precios
- [ ] **Mobile App** para inversores

### Mejoras Técnicas
- [ ] **Kubernetes** deployment
- [ ] **Redis** para caching distribuido
- [ ] **Microservices** architecture
- [ ] **Event Sourcing** para auditoría
- [ ] **GraphQL** API alternative

## 👥 Contribuciones

### Cómo Contribuir

1. Fork el repositorio
2. Crear branch de feature (`git checkout -b feature/nueva-funcionalidad`)
3. Commit cambios (`git commit -am 'Agregar nueva funcionalidad'`)
4. Push al branch (`git push origin feature/nueva-funcionalidad`)
5. Crear Pull Request

### Estándares de Código

- Seguir convenciones de naming en español
- Tests unitarios obligatorios para nueva funcionalidad
- Documentación en código
- Code review requerido antes de merge

## 📄 Licencia

Este proyecto está bajo la Licencia MIT. Ver `LICENSE` para más detalles.

## 🙋‍♂️ Soporte

Para preguntas o soporte:
- **Issues**: GitHub Issues
- **Email**: aldobaldeon20@gmail.com
- **Documentación**: Swagger UI en `/swagger-ui.html`

---

**BonoYa Platform** - Democratizando el acceso al mercado de bonos corporativos 🚀
