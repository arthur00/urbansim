package urbansim.opensim;

//Put things on the Queue


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

public class recvWork implements Runnable{
	private BlockingQueue Inqueue;
	private InputStreamReader inputStreamReader;
	BufferedReader bufferReader;
	String receiveText;
	String splitArray[];
	char readChar[];
	int readCharSize ;
	String suffix;
	int byteRead;
	
	public recvWork(BlockingQueue q,InputStreamReader red) throws IOException{
		this.inputStreamReader = red;
		this.Inqueue = q;
		bufferReader = new BufferedReader(red);
		
		suffix = "\r\n\r\n";
		receiveText = "";
		readCharSize =128;

		
	}
	
	private void log( String message )
	{
		System.out.println( "receive work: " + message );
	}

	@Override
	public void run() { 
		log("Run recv");

		try {
			while(true){
				log("loop");
				
				char readChar[] = new char[readCharSize];			
				if((byteRead =( bufferReader.read(readChar,0,readCharSize))) != -1)
					{
					
					receiveText =  receiveText + new String(readChar, 0, byteRead);
					int flag=0;
					
					flag = receiveText.endsWith(suffix) ? 1 :0;
					
					if(receiveText.contains(suffix)){
						
						splitArray = receiveText.split(suffix);	

						int i=0;
						do{
							receiveText  = splitArray[i];
							Inqueue.put(receiveText);
							i++;
						}while( i< splitArray.length);
						
						if(flag ==1){receiveText = "";}						
						else{receiveText = splitArray[i-1];}
						
						}				
				}
						
				
			
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
		
	}
}
