# Ecosistema de Microservicios - Spring Boot 3.4 / Java 21

Arquitectura de microservicios con Spring Boot 3.4, Java 21 y Maven Multi-módulo.

## Estructura de Módulos

| Módulo | Puerto | Descripción |
|--------|--------|-------------|
| **config-server** | 8888 | Configuración centralizada (filesystem) |
| **eureka-server** | 8761 | Service Discovery |
| **auth-service** | 8083 | Servicio de Identidad (JWT, BCrypt, H2) |
| **api-gateway** | 8080 | Spring Cloud Gateway - OAuth2 Resource Server |
| **inventory-service** | 8082 | Microservicio de inventario protegido (H2) |
| **notification-service** | 8084 | Consumidor de eventos (RabbitMQ o Kafka) |
| **RabbitMQ** | 5672 / 15672 | Message Broker opción 1 (management UI en 15672) |
| **Apache Kafka** | 9092 | Message Broker opción 2 (KRaft, sin Zookeeper) |

## Requisitos Previos

- Java 21
- Maven 3.9+
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (para ejecución con Docker)

## Arranque con Docker (recomendado)

> **Importante:** Docker Desktop debe estar corriendo antes de ejecutar estos comandos.
> Verificá que el ícono en la bandeja del sistema no diga "starting...".

**Con RabbitMQ (por defecto):**
```bash
docker compose --profile rabbitmq up --build
```

**Con Apache Kafka:**
```bash
docker compose --profile kafka up --build
```

Esto levanta todos los servicios en el orden correcto gracias a los `depends_on` y healthchecks configurados en `docker-compose.yml`. El perfil elegido determina qué broker y qué adaptadores de mensajería se usan.

Para detener los servicios, presioná `Ctrl + C` en la terminal donde está corriendo y luego:

```bash
docker compose --profile rabbitmq down
# o
docker compose --profile kafka down
```

> `Ctrl + C` detiene los contenedores. `docker compose down` además los elimina junto con la red creada.

## Arranque manual (sin Docker)

Requiere levantar cada servicio en una terminal separada, en este orden:

1. **Eureka Server** (Service Discovery)
2. **Config Server** (opcional si usas config externa)
3. **Auth Service**
4. **API Gateway**
5. **Inventory Service**
6. **Notification Service**

> **Broker de mensajería:** Necesitás tener corriendo RabbitMQ o Kafka en localhost.
>
> RabbitMQ: `docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management-alpine`
>
> Kafka: `docker run -d --name kafka -p 9092:9092 -e KAFKA_CFG_NODE_ID=1 -e KAFKA_CFG_PROCESS_ROLES=broker,controller -e KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 -e KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093 -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT -e KAFKA_CFG_CONTROLLER_LISTENER_NAMES=CONTROLLER bitnami/kafka:latest`

```bash
# Terminal 1 - Eureka
cd eureka-server && mvn spring-boot:run

# Terminal 2 - Auth Service
cd auth-service && mvn spring-boot:run

# Terminal 3 - API Gateway
cd api-gateway && mvn spring-boot:run

# Terminal 4 - Inventory Service (elegir profile)
cd inventory-service && mvn spring-boot:run -Dspring-boot.run.profiles=rabbitmq
# o
cd inventory-service && mvn spring-boot:run -Dspring-boot.run.profiles=kafka

# Terminal 5 - Notification Service (mismo profile)
cd notification-service && mvn spring-boot:run -Dspring-boot.run.profiles=rabbitmq
# o
cd notification-service && mvn spring-boot:run -Dspring-boot.run.profiles=kafka
```

## Usuarios de Prueba (Auth Service)

| Usuario | Password | Roles |
|---------|----------|-------|
| admin | admin123 | ADMIN, USER |
| user | user123 | USER |

## Comandos cURL

### 1. Obtener Token JWT (Login)

**Linux / macOS / Git Bash:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

**Windows CMD:**
```cmd
curl -X POST http://localhost:8080/auth/login -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}"
```

**Windows PowerShell:**
```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
$response.token
```

Respuesta esperada:
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "type": "Bearer",
  "expiresIn": 3600
}
```

### 2. Probar Endpoint Protegido (con Token)

Guarda el token en una variable y úsalo en el header `Authorization`:

**Linux / macOS / Git Bash:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r '.token')

curl -X GET http://localhost:8080/api/inventory/products \
  -H "Authorization: Bearer $TOKEN"
```

**Windows CMD:**
```cmd
curl -X GET http://localhost:8080/api/inventory/products -H "Authorization: Bearer TOKEN_AQUI"
```
> Reemplazá `TOKEN_AQUI` por el token obtenido en el paso 1.

**Windows PowerShell:**
```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/auth/login" -Method POST -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}'
$token = $response.token
Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/products" -Headers @{ Authorization = "Bearer $token" }
```

### 3. Probar sin Token (debe fallar con 401)

**Linux / macOS / Git Bash:**
```bash
curl -X GET http://localhost:8080/api/inventory/products
```

**Windows CMD:**
```cmd
curl -X GET http://localhost:8080/api/inventory/products
```

### 4. Crear Producto (con Token)

