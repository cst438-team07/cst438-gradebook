package com.cst438.controller;

import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;
import com.cst438.domain.SectionRepository;
import com.cst438.domain.UserRepository;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.dto.SectionDTO;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

public class EnrollmentControllerTest{
    @Autowired
    private WebTestClient client ;
    @Autowired
    private SectionRepository sectionRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private UserRepository userRepository;
    // default behavior for a Mock bean
    // return 0 or null for a method that returns a value
    // for method that returns void, the mock method records the call but does nothing
    @MockitoBean
    RegistrarServiceProxy RegistrarService;
    Random random = new Random();

    @BeforeEach
    void setupTestEnrollment() {
        // Create enrollment for Sam (id=2) in section 1 (cst489) with grade 'A'
        Enrollment testEnrollment = new Enrollment();
        testEnrollment.setEnrollmentId(1);
        testEnrollment.setSection(sectionRepository.findById(1).orElseThrow());
        testEnrollment.setStudent(userRepository.findById(2).orElseThrow()); // Sam
        testEnrollment.setGrade("A");
        enrollmentRepository.save(testEnrollment);
    }

    @Test
    public void CheckBadMatch() throws Exception{
        // login as Sam and get the security token
        String adminEmail = "sam@csumb.edu";
        String password = "sam2025";
        EntityExchangeResult<LoginDTO> login_dto =  client.get()
                .uri("/login")
                .headers(headers -> headers.setBasicAuth(adminEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        LoginDTO loginDto = login_dto.getResponseBody();
        assertNotNull(loginDto);
        String jwt = loginDto.jwt();
        assertNotNull(jwt);
        // Will confirm a user miss match
        client.get()
                .uri("/sections/{sectionNo}/enrollments", 1)
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                // Will be isForbidden because Ted is the instructor not Sam
                .expectStatus().isForbidden()
                .expectBody().isEmpty();
    }

    @Test
    public void CheckGoodMatch() throws Exception{
        // login as Ted and get the security token
        String adminEmail = "ted@csumb.edu";
        String password = "ted2025";
        EntityExchangeResult<LoginDTO> login_dto =  client.get()
                .uri("/login")
                .headers(headers -> headers.setBasicAuth(adminEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        LoginDTO loginDto = login_dto.getResponseBody();
        assertNotNull(loginDto);
        String jwt = loginDto.jwt();
        assertNotNull(jwt);

        // Will confirm a user match
        EntityExchangeResult<List<EnrollmentDTO>> result = client.get()
                .uri("/sections/{sectionNo}/enrollments", 1)
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class)
                .returnResult();
        List<EnrollmentDTO> enrollments = result.getResponseBody();
        assertNotNull(enrollments);
        // Will confirm Enrollment expected values
        assertEquals(1, enrollments.size(), "Should return 1 enrollment (Sam)");
        assertEquals("A", enrollments.get(0).grade(), "Sam should have grade A");
    }

    @Test
    public void UpdateGrades() throws Exception{
        // login as Ted and get the security token
        String adminEmail = "ted@csumb.edu";
        String password = "ted2025";
        EntityExchangeResult<LoginDTO> login_dto =  client.get()
                .uri("/login")
                .headers(headers -> headers.setBasicAuth(adminEmail, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        LoginDTO loginDto = login_dto.getResponseBody();
        assertNotNull(loginDto);
        String jwt = loginDto.jwt();
        assertNotNull(jwt);

        // Will confirm a user match and get enrollments
        EntityExchangeResult<List<EnrollmentDTO>> result = client.get()
                .uri("/sections/{sectionNo}/enrollments", 1)
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class)
                .returnResult();
        List<EnrollmentDTO> enrollments = result.getResponseBody();
        assertNotNull(enrollments);

        // Will confirm Enrollment expected values
        assertEquals(1, enrollments.size(), "Should return 1 enrollment (Sam)");
        assertEquals("A", enrollments.get(0).grade(), "Sam should have grade A");

        // Update Grade to a "B" but dont call updateEnrollmentGrade yet
        EnrollmentDTO updatedDto = new EnrollmentDTO(
                enrollments.get(0).enrollmentId(),
                // CHANGED grade
                "B",
                enrollments.get(0).studentId(),
                enrollments.get(0).name(),
                enrollments.get(0).email(),
                enrollments.get(0).courseId(),
                enrollments.get(0).title(),
                enrollments.get(0).sectionId(),
                enrollments.get(0).sectionNo(),
                enrollments.get(0).building(),
                enrollments.get(0).room(),
                enrollments.get(0).times(),
                enrollments.get(0).credits(),
                enrollments.get(0).year(),
                enrollments.get(0).semester()
        );
        List<EnrollmentDTO> updateList = List.of(updatedDto);

        // grade is still "A" before updateEnrollmentGrade
        result = client.get()
                .uri("/sections/{sectionNo}/enrollments", 1)
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class)
                .returnResult();
        enrollments = result.getResponseBody();
        assertNotNull(enrollments);
        // Will confirm Enrollment expected values
        assertEquals(1, enrollments.size(), "Should return 1 enrollment (Sam)");
        assertEquals("A", enrollments.get(0).grade(), "Sam should have grade A");

        // Calls updateEnrollmentGrade to update grade
        client.put()
                .uri("/enrollments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateList)
                .exchange()
                .expectStatus().isOk();

        // Will confirm grade change to "B"
        result = client.get()
                .uri("/sections/{sectionNo}/enrollments", 1)
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(EnrollmentDTO.class)
                .returnResult();
        enrollments = result.getResponseBody();
        assertNotNull(enrollments);
        // Will confirm Enrollment expected values
        assertEquals(1, enrollments.size(), "Should return 1 enrollment (Sam)");
        assertEquals("B", enrollments.get(0).grade(), "Sam should have grade B");
    }

    @Service
    public class NotUsed{
        public void sendMessage(String cmd, Object data) {
            return; // do nothing
        }
    }

}

