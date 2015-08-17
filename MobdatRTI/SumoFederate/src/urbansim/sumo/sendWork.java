
package urbansim.sumo;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class sendWork implements Runnable{
	private BlockingQueue outQueue;
	private OutputStreamWriter outputStreamWriter;
	String suffix;


	public sendWork(BlockingQueue q,OutputStreamWriter input) throws IOException{
		this.outputStreamWriter = input;
		this.outQueue = q;		
		suffix ="\r\n\r\n";
	}
	
	private void log( String message ){
		System.out.println( "receive work: " + message );
	}

	@Override
	public void run() { 
		//receive the string from the queue
		String fromClient;
			while(true){
				
				try {
					//wait until there is sothing to take from the queue
					if((fromClient = outQueue.take().toString()) != null){
						//write 
						outputStreamWriter.write(fromClient+suffix);
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
//}