**Linux / macOS / Git Bash:**
```bash
curl -X POST http://localhost:8080/api/inventory/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"Monitor","quantity":15,"price":299.99}'
```

**Windows CMD:**
```cmd
curl -X POST http://localhost:8080/api/inventory/products -H "Authorization: Bearer TOKEN_AQUI" -H "Content-Type: application/json" -d "{\"name\":\"Monitor\",\"quantity\":15,\"price\":299.99}"
```

**Windows PowerShell:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/inventory/products" -Method POST -ContentType "application/json" -Headers @{ Authorization = "Bearer $token" } -Body '{"name":"Monitor","quantity":15,"price":299.99}'
```

## Consola H2

Cada microservicio con persistencia tiene su **propia base de datos H2 en memoria**:

- **Auth Service**: http://localhost:8083/h2-console  
  - JDBC URL: `jdbc:h2:mem:authdb`
  
- **Inventory Service**: http://localhost:8082/h2-console  
  - JDBC URL: `jdbc:h2:mem:inventorydb`

Usuario: `sa` | Password: (vacío)

> **Importante**: Si accedes a la consola del auth-service, no verás las tablas del inventory-service. Son bases de datos separadas.

## Configuración JWT

La clave secreta se define en `application.yml` de cada servicio que la usa:

```yaml
jwt:
  secret: MiClaveSecretaParaJWT_MuyLarga_Y_Segura_123456789
  expiration-ms: 3600000  # 1 hora (solo auth-service)
```

**Debe ser la misma** en auth-service, api-gateway e inventory-service.

## Seguridad

- **BCrypt** para hash de contraseñas (auth-service)
- **JWT** generado con jjwt (HS384)
- **Gateway**: WebFlux Security, Lambda DSL, validación JWT en cada petición
- **GatewayFilter**: Propaga el header `Authorization` a los servicios downstream
- **Inventory Service**: OAuth2 Resource Server, valida JWT recibido del Gateway

---

## Arquitectura Hexagonal (inventory-service)

El `inventory-service` fue refactorizado de una arquitectura clásica en capas a **Arquitectura Hexagonal (Puertos y Adaptadores)**.

### Estructura ANTES (capas tradicionales)

```
inventory-service/src/main/java/com/uade/inventory/
├── InventoryServiceApplication.java
├── config/
│   ├── DataInitializer.java
│   └── SecurityConfig.java
├── controller/
│   └── InventoryController.java        ← Accede directo al Repository
├── entity/
│   └── Product.java                    ← Entidad JPA (dominio acoplado a JPA)
└── repository/
    └── ProductRepository.java          ← Spring Data JPA
```

**Problemas de esta estructura:**
- El controlador accede directamente al repositorio JPA, sin capa de negocio intermedia.
- La entidad de dominio (`Product`) está acoplada a JPA (`@Entity`, `@Table`, `@Id`).
- Si se quisiera cambiar la base de datos (ej: de H2 a MongoDB), habría que modificar el dominio y el controlador.
- No hay separación clara entre lógica de negocio e infraestructura.

### Estructura DESPUÉS (hexagonal)

```
inventory-service/src/main/java/com/uade/inventory/
├── InventoryServiceApplication.java
│
├── domain/                              ← NÚCLEO (sin dependencias de frameworks)
│   ├── model/
│   │   └── Product.java                ← POJO puro, sin anotaciones JPA
│   └── port/
│       ├── in/
│       │   └── ProductUseCase.java      ← Puerto de ENTRADA (interfaz)
│       └── out/
│           └── ProductRepositoryPort.java  ← Puerto de SALIDA (interfaz)
│
├── application/                         ← CASOS DE USO
│   └── service/
│       └── ProductService.java          ← Implementa ProductUseCase
│                                           usando ProductRepositoryPort
│
└── infrastructure/                      ← ADAPTADORES (frameworks, BD, HTTP)
    ├── adapter/
    │   ├── in/web/
    │   │   └── InventoryController.java ← Adaptador REST (usa ProductUseCase)
    │   └── out/persistence/
    │       ├── ProductJpaEntity.java     ← Entidad JPA (solo infraestructura)
    │       ├── ProductJpaRepository.java ← Spring Data JPA
    │       ├── ProductPersistenceAdapter.java ← Implementa ProductRepositoryPort
    │       └── ProductMapper.java        ← Mapea domain ↔ JPA
    └── config/
        ├── SecurityConfig.java
        └── DataInitializer.java
