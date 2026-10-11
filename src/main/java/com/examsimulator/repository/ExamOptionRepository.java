package com.examsimulator.repository;

import com.examsimulator.entity.ExamOption;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExamOptionRepository extends JpaRepository<ExamOption, Long> {
}
