package com.cst438.controller;

import com.cst438.domain.*;
import com.cst438.dto.EnrollmentDTO;
import com.cst438.service.RegistrarServiceProxy;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final SectionRepository sectionRepository;
    private final RegistrarServiceProxy registrar;

    public EnrollmentController (
            EnrollmentRepository enrollmentRepository,
            SectionRepository sectionRepository,
            RegistrarServiceProxy registrar
    ) {
        this.enrollmentRepository = enrollmentRepository;
        this.sectionRepository = sectionRepository;
        this.registrar = registrar;
    }

    //~ Reece Ram
    // instructor gets student enrollments with grades for a section
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @GetMapping("/sections/{sectionNo}/enrollments")
    public List<EnrollmentDTO> getEnrollments(
            @PathVariable("sectionNo") int sectionNo, Principal principal ) {

		// check that the sectionNo belongs to the logged in instructor.

        // Logged in user email
        String loggedInEmail = principal.getName();
        // get all section info
        Section section = sectionRepository.findById(sectionNo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"No class found"));
        // get section instructor email
        String instructorEmail = section.getInstructorEmail();
        // check for miss match email
		if(!loggedInEmail.equals(instructorEmail)){
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "sectionNo doesn't belong to the logged in instructor");
        }
		// use the EnrollmentRepository findEnrollmentsBySectionNoOrderByStudentName
		// to get a list of Enrollments for the given sectionNo.
        // Return a list of EnrollmentDTOs

        List<Enrollment> listOfEnrollments = enrollmentRepository.findEnrollmentsBySectionNoOrderByStudentName(sectionNo);
        // AI assisted
        // Convert List<Enrollment> -> List<EnrollmentDTO>
        return listOfEnrollments.stream()
                .map(e -> new EnrollmentDTO(
                        e.getEnrollmentId(),
                        e.getGrade(),
                        e.getStudent().getId(),
                        e.getStudent().getName(),
                        e.getStudent().getEmail(),
                        e.getSection().getCourse().getCourseId(),
                        e.getSection().getCourse().getTitle(),
                        e.getSection().getSectionId(),
                        e.getSection().getSectionNo(),
                        e.getSection().getBuilding(),
                        e.getSection().getRoom(),
                        e.getSection().getTimes(),
                        e.getSection().getCourse().getCredits(),
                        e.getSection().getTerm().getYear(),
                        e.getSection().getTerm().getSemester()
                /*
                        enrollmentId,
                        grade,
                        studentId,
                        name,
                        email,
                        courseId,
                        title,
                        sectionId,
                        sectionNo,
                        building,
                        room,
                        times,
                        credits,
                        year,
                        semester

                 */

                ))
                .collect(Collectors.toList());
    }

    //~ Reece Ram
    // instructor updates enrollment grades
    @PreAuthorize("hasAuthority('SCOPE_ROLE_INSTRUCTOR')")
    @PutMapping("/enrollments")
    public void updateEnrollmentGrade(@Valid @RequestBody List<EnrollmentDTO> dtoList, Principal principal) {
		// for each EnrollmentDTO
        //    check that logged in user is instructor for the section

        // get logged un user email
        String loggedInEmail = principal.getName();

        // for each item in list
        for (EnrollmentDTO dto : dtoList){
            // get each section
            Enrollment enrollment = enrollmentRepository.findById(dto.enrollmentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Enrollment not found"));
            // get section instructor Email
            Section section = enrollment.getSection();

            if (!loggedInEmail.equals(section.getInstructorEmail())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "sectionNo doesn't belong to the logged in instructor");
            }

            // update the enrollment grade
            // send message to Registrar service for grade update
            enrollment.setGrade(dto.grade());
            enrollmentRepository.save(enrollment);
        }

    }
}
