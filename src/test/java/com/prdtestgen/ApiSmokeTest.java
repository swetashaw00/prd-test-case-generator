package com.prdtestgen;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Black-box API tests against the ALREADY RUNNING app on localhost:8080 — not @SpringBootTest.
 * These exercise the real HTTP contract (routing, serialization, multipart handling) exactly as
 * a client would, including the real Gemini call. Start the app first (`mvnw spring-boot:run`),
 * then run: mvnw test -Dtest=ApiSmokeTest
 */
class ApiSmokeTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost:8080";
    }

    @Test
    void analyzePrd_withRealDocument_returnsStructuredTestPlan() {
        File samplePrd = new File("samples/password-reset-prd.md");

        given()
            .multiPart("file", samplePrd, "text/markdown")
        .when()
            .post("/api/prd/analyze")
        .then()
            .statusCode(200)
            .contentType("application/json")
            .body("summary", notNullValue())
            .body("scope", notNullValue())
            .body("testCases", notNullValue())
            .body("testCases.size()", greaterThan(0))
            .body("testCases[0].id", notNullValue())
            .body("testCases[0].priority", notNullValue());
    }

    @Test
    void analyzePrd_withNoFile_returns400() {
        given()
        .when()
            .post("/api/prd/analyze")
        .then()
            .statusCode(400);
    }

    @Test
    void listHistory_returnsJsonArray() {
        given()
        .when()
            .get("/api/history")
        .then()
            .statusCode(200)
            .contentType("application/json")
            .body("size()", greaterThanOrEqualTo(0));
    }

    @Test
    void getHistoryItem_withUnknownId_returns404() {
        given()
        .when()
            .get("/api/history/999999999")
        .then()
            .statusCode(404);
    }
}
