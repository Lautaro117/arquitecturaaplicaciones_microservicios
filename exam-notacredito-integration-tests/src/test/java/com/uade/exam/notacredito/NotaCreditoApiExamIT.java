package com.uade.exam.notacredito;

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
@DisplayName("Examen notacredito-service (vía gateway, JWT)")
class NotaCreditoApiExamIT {

    private static final String HEADER_STUDENT = "X-Exam-Student-Id";
    private static final String HEADER_MACHINE = "X-Exam-Machine-Id";

    private static String gatewayBaseUrl;
    private static String accessToken;
    private static String examStudentId;
    private static String examMachineId;

    private static Long createdId;
    private static String createdNumero;
    private static String createdMotivo;

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
        createdNumero = "NC-EXAM-" + suffix;
        createdMotivo = "Motivo examen " + suffix + " — devolución";
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
    @DisplayName("POST /api/notas-credito → 201 e id generado")
    void createNotaCredito() {
        String body = authSpec()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "numero", createdNumero,
                        "motivo", createdMotivo))
                .when()
                .post("/api/notas-credito")
                .then()
                .statusCode(201)
                .extract()
                .body()
                .asString();

        createdId = JsonPath.from(body).getLong("id");
        assertNotNull(createdId);
        assertTrue(createdId > 0);
        assertEquals(createdNumero, JsonPath.from(body).getString("numero"));
        assertEquals(createdMotivo, JsonPath.from(body).getString("motivo"));
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/notas-credito/{id} → 200")
    void getNotaCreditoById() {
        assertNotNull(createdId);

        String body = authSpec()
                .when()
                .get("/api/notas-credito/" + createdId)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals(createdId, JsonPath.from(body).getLong("id"));
        assertEquals(createdNumero, JsonPath.from(body).getString("numero"));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/notas-credito → 200 y contiene la creada")
    void listNotasCreditoContainsCreated() {
        assertNotNull(createdId);

        String body = authSpec()
                .when()
                .get("/api/notas-credito")
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        int n = JsonPath.from(body).getList("$").size();
        assertTrue(n >= 1, "La lista debe incluir al menos una nota de crédito");

        boolean found = JsonPath.from(body).getList("$").stream().anyMatch(o -> {
            if (!(o instanceof Map)) {
                return false;
            }
            Object id = ((Map<?, ?>) o).get("id");
            return id != null && id.toString().equals(createdId.toString());
        });
        assertTrue(found, "La nota de crédito creada debe aparecer en el listado");
    }

    @Test
    @Order(4)
    @DisplayName("PUT /api/notas-credito/{id} → 200")
    void updateNotaCredito() {
        assertNotNull(createdId);

        String nuevoNumero = createdNumero + "-R";
        String nuevoMotivo = createdMotivo + " (actualizado)";

        String body = authSpec()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "numero", nuevoNumero,
                        "motivo", nuevoMotivo))
                .when()
                .put("/api/notas-credito/" + createdId)
                .then()
                .statusCode(200)
                .extract()
                .body()
                .asString();

        assertEquals(createdId, JsonPath.from(body).getLong("id"));
        assertEquals(nuevoNumero, JsonPath.from(body).getString("numero"));
        assertEquals(nuevoMotivo, JsonPath.from(body).getString("motivo"));

        createdNumero = nuevoNumero;
        createdMotivo = nuevoMotivo;
    }

    @Test
    @Order(5)
    @DisplayName("DELETE /api/notas-credito/{id} → 204")
    void deleteNotaCredito() {
        assertNotNull(createdId);

        authSpec()
                .when()
                .delete("/api/notas-credito/" + createdId)
                .then()
                .statusCode(204);
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/notas-credito/{id} tras borrar → 404")
    void getNotaCreditoAfterDeleteReturns404() {
        assertNotNull(createdId);

        authSpec()
                .when()
                .get("/api/notas-credito/" + createdId)
                .then()
                .statusCode(404);
    }

    @Test
    @Order(7)
    @DisplayName("POST /api/notas-credito con datos inválidos → 400")
    void createInvalidReturns400() {
        authSpec()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "numero", "",
                        "motivo", "x"))
                .when()
                .post("/api/notas-credito")
                .then()
                .statusCode(400);
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/notas-credito/{id} inexistente → 404")
    void getNonExistentReturns404() {
        authSpec()
                .when()
                .get("/api/notas-credito/999999999")
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
                .get("/api/notas-credito")
                .then()
                .statusCode(401);
    }
}
