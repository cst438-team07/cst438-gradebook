package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AssignmentControllerUnitTest {

    @Autowired
    private WebTestClient webClient;
    @MockitoBean
    private RegistrarServiceProxy registrarServiceProxy;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    public void AssignmentCreationDisplay() throws Exception {

        String email = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> login_dto = webClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);

        AssignmentDTO assignmentDTO = new AssignmentDTO(
                0,
                "New Assignment",
                "2026-02-28",
                "cst489",
                1,
                1
        );

        EntityExchangeResult<AssignmentDTO> AssignmentResponse = webClient.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(assignmentDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class).returnResult();

        AssignmentDTO actualAssignment = AssignmentResponse.getResponseBody();
        assertTrue(actualAssignment.id() > 0);

        Assignment assignment = assignmentRepository.findByID(actualAssignment.id());
        assertNotNull(assignment);

        // login as student
        String sEmail = "sam@csumb.edu";
        String sPassword = "sam2025";

        EntityExchangeResult<LoginDTO> studentLogin = webClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(sEmail, sPassword))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String studentJwt = studentLogin.getResponseBody().jwt();
        assertNotNull(studentJwt);

        // student cannot create
        webClient.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(studentJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(assignmentDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError();

        // student get assignments
        EntityExchangeResult<List<AssignmentStudentDTO>> assignmentDTOs = webClient.get()
                .uri("/assignments?year=2025&semester=Fall")
                .headers(headers -> headers.setBearerAuth(studentJwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AssignmentStudentDTO.class)
                .returnResult();

        assertNotNull(assignmentDTOs.getResponseBody());
    }

    @Test
    public void AssignmentUpdateDelete() throws Exception {

        String email = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> login_dto = webClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);

        AssignmentDTO assignmentDTO = new AssignmentDTO(
                0,
                "New Assignment",
                "2026-02-28",
                "cst489",
                1,
                1
        );

        EntityExchangeResult<AssignmentDTO> AssignmentResponse = webClient.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(assignmentDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class).returnResult();

        AssignmentDTO actualAssignment = AssignmentResponse.getResponseBody();
        assertTrue(actualAssignment.id() > 0);

        Assignment assignment = assignmentRepository.findByID(actualAssignment.id());
        assertNotNull(assignment);

        AssignmentDTO updatedAssignmentDTO = new AssignmentDTO(
                actualAssignment.id(),
                "Updated Assignment",
                "2026-02-25",
                "cst489",
                1,
                1
        );

        webClient.put().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedAssignmentDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        Assignment testAssignment = assignmentRepository.findByID(actualAssignment.id());
        assertEquals("Updated Assignment", testAssignment.getTitle());

        // ✅ FIXED DELETE
        webClient.delete().uri("/assignments/" + actualAssignment.id())
                .headers(headers -> headers.setBearerAuth(jwt))
                .exchange()
                .expectStatus().isOk();

        Assignment assignment2 = assignmentRepository.findByID(actualAssignment.id());
        assertNull(assignment2);
    }

    @Test
    public void displaySectionsandAssignments() throws Exception {

        String email = "ted@csumb.edu";
        String password = "ted2025";

        EntityExchangeResult<LoginDTO> login_dto = webClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);

        // ✅ FIXED TERM
        EntityExchangeResult<List<SectionDTO>> Sections = webClient.get()
                .uri("/sections?year=2026&semester=Spring")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SectionDTO.class)
                .returnResult();

        List<SectionDTO> actualSections = Sections.getResponseBody();
        assertTrue(actualSections.size() > 0);

        AssignmentDTO assignmentDTO = new AssignmentDTO(
                0,
                "New Assignment",
                "2026-02-28",
                "cst489",
                1,
                1
        );

        webClient.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(assignmentDTO)
                .exchange()
                .expectStatus().isOk();

        EntityExchangeResult<List<AssignmentDTO>> AssignmentsResponse = webClient.get()
                .uri("/sections/1/assignments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(AssignmentDTO.class)
                .returnResult();

        List<AssignmentDTO> actualAssignments = AssignmentsResponse.getResponseBody();
        assertTrue(actualAssignments.size() > 0);
    }
}