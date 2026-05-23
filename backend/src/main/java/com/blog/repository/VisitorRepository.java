package com.blog.repository;

import com.blog.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    boolean existsByVisitorIdAndVisitDate(String visitorId, LocalDate visitDate);

    @Query("SELECT COUNT(DISTINCT v.visitorId) FROM Visitor v WHERE v.visitDate = :date")
    long countDistinctByVisitDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(DISTINCT v.visitorId) FROM Visitor v WHERE v.visitDate BETWEEN :start AND :end")
    long countDistinctByVisitDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);
}