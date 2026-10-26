package com.ong.doacoes.service;

import com.ong.doacoes.entity.Donation;
import com.ong.doacoes.entity.Donation.DonationStatus;
import com.ong.doacoes.entity.Donation.DonationType;
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
import com.ong.doacoes.util.Paginas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final LocalDate DATA = LocalDate.of(2026, 10, 1);

    @Mock
    private DonationRepository repository;
    @Mock
    private DonorService donorService;
    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private StockService stockService;

    @InjectMocks
    private DonationService service;

    private Donor doador;
    private User usuario;
    private Item arroz;
    private Item feijao;

    @BeforeEach
    void preparar() {
        doador = new Donor("Maria Silva", "maria@exemplo.com", "22999990000");
        usuario = new User("Ana", EMAIL, "hash");
        arroz = new Item("Arroz", "Alimentos", "kg");
        feijao = new Item("Feijão", "Alimentos", "kg");
        lenient().when(repository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario));
    }

    // ---------- registro ----------

    @Test
    void registrarDinheiro_salvaDoacaoEmDinheiroSemMexerNoEstoque() {
        when(donorService.buscarEntidade(1L)).thenReturn(doador);

        DonationResponse resposta = service.registrarDinheiro(
                new MoneyDonationRequest(1L, new BigDecimal("150.5"), DATA), EMAIL);

        assertThat(resposta.tipo()).isEqualTo(DonationType.DINHEIRO);
        assertThat(resposta.valor()).isEqualByComparingTo(new BigDecimal("150.50"));
        assertThat(resposta.status()).isEqualTo(DonationStatus.REGISTRADA);
        assertThat(resposta.dataDoacao()).isEqualTo(DATA);
        assertThat(resposta.registradoPor()).isEqualTo("Ana");
        verifyNoInteractions(stockService);
    }

    @Test
    void registrarItens_salvaDoacaoEDaEntradaNoEstoqueDeCadaItem() {
        when(donorService.buscarEntidade(1L)).thenReturn(doador);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(arroz));
        when(itemRepository.findById(11L)).thenReturn(Optional.of(feijao));

        DonationResponse resposta = service.registrarItens(new ItemsDonationRequest(1L, DATA,
                List.of(new DonationItemRequest(10L, 5), new DonationItemRequest(11L, 3))), EMAIL);

        assertThat(resposta.tipo()).isEqualTo(DonationType.ITENS);
        assertThat(resposta.valor()).isNull();
        assertThat(resposta.itens()).hasSize(2);
        verify(stockService).entrada(eq(arroz), eq(5), eq(MovementReason.DOACAO), any(Donation.class), eq(usuario));
        verify(stockService).entrada(eq(feijao), eq(3), eq(MovementReason.DOACAO), any(Donation.class), eq(usuario));
    }

    @Test
    void registrarItens_semDataUsaADataDeHoje() {
        when(donorService.buscarEntidade(1L)).thenReturn(doador);
        when(itemRepository.findById(10L)).thenReturn(Optional.of(arroz));

        DonationResponse resposta = service.registrarItens(new ItemsDonationRequest(1L, null,
                List.of(new DonationItemRequest(10L, 5))), EMAIL);

        assertThat(resposta.dataDoacao()).isEqualTo(LocalDate.now());
    }

    @Test
    void registrarItens_recusaOMesmoItemDuasVezesENaoGravaNada() {
        ItemsDonationRequest request = new ItemsDonationRequest(1L, DATA,
                List.of(new DonationItemRequest(10L, 1), new DonationItemRequest(10L, 2)));

        assertThatThrownBy(() -> service.registrarItens(request, EMAIL))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any(Donation.class));
        verifyNoInteractions(stockService);
    }

    @Test
    void registrarItens_lancaNaoEncontradoQuandoItemNaoExisteENaoGravaNada() {
        when(donorService.buscarEntidade(1L)).thenReturn(doador);
        when(itemRepository.findById(99L)).thenReturn(Optional.empty());
        ItemsDonationRequest request = new ItemsDonationRequest(1L, DATA,
                List.of(new DonationItemRequest(99L, 1)));

        assertThatThrownBy(() -> service.registrarItens(request, EMAIL))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(repository, never()).save(any(Donation.class));
        verifyNoInteractions(stockService);
    }

    @Test
    void registrarDinheiro_propagaNaoEncontradoQuandoDoadorNaoExiste() {
        when(donorService.buscarEntidade(5L)).thenThrow(new ResourceNotFoundException("Doador não encontrado"));

        assertThatThrownBy(() -> service.registrarDinheiro(
                new MoneyDonationRequest(5L, new BigDecimal("10.00"), DATA), EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- cancelamento ----------

    @Test
    void cancelar_doacaoDeItensEstornaOEstoqueDeCadaItem() {
        Donation doacao = Donation.deItens(doador, DATA, usuario);
        doacao.adicionarItem(arroz, 5);
        doacao.adicionarItem(feijao, 3);
        when(repository.buscarParaCancelamento(7L)).thenReturn(Optional.of(doacao));

        DonationResponse resposta = service.cancelar(7L, "  Lançamento duplicado ", EMAIL);

        assertThat(resposta.status()).isEqualTo(DonationStatus.CANCELADA);
        assertThat(resposta.motivoCancelamento()).isEqualTo("Lançamento duplicado");
        assertThat(resposta.canceladaPor()).isEqualTo("Ana");
        verify(stockService).saida(eq(arroz), eq(5), eq(MovementReason.CANCELAMENTO_DOACAO), eq(doacao), eq(usuario));
        verify(stockService).saida(eq(feijao), eq(3), eq(MovementReason.CANCELAMENTO_DOACAO), eq(doacao), eq(usuario));
    }

    @Test
    void cancelar_doacaoEmDinheiroNaoMexeNoEstoque() {
        Donation doacao = Donation.emDinheiro(doador, new BigDecimal("100.00"), DATA, usuario);
        when(repository.buscarParaCancelamento(7L)).thenReturn(Optional.of(doacao));

        DonationResponse resposta = service.cancelar(7L, "Valor digitado errado", EMAIL);

        assertThat(resposta.status()).isEqualTo(DonationStatus.CANCELADA);
        verifyNoInteractions(stockService);
    }

    @Test
    void cancelar_lancaConflitoQuandoJaEstaCancelada() {
        Donation doacao = Donation.emDinheiro(doador, new BigDecimal("100.00"), DATA, usuario);
        doacao.cancelar(usuario, "primeiro cancelamento");
        when(repository.buscarParaCancelamento(7L)).thenReturn(Optional.of(doacao));

        assertThatThrownBy(() -> service.cancelar(7L, "outra vez", EMAIL))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(stockService);
    }

    @Test
    void cancelar_recusaQuandoOEstoqueNaoCobreOEstornoEMantemARegistrada() {
        Donation doacao = Donation.deItens(doador, DATA, usuario);
        doacao.adicionarItem(arroz, 5);
        when(repository.buscarParaCancelamento(7L)).thenReturn(Optional.of(doacao));
        when(stockService.saida(eq(arroz), anyInt(), any(), any(), any()))
                .thenThrow(new BusinessRuleException("Estoque insuficiente de 'Arroz': disponível 2, solicitado 5"));

        assertThatThrownBy(() -> service.cancelar(7L, "Erro de lançamento", EMAIL))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageStartingWith("Não é possível cancelar a doação")
                .hasMessageContaining("Estoque insuficiente");

        assertThat(doacao.getStatus()).isEqualTo(DonationStatus.REGISTRADA);
    }

    @Test
    void cancelar_lancaNaoEncontradoQuandoDoacaoNaoExiste() {
        when(repository.buscarParaCancelamento(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelar(99L, "motivo", EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- consulta ----------

    @Test
    void buscar_lancaNaoEncontradoQuandoDoacaoNaoExiste() {
        when(repository.findDetalheById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listar_porDoadorEStatusUsaABuscaCombinada() {
        Donation doacao = Donation.emDinheiro(doador, new BigDecimal("100.00"), DATA, usuario);
        Pageable pagina = Paginas.decrescente(0, 20, "dataDoacao");
        when(repository.findByDonorIdAndStatus(eq(1L), eq(DonationStatus.REGISTRADA), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doacao)));

        PageResponse<DonationSummaryResponse> resposta = service.listar(1L, DonationStatus.REGISTRADA, pagina);

        assertThat(resposta.content()).hasSize(1);
        assertThat(resposta.content().get(0).doadorNome()).isEqualTo("Maria Silva");
    }

    @Test
    void listar_semFiltrosUsaFindAll() {
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        PageResponse<DonationSummaryResponse> resposta =
                service.listar(null, null, Paginas.decrescente(0, 20, "dataDoacao"));

        assertThat(resposta.content()).isEmpty();
    }
}