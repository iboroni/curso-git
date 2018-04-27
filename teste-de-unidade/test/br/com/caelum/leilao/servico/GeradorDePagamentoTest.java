package br.com.caelum.leilao.servico;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.dominio.Pagamento;
import br.com.caelum.leilao.dominio.Usuario;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;
import br.com.caelum.leilao.infra.dao.RepositorioDePagamentos;
import br.com.caelum.leilao.infra.relogio.Relogio;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Calendar;

public class GeradorDePagamentoTest {
	@Test	
	public void deveGerarPagamentoParaUmLeilaoEncerrado() {
		RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
		RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);
		Avaliador avaliador = new Avaliador();
		
		Leilao leilao = new CriadorDeLeilao().para("Playstation 4")
				.lance(new Usuario("Jose"),2000.0)
				.lance(new Usuario("Maria"),2500.0)
				.constroi();
		
		
		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, avaliador);
		gerador.gera();
		assertEquals(2500.0, avaliador.getMaiorLance(), 0.00001);
		
		/*
		 * Para recuperarmos uma classe, usamos a classe ArgumentCaptor do Mockito
		 */
		
		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salvar(argumento.capture());
		
		Pagamento pagamentoGerado = argumento.getValue();
		assertEquals(2500.0, pagamentoGerado.getValor(), 0.00001);
		}
	
	@Test
	public void naoDeveGerarPagamentoDeSabado() {
		
		RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
		RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);
		Relogio relogio = mock(Relogio.class);
		
		Avaliador avaliador = new Avaliador();
		
		Leilao leilao = new CriadorDeLeilao()
				.para("Playstation 4")
				.lance(new Usuario("Jose"),2000.0)
				.lance(new Usuario("Maria"),2500.0)
				.constroi(); 
		
		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		
		Calendar sabado = Calendar.getInstance();
		sabado.set(2012, Calendar.APRIL, 7);
		
		when(relogio.hoje()).thenReturn(sabado);
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, avaliador, relogio);
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salvar(argumento.capture());
		
		Pagamento pagamentoGerado = argumento.getValue();
		
		// colocamos segunda-feira, porque mockamos a classe relogio para entender o dia como sábado, se for sábado o pagamento cai na segunda
		assertEquals(Calendar.MONDAY, pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK));
		// colocamos 9, porque se for sábado, somamos dois dias, 7 + 2 = 9
		assertEquals(9, pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH));
	}
	
	@Test
	public void naoDeveGerarPagamentoDeDomingo() {
		
		RepositorioDeLeiloes leiloes = mock(RepositorioDeLeiloes.class);
		RepositorioDePagamentos pagamentos = mock(RepositorioDePagamentos.class);
		Relogio relogio = mock(Relogio.class);
		
		Avaliador avaliador = new Avaliador();
		
		Leilao leilao = new CriadorDeLeilao()
				.para("Playstation 4")
				.lance(new Usuario("Jose"),2000.0)
				.lance(new Usuario("Maria"),2500.0)
				.constroi(); 
		
		when(leiloes.encerrados()).thenReturn(Arrays.asList(leilao));
		
		Calendar domingo = Calendar.getInstance();
		domingo.set(2012, Calendar.APRIL, 8);
		
		when(relogio.hoje()).thenReturn(domingo);
		GeradorDePagamento gerador = new GeradorDePagamento(leiloes, pagamentos, avaliador, relogio);
		gerador.gera();
		
		ArgumentCaptor<Pagamento> argumento = ArgumentCaptor.forClass(Pagamento.class);
		verify(pagamentos).salvar(argumento.capture());
		
		Pagamento pagamentoGerado = argumento.getValue();
		
		// colocamos segunda-feira, porque mockamos a classe relogio para entender o dia como sábado, se for sábado o pagamento cai na segunda
		assertEquals(Calendar.MONDAY, pagamentoGerado.getData().get(Calendar.DAY_OF_WEEK));
		// colocamos 9, porque se for sábado, somamos dois dias, 7 + 2 = 9
		assertEquals(9, pagamentoGerado.getData().get(Calendar.DAY_OF_MONTH));
	}
}
