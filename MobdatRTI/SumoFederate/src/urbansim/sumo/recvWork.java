
package urbansim.sumo;


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
	//Receive the string from the buffer
	String receiveText;
	//used to store the elements that are in the string separeted by the terminatpr
	String splitArray[];
	//define the size of readChar[]
	int readCharSize ;
	//define the Terminator of the elements 
	String suffix;
	//store the number of characters that the function Bufferreade.read() has read
	int byteRead;
	
	public recvWork(BlockingQueue q,InputStreamReader red) throws IOException{
		this.inputStreamReader = red;
		this.Inqueue = q;
		bufferReader = new BufferedReader(red);
		suffix = "\r\n\r\n";
		receiveText = "";
		readCharSize =1024;	
	}
	
	private void log( String message ){
		System.out.println( "receive work: " + message );
	}


	@Override
	//A thread will run here receiving messages from the socket
	//This piece of code is responsible to receive all information from the socket and guarantee that break information is treated correctly
	public void run() { 
		
		//indicate if the terminator is the last element in the string 
		int flag;
		int i;
		try {
			while(true){	
				//used to read from the information from the buffer and transform it in a string 
				char readChar[] = new char[readCharSize];	
				//Wait until there is something to read from the buffer 
				if((byteRead =( bufferReader.read(readChar,0,readCharSize))) != -1)
					{
					//receive the new information from the buffer
					//concatenate the new string if there was something something in receive text 
					receiveText =  receiveText + new String(readChar, 0, byteRead);
					
					//indicate if the terminator is the last element in the string 
					flag=0;
					//check if the string ends in a terminator 
					flag = (receiveText.endsWith(suffix))? 1:0;
					
					//If there is a terminator in the string 
					//means that there is elements to put in the queue to be processed
					if(receiveText.contains(suffix)){
						
						splitArray = receiveText.split(suffix);	
						i=0;
						//for each element put in the queue
						do{
							receiveText  = splitArray[i];
							Inqueue.put(receiveText);
							i++;
						}while( i< splitArray.length);
						
						// if the flag is on, means that the last element is no complete 
						//so insert this last element back in string	
						receiveText = (flag==1)? "":splitArray[i-1]; 
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

