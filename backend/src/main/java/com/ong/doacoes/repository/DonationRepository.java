package com.ong.doacoes.repository;

import com.ong.doacoes.entity.Donation;
import com.ong.doacoes.entity.Donation.DonationType;
import com.ong.doacoes.entity.Donation.DonationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    // As listagens trazem doador e usuário junto (EntityGraph), evitando uma consulta extra por linha.

    @Override
    @EntityGraph(attributePaths = {"donor", "registradoPor"})
    Page<Donation> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"donor", "registradoPor"})
    Page<Donation> findByDonorId(Long donorId, Pageable pageable);

    @EntityGraph(attributePaths = {"donor", "registradoPor"})
    Page<Donation> findByStatus(DonationStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"donor", "registradoPor"})
    Page<Donation> findByDonorIdAndStatus(Long donorId, DonationStatus status, Pageable pageable);

    /** Detalhe completo, com os itens, carregado numa única consulta. */
    @EntityGraph(attributePaths = {"donor", "registradoPor", "canceladaPor", "itens", "itens.item"})
    Optional<Donation> findDetalheById(Long id);

    /**
     * Busca com bloqueio de escrita (SELECT ... FOR UPDATE). Se dois admins cancelarem a mesma
     * doação ao mesmo tempo, o segundo espera o primeiro terminar e então vê o status CANCELADA,
     * em vez de estornar o estoque duas vezes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Donation d where d.id = :id")
    Optional<Donation> buscarParaCancelamento(@Param("id") Long id);

    /**
     * Total e quantidade de doações por mês, no intervalo [inicio, fim). Os meses sem doação
     * não aparecem aqui; o serviço completa com zero.
     *
     * O filtro de data compara o campo direto (>= e <) em vez de usar extract(year ...), para o
     * banco poder aproveitar o índice idx_donations_data_doacao.
     */
    @Query("""
            select extract(month from d.dataDoacao) as mes,
                   sum(d.valor) as total,
                   count(d) as quantidade
            from Donation d
            where d.tipo = :tipo
              and d.status = :status
              and d.dataDoacao >= :inicio
              and d.dataDoacao < :fim
            group by extract(month from d.dataDoacao)
            order by extract(month from d.dataDoacao)
            """)
    List<MonthlyTotalProjection> totaisPorMes(@Param("tipo") DonationType tipo,
                                              @Param("status") DonationStatus status,
                                              @Param("inicio") LocalDate inicio,
                                              @Param("fim") LocalDate fim);
}