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

    // constructor injection for repositories
    public GradeController(
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
    }

    // retrieve all grades for a given assignment
    // only the instructor of the section is allowed to access
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @GetMapping("/assignments/{assignmentId}/grades")
    public List<GradeDTO> getAssignmentGrades(
            @PathVariable("assignmentId") int assignmentId,
            Principal principal) {

        // find assignment or return 404 if not found
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assignment not found"));

        // verify that the logged-in user is the instructor of the section
        if (!assignment.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Not instructor of this section");
        }

        List<GradeDTO> dtos = new ArrayList<>();

        // get all enrollments (students) in the section
        List<Enrollment> enrollments = assignment.getSection().getEnrollments();

        // if no enrollments, return empty list
        if (enrollments == null) {
            return dtos;
        }

        // loop through each student enrollment
        for (Enrollment e : enrollments) {

            // find existing grade for this student and assignment
            Grade grade = gradeRepository
                    .findByStudentEmailAndAssignmentId(
                            e.getStudent().getEmail(),
                            assignmentId
                    );

            // if grade does not exist, create it with null score
            if (grade == null) {
                grade = new Grade();
                grade.setAssignment(assignment);
                grade.setEnrollment(e);
                grade.setScore(null);
                gradeRepository.save(grade);
            }

            // convert grade entity to DTO and add to response list
            dtos.add(new GradeDTO(
                    grade.getGradeId(),
                    e.getStudent().getName(),
                    e.getStudent().getEmail(),
                    assignment.getTitle(),
                    assignment.getSection().getCourse().getCourseId(),
                    assignment.getSection().getSectionId(),
                    grade.getScore()
            ));
        }

        return dtos;
    }

    // update grades for an assignment
    // only the instructor of the section is allowed to update
    @PutMapping("/grades")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateGrades(
            @Valid @RequestBody List<GradeDTO> dtoList,
            Principal principal) {

        // loop through each grade update request
        for (GradeDTO dto : dtoList) {

            // find grade by id or return 404 if not found
            Grade grade = gradeRepository.findById(dto.gradeId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Grade not found"));

            // verify instructor owns the section for this grade
            if (!grade.getAssignment().getSection().getInstructorEmail().equals(principal.getName())) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Not instructor of this section");
            }

            // update score and save
            grade.setScore(dto.score());
            gradeRepository.save(grade);
        }
    }
}