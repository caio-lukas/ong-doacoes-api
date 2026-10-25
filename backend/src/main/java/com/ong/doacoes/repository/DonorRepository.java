package com.ong.doacoes.repository;

import com.ong.doacoes.entity.Donor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DonorRepository extends JpaRepository<Donor, Long> {

    /** Busca por parte do nome, sem diferenciar maiúsculas de minúsculas. */
    Page<Donor> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}