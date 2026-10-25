package com.ong.doacoes.service;

import com.ong.doacoes.entity.Item;
import com.ong.doacoes.entity.StockMovement;
import com.ong.doacoes.entity.StockMovement.MovementReason;
import com.ong.doacoes.entity.StockMovement.MovementType;
import com.ong.doacoes.entity.User;
import com.ong.doacoes.dto.StockMovementResponse;
import com.ong.doacoes.exception.BusinessRuleException;
import com.ong.doacoes.exception.ResourceNotFoundException;
import com.ong.doacoes.repository.ItemRepository;
import com.ong.doacoes.repository.StockMovementRepository;
import com.ong.doacoes.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    private static final String EMAIL = "ana@exemplo.com";

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private StockMovementRepository movementRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StockService service;

    private Item item;
    private User usuario;

    @BeforeEach
    void preparar() {
        item = new Item("Arroz", "Alimentos", "kg");
        item.adicionarEstoque(10);
        usuario = new User("Ana", EMAIL, "hash");
        lenient().when(movementRepository.save(any(StockMovement.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void distribuir_reduzEstoqueERegistraSaidaPorDistribuicao() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        StockMovementResponse resposta = service.distribuir(1L, 4, EMAIL);

        assertThat(item.getQuantidadeEstoque()).isEqualTo(6);
        assertThat(resposta.tipo()).isEqualTo(MovementType.SAIDA);
        assertThat(resposta.motivo()).isEqualTo(MovementReason.DISTRIBUICAO);
        assertThat(resposta.quantidade()).isEqualTo(4);
        assertThat(resposta.registradoPor()).isEqualTo("Ana");
    }

    @Test
    void distribuir_lancaRegraDeNegocioQuandoEstoqueInsuficienteENaoGrava() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.distribuir(1L, 11, EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Estoque insuficiente");

        assertThat(item.getQuantidadeEstoque()).isEqualTo(10);
        verify(movementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void distribuir_lancaNaoEncontradoQuandoItemNaoExiste() {
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.distribuir(99L, 1, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void ajustar_entradaAumentaOEstoque() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        StockMovementResponse resposta = service.ajustar(1L, MovementType.ENTRADA, 5, EMAIL);

        assertThat(item.getQuantidadeEstoque()).isEqualTo(15);
        assertThat(resposta.tipo()).isEqualTo(MovementType.ENTRADA);
        assertThat(resposta.motivo()).isEqualTo(MovementReason.AJUSTE);
    }

    @Test
    void ajustar_saidaDiminuiOEstoque() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));

        service.ajustar(1L, MovementType.SAIDA, 3, EMAIL);

        assertThat(item.getQuantidadeEstoque()).isEqualTo(7);
    }

    @Test
    void entrada_somaAoEstoqueERegistraOMotivoInformado() {
        StockMovement movimento = service.entrada(item, 5, MovementReason.DOACAO, null, usuario);

        assertThat(item.getQuantidadeEstoque()).isEqualTo(15);
        assertThat(movimento.getTipo()).isEqualTo(MovementType.ENTRADA);
        assertThat(movimento.getMotivo()).isEqualTo(MovementReason.DOACAO);
    }

    @Test
    void historico_lancaNaoEncontradoQuandoItemNaoExiste() {
        when(itemRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.historico(99L, com.ong.doacoes.util.Paginas.decrescente(0, 20, "criadoEm")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}