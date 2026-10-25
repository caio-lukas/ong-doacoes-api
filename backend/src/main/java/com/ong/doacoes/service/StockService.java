package com.ong.doacoes.service;

import com.ong.doacoes.entity.Donation;
import com.ong.doacoes.entity.Item;
import com.ong.doacoes.entity.StockMovement;
import com.ong.doacoes.entity.StockMovement.MovementReason;
import com.ong.doacoes.entity.StockMovement.MovementType;
import com.ong.doacoes.entity.User;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.dto.StockMovementResponse;
import com.ong.doacoes.exception.BusinessRuleException;
import com.ong.doacoes.exception.ResourceNotFoundException;
import com.ong.doacoes.repository.ItemRepository;
import com.ong.doacoes.repository.StockMovementRepository;
import com.ong.doacoes.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Único lugar onde o estoque muda: toda alteração na quantidade de um item gera,
 * na mesma transação, um StockMovement. As doações (fatia 3) também usam {@link #entrada}
 * e {@link #saida}, para a regra não ficar duplicada.
 *
 * Concorrência: o Item tem @Version. Se duas operações alterarem o mesmo item ao mesmo tempo,
 * a segunda falha (ObjectOptimisticLockingFailureException) e a API responde 409.
 */
@Service
public class StockService {

    private final ItemRepository itemRepository;
    private final StockMovementRepository movementRepository;
    private final UserRepository userRepository;

    public StockService(ItemRepository itemRepository,
                        StockMovementRepository movementRepository,
                        UserRepository userRepository) {
        this.itemRepository = itemRepository;
        this.movementRepository = movementRepository;
        this.userRepository = userRepository;
    }

    /** Saída por distribuição: item entregue a quem recebe. */
    @Transactional
    public StockMovementResponse distribuir(Long itemId, int quantidade, String emailUsuario) {
        Item item = buscarItem(itemId);
        User usuario = buscarUsuario(emailUsuario);
        return StockMovementResponse.from(saida(item, quantidade, MovementReason.DISTRIBUICAO, null, usuario));
    }

    /** Correção manual de estoque, para mais (ENTRADA) ou para menos (SAIDA). */
    @Transactional
    public StockMovementResponse ajustar(Long itemId, MovementType tipo, int quantidade, String emailUsuario) {
        Item item = buscarItem(itemId);
        User usuario = buscarUsuario(emailUsuario);
        StockMovement movimento = (tipo == MovementType.ENTRADA)
                ? entrada(item, quantidade, MovementReason.AJUSTE, null, usuario)
                : saida(item, quantidade, MovementReason.AJUSTE, null, usuario);
        return StockMovementResponse.from(movimento);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> historico(Long itemId, Pageable pageable) {
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item não encontrado");
        }
        return PageResponse.from(movementRepository.findByItemId(itemId, pageable)
                .map(StockMovementResponse::from));
    }

    /** Soma ao estoque e registra a movimentação. Usado também pelas doações de itens. */
    @Transactional
    public StockMovement entrada(Item item, int quantidade, MovementReason motivo,
                                 Donation doacao, User usuario) {
        item.adicionarEstoque(quantidade);
        return movementRepository.save(
                new StockMovement(item, MovementType.ENTRADA, motivo, quantidade, doacao, usuario));
    }

    /** Tira do estoque e registra a movimentação. Não deixa o estoque ficar negativo (422). */
    @Transactional
    public StockMovement saida(Item item, int quantidade, MovementReason motivo,
                               Donation doacao, User usuario) {
        if (quantidade > item.getQuantidadeEstoque()) {
            throw new BusinessRuleException(String.format(
                    "Estoque insuficiente de '%s': disponível %d, solicitado %d",
                    item.getNome(), item.getQuantidadeEstoque(), quantidade));
        }
        item.removerEstoque(quantidade);
        return movementRepository.save(
                new StockMovement(item, MovementType.SAIDA, motivo, quantidade, doacao, usuario));
    }

    private Item buscarItem(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado"));
    }

    private User buscarUsuario(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }
}