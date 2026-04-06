package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.GradeDTO;
import com.cst438.dto.LoginDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                // disable RabbitMQ listener during tests so tests run independently
                "spring.rabbitmq.listener.simple.auto-startup=false"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
// reset database before each test for isolation and repeatability
public class GradeControllerUnitTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SectionRepository sectionRepository;

    /**
     * Helper method to authenticate a user and retrieve a JWT token.
     * This simulates login and is required for secured endpoints.
     */
    private String login(String email, String password) {
        EntityExchangeResult<LoginDTO> result = client.get()
                .uri("/login")
                .headers(h -> h.setBasicAuth(email, password))
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class)
                .returnResult();

        return result.getResponseBody().jwt();
    }

    /**
     * Test verifies full workflow for updating grades:
     * 1. GET grades before update
     * 2. PUT update request
     * 3. Validate database update
     * 4. GET grades after update
     */
    @Test
    public void testUpdateGradesSuccess() {

        // login as instructor to obtain valid JWT token
        String jwt = login("ted@csumb.edu", "ted2025");

        // retrieve an existing section from database
        Section section = sectionRepository.findAll().iterator().next();

        // ensure instructor matches logged-in user (required for authorization check)
        section.setInstructorEmail("ted@csumb.edu");
        sectionRepository.save(section);

        // create assignment linked to section
        Assignment assignment = new Assignment();
        assignment.setTitle("Test Assignment");
        assignment.setSection(section);
        assignment = assignmentRepository.save(assignment);

        // retrieve student user
        User student = userRepository.findById(2).orElseThrow();

        // create enrollment linking student to section
        Enrollment enrollment = new Enrollment();
        enrollment.setSection(section);
        enrollment.setStudent(student);
        enrollment.setGrade(null);
        enrollment = enrollmentRepository.save(enrollment);

        // create initial grade with score = 80
        Grade grade = new Grade();
        grade.setAssignment(assignment);
        grade.setEnrollment(enrollment);
        grade.setScore(80);
        grade = gradeRepository.save(grade);

        // -------------------------------
        // GET BEFORE UPDATE
        // -------------------------------
        // Calls GET endpoint to verify initial grade is returned correctly
        client.get()
                .uri("/assignments/{assignmentId}/grades", assignment.getAssignmentId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GradeDTO.class)
                .consumeWith(result -> {
                    List<GradeDTO> grades = result.getResponseBody();
                    assertNotNull(grades);
                    assertFalse(grades.isEmpty());

                    // verify initial score is 80
                    assertEquals(80, grades.get(0).score());
                });

        // build DTO with updated score = 95
        GradeDTO dto = new GradeDTO(
                grade.getGradeId(),
                student.getName(),
                student.getEmail(),
                assignment.getTitle(),
                section.getCourse().getCourseId(),
                section.getSectionId(),
                95
        );

        // -------------------------------
        // PUT UPDATE
        // -------------------------------
        // Calls PUT endpoint to update grade score
        client.put()
                .uri("/grades")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(dto))
                .exchange()
                .expectStatus().isOk();

        // -------------------------------
        // DATABASE VALIDATION
        // -------------------------------
        // Verify that grade was updated in database
        Grade updated = gradeRepository.findById(grade.getGradeId()).orElse(null);
        assertNotNull(updated);
        assertEquals(95, updated.getScore());

        // -------------------------------
        // GET AFTER UPDATE
        // -------------------------------
        // Calls GET endpoint again to verify updated value is returned
        client.get()
                .uri("/assignments/{assignmentId}/grades", assignment.getAssignmentId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GradeDTO.class)
                .consumeWith(result -> {
                    List<GradeDTO> grades = result.getResponseBody();
                    assertNotNull(grades);
                    assertFalse(grades.isEmpty());

                    // verify updated score is 95
                    assertEquals(95, grades.get(0).score());
                });
    }

    /**
     * Test verifies that a student (non-instructor) is forbidden
     * from updating grades.
     */
    @Test
    public void testUpdateGradesForbidden() {

        // login as student
        String jwt = login("sam@csumb.edu", "sam2025");

        // retrieve section
        Section section = sectionRepository.findAll().iterator().next();

        // create assignment
        Assignment assignment = new Assignment();
        assignment.setTitle("Test Assignment");
        assignment.setSection(section);
        assignment = assignmentRepository.save(assignment);

        // retrieve student
        User student = userRepository.findById(2).orElseThrow();

        // create enrollment
        Enrollment enrollment = new Enrollment();
        enrollment.setSection(section);
        enrollment.setStudent(student);
        enrollment.setGrade(null);
        enrollment = enrollmentRepository.save(enrollment);

        // create grade
        Grade grade = new Grade();
        grade.setAssignment(assignment);
        grade.setEnrollment(enrollment);
        grade.setScore(80);
        grade = gradeRepository.save(grade);

        // build DTO attempting to update score
        GradeDTO dto = new GradeDTO(
                grade.getGradeId(),
                student.getName(),
                student.getEmail(),
                assignment.getTitle(),
                section.getCourse().getCourseId(),
                section.getSectionId(),
                95
        );

        // attempt update as student (should fail with 403 Forbidden)
        client.put()
                .uri("/grades")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(dto))
                .exchange()
                .expectStatus().isForbidden();

        // verify grade was NOT changed in database
        Grade unchanged = gradeRepository.findById(grade.getGradeId()).orElse(null);
        assertNotNull(unchanged);
        assertEquals(80, unchanged.getScore());
    }
}