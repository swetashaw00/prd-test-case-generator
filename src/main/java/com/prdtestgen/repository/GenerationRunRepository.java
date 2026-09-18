package com.prdtestgen.repository;

import com.prdtestgen.model.GenerationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GenerationRunRepository extends JpaRepository<GenerationRun, Long> {

    List<GenerationRun> findAllByOrderByCreatedAtDesc();
}
