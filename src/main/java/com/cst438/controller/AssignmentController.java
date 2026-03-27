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

    // return sections for instructor for a given term
    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        List<Section> sections =
                sectionRepository.findByInstructorEmailAndYearAndSemester(
                        principal.getName(), year, semester);

        return sections.stream().map(section ->
                new SectionDTO(
                        section.getSectionNo(),
                        section.getTerm().getYear(),
                        section.getTerm().getSemester(),
                        section.getCourse().getCourseId(),
                        section.getCourse().getTitle(),
                        section.getSectionId(),
                        section.getBuilding(),
                        section.getRoom(),
                        section.getTimes(),
                        userRepository.findByEmail(section.getInstructorEmail()).getName(),
                        section.getInstructorEmail()
                )).toList();
    }

    // return assignments for a section
    @GetMapping("/sections/{secNo}/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo,
            Principal principal) {

        Section section =
                sectionRepository.findByEmailandSectionNo(principal.getName(), secNo);

        if (section == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        if (!section.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return section.getAssignments().stream().map(assignment ->
                new AssignmentDTO(
                        assignment.getAssignmentId(),
                        assignment.getTitle(),
                        assignment.getDueDate().toString(),
                        assignment.getSection().getCourse().getCourseId(),
                        assignment.getSection().getSectionId(),
                        assignment.getSection().getSectionNo()
                )).toList();
    }

    // create assignment
    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {

        Section s =
                sectionRepository.findByEmailandSectionNo(principal.getName(), dto.secNo());

        if (s == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (!s.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // check due date is within term
        if (Date.valueOf(dto.dueDate()).compareTo(s.getTerm().getAddDate()) < 0 ||
                Date.valueOf(dto.dueDate()).compareTo(s.getTerm().getEndDate()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(dto.title());
        assignment.setDueDate(Date.valueOf(dto.dueDate()));
        assignment.setSection(s);

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

    // update assignment title or due date
    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {

        Assignment assignment = assignmentRepository.findByID(dto.id());

        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Section s = assignment.getSection();

        if (!s.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (dto.title() != null) {
            assignment.setTitle(dto.title());
        }

        if (dto.dueDate() != null) {
            if ((0 >= Date.valueOf(dto.dueDate()).compareTo(s.getTerm().getEndDate())) &&
                    0 <= Date.valueOf(dto.dueDate()).compareTo(s.getTerm().getStartDate())) {
                assignment.setDueDate(Date.valueOf(dto.dueDate()));
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            }
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

    // delete assignment
    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(
            @PathVariable("assignmentId") int assignmentId,
            Principal principal) {

        Assignment assignment = assignmentRepository.findByID(assignmentId);

        if (assignment == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Section s = assignment.getSection();

        if (!s.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        assignmentRepository.delete(assignment);
    }

    // student view assignments and grades
    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

        User user = userRepository.findByEmail(principal.getName());

        List<Assignment> assignments =
                assignmentRepository.findByStudentEmailAndYearAndSemester(
                        user.getEmail(), year, semester);

        return assignments.stream().map(assignment -> {

            Grade g =
                    gradeRepository.findByStudentEmailAndAssignmentId(
                            user.getEmail(),
                            assignment.getAssignmentId()
                    );

            Integer score = null;
            if (g != null) {
                score = g.getScore();
            }

            return new AssignmentStudentDTO(
                    assignment.getAssignmentId(),
                    assignment.getTitle(),
                    assignment.getDueDate(),
                    assignment.getSection().getCourse().getCourseId(),
                    assignment.getSection().getSectionId(),
                    score
            );
        }).toList();
    }
}