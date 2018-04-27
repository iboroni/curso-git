package br.com.caelum.leilao.servico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import br.com.caelum.leilao.builder.CriadorDeLeilao;
import br.com.caelum.leilao.dominio.Leilao;
import br.com.caelum.leilao.dominio.Usuario;
import br.com.caelum.leilao.infra.dao.EnviadorDeEmail;
import br.com.caelum.leilao.infra.dao.LeilaoDao;
import br.com.caelum.leilao.infra.dao.LeilaoDaoFalso;
import br.com.caelum.leilao.infra.dao.RepositorioDeLeiloes;


import static org.mockito.Mockito.*;

public class EncerradorDeLeilaoTest {
	
	private EnviadorDeEmail carteiroFalso;
	private EncerradorDeLeilao encerrador;
	private RepositorioDeLeiloes daoFalso;
	
	@Before
	//vai ser invocado antes de cada teste
	public void setUp() {
		this.carteiroFalso = mock(EnviadorDeEmail.class);
		this.daoFalso = mock(RepositorioDeLeiloes.class);
		this.encerrador = new EncerradorDeLeilao(this.daoFalso, this.carteiroFalso);
	}
	
	@Test
	public void deveEncerrarLeiloesQueComecaramUmaSemanaAtras() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de Plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(antiga).constroi();
		
		// UTILIZANDO O MOCKITO			
		List<Leilao> leiloesCorrentes = Arrays.asList(leilao1, leilao2);
		// ensinando o mock a reagir da maneira que esperamos
		when(daoFalso.correntes()).thenReturn(leiloesCorrentes); // correntes é a lista dos leilões encerrados
		// este when diz que quando chamarmos o método correntes() do dao, ele retornará a lista de leilões antigos!
		
		encerrador.encerra();
		
		assertEquals(2, encerrador.getTotalEncerrados());
		assertTrue(leilao1.isEncerrado());
		assertTrue(leilao2.isEncerrado());
		
		}
	
	@Test
	public void entendeLeiloesQueComecaramHaMenosDeUmaSemana() {
		Calendar data = Calendar.getInstance();
		data.add(Calendar.DAY_OF_MONTH, - 1);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de Plasma").naData(data).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(data).constroi();		
		
		List<Leilao> leiloesEncerrados = Arrays.asList(leilao1, leilao2);
		
		
		when(daoFalso.correntes()).thenReturn(leiloesEncerrados);
	
		encerrador.encerra();
		
		assertEquals(0, encerrador.getTotalEncerrados());
		assertFalse(leilao1.isEncerrado());
		assertFalse(leilao2.isEncerrado());
		
		for(Leilao l : leiloesEncerrados) {
			verify(daoFalso, never()).atualiza(l);
		}		
	}
	
	@Test
	public void entendeQuandoNaoExistemLeiloes() {
		/*Calendar data = Calendar.getInstance();
		data.add(Calendar.DAY_OF_MONTH, - 1);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de Plasma").naData(data).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("Geladeira").naData(data).constroi();		
		*/
		List<Leilao> leiloesEncerrados = Arrays.asList();
		
		when(daoFalso.correntes()).thenReturn(leiloesEncerrados);		
		encerrador.encerra();
		
		assertEquals(0, encerrador.getTotalEncerrados());
		
	}
	
	@Test
	public void deveAtualizarLeiloesEncerrados() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de Plasma").naData(antiga).constroi();
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1));		
		encerrador.encerra();
		verify(daoFalso, times(1)).atualiza(leilao1);
	}
	
	@Test
	public void deveEnviarLeilaoPorEmail() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999, 1, 20);		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de Plasma").naData(antiga).constroi();
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1));		
		encerrador.encerra();
		
		InOrder inOrder = inOrder(daoFalso, carteiroFalso);
		inOrder.verify(daoFalso, times(1)).atualiza(leilao1);
		inOrder.verify(carteiroFalso, times(1)).envia(leilao1);
	}
	
	@Test
	public void deveContinuarAExecuçãoMesmoQuandoDaoFalha() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999,  1, 20);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("TV de lcd").naData(antiga).constroi();
		
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));		
		doThrow(new RuntimeException()).when(daoFalso).atualiza(leilao1);
		// lê-se: jogue runtime exception quando o daoFalso atualizar o leilao1
		encerrador.encerra();
		
		verify(daoFalso).atualiza(leilao2);
		verify(carteiroFalso).envia(leilao2);
		
		verify(carteiroFalso, times(0)).envia(leilao1);

	}
	
	@Test
	public void deveContinuarAExecuçãoMesmoQuandoCarteiroFalha() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999,  1, 20);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("TV de lcd").naData(antiga).constroi();
		
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));		
		doThrow(new RuntimeException()).when(carteiroFalso).envia(leilao1);
		// lê-se: jogue runtime exception quando o daoFalso atualizar o leilao1
		encerrador.encerra();
		
		verify(daoFalso).atualiza(leilao2);
		verify(carteiroFalso).envia(leilao2);			

	}
	
	@Test
	public void deveEntenderQuandoTodosOsLeiloesFalham() {
		Calendar antiga = Calendar.getInstance();
		antiga.set(1999,  1, 20);
		
		Leilao leilao1 = new CriadorDeLeilao().para("TV de plasma").naData(antiga).constroi();
		Leilao leilao2 = new CriadorDeLeilao().para("TV de lcd").naData(antiga).constroi();
		
		when(daoFalso.correntes()).thenReturn(Arrays.asList(leilao1, leilao2));		
		doThrow(new RuntimeException()).when(daoFalso).atualiza(any(Leilao.class)); //joga exceção para qualquer leilao
		
		encerrador.encerra();		
		verify(carteiroFalso, never()).envia(any(Leilao.class));				

	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
