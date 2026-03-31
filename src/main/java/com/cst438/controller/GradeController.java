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
import java.util.stream.Collectors;

@RestController
public class GradeController {
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;

    public GradeController (
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository) {
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @GetMapping("/assignments/{assignmentId}/grades")
    public List<GradeDTO> getAssignmentGrades(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        // Check that the Section of the assignment belongs to the
        // logged in instructor
        // return a list of GradeDTOs containing student scores for an assignment
        // if a Grade entity does not exist, then create the Grade entity
        // with a null score and return the gradeId.
        Assignment assignment= assignmentRepository.findByID(assignmentId);
        Section s=assignment.getSection();
        if(!(s.getInstructorEmail().equals(principal.getName()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<GradeDTO> grades=assignment.getGrades().stream().map(grade ->
                new GradeDTO(
                        grade.getGradeId(),
                        grade.getEnrollment().getStudent().getName(),
                        grade.getEnrollment().getStudent().getEmail(),
                        grade.getAssignment().getTitle(),
                        grade.getAssignment().getSection().getCourse().getCourseId(),
                        grade.getAssignment().getSection().getSectionId(),
                        grade.getScore()
                )).collect(Collectors.toCollection(ArrayList::new));
        List<Integer> gradedStudentIds = grades.stream()
                .map(g -> userRepository.findByEmail(g.studentEmail()).getId()).toList();
        for (Enrollment e : assignment.getSection().getEnrollments()) {
            Integer id = e.getStudent().getId();

            if (!gradedStudentIds.contains(id)) {
                Grade grade=new Grade();
                grade.setAssignment(assignment);
                grade.setEnrollment(e);
                gradeRepository.save(grade);
                grades.add(new GradeDTO(
                        grade.getGradeId(),                                     // no gradeId yet
                        e.getStudent().getName(),
                        e.getStudent().getEmail(),
                        assignment.getTitle(),
                        assignment.getSection().getCourse().getCourseId(),
                        assignment.getSection().getSectionId(),
                        null                                   // no score yet
                ));
            }
        }
        return grades;
    }
    @PutMapping("/grades")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void updateGrades(@Valid @RequestBody List<GradeDTO> dtoList, Principal principal) {
        // for each GradeDTO
        // check that the logged in instructor is the owner of the section
        // update the assignment score
        for(GradeDTO dto: dtoList) {
            Grade grade=  gradeRepository.findById(dto.gradeId());
            if (grade != null) {
                if(!(principal.getName().equals(grade.getAssignment().getSection().getInstructorEmail()))){
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN);
                }
                grade.setScore(dto.score());
                gradeRepository.save(grade);
            }

        }

    }
}