package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.GradeDTO;
import com.cst438.dto.LoginDTO;
import com.cst438.service.RegistrarServiceProxy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import java.util.Random;


import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GradeControllerUnitTest {
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
    @Autowired
    private GradeRepository gradeRepository;

    @MockitoBean
    RegistrarServiceProxy registrarService;

    @Test
    public void GetGradeList() throws Exception {
        //login as teacher
        String email = "ted@csumb.edu";
        String password = "ted2025";
        EntityExchangeResult<LoginDTO> login_dto =  webClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);
        AssignmentDTO assignmentDTO= new AssignmentDTO(
                0,
                "New Assignment",
                "2026-02-30",
                "cst489",
                1,
                1

        );
        EntityExchangeResult<AssignmentDTO> AssignmentResponse =  webClient.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(assignmentDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class).returnResult();
        AssignmentDTO actualAssignment = AssignmentResponse.getResponseBody();
        assertTrue(actualAssignment.id()>0,"new key not generated");
        Assignment assignment=assignmentRepository.findByID(actualAssignment.id());
        assertNotNull(assignment);
        //use get command to list grades for teachers
        EntityExchangeResult <List<GradeDTO>> GradeResposne=webClient.get().uri("/assignments/"+assignment.getAssignmentId()+"/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GradeDTO.class).returnResult();
        //assert a response comes back
        assertNotNull(GradeResposne);
        //check to make sure grades returned are for the right assignments
        String title=assignment.getTitle();
        List<GradeDTO> grades = GradeResposne.getResponseBody();
        for(GradeDTO gradeDTO : grades) {
            assertEquals(gradeDTO.assignmentTitle(), title);
        }
        //sign in as student
        String sEmail="sam@csumb.edu";
        String sPassword="sam2025";
        EntityExchangeResult<LoginDTO> studentLogin =  webClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(sEmail, sPassword))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        String studentJwt = studentLogin.getResponseBody().jwt();
        assertNotNull(studentJwt);
        //student cannot view all grades
        EntityExchangeResult <List<GradeDTO>> GradeResposneStudent=webClient.get().uri("/assignments/"+assignment.getAssignmentId()+"/grades")
                .headers(headers -> headers.setBearerAuth(studentJwt))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBodyList(GradeDTO.class).returnResult();



    }

    @Test
    public void updateGrades() throws Exception {
        //login as teacher
        String email = "ted@csumb.edu";
        String password = "ted2025";
        EntityExchangeResult<LoginDTO> login_dto =  webClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(email, password))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();

        String jwt = login_dto.getResponseBody().jwt();
        assertNotNull(jwt);
        AssignmentDTO assignmentDTO= new AssignmentDTO(
                0,
                "HW 1",
                "2026-02-30",
                "cst489",
                1,
                1

        );
        EntityExchangeResult<AssignmentDTO> AssignmentResponse =  webClient.post().uri("/assignments")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(assignmentDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AssignmentDTO.class).returnResult();
        AssignmentDTO actualAssignment = AssignmentResponse.getResponseBody();
        assertTrue(actualAssignment.id()>0,"new key not generated");
        Assignment assignment=assignmentRepository.findByID(actualAssignment.id());
        assertNotNull(assignment);
        //updated grade DTO
        List<GradeDTO> gradeDTO=List.of(new GradeDTO(
                1,
                "sam",
                "sam@csumb.edu",
                "HW 1",
                "cst 489",
                1,
                85));
        //submit DTO and update data base
        EntityExchangeResult<List<GradeDTO>> GradeResponse =  webClient.put().uri("/grades")
                .headers(headers -> headers.setBearerAuth(jwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gradeDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GradeDTO.class).returnResult();
        //assert that a response is given with DTO
        assertNotNull(GradeResponse);
        List<GradeDTO> gradeDTO1=GradeResponse.getResponseBody();
        //check that grades returned match updated DTO
        for(GradeDTO grade : gradeDTO1) {
            assertEquals(grade.score(),gradeRepository.findById(1).getScore());
        }
        //sign in as student
        String sEmail="sam@csumb.edu";
        String sPassword="sam2025";
        EntityExchangeResult<LoginDTO> studentLogin =  webClient.get().uri("/login")
                .headers(headers -> headers.setBasicAuth(sEmail, sPassword))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectBody(LoginDTO.class).returnResult();
        String studentJwt = studentLogin.getResponseBody().jwt();
        assertNotNull(studentJwt);
        //check to make sure students cannot update grades
        EntityExchangeResult<List<GradeDTO>> GradeResponseStudent =  webClient.put().uri("/grades")
                .headers(headers -> headers.setBearerAuth(studentJwt))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(gradeDTO)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().is4xxClientError()
                .expectBodyList(GradeDTO.class).returnResult();
    }
}