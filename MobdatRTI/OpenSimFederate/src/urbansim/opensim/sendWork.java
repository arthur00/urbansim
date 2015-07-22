package urbansim.opensim;
// Send things from the Queue

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class sendWork implements Runnable {

	private BlockingQueue outQueue;
	private OutputStreamWriter outputStreamWriter;
	
	String text;
	
	public sendWork(BlockingQueue q,OutputStreamWriter input) throws IOException{
		this.outputStreamWriter = input;
		this.outQueue = q;		
	}
	
	private void log( String message )
	{
		System.out.println( "receive work: " + message );
	}
	
	
	@Override
	public void run() {
		
		String fromClient;
			while(true){
				
				try {
					if((fromClient = outQueue.take().toString()) != null){
						
						outputStreamWriter.write(fromClient+"\r\n");
						outputStreamWriter.flush();
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
				
			}
		
	}
	
	

