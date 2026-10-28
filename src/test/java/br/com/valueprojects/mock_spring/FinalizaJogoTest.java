package br.com.valueprojects.mock_spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import br.com.valueprojects.mock_spring.builder.CriadorDeJogo;
import br.com.valueprojects.mock_spring.model.EnviaSms;
import br.com.valueprojects.mock_spring.model.FinalizaJogo;
import br.com.valueprojects.mock_spring.model.Jogo;
import br.com.valueprojects.mock_spring.model.Participante;
import br.com.valueprojects.mock_spring.model.Sms;
import infra.JogoDaoFalso;
import infra.VencedorDaoFalso;

public class FinalizaJogoTest {

	@Test
	public void deveFinalizarJogoDaSemanaAnteriorSalvarVencedorEEnviarSmsNaOrdemCorreta() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Calendar hoje = Calendar.getInstance();

		Participante p1 = new Participante("João");
		Participante p2 = new Participante("Maria");

		Jogo jogoAntigo = new CriadorDeJogo()
				.para("Caça moedas")
				.naData(antiga)
				.resultado(p1, 150.0)
				.resultado(p2, 200.0)
				.constroi();

		Jogo jogoDeHoje = new CriadorDeJogo()
				.para("Jogo atual")
				.naData(hoje)
				.resultado(p1, 100.0)
				.constroi();

		List<Jogo> jogosEmAndamento = Arrays.asList(jogoAntigo, jogoDeHoje);

		// Mocks das dependências
		JogoDaoFalso jogoDaoMock = mock(JogoDaoFalso.class);
		VencedorDaoFalso vencedorDaoMock = mock(VencedorDaoFalso.class);
		EnviaSms enviaSmsMock = mock(EnviaSms.class);

		// Stub: retorna um jogo da semana anterior e um jogo de hoje
		when(jogoDaoMock.emAndamento()).thenReturn(jogosEmAndamento);

		FinalizaJogo finalizador = new FinalizaJogo(jogoDaoMock, vencedorDaoMock, enviaSmsMock);
		finalizador.finaliza();

		assertTrue(jogoAntigo.isFinalizado());
		assertFalse(jogoDeHoje.isFinalizado());
		assertEquals(1, finalizador.getTotalFinalizados());

		// Garante a ordem estrita de execução exigida pela regra de negócio:
		// 1º Salva o jogo -> 2º Salva o vencedor -> 3º Envia o SMS
		InOrder inOrder = inOrder(jogoDaoMock, vencedorDaoMock, enviaSmsMock);
		inOrder.verify(jogoDaoMock, times(1)).atualiza(jogoAntigo);
		inOrder.verify(vencedorDaoMock, times(1)).salva(p2);
		inOrder.verify(enviaSmsMock, times(1)).envia(any(Sms.class), eq(p2));

		// Garante que o jogo da semana atual não foi salvo na base
		verify(jogoDaoMock, never()).atualiza(jogoDeHoje);
	}

	@Test
	public void naoDeveSalvarVencedorNemEnviarSmsQuandoFalharAoSalvarJogo() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Participante p1 = new Participante("Maria");
		Jogo jogoAntigo = new CriadorDeJogo()
				.para("Caça moedas")
				.naData(antiga)
				.resultado(p1, 200.0)
				.constroi();

		JogoDaoFalso jogoDaoMock = mock(JogoDaoFalso.class);
		VencedorDaoFalso vencedorDaoMock = mock(VencedorDaoFalso.class);
		EnviaSms enviaSmsMock = mock(EnviaSms.class);

		when(jogoDaoMock.emAndamento()).thenReturn(Arrays.asList(jogoAntigo));
		doThrow(new RuntimeException("Erro ao atualizar jogo na base")).when(jogoDaoMock).atualiza(jogoAntigo);

		FinalizaJogo finalizador = new FinalizaJogo(jogoDaoMock, vencedorDaoMock, enviaSmsMock);

		assertThrows(RuntimeException.class, () -> finalizador.finaliza());

		// Em hipótese alguma deve interagir com o VencedorDao ou EnviaSms se o JogoDao falhou
		verifyNoInteractions(vencedorDaoMock);
		verifyNoInteractions(enviaSmsMock);
	}

	@Test
	public void naoDeveEnviarSmsQuandoFalharAoSalvarVencedor() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);

		Participante p1 = new Participante("Maria");
		Jogo jogoAntigo = new CriadorDeJogo()
				.para("Caça moedas")
				.naData(antiga)
				.resultado(p1, 200.0)
				.constroi();

		JogoDaoFalso jogoDaoMock = mock(JogoDaoFalso.class);
		VencedorDaoFalso vencedorDaoMock = mock(VencedorDaoFalso.class);
		EnviaSms enviaSmsMock = mock(EnviaSms.class);

		when(jogoDaoMock.emAndamento()).thenReturn(Arrays.asList(jogoAntigo));
		doThrow(new RuntimeException("Erro ao salvar vencedor na base")).when(vencedorDaoMock).salva(p1);

		FinalizaJogo finalizador = new FinalizaJogo(jogoDaoMock, vencedorDaoMock, enviaSmsMock);

		assertThrows(RuntimeException.class, () -> finalizador.finaliza());

		verify(jogoDaoMock, times(1)).atualiza(jogoAntigo);
		verifyNoInteractions(enviaSmsMock);
	}
}