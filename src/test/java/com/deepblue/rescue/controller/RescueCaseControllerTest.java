package com.deepblue.rescue.controller;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.GlobalExceptionHandler;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.service.RescueCaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RescueCaseController.class)
@Import(GlobalExceptionHandler.class)
class RescueCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RescueCaseService service;

    @Test
    void shouldReturnRescueCaseByCode() throws Exception {
        when(service.findByCode("RES-001")).thenReturn(response("RES-001"));

        mockMvc.perform(get("/api/rescue-cases/{caseCode}", "RES-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RES-001"));

        verify(service).findByCode("RES-001");
    }

    @Test
    void shouldReturn404WhenRescueCaseDoesNotExist() throws Exception {
        when(service.findByCode("RES-999"))
                .thenThrow(new ResourceNotFoundException("Rescue case not found: RES-999"));

        mockMvc.perform(get("/api/rescue-cases/{caseCode}", "RES-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Rescue case not found: RES-999"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void shouldReturnCasesByStatus() throws Exception {
        when(service.findByStatus(RescueStatus.IN_REHABILITATION))
                .thenReturn(List.of(response("RES-001"), response("RES-002")));

        mockMvc.perform(get("/api/rescue-cases").param("status", "IN_REHABILITATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(service).findByStatus(RescueStatus.IN_REHABILITATION);
    }

    @Test
    void shouldReturn400ForInvalidStatusParameter() throws Exception {
        mockMvc.perform(get("/api/rescue-cases").param("status", "FLYING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details.status").exists());

        verify(service, never()).findByStatus(any());
    }

    @Test
    void shouldChangeRescueCaseStatus() throws Exception {
        when(service.changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class)))
                .thenReturn(response("RES-001"));

        mockMvc.perform(patch("/api/rescue-cases/{caseCode}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY_FOR_RELEASE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseCode").value("RES-001"));

        verify(service).changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class));
    }

    @Test
    void shouldReturn400AndNotCallServiceForInvalidStatusRequest() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/{caseCode}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.details.status").value("Status is required"));

        verify(service, never()).changeStatus(any(), any());
    }

    @Test
    void shouldReturn409ForInvalidStatusTransition() throws Exception {
        when(service.changeStatus(eq("RES-001"), any(ChangeRescueStatusRequest.class)))
                .thenThrow(new BusinessRuleException("Invalid status transition"));

        mockMvc.perform(patch("/api/rescue-cases/{caseCode}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY_FOR_RELEASE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Invalid status transition"));
    }

    @Test
    void shouldReturn400ForInvalidJsonEnum() throws Exception {
        mockMvc.perform(patch("/api/rescue-cases/{caseCode}/status", "RES-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FLYING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or invalid JSON request"));

        verify(service, never()).changeStatus(any(), any());
    }

    @Test
    void shouldReturn500ForUnexpectedError() throws Exception {
        when(service.findByCode("RES-001")).thenThrow(new IllegalStateException());

        mockMvc.perform(get("/api/rescue-cases/{caseCode}", "RES-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }

    private RescueCaseResponse response(String code) {
        return new RescueCaseResponse(1L, code, LocalDate.of(2026, 8, 20), "Beach",
                RescueStatus.IN_REHABILITATION, "DB-CAR", "AN-001");
    }
}
