package merkleClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MerkleValidityRequest {

	/**
	 * IP address of the authority
	 * */
	private final String authIPAddr;
	/**
	 * Port number of the authority
	 * */
	private final int  authPort;
	/**
	 * Hash value of the merkle tree root. 
	 * Known before-hand.
	 * */
	private final String mRoot;
	/**
	 * List of transactions this client wants to verify 
	 * the existence of.
	 * */
	private List<String> mRequests;
	
	/**
	 * Sole constructor of this class - marked private.
	 * */
	private MerkleValidityRequest(Builder b){
		this.authIPAddr = b.authIPAddr;
		this.authPort = b.authPort;
		this.mRoot = b.mRoot;
		this.mRequests = b.mRequest;
	}
	
	/**
	 * <p>Method implementing the communication protocol between the client and the authority.</p>
	 * <p>The steps involved are as follows:</p>
	 * 		<p>0. Opens a connection with the authority</p>
	 * 	<p>For each transaction the client does the following:</p>
	 * 		<p>1.: asks for a validityProof for the current transaction</p>
	 * 		<p>2.: listens for a list of hashes which constitute the merkle nodes contents</p>
	 * 	<p>Uses the utility method {@link #isTransactionValid(String, String, List<String>) isTransactionValid} </p>
	 * 	<p>method to check whether the current transaction is valid or not.</p>
	 * */
	public Map<Boolean, List<String>> checkWhichTransactionValid() throws IOException {
		Map<Boolean, List<String>> validityRequest = new HashMap<>();

		validityRequest.put(true, new ArrayList<>());
		validityRequest.put(false, new ArrayList<>());

		Socket mySocket = null;
		PrintWriter out = null;
		BufferedReader in = null;

		try {
			mySocket = new Socket(authIPAddr, authPort);
			out = new PrintWriter(mySocket.getOutputStream(), true);    //oggetto che uso per comunicare con il mio server
			in = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));

			for(String actualTransaction: mRequests){

				//mando la mia transazione da verificare al server
				out.println(actualTransaction);

				//Mi salvo in una Lista di stringhe l'hash che viene tornato dal server
				List<String> hashServer = new ArrayList<>();

				//leggo dal bufferReader riempito dal server tutte quanti gli hash con un ciclo e li salvo in una lista
				String actualLine = in.readLine();

				List<String> hashMerkle = new ArrayList<>();

				while(actualLine != null){
					hashMerkle.add(actualLine);
					actualLine = in.readLine();
				}

				//controllo la transazione attuale
				if(isTransactionValid(actualTransaction, hashServer))
					validityRequest.get(true).add(actualTransaction);
				else
					validityRequest.get(false).add(actualTransaction);
			}
		}
		catch (UnknownHostException a) {			//errore host non trovato
			a.printStackTrace();
		}
		catch (IOException ex) {				//gestisco caso eccezione generica
			ex.printStackTrace();
		}

		mySocket.close();	//chiudo la connessione con il server gestita tramite socket
		out.close();		//chiudo il printWriter usato per comunicare con l'host
		in.close();			//chiudo il buffer usato per salvare tutti gli hash

		return validityRequest;        //torno la mia Map con le transioni valide
	}
	
	/**
	 * 	Checks whether a transaction 'merkleTx' is part of the merkle tree.
	 * 
	 *  @param merkleTx String: the transaction we want to validate
	 *  @param merkleNodes String: the hash codes of the merkle nodes required to compute 
	 *  the merkle root
	 *  
	 *  @return: boolean value indicating whether this transaction was validated or not.
	 * */
	private boolean isTransactionValid(String merkleTx, List<String> merkleNodes) {

		String mySingleHash = new String(HashUtil.md5Java(merkleTx));		//do in pasto la mia stringa al metodo md5
		for(String s: mRequests)
			mySingleHash.concat(HashUtil.md5Java(s));        //concateno tutte le stringhe ottenute
		return mySingleHash.equals(mRoot);			//confronto l'hash ottenuto con mRoot
	}

	/**
	 * Builder for the MerkleValidityRequest class. 
	 * */
	public static class Builder {
		private String authIPAddr;
		private int authPort;
		private String mRoot;
		private List<String> mRequest;	
		
		public Builder(String authorityIPAddr, int authorityPort, String merkleRoot) {
			this.authIPAddr = authorityIPAddr;
			this.authPort = authorityPort;
			this.mRoot = merkleRoot;
			mRequest = new ArrayList<>();
		}
				
		public Builder addMerkleValidityCheck(String merkleHash) {
			mRequest.add(merkleHash);
			return this;
		}
		
		public MerkleValidityRequest build() {
			return new MerkleValidityRequest(this);
		}
	}
}