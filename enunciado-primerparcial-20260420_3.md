# Examen práctico — Microservicio `notacredito-service`

## Objetivo

Implementar el microservicio **`notacredito-service`** en el ecosistema existente (Spring Boot 3, Java 21), exponiendo un CRUD de **Nota de crédito** y haciendo que las pruebas automáticas oficiales pasen **solo a través del `api-gateway`**, con **JWT** obtenido del `auth-service`.

**Sin Docker**: todo se ejecuta en la máquina local (IDE o `java -jar` / `mvn spring-boot:run`).

---

## Servicios que deben estar en ejecución

Antes de correr los tests de integración, deben levantarse al menos:

| Servicio        | Puerto por defecto | Rol |
|-----------------|-------------------|-----|
| `eureka-server` | 8761              | Registro de servicios |
| `config-server` | 8888              | Configuración centralizada (opcional si `notacredito-service` no la usa; recomendado alinear con el resto del proyecto) |
| `auth-service`  | 8083              | Emisión de JWT (`POST /auth/login`) |
| `api-gateway`   | 8080              | Único punto de entrada HTTP para los tests |
| `notacredito-service` | **8087** (definido abajo) | CRUD de notas de crédito |

**Orden sugerido**: Eureka → Config (si aplica) → Auth → **Nota de crédito** → Gateway (el gateway necesita Eureka para resolver `lb://notacredito-service`).

---

## Registro en Eureka y nombre del servicio

- `spring.application.name` **debe ser** exactamente: `notacredito-service`
- El servicio debe registrarse en Eureka para que el gateway use `lb://notacredito-service`.

---

## Ruta en el API Gateway (obligatorio)

En el repositorio base ya se incluye la ruta hacia `notacredito-service`. Si trabajan en una copia antigua, deben asegurarse de que el gateway enrute:

- **Predicado**: `Path=/api/notas-credito/**`
- **URI**: `lb://notacredito-service`
- **Filtro**: el mismo mecanismo que el resto de rutas protegidas (propagación de `Authorization`), como en `inventory-service` o `graph-service`.

Los tests **no** llaman al puerto 8087 directamente; solo a `http://localhost:8080` (gateway).

---

## Seguridad

- Todas las operaciones del CRUD bajo `/api/notas-credito/**` deben exigir **JWT válido** (mismo `jwt.secret` que `auth-service` y `api-gateway`: recurso OAuth2 JWT, algoritmo **HS384**, igual que `inventory-service`).
- Rutas públicas típicas: `/actuator/**`, `/h2-console/**` (si habilitan consola H2), según configuren.

**Credenciales de prueba** (usuarios precargados en `auth-service`):

- Usuario: `admin` — Contraseña: `admin123`

Los tests obtienen el token con `POST /auth/login` contra el **gateway** (puerto 8080).

---

## Persistencia

- Base **H2 en memoria** (`jdbc:h2:mem:...`).
- El **`id`** es **entero largo (`Long`)**, **generado por la base** (identidad).  
  **No** debe enviarse en el cuerpo del **POST** de creación; si se envía, debe ignorarse o no estar presente.

---

## Contrato HTTP exacto (cumplimiento estricto)

**Base path en el microservicio**: `/api/notas-credito`  
(Todo ello accesible vía gateway como `http://localhost:8080/api/notas-credito/...`.)

### Modelo JSON — Nota de crédito

| Campo     | Tipo   | Reglas |
|-----------|--------|--------|
| `id`      | number | Solo lectura; generado por H2 en INSERT |
| `numero`  | string | Obligatorio; no vacío; máximo 200 caracteres |
| `motivo`  | string | Obligatorio; no vacío; máximo 500 caracteres |

Nombres de propiedad en JSON: **`id`**, **`numero`**, **`motivo`** (minúsculas, como en la tabla).

### Crear — `POST /api/notas-credito`

- **Headers**: `Authorization: Bearer <token>`, `Content-Type: application/json`
- **Body** (ejemplo):

```json
{
  "numero": "NC-2026-0001",
  "motivo": "Devolución parcial — factura A-100"
}
```

- **Respuesta exitosa**: **201 Created**
- **Body**: objeto Nota de crédito completo **incluyendo** `id` generado
- **Errores de validación**: **400 Bad Request** (cuerpo opcional; los tests solo verifican el código)

### Listar — `GET /api/notas-credito`

- **Headers**: `Authorization: Bearer <token>`
- **Respuesta**: **200 OK** con un **array JSON** de notas de crédito (puede ser `[]` si no hay datos)

### Obtener uno — `GET /api/notas-credito/{id}`

- **`{id}`**: entero largo
- **200 OK** + Nota de crédito si existe  
- **404 Not Found** si no existe

### Actualizar — `PUT /api/notas-credito/{id}`

- **Body** (misma forma que crear, sin requerir `id` en el body):

```json
{
  "numero": "NC-2026-0001-R",
  "motivo": "Ajuste por error de facturación"
}
```

- **200 OK** + Nota de crédito actualizada si existe  
- **404** si no existe  
- **400** si datos inválidos

### Eliminar — `DELETE /api/notas-credito/{id}`

- **204 No Content** si se borró correctamente  
- **404** si no existía

---

## Headers de trazabilidad (examen / anti-copia)

Los tests automáticos envían en **cada** petición autenticada al gateway (y deben poder reenviarse al servicio si el gateway propaga headers por defecto; en Spring Cloud Gateway suele propagarse el conjunto de headers de la petición):

| Header              | Origen |
|---------------------|--------|
| `X-Exam-Student-Id` | Variable de entorno **`EXAM_STUDENT_ID`** (legajo o ID que asigne el docente). Si no está definida, el test usa un valor por defecto marcador. |
| `X-Exam-Machine-Id` | **Nombre del host** de la máquina donde se ejecutan los tests (`InetAddress.getLocalHost().getHostName()` en el runner). |

**Qué sí y qué no hace esto**

- **Sí** ayuda a correlacionar ejecuciones (por ejemplo logs o capturas) con un legajo y una máquina.
- **No** impide que dos personas copien el **código** del microservicio: para eso hacen falta otras medidas (entrega individual, preguntas orales, revisión de commits, herramientas anti-plagio, variantes del enunciado, etc.).

Se recomienda que cada alumno configure **`EXAM_STUDENT_ID`** antes de correr los tests (por ejemplo en variables de entorno del IDE o del sistema).

---

## Cómo ejecutar los tests oficiales

Los tests de integración están en el módulo `exam-notacredito-integration-tests` y se ejecutan en la fase **`verify`** (plugin **Failsafe**), no en `test`, porque requieren los microservicios levantados.

Desde la raíz del repositorio:

```bash
mvn -pl exam-notacredito-integration-tests verify
```

(`mvn test` en ese módulo no ejecuta los `*IT.java`.)

Variables opcionales:

| Variable           | Significado | Por defecto |
|--------------------|-------------|-------------|
| `GATEWAY_BASE_URL` | URL base del gateway | `http://localhost:8080` |
| `EXAM_STUDENT_ID`  | Legajo / ID alumno   | `NO-ASIGNADO` |

---

## Entrega

Según indique el docente: repositorio, ZIP o branch con el módulo `notacredito-service` completo y cualquier cambio necesario en configuración compartida (sin romper otros servicios).

---

## Criterio de aprobación sugerido

- Los tests del módulo `exam-notacredito-integration-tests` pasan en la máquina del docente con el gateway y los servicios comunes levantados según esta consigna.
