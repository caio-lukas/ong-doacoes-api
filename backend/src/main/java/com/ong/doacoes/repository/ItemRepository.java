package com.ong.doacoes.repository;

import com.ong.doacoes.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Long> {

    Page<Item> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Item> findByCategoriaIgnoreCase(String categoria, Pageable pageable);

    Page<Item> findByNomeContainingIgnoreCaseAndCategoriaIgnoreCase(String nome, String categoria, Pageable pageable);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);
}