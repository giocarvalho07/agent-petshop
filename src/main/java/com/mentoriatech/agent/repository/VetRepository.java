package com.mentoriatech.agent.repository;

import com.mentoriatech.agent.entity.Vet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VetRepository extends JpaRepository<Vet, Long> {

    Optional<Vet> findByNameIgnoreCase(String name);

    List<Vet> findBySpecialtyIgnoreCase(String specialty);

    @Query("SELECT DISTINCT v.specialty FROM Vet v")
    List<String> findDistinctSpecialties();
}
