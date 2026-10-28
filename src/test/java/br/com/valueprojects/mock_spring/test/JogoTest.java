package br.com.valueprojects.mock_spring.test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Calendar;
import org.junit.jupiter.api.Test;
import br.com.valueprojects.mock_spring.builder.CriadorDeJogo;
import br.com.valueprojects.mock_spring.controller.JogoController;
import br.com.valueprojects.mock_spring.model.*;

public class JogoTest {

	@Test
	public void deveTerJogoComUnicoParticipante() {
		Jogo jogo = new Jogo("Jogo de corrida");
		assertEquals(0, jogo.getResultados().size());

		Participante leonardo = new Participante(1, "Leonardo");
		jogo.anota(new Resultado(leonardo, 150));

		assertEquals(1, jogo.getResultados().size());
		assertEquals(150.0, jogo.getResultados().get(0).getMetrica(), 0.00001);
		assertEquals(1, leonardo.getId());
		assertEquals("Leonardo", leonardo.getNome());
	}

	@Test
	public void deveTerVariosResultadosECobrirBuilder() {
		CriadorDeJogo criador = new CriadorDeJogo();
		Jogo jogo = criador
				.para("Cata moedas")
				.comId(10)
				.resultado(new Participante("Nelson"), 150.0)
				.resultado(new Participante("Pedro"), 200.0)
				.finaliza()
				.constroi();

		assertEquals(2, jogo.getResultados().size());
		assertEquals(10, criador.getId());
		assertEquals(10, jogo.getId());
		assertEquals("Cata moedas", jogo.getDescricao());
		assertTrue(criador.isFinalizado());
	}

	@Test
	public void naoDeveAceitarDoisResultadosSeguidosNemMaisDeCincoDoMesmoParticipante() {
		Jogo jogo = new Jogo("Caça peças");
		Participante leonardo = new Participante(1, "Leonardo");
		Participante maria = new Participante(2, "Maria");

		// Tenta dois seguidos (o segundo é ignorado)
		jogo.anota(new Resultado(leonardo, 500.0));
		jogo.anota(new Resultado(leonardo, 600.0));
		assertEquals(1, jogo.getResultados().size());

		// Intercala até passar do limite de 5 por participante
		for (int i = 0; i < 5; i++) {
			jogo.anota(new Resultado(maria, 100.0 + i));
			jogo.anota(new Resultado(leonardo, 200.0 + i));
		}
		assertEquals(10, jogo.getResultados().size());
	}

	@Test
	public void deveCompararParticipantesEPagamento() {
		Participante p1 = new Participante(1, "Joao");
		assertTrue(p1.equals(p1));
		assertFalse(p1.equals(null));
		assertFalse(p1.equals("OutroTipo"));
		assertFalse(p1.equals(new Participante(2, "Joao")));
		assertFalse(new Participante(1, null).equals(p1));
		assertTrue(new Participante(1, null).equals(new Participante(1, null)));
		assertFalse(p1.equals(new Participante(1, "Pedro")));
		assertTrue(p1.equals(new Participante(1, "Joao")));

		Calendar hoje = Calendar.getInstance();
		Pagamento pag = new Pagamento(100.0, hoje);
		assertEquals(100.0, pag.getValor());
		assertEquals(hoje, pag.getData());
	}

	@Test
	public void deveCriarEJulgarJogoPeloController() {
		JogoController controller = new JogoController();
		Jogo jogo = controller.criarJogo("Campeonato");
		jogo.anota(new Resultado(new Participante("A"), 200.0));
		jogo.anota(new Resultado(new Participante("B"), 100.0));

		assertEquals(1, controller.listarJogos().size());
		assertTrue(controller.julgarJogo(0).contains("200.0"));
	}
}