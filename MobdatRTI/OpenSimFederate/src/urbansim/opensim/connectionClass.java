package urbansim.opensim;

// ********************************* THe methods are********************************
//		initServer() --- initiate the server
//		initClient() --- initiate the client
//		recvData() --- receive a json object
//		sendData(jsonObj) --- send a json object
//
// *********************************************************************************

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;


public class connectionClass {
	private ServerSocket server;
	private Socket socket;
	private Socket socket2;
	private Socket client2;
	private int port;
	private String host;
	protected BlockingQueue sendQueue;
	protected BlockingQueue recevQueue;
	private OutputStreamWriter out ;
	private InputStreamReader in;
	
		
	private void log( String message )
	{
		System.out.println( "Class Connection: " + message );
	}
	
	
	// Constructor with host, port, queue and choose (server or client)
	public connectionClass(String host,int port){
		this.host = host;
		this.port = port;
		this.recevQueue = new ArrayBlockingQueue(1024);
		this.sendQueue = new ArrayBlockingQueue(1024);
		initServer();	
	}
	
	
	
	
	//Create the socket (server) and Thread (Consumer)
	public void initServer(){
		try{
			server = new ServerSocket(port);
			log("Socket in port "+port);
			//return server;

			//while(true){
				try{
				// Keep the loop to accept the connection and make a new Tread
					socket = server.accept();
					this.out= new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
					this.in = new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8);
					//Initializes Consumer
					recvWork recvWork = new recvWork(recevQueue,in);
					//Thread for the Consumer
					Thread t = new Thread(recvWork);
					t.start();
					// Initializes Producer
					sendWork send = new sendWork(sendQueue,out); 
					//Thread to sendWork class
					Thread s = new Thread(send);
					s.start();
					//return client;
				}catch(IOException i ){
					log("Client not accepted"+i);
					System.exit(-1);
				}
			//}	
		}catch (IOException e){
			log("could not listene on port 8080");
			System.exit(-1);
			//return null;
		}	
	}
	

	public BlockingQueue getSendQueue() {
		return sendQueue;
	}


	public BlockingQueue getRecevQueue() {
		return recevQueue;
	}
	

}