```

### Principios aplicados

| Concepto | Cómo se aplica |
|----------|---------------|
| **Dominio puro** | `Product.java` es un POJO sin `@Entity`. No conoce JPA ni Spring. |
| **Puerto de entrada** | `ProductUseCase` define qué operaciones ofrece el dominio. |
| **Puerto de salida** | `ProductRepositoryPort` define qué necesita el dominio de persistencia. |
| **Adaptador de entrada** | `InventoryController` recibe HTTP y delega al caso de uso. |
| **Adaptador de salida** | `ProductPersistenceAdapter` implementa el puerto usando JPA. |
| **Mapper** | `ProductMapper` traduce entre `Product` (dominio) y `ProductJpaEntity` (JPA). |
| **Inversión de dependencias** | El dominio define interfaces; la infraestructura las implementa. |

### Beneficio clave

Si en el futuro se quisiera reemplazar H2/JPA por MongoDB, solo habría que:
1. Crear un nuevo `ProductMongoAdapter` que implemente `ProductRepositoryPort`.
2. No se toca ni el dominio, ni los casos de uso, ni el controlador.

---

## Comunicación Asincrónica - Event-Driven Architecture

El ecosistema implementa comunicación asincrónica entre microservicios con soporte para **RabbitMQ** y **Apache Kafka**, seleccionables mediante Spring Profiles.

### Selección del broker

| Comando Docker | Broker | Spring Profile |
|---------------|--------|---------------|
| `docker compose --profile rabbitmq up --build` | RabbitMQ | `rabbitmq` |
| `docker compose --profile kafka up --build` | Apache Kafka (KRaft) | `kafka` |

El cambio de broker **no requiere modificar código**. Solo se cambia el perfil.

### Flujo del evento

```
┌──────────────────┐    POST /products    ┌──────────────────────┐
│   API Gateway    │ ──────────────────── │   inventory-service   │
│   (puerto 8080)  │                      │    (puerto 8082)      │
└──────────────────┘                      └──────────┬───────────┘
                                                     │
                                            1. Guarda en H2
                                            2. Publica evento
                                                     │
                                          ┌──────────▼───────────┐
                                          │   Message Broker      │
                                          │                       │
                                          │ ┌─────────────────┐   │
                                          │ │ Profile:rabbitmq │   │
                                          │ │ Exchange+Queue   │   │
                                          │ └─────────────────┘   │
                                          │        O              │
                                          │ ┌─────────────────┐   │
                                          │ │ Profile: kafka   │   │
                                          │ │ Topic:           │   │
                                          │ │ product-created  │   │
                                          │ └─────────────────┘   │
                                          └──────────┬───────────┘
                                                     │
                                            Consume evento
                                                     │
                                          ┌──────────▼───────────┐
                                          │ notification-service  │
                                          │   (puerto 8084)       │
                                          │                       │
                                          │ Loguea la notificación│
                                          │ del nuevo producto    │
                                          └──────────────────────┘
```

### Componentes involucrados

**inventory-service (productor):**

| Archivo | Rol | Profile |
|---------|-----|---------|
| `domain/event/ProductCreatedEvent.java` | Evento de dominio | todos |
| `domain/port/out/EventPublisherPort.java` | Puerto de salida (interfaz) | todos |
| `infrastructure/adapter/out/messaging/RabbitMQPublisherAdapter.java` | Adaptador RabbitMQ | `rabbitmq` |
| `infrastructure/adapter/out/messaging/RabbitMQConfig.java` | Exchange, Queue, Binding | `rabbitmq` |
| `infrastructure/adapter/out/messaging/KafkaPublisherAdapter.java` | Adaptador Kafka | `kafka` |
| `infrastructure/adapter/out/messaging/KafkaConfig.java` | Producer config | `kafka` |

**notification-service (consumidor):**

| Archivo | Rol | Profile |
|---------|-----|---------|
| `event/ProductCreatedEvent.java` | DTO del evento recibido | todos |
| `listener/ProductEventListener.java` | Listener `@RabbitListener` | `rabbitmq` |
| `config/RabbitMQConfig.java` | Exchange, Queue, Binding | `rabbitmq` |
| `listener/KafkaProductEventListener.java` | Listener `@KafkaListener` | `kafka` |
| `config/KafkaConfig.java` | Consumer config | `kafka` |

### Cómo probarlo

Al crear un producto (paso 4 de los comandos cURL), se publica automáticamente un evento al broker activo. En los logs del `notification-service` vas a ver:

**Con RabbitMQ:**
```
=== NOTIFICACIÓN RECIBIDA ===
Nuevo producto creado:
  ID:       4
  Nombre:   Monitor
  ...
=============================
```

**Con Kafka:**
```
=== NOTIFICACIÓN RECIBIDA (Kafka) ===
Nuevo producto creado:
  ID:       4
  Nombre:   Monitor
  ...
=====================================
```

### Consolas de administración

- **RabbitMQ Management**: http://localhost:15672 (guest / guest)
- **Kafka**: No tiene UI incluida. Podés ver los topics con:

```bash
docker exec -it <kafka-container> kafka-topics.sh --bootstrap-server localhost:9092 --list
```

### Integración con la arquitectura hexagonal

El evento se publica desde la capa de **aplicación** (`ProductService`) a través de un **puerto de salida** (`EventPublisherPort`). Los adaptadores concretos viven en **infraestructura** y se activan según el profile:

```
EventPublisherPort (interfaz del dominio)
    ├── RabbitMQPublisherAdapter  → @Profile("rabbitmq")
    └── KafkaPublisherAdapter     → @Profile("kafka")
```

Esto demuestra un beneficio real de la hexagonal: el dominio y los casos de uso no saben ni les importa si el mensaje viaja por RabbitMQ o por Kafka. La decisión es puramente de infraestructura.
