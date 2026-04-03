package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.AssignmentDTO;
import com.cst438.dto.AssignmentStudentDTO;
import com.cst438.dto.SectionDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


import java.security.Principal;
import java.sql.Date;
import java.util.ArrayList;
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

<<<<<<< HEAD
    // get Sections for an instructor
=======
    //  get sections for instructor
>>>>>>> 08e4b12461659beb2dd76440fd16658f0716b279
    @GetMapping("/sections")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<SectionDTO> getSectionsForInstructor(
            @RequestParam("year") int year ,
            @RequestParam("semester") String semester,
            Principal principal)  {
        // return the Sections that have instructorEmail for the
        // logged in instructor user for the given term.
        return null;
    }

<<<<<<< HEAD
    // instructor lists assignments for a section.
=======
    //  get assignments for section
>>>>>>> 08e4b12461659beb2dd76440fd16658f0716b279
    @GetMapping("/sections/{secNo}/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public List<AssignmentDTO> getAssignments(
            @PathVariable("secNo") int secNo,
            Principal principal) {

<<<<<<< HEAD
        // verify that user is the instructor for the section
        //  return list of assignments for the Section
        return null;
    }


=======
        Section section = sectionRepository.findById(secNo).orElse(null);

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

    //  create assignment
>>>>>>> 08e4b12461659beb2dd76440fd16658f0716b279
    @PostMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO createAssignment(
            @Valid @RequestBody AssignmentDTO dto,
            Principal principal) {

<<<<<<< HEAD
        //  user must be the instructor for the Section
        //  check that assignment dueDate is between start date and
        //  end date of the term
        //  create and save an Assignment entity
        //  return AssignmentDTO with database generated primary key
        return null;
    }


    @PutMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public AssignmentDTO updateAssignment(@Valid @RequestBody AssignmentDTO dto, Principal principal) {
        //  update Assignment Entity.  only title and dueDate fields can be changed.
        //  user must be instructor of the Section
        return null;
    }


=======
        Section s = sectionRepository.findById(dto.secNo()).orElse(null);

        if (s == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        if (!s.getInstructorEmail().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Date dueDate = Date.valueOf(dto.dueDate());

        if (dueDate.before(s.getTerm().getAddDate()) ||
                dueDate.after(s.getTerm().getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(dto.title());
        assignment.setDueDate(dueDate);
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

    //  update assignment
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
            Date newDate = Date.valueOf(dto.dueDate());

            if (!newDate.before(s.getTerm().getStartDate()) &&
                    !newDate.after(s.getTerm().getEndDate())) {
                assignment.setDueDate(newDate);
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

    //  delete assignment
>>>>>>> 08e4b12461659beb2dd76440fd16658f0716b279
    @DeleteMapping("/assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    public void deleteAssignment(@PathVariable("assignmentId") int assignmentId, Principal principal) {
        // verify that user is the instructor of the section
        // delete the Assignment entity

    }

<<<<<<< HEAD
    // student lists their assignments/grades  ordered by due date
=======
    //  student view
>>>>>>> 08e4b12461659beb2dd76440fd16658f0716b279
    @GetMapping("/assignments")
    @PreAuthorize("hasAuthority('SCOPE_ROLE_STUDENT')")
    public List<AssignmentStudentDTO> getStudentAssignments(
            @RequestParam("year") int year,
            @RequestParam("semester") String semester,
            Principal principal) {

<<<<<<< HEAD
        //  return AssignmentStudentDTOs with scores of a
        //  Grade entity exists.
        //  hint: use the GradeRepository findByStudentEmailAndAssignmentId
        //  If assignment has not been graded, return a null score.
        return null;
=======
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

            Integer score = (g != null) ? g.getScore() : null;

            return new AssignmentStudentDTO(
                    assignment.getAssignmentId(),
                    assignment.getTitle(),
                    assignment.getDueDate(),
                    assignment.getSection().getCourse().getCourseId(),
                    assignment.getSection().getSectionId(),
                    score
            );
        }).toList();
>>>>>>> 08e4b12461659beb2dd76440fd16658f0716b279
    }
}