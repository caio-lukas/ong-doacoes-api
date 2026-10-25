package com.ong.doacoes.service;

import com.ong.doacoes.entity.Item;
import com.ong.doacoes.dto.ItemRequest;
import com.ong.doacoes.dto.ItemResponse;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.exception.ConflictException;
import com.ong.doacoes.exception.ResourceNotFoundException;
import com.ong.doacoes.repository.ItemRepository;
import com.ong.doacoes.util.Paginas;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository repository;

    @InjectMocks
    private ItemService service;

    private final Pageable pagina = Paginas.de(0, 20, "nome");

    @Test
    void criar_salvaItemComEstoqueZeroENomeSemEspacosNasPontas() {
        when(repository.existsByNomeIgnoreCase("Arroz")).thenReturn(false);
        when(repository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));

        ItemResponse resposta = service.criar(new ItemRequest("  Arroz ", "Alimentos", "kg"));

        assertThat(resposta.nome()).isEqualTo("Arroz");
        assertThat(resposta.quantidadeEstoque()).isZero();
    }

    @Test
    void criar_lancaConflitoQuandoNomeJaExiste() {
        when(repository.existsByNomeIgnoreCase("Arroz")).thenReturn(true);

        assertThatThrownBy(() -> service.criar(new ItemRequest("Arroz", "Alimentos", "kg")))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any(Item.class));
    }

    @Test
    void atualizar_alteraOsDadosSemMexerNoEstoque() {
        Item item = new Item("Arroz", "Alimentos", "kg");
        item.adicionarEstoque(10);
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Arroz branco", 1L)).thenReturn(false);

        ItemResponse resposta = service.atualizar(1L, new ItemRequest("Arroz branco", "Alimentos", "pacote"));

        assertThat(resposta.nome()).isEqualTo("Arroz branco");
        assertThat(resposta.unidade()).isEqualTo("pacote");
        assertThat(resposta.quantidadeEstoque()).isEqualTo(10);
    }

    @Test
    void atualizar_lancaConflitoQuandoOutroItemTemOMesmoNome() {
        Item item = new Item("Arroz", "Alimentos", "kg");
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Feijão", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.atualizar(1L, new ItemRequest("Feijão", "Alimentos", "kg")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void buscar_lancaNaoEncontradoQuandoIdNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void excluir_lancaConflitoQuandoItemTemHistorico() {
        Item item = new Item("Arroz", "Alimentos", "kg");
        when(repository.findById(1L)).thenReturn(Optional.of(item));
        doThrow(new DataIntegrityViolationException("fk_stock_movements_item")).when(repository).flush();

        assertThatThrownBy(() -> service.excluir(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("movimentações");
    }

    @Test
    void listar_semFiltrosUsaFindAll() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResponse<ItemResponse> resposta = service.listar(" ", null, pagina);

        assertThat(resposta.content()).isEmpty();
        verify(repository).findAll(any(Pageable.class));
    }

    @Test
    void listar_comNomeECategoriaUsaABuscaCombinada() {
        Item item = new Item("Arroz", "Alimentos", "kg");
        when(repository.findByNomeContainingIgnoreCaseAndCategoriaIgnoreCase(
                eq("arr"), eq("alimentos"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item)));

        PageResponse<ItemResponse> resposta = service.listar(" arr ", " alimentos ", pagina);

        assertThat(resposta.content()).hasSize(1);
    }
}