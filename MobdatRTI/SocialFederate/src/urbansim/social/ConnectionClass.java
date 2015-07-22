package urbansim.social;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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


public class ConnectionClass {
	private ServerSocket server;
	private Socket socket;
	private Socket client2;
	private int port;
	private String host;
	private BlockingQueue inQueue;
	private BlockingQueue outQueue;
	private OutputStreamWriter out ;
	private InputStreamReader in;
	
		
	private void log( String message )
	{
		System.out.println( "Class Connection: " + message );
	}
	
	
	
	// Constructor with host, port, queue and choose (server or client)
	public ConnectionClass(String host,int port){
		this.host = host;
		this.port = port;

		this.inQueue = new ArrayBlockingQueue(1024);
		this.outQueue = new ArrayBlockingQueue(1024);
		initServer();

		
	}
	
	//Create the socket
	public void initServer(){
		try{
			server = new ServerSocket(port);
			log("Socket in port: " + port);
			//return server;

			
				try{
				// Keep the loop to accept the connection and make a new Tread
				
				socket = server.accept();
				this.out= new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
				this.in = new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8);
				
				ReceiveWork rec = new ReceiveWork(inQueue,in);
				Thread thread1 = new Thread(rec);
				thread1.start();
				
				SendWork send = new SendWork(outQueue,out);
				Thread thread2 = new Thread(send);
				thread2.start();
				
				
				socket.setKeepAlive(true);
				log("Connected succeed345");
				
				
				}catch(IOException i ){
					log("Client not accepted"+i);
					System.exit(-1);
				}
		
			
			
		}catch (IOException e){
			log("could not listene on port 8080");
			System.exit(-1);
			//return null;
		}	
	}
	

	
	public BlockingQueue GetInQueue(){
		return inQueue;
	}
	public BlockingQueue GetOutQueue(){
		return outQueue;
	}
	
	public Socket GetSocket(){
		return socket;
	}
}
