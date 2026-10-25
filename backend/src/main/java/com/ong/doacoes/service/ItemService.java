package com.ong.doacoes.service;

import com.ong.doacoes.entity.Item;
import com.ong.doacoes.dto.ItemRequest;
import com.ong.doacoes.dto.ItemResponse;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.exception.ConflictException;
import com.ong.doacoes.exception.ResourceNotFoundException;
import com.ong.doacoes.repository.ItemRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** CRUD de itens. A quantidade em estoque é responsabilidade do {@link StockService}. */
@Service
public class ItemService {

    private final ItemRepository repository;

    public ItemService(ItemRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ItemResponse criar(ItemRequest request) {
        String nome = request.nome().trim();
        if (repository.existsByNomeIgnoreCase(nome)) {
            throw new ConflictException("Já existe um item com este nome");
        }
        Item item = new Item(nome, request.categoria().trim(), request.unidade().trim());
        return ItemResponse.from(repository.save(item));
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> listar(String nome, String categoria, Pageable pageable) {
        boolean temNome = nome != null && !nome.isBlank();
        boolean temCategoria = categoria != null && !categoria.isBlank();

        Page<Item> pagina;
        if (temNome && temCategoria) {
            pagina = repository.findByNomeContainingIgnoreCaseAndCategoriaIgnoreCase(
                    nome.trim(), categoria.trim(), pageable);
        } else if (temNome) {
            pagina = repository.findByNomeContainingIgnoreCase(nome.trim(), pageable);
        } else if (temCategoria) {
            pagina = repository.findByCategoriaIgnoreCase(categoria.trim(), pageable);
        } else {
            pagina = repository.findAll(pageable);
        }
        return PageResponse.from(pagina.map(ItemResponse::from));
    }

    @Transactional(readOnly = true)
    public ItemResponse buscar(Long id) {
        return ItemResponse.from(buscarEntidade(id));
    }

    @Transactional
    public ItemResponse atualizar(Long id, ItemRequest request) {
        Item item = buscarEntidade(id);
        String nome = request.nome().trim();
        if (repository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new ConflictException("Já existe outro item com este nome");
        }
        item.atualizar(nome, request.categoria().trim(), request.unidade().trim());
        return ItemResponse.from(item);
    }

    /** Item com movimentações de estoque ou doações não pode ser excluído (chaves estrangeiras). */
    @Transactional
    public void excluir(Long id) {
        Item item = buscarEntidade(id);
        try {
            repository.delete(item);
            repository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Item com movimentações de estoque ou doações não pode ser excluído");
        }
    }

    @Transactional(readOnly = true)
    public Item buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item não encontrado"));
    }
}