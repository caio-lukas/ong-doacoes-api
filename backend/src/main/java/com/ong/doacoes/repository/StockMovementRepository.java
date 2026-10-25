package com.ong.doacoes.repository;

import com.ong.doacoes.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /** Histórico de um item. O EntityGraph traz o usuário junto, evitando uma consulta por linha. */
    @EntityGraph(attributePaths = "registradoPor")
    Page<StockMovement> findByItemId(Long itemId, Pageable pageable);
}