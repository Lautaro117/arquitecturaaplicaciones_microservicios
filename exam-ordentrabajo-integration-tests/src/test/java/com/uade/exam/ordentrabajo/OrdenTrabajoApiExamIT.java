package com.uade.exam.ordentrabajo;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests oficiales del examen: solo API Gateway + JWT obtenido por login.
 * Headers de examen: X-Exam-Student-Id (EXAM_STUDENT_ID), X-Exam-Machine-Id (hostname).
 */
@ExtendWith(ExamReportExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Examen ordentrabajo-service (vía gateway, JWT)")
class OrdenTrabajoApiExamIT {

    private static final String HEADER_STUDENT = "X-Exam-Student-Id";
    private static final String HEADER_MACHINE = "X-Exam-Machine-Id";

    private static String gatewayBaseUrl;
    private static String accessToken;
    private static String examStudentId;
    private static String examMachineId;

    private static Long createdId;
    private static String createdTitulo;
    private static String createdEstado;

    @BeforeAll
    static void beforeAll() throws Exception {
        gatewayBaseUrl = System.getenv().getOrDefault("GATEWAY_BASE_URL", "http://localhost:8080");
        examStudentId = System.getenv().getOrDefault("EXAM_STUDENT_ID", "NO-ASIGNADO");
        examMachineId = InetAddress.getLocalHost().getHostName();

        RestAssured.baseURI = gatewayBaseUrl;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        accessToken = fetchAccessToken();
        assertNotNull(accessToken, "No se obtuvo token JWT. ¿auth-service y api-gateway en " + gatewayBaseUrl + "?");

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        createdTitulo = "Examen OT " + suffix;
        createdEstado = "PEND-" + suffix.substring(0, Math.min(5, suffix.length()));
    }

    private static String fetchAccessToken() {
        String body = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "admin", "password", "admin123"))
                .when()
                .post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        return JsonPath.from(body).getString("token");
    }

    private static io.restassured.specification.RequestSpecification authSpec() {
        return given()
                .header("Authorization", "Bearer " + accessToken)
                .header(HEADER_STUDENT, examStudentId)
                .header(HEADER_MACHINE, examMachineId);
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/ordenes-trabajo → 201 e id generado")
    void createOrdenTrabajo() {
        String body = authSpec()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "titulo", createdTitulo,
                        "estado", createdEstado))
                .when()
                .post("/api/ordenes-trabajo")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        createdId = JsonPath.from(body).getLong("id");
        assertNotNull(createdId);
        assertTrue(createdId > 0);
        assertEquals(createdTitulo, JsonPath.from(body).getString("titulo"));
        assertEquals(createdEstado, JsonPath.from(body).getString("estado"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/ordenes-trabajo/{id} → 200")
    void getOrdenTrabajoById() {
        assertNotNull(createdId);

        String body = authSpec()
                .when()
                .get("/api/ordenes-trabajo/" + createdId)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals(createdId, JsonPath.from(body).getLong("id"));
        assertEquals(createdTitulo, JsonPath.from(body).getString("titulo"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/ordenes-trabajo → 200 y contiene el creado")
    void listOrdenesTrabajoContainsCreated() {
        assertNotNull(createdId);

        String body = authSpec()
                .when()
                .get("/api/ordenes-trabajo")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        int n = JsonPath.from(body).getList("$").size();
        assertTrue(n >= 1, "La lista debe incluir al menos una orden de trabajo");

        boolean found = JsonPath.from(body).getList("$").stream().anyMatch(o -> {
            if (!(o instanceof Map)) {
                return false;
            }
            Object id = ((Map<?, ?>) o).get("id");
            return id != null && id.toString().equals(createdId.toString());
        });
        assertTrue(found, "La orden creada debe aparecer en el listado");
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/ordenes-trabajo/{id} → 200")
    void updateOrdenTrabajo() {
        assertNotNull(createdId);

        String nuevoTitulo = createdTitulo + " (editado)";
        String nuevoEstado = "EN_CURSO";

        String body = authSpec()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "titulo", nuevoTitulo,
                        "estado", nuevoEstado))
                .when()
                .put("/api/ordenes-trabajo/" + createdId)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals(createdId, JsonPath.from(body).getLong("id"));
        assertEquals(nuevoTitulo, JsonPath.from(body).getString("titulo"));
        assertEquals(nuevoEstado, JsonPath.from(body).getString("estado"));

        createdTitulo = nuevoTitulo;
        createdEstado = nuevoEstado;
    }

    @Test
    @Order(5)
    @DisplayName("DELETE /api/ordenes-trabajo/{id} → 204")
    void deleteOrdenTrabajo() {
        assertNotNull(createdId);

        authSpec()
                .when()
                .delete("/api/ordenes-trabajo/" + createdId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/ordenes-trabajo/{id} tras borrar → 404")
    void getOrdenTrabajoAfterDeleteReturns404() {
        assertNotNull(createdId);

        authSpec()
                .when()
                .get("/api/ordenes-trabajo/" + createdId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/ordenes-trabajo con datos inválidos → 400")
    void createInvalidReturns400() {
        authSpec()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "titulo", "",
                        "estado", "x"))
                .when()
                .post("/api/ordenes-trabajo")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/ordenes-trabajo/{id} inexistente → 404")
    void getNonExistentReturns404() {
        authSpec()
                .when()
                .get("/api/ordenes-trabajo/999999999")
                .then()
                .statusCode(404);
    }

    @Test
    @Order(9)
    @DisplayName("Sin JWT → 401 en recurso protegido")
    void withoutTokenReturns401() {
        given()
                .header(HEADER_STUDENT, examStudentId)
                .header(HEADER_MACHINE, examMachineId)
                .when()
                .get("/api/ordenes-trabajo")
                .then()
                .statusCode(401);
    }
}
