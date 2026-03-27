package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.GradeDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
public class GradeController {

    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;

    public GradeController(
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
    }

    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @GetMapping("/assignments/{assignmentId}/grades")
    public List<GradeDTO> getAssignmentGrades(
            @PathVariable("assignmentId") int assignmentId,
            Principal principal) {

        Assignment assignment = assignmentRepository.findByID(assignmentId);

        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // verify instructor owns the section
        Section section = assignment.getSection();
        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<GradeDTO> result = new ArrayList<>();

        // get all enrollments (students) in this section
        List<Enrollment> enrollments = section.getEnrollments();

        for (Enrollment e : enrollments) {

            String studentEmail = e.getStudentEmail();

            // find existing grade
            Grade grade = gradeRepository.findByStudentEmailAndAssignmentId(
                    studentEmail, assignmentId);

            // if no grade exists, create one with null score
            if (grade == null) {
                grade = new Grade();
                grade.setAssignment(assignment);
                grade.setStudentEmail(studentEmail);
                grade.setScore(null);
                gradeRepository.save(grade);
            }

            result.add(new GradeDTO(
                    grade.getId(),
                    grade.getStudentEmail(),
                    grade.getScore()
            ));
        }

        return result;
    }

    @PutMapping("/grades")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateGrades(
            @Valid @RequestBody List<GradeDTO> dtoList,
            Principal principal) {

        for (GradeDTO dto : dtoList) {

            Grade grade = gradeRepository.findById(dto.id()).orElse(null);

            if (grade == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }

            Assignment assignment = grade.getAssignment();
            Section section = assignment.getSection();

            // verify instructor owns section
            if (!section.getInstructorEmail().equals(principal.getName())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN);
            }

            // update score
            grade.setScore(dto.score());

            gradeRepository.save(grade);
        }
    }
}