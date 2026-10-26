package com.ong.doacoes.service;

import com.ong.doacoes.entity.Donation;
import com.ong.doacoes.entity.Donation.DonationStatus;
import com.ong.doacoes.entity.Donation.DonationType;
import com.ong.doacoes.entity.DonationItem;
import com.ong.doacoes.entity.Donor;
import com.ong.doacoes.entity.Item;
import com.ong.doacoes.entity.StockMovement.MovementReason;
import com.ong.doacoes.entity.User;
import com.ong.doacoes.dto.DonationItemRequest;
import com.ong.doacoes.dto.DonationResponse;
import com.ong.doacoes.dto.DonationSummaryResponse;
import com.ong.doacoes.dto.ItemsDonationRequest;
import com.ong.doacoes.dto.MoneyDonationRequest;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.exception.BusinessRuleException;
import com.ong.doacoes.exception.ConflictException;
import com.ong.doacoes.exception.ResourceNotFoundException;
import com.ong.doacoes.repository.DonationRepository;
import com.ong.doacoes.repository.ItemRepository;
import com.ong.doacoes.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Registro e cancelamento de doações.
 *
 * Tudo acontece em UMA transação: a doação, os itens, as movimentações de estoque e a
 * atualização das quantidades. Se qualquer parte falhar (ex.: item inexistente), nada é gravado.
 */
@Service
public class DonationService {

    private final DonationRepository repository;
    private final DonorService donorService;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final StockService stockService;

    public DonationService(DonationRepository repository,
                           DonorService donorService,
                           ItemRepository itemRepository,
                           UserRepository userRepository,
                           StockService stockService) {
        this.repository = repository;
        this.donorService = donorService;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.stockService = stockService;
    }

    @Transactional
    public DonationResponse registrarDinheiro(MoneyDonationRequest request, String emailUsuario) {
        Donor doador = donorService.buscarEntidade(request.doadorId());
        User usuario = buscarUsuario(emailUsuario);

        Donation doacao = Donation.emDinheiro(doador, request.valor(), dataOuHoje(request.dataDoacao()), usuario);
        return DonationResponse.from(repository.save(doacao));
    }

    @Transactional
    public DonationResponse registrarItens(ItemsDonationRequest request, String emailUsuario) {
        exigirItensDistintos(request.itens());
        Donor doador = donorService.buscarEntidade(request.doadorId());
        User usuario = buscarUsuario(emailUsuario);

        Donation doacao = Donation.deItens(doador, dataOuHoje(request.dataDoacao()), usuario);
        for (DonationItemRequest linha : request.itens()) {
            Item item = itemRepository.findById(linha.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado: " + linha.itemId()));
            doacao.adicionarItem(item, linha.quantidade());
        }

        // Salva primeiro: as movimentações precisam apontar para uma doação que já tem id
        Donation salva = repository.save(doacao);
        for (DonationItem linha : salva.getItens()) {
            stockService.entrada(linha.getItem(), linha.getQuantidade(), MovementReason.DOACAO, salva, usuario);
        }
        return DonationResponse.from(salva);
    }

    @Transactional(readOnly = true)
    public PageResponse<DonationSummaryResponse> listar(Long doadorId, DonationStatus status, Pageable pageable) {
        Page<Donation> pagina;
        if (doadorId != null && status != null) {
            pagina = repository.findByDonorIdAndStatus(doadorId, status, pageable);
        } else if (doadorId != null) {
            pagina = repository.findByDonorId(doadorId, pageable);
        } else if (status != null) {
            pagina = repository.findByStatus(status, pageable);
        } else {
            pagina = repository.findAll(pageable);
        }
        return PageResponse.from(pagina.map(DonationSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public DonationResponse buscar(Long id) {
        Donation doacao = repository.findDetalheById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doação não encontrada"));
        return DonationResponse.from(doacao);
    }

    /**
     * Cancela a doação. Em doação de itens, estorna o estoque com saídas do tipo
     * CANCELAMENTO_DOACAO. Se parte dos itens já foi distribuída e o estoque não cobre o
     * estorno, o cancelamento inteiro é recusado (422) e nada é alterado.
     */
    @Transactional
    public DonationResponse cancelar(Long id, String motivo, String emailUsuario) {
        Donation doacao = repository.buscarParaCancelamento(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doação não encontrada"));
        if (doacao.getStatus() == DonationStatus.CANCELADA) {
            throw new ConflictException("A doação já está cancelada");
        }
        User usuario = buscarUsuario(emailUsuario);

        if (doacao.getTipo() == DonationType.ITENS) {
            for (DonationItem linha : doacao.getItens()) {
                try {
                    stockService.saida(linha.getItem(), linha.getQuantidade(),
                            MovementReason.CANCELAMENTO_DOACAO, doacao, usuario);
                } catch (BusinessRuleException e) {
                    throw new BusinessRuleException("Não é possível cancelar a doação: " + e.getMessage());
                }
            }
        }
        doacao.cancelar(usuario, motivo.trim());
        return DonationResponse.from(doacao);
    }

    private static void exigirItensDistintos(List<DonationItemRequest> itens) {
        Set<Long> vistos = new HashSet<>();
        for (DonationItemRequest linha : itens) {
            if (!vistos.add(linha.itemId())) {
                throw new BusinessRuleException("O mesmo item não pode aparecer duas vezes na doação");
            }
        }
    }

    private static LocalDate dataOuHoje(LocalDate data) {
        return data != null ? data : LocalDate.now();
    }

    private User buscarUsuario(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}