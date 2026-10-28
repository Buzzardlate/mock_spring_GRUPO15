package br.com.valueprojects.mock_spring.model;

import java.util.Calendar;
import java.util.List;
import infra.JogoDaoFalso;
import infra.VencedorDaoFalso;

public class FinalizaJogo {

	private int total = 0;
	private final JogoDaoFalso dao;
	private final VencedorDaoFalso vencedorDao;
	private final EnviaSms enviaSms;

	public FinalizaJogo(JogoDaoFalso dao, VencedorDaoFalso vencedorDao, EnviaSms enviaSms) {
		this.dao = dao;
		this.vencedorDao = vencedorDao;
		this.enviaSms = enviaSms;
	}

	public void finaliza() {
		List<Jogo> todosJogosEmAndamento = dao.emAndamento();

		for (Jogo jogo : todosJogosEmAndamento) {
			if (iniciouSemanaAnterior(jogo)) {
				jogo.finaliza();
				total++;

				// 1. Salva o jogo finalizado na base
				dao.atualiza(jogo);

				// 2. Identifica o vencedor e salva na base
				List<Resultado> resultados = jogo.getResultados();
				Participante vencedor = resultados.get(resultados.size() - 1).getParticipante();
				vencedorDao.salva(vencedor);

				// 3. Apenas depois de salvar jogo e vencedor, envia o SMS
				Sms sms = new Sms("Parabéns Vencedor!");
				enviaSms.envia(sms, vencedor);
			}
		}
	}

	private boolean iniciouSemanaAnterior(Jogo jogo) {
		return diasEntre(jogo.getData(), Calendar.getInstance()) >= 7;
	}

	private int diasEntre(Calendar inicio, Calendar fim) {
		Calendar data = (Calendar) inicio.clone();
		int diasNoIntervalo = 0;
		while (data.before(fim)) {
			data.add(Calendar.DAY_OF_MONTH, 1);
			diasNoIntervalo++;
		}
		return diasNoIntervalo;
	}

	public int getTotalFinalizados() {
		return total;
	}
}