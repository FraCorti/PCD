package pcd2018.exe2;


import pcd2018.exe2.DiffieHellmanUtils;
import java.util.ArrayList;
import java.util.List;
import java.lang.Iterable;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.concurrent.*;
import java.util.function.Supplier;


/**
 * Classe da completare per l'esercizio 2.
 */
public class DiffieHellman {

  /**
   * Limite massimo dei valori segreti da cercare
   */
  private static final int LIMIT = 65536;

  private final long p;
  private final long g;

  private static List<Integer> risultati;   // lista che uso per salvare i risultati di tutti i possibili a e b


  static void add(int a, int b){
	risultati.add(a);
	risultati.add(b);
  }

  static void clearRisultati(){
  	risultati.clear();
  }

  static List<Integer> getRisultati(){
  	return risultati;
  }

  public DiffieHellman(long p, long g) {
    this.p = p;
    this.g = g;
    this.risultati = new ArrayList<Integer>();
  }


  public class MyDiffieHellmanThread implements Runnable{

	  private int begin;  		// primo valore da trattare
	  private int end;	  		// ultimo valore da trattare
	  private long publicB; 	// valore pubblico di b

	  Thread myThread;			//Thread che istanzio quando chiamo la mia sottoclasse per parallelizzare i calcoli
	  private List<Long> aLista;

	  public MyDiffieHellmanThread(int begin, int end, List<Long> aLista, long publicB){
		  this.begin = begin;
		  this.end = end;
		  this.aLista =  aLista;
		  this.publicB = publicB;

		  myThread = new Thread(this);
		  myThread.start();			//inizio il mio Thread
	  }

	  // @ Override del metodo run
	  public void run() {
		  for (int i = begin; i < end; ++i) {
			  long valoreModPowB = DiffieHellmanUtils.modPow(publicB, i, p);
			  for (int y = 0; y < LIMIT; ++y) {
				  if (aLista.get(y) == valoreModPowB) {   // se trovo due valori uguali allora i seguenti valori di a e b sono possibili
					  DiffieHellman.add(i,y);			  // salvo i valori nella mia lista di risultati
				  }
			  }
		  }
	  }
  }

  /**
   * Metodo da completare
   *
   * @param publicA valore di A
   * @param publicB valore di B
   * @return tutte le coppie di possibili segreti a,b
   *
   *  I valori di a e b sono compresi tra 0 e LIMIT
   */
  public List<Integer> crack(long publicA, long publicB) {

    int coresNumber = Runtime.getRuntime().availableProcessors();  // trovo il numero di core nel mio computer
    int partizionamento = LIMIT / coresNumber;		// porzione di numeri da trattare per ogni core

	List<Long> valoriA = IntStream.range(0, LIMIT).mapToObj(i->DiffieHellmanUtils.modPow(publicA, i, p)).collect(Collectors.toList());
    List<MyDiffieHellmanThread> myThreads = new ArrayList<MyDiffieHellmanThread>();


    for(int i=0; i< coresNumber; ++i)
		myThreads.add(new MyDiffieHellmanThread(partizionamento * i, partizionamento * (i + 1), valoriA, publicB));

    myThreads.forEach(currentThread ->{
		try {
			currentThread.myThread.interrupt();  	// interrompo i Thread che ho istanziato per i miei calcoli
		}
		catch (SecurityException e) {
			e.printStackTrace();
		}
	});

    List<Integer> risultatiOttenuti = DiffieHellman.getRisultati();
    risultati.clear();													// "pulisco" la Lista per eventuali operazioni di "crack"future
    return risultatiOttenuti;
  }
}
