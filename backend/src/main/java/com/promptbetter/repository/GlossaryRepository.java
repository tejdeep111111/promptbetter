package com.promptbetter.repository;

import com.promptbetter.model.GlossaryTerm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlossaryRepository extends JpaRepository<GlossaryTerm, Long> {
}
