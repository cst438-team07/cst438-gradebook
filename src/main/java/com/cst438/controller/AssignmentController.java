package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.SectionDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.sql.Date;
import java.util.List;

@RestController
public class AssignmentController {

    private final SectionRepository sectionRepository;
    private final AssignmentRepository assignmentRepository;
    private final GradeRepository gradeRepository;
    private final UserRepository userRepository;

    // constructor injection for repositories
    public AssignmentController(
            SectionRepository sectionRepository,
            AssignmentRepository assignmentRepository,
            GradeRepository gradeRepository,
            UserRepository userRepository
    ) {
        this.sectionRepository = sectionRepository;
        this.assignmentRepository = assignmentRepository;
        this.gradeRepository = gradeRepository;
        this.userRepository = userRepository;
    }

    // get sections for the logged-in instructor for a given term
    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        List<Section> sections =
                sectionRepository.findByInstructorEmailAndYearAndSemester(
                        principal.getName(), year, semester);

        return sections.stream().map(section -> {
            User instructor = userRepository.findByEmail(principal.getName());

            return new SectionDTO(
                    section.getSectionNo(),
                    section.getTerm().getYear(),
                    section.getTerm().getSemester(),
                    section.getCourse().getCourseId(),
                    section.getCourse().getTitle(),
                    section.getSectionId(),
                    section.getBuilding(),
                    section.getRoom(),
                    section.getTimes(),
                    instructor.getName(),
                    section.getInstructorEmail()
            );
        }).toList();
    }

    // get all assignments for one instructor section
    @GetMapping("/sections/{secNo}/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo,
            Principal principal) {

        // section must exist and belong to the logged-in instructor
        Section section = sectionRepository.findById(secNo).orElse(null);

        if (section == null || !section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid section no");
        }

        return section.getAssignments().stream().map(assignment ->
                new AssignmentDTO(
                        assignment.getAssignmentId(),
                        assignment.getTitle(),
                        assignment.getDueDate().toString(),
                        assignment.getSection().getCourse().getCourseId(),
                        assignment.getSection().getSectionId(),
                        secNo
                )).toList();
    }

    // create a new assignment for a section
    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {

        // section must exist and belong to the logged-in instructor
        Section section = sectionRepository.findById(dto.secNo()).orElse(null);

        if (section == null || !section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid section no");
        }

        Assignment assignment = new Assignment();
        assignment.setSection(section);
        assignment.setTitle(dto.title());

        try {
            Date dueDate = Date.valueOf(dto.dueDate());

            // due date must fall within the term dates
            if (dueDate.before(section.getTerm().getStartDate()) ||
                    dueDate.after(section.getTerm().getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "due date out of range");
            }

            assignment.setDueDate(dueDate);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "due date invalid format");
        }

        assignmentRepository.save(assignment);

        return new AssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTitle(),
                assignment.getDueDate().toString(),
                assignment.getSection().getCourse().getCourseId(),
                assignment.getSection().getSectionId(),
                assignment.getSection().getSectionNo()
        );
    }

    // update an existing assignment
    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {

        // assignment must exist and belong to the logged-in instructor
        Assignment assignment = assignmentRepository.findById(dto.id()).orElse(null);

        if (assignment == null ||
                !assignment.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid assignment id");
        }

        assignment.setTitle(dto.title());

        try {
            Date dueDate = Date.valueOf(dto.dueDate());

            // due date must fall within the term dates
            if (dueDate.before(assignment.getSection().getTerm().getStartDate()) ||
                    dueDate.after(assignment.getSection().getTerm().getEndDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "due date out of range");
            }

            assignment.setDueDate(dueDate);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "due date invalid format");
        }

        assignmentRepository.save(assignment);

        return new AssignmentDTO(
                assignment.getAssignmentId(),
                assignment.getTitle(),
                assignment.getDueDate().toString(),
                assignment.getSection().getCourse().getCourseId(),
                assignment.getSection().getSectionId(),
                assignment.getSection().getSectionNo()
        );
    }

    // delete an assignment
    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(
            @PathVariable("assignmentId") int assignmentId,
            Principal principal) {

        // assignment must exist and belong to the logged-in instructor
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);

        if (assignment == null ||
                !assignment.getSection().getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid assignment id");
        }

        assignmentRepository.delete(assignment);
    }

    // get assignments and grades for the logged-in student
    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        List<Assignment> assignments =
                assignmentRepository.findByStudentEmailAndYearAndSemester(
                        principal.getName(), year, semester);

        return assignments.stream().map(assignment -> {
            Grade grade = gradeRepository.findByStudentEmailAndAssignmentId(
                    principal.getName(),
                    assignment.getAssignmentId()
            );

            return new AssignmentStudentDTO(
                    assignment.getAssignmentId(),
                    assignment.getTitle(),
                    assignment.getDueDate(),
                    assignment.getSection().getCourse().getCourseId(),
                    assignment.getSection().getSectionId(),
                    (grade == null) ? null : grade.getScore()
            );
        }).toList();
    }
}