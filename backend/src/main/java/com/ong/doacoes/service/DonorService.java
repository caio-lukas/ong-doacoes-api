package com.ong.doacoes.service;

import com.ong.doacoes.entity.Donor;
import com.ong.doacoes.dto.DonorRequest;
import com.ong.doacoes.dto.DonorResponse;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.exception.ConflictException;
import com.ong.doacoes.exception.ResourceNotFoundException;
import com.ong.doacoes.repository.DonorRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonorService {

    private final DonorRepository repository;

    public DonorService(DonorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DonorResponse criar(DonorRequest request) {
        Donor donor = new Donor(request.nome().trim(), request.email(), normalizarTelefone(request.telefone()));
        return DonorResponse.from(repository.save(donor));
    }

    @Transactional(readOnly = true)
    public PageResponse<DonorResponse> listar(String nome, Pageable pageable) {
        Page<Donor> pagina = (nome == null || nome.isBlank())
                ? repository.findAll(pageable)
                : repository.findByNomeContainingIgnoreCase(nome.trim(), pageable);
        return PageResponse.from(pagina.map(DonorResponse::from));
    }

    @Transactional(readOnly = true)
    public DonorResponse buscar(Long id) {
        return DonorResponse.from(buscarEntidade(id));
    }

    @Transactional
    public DonorResponse atualizar(Long id, DonorRequest request) {
        Donor donor = buscarEntidade(id);
        donor.atualizar(request.nome().trim(), request.email(), normalizarTelefone(request.telefone()));
        return DonorResponse.from(donor);   // o JPA grava a mudança ao final da transação
    }

    /** Só é possível excluir doador sem doações: a chave estrangeira do banco impede o resto. */
    @Transactional
    public void excluir(Long id) {
        Donor donor = buscarEntidade(id);
        try {
            repository.delete(donor);
            repository.flush();   // força o DELETE agora, para capturar a violação aqui
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Doador com doações registradas não pode ser excluído");
        }
    }

    /**
     * Guarda só os dígitos (DDD + número) e remove o código do país, se houver.
     * O formato já foi validado no DonorRequest: chegam aqui 10 a 13 dígitos.
     */
    private static String normalizarTelefone(String telefone) {
        String digitos = telefone.replaceAll("\\D", "");
        return digitos.length() > 11 ? digitos.substring(2) : digitos;
    }

    /** Usado também pelo serviço de doações (busca a entidade, não o DTO). */
    @Transactional(readOnly = true)
    public Donor buscarEntidade(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doador não encontrado"));
    }
}