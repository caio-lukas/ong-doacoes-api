package com.ong.doacoes.service;

import com.ong.doacoes.entity.Donor;
import com.ong.doacoes.dto.DonorRequest;
import com.ong.doacoes.dto.DonorResponse;
import com.ong.doacoes.dto.PageResponse;
import com.ong.doacoes.exception.ConflictException;
import com.ong.doacoes.exception.ResourceNotFoundException;
import com.ong.doacoes.repository.DonorRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonorServiceTest {

    @Mock
    private DonorRepository repository;

    @InjectMocks
    private DonorService service;

    private final Pageable pagina = Paginas.de(0, 20, "nome");

    @Test
    void criar_salvaDoadorComNomeSemEspacosNasPontas() {
        when(repository.save(any(Donor.class))).thenAnswer(inv -> inv.getArgument(0));

        DonorResponse resposta = service.criar(new DonorRequest("  Maria Silva  ", "maria@exemplo.com", "22999990000"));

        assertThat(resposta.nome()).isEqualTo("Maria Silva");
        assertThat(resposta.email()).isEqualTo("maria@exemplo.com");
    }

    @Test
    void buscar_lancaNaoEncontradoQuandoIdNaoExiste() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void atualizar_alteraOsDadosDoDoador() {
        Donor donor = new Donor("Maria", "maria@exemplo.com", "22999990000");
        when(repository.findById(1L)).thenReturn(Optional.of(donor));

        DonorResponse resposta = service.atualizar(1L,
                new DonorRequest("Maria Silva", "maria.silva@exemplo.com", "22988887777"));

        assertThat(resposta.nome()).isEqualTo("Maria Silva");
        assertThat(donor.getEmail()).isEqualTo("maria.silva@exemplo.com");
    }

    @Test
    void excluir_removeDoadorSemDoacoes() {
        Donor donor = new Donor("Maria", "maria@exemplo.com", "22999990000");
        when(repository.findById(1L)).thenReturn(Optional.of(donor));

        service.excluir(1L);

        verify(repository).delete(donor);
    }

    @Test
    void excluir_lancaConflitoQuandoDoadorTemDoacoes() {
        Donor donor = new Donor("Maria", "maria@exemplo.com", "22999990000");
        when(repository.findById(1L)).thenReturn(Optional.of(donor));
        doThrow(new DataIntegrityViolationException("fk_donations_donor")).when(repository).flush();

        assertThatThrownBy(() -> service.excluir(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("doações");
    }

    @Test
    void listar_semFiltroUsaFindAll() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResponse<DonorResponse> resposta = service.listar("  ", pagina);

        assertThat(resposta.content()).isEmpty();
        verify(repository).findAll(any(Pageable.class));
    }

    @Test
    void listar_comNomeBuscaPorParteDoNome() {
        Donor donor = new Donor("Maria Silva", "maria@exemplo.com", "22999990000");
        when(repository.findByNomeContainingIgnoreCase(eq("mar"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(donor)));

        PageResponse<DonorResponse> resposta = service.listar(" mar ", pagina);

        assertThat(resposta.content()).hasSize(1);
        assertThat(resposta.content().get(0).nome()).isEqualTo("Maria Silva");
    }
}