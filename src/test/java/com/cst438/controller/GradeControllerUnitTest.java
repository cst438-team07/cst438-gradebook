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
                // disable RabbitMQ listener during tests
                "spring.rabbitmq.listener.simple.auto-startup=false"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
// reset database before each test to ensure isolation
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

    // helper method to authenticate and retrieve JWT token
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

    // test that instructor can successfully update grades
    @Test
    public void testUpdateGradesSuccess() {

        // login as instructor
        String jwt = login("ted@csumb.edu", "ted2025");

        // retrieve an existing section from the database
        Section section = sectionRepository.findAll().iterator().next();

        // create a new assignment for the section
        Assignment assignment = new Assignment();
        assignment.setTitle("Test Assignment");
        assignment.setSection(section);
        assignment = assignmentRepository.save(assignment);

        // retrieve an existing student user
        User student = userRepository.findById(2).orElseThrow();

        // create enrollment linking student to section
        Enrollment enrollment = new Enrollment();
        enrollment.setSection(section);
        enrollment.setStudent(student);
        enrollment.setGrade(null);
        enrollment = enrollmentRepository.save(enrollment);

        // create initial grade for the assignment
        Grade grade = new Grade();
        grade.setAssignment(assignment);
        grade.setEnrollment(enrollment);
        grade.setScore(80);
        grade = gradeRepository.save(grade);

        // build DTO with updated score
        GradeDTO dto = new GradeDTO(
                grade.getGradeId(),
                student.getName(),
                student.getEmail(),
                assignment.getTitle(),
                section.getCourse().getCourseId(),
                section.getSectionId(),
                95
        );

        // call API to update grade
        client.put()
                .uri("/grades")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(dto))
                .exchange()
                .expectStatus().isOk();

        // verify grade was updated in database
        Grade updated = gradeRepository.findById(grade.getGradeId()).orElse(null);
        assertNotNull(updated);
        assertEquals(95, updated.getScore());
    }

    // test that student is forbidden from updating grades
    @Test
    public void testUpdateGradesForbidden() {

        // login as student
        String jwt = login("sam@csumb.edu", "sam2025");

        // retrieve an existing section
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

        // attempt update as student (should be forbidden)
        client.put()
                .uri("/grades")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(dto))
                .exchange()
                .expectStatus().isForbidden();

        // verify grade was NOT changed
        Grade unchanged = gradeRepository.findById(grade.getGradeId()).orElse(null);
        assertNotNull(unchanged);
        assertEquals(80, unchanged.getScore());
    }
}