package infra;

import java.util.List;
import br.com.valueprojects.mock_spring.model.Jogo;

public interface JogoDaoFalso {
	void salva(Jogo jogo);
	List<Jogo> finalizados();
	List<Jogo> emAndamento();
	void atualiza(Jogo jogo);
}