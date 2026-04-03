package com.cst438.domain;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AssignmentRepository extends CrudRepository<Assignment, Integer> {

    @Query("select a from Assignment a join a.section.term t join a.section.enrollments e where e.student.email=:email and t.year=:year and t.semester=:semester order by a.dueDate")
<<<<<<< HEAD
    List<Assignment> findByStudentEmailAndYearAndSemester(String email, int year, String semester);;
=======
    List<Assignment> findByStudentEmailAndYearAndSemester(String email, int year, String semester);
    @Query("select a from Assignment a where a.assignmentId=:ID")
    Assignment findByID( @Param("ID") int ID);
>>>>>>> 08e4b12461659beb2dd76440fd16658f0716b279
}
