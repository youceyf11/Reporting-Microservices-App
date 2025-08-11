package org.project.excelservice.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.project.excelservice.service.ExcelSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = ExcelController.class)
class ExcelControllerTest {

    @Autowired
    WebTestClient client;

    @MockBean
    ExcelSyncService excelSyncService;

    @Test
    @DisplayName("GET /api/excel/{projectKey} returns 200 when sync succeeds")
    void sync_success() {
        Mockito.when(excelSyncService.sync("PROJ"))
               .thenReturn(Mono.just("Sync completed successfully"));

        client.get()
              .uri("/api/excel/PROJ")
              .accept(MediaType.TEXT_PLAIN)
              .exchange()
              .expectStatus().isOk()
              .expectBody(String.class)
              .isEqualTo("Sync completed successfully");
    }

    @Test
    @DisplayName("GET /api/excel/{projectKey} returns 500 when service emits error")
    void sync_error() {
        Mockito.when(excelSyncService.sync("ERR"))
               .thenReturn(Mono.error(new RuntimeException("boom")));

        client.get()
              .uri("/api/excel/ERR")
              .exchange()
              .expectStatus().is5xxServerError()
              .expectBody(String.class)
              .value(body -> org.assertj.core.api.Assertions.assertThat(body).contains("Failed to sync data for project: ERR"));
    }
}
