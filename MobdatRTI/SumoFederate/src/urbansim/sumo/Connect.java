package urbansim.sumo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;


	
	class Connect extends Thread implements Runnable{

	    private Socket          sock;
	    private InputStream     in;
	    private OutputStream    out;
	    BufferedReader bufIn;
	    BufferedWriter bufOut;
	    public BlockingQueue pilha;

	    Connect(InputStreamReader in ,OutputStreamWriter out,Socket sock ) {
	    	
	    	 pilha = new ArrayBlockingQueue(1024) ;
	    	 bufIn = new BufferedReader( in );
	    	 bufOut = new BufferedWriter(  out  );
	    	 this.sock = sock;
	    }

	    
	    public void InsertDaque(String element){
	    	try {
				pilha.put(element);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    //Echos whatever the client sends to it
	    public void run() {
	    	String msg = null;
	        while ( true ) {
	        		
	        		try {
						msg = pilha.take().toString();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
	        		try {
	        			if (msg != null)
	        			{
							bufOut.write( msg );
							bufOut.newLine(); //HERE!!!!!!
				            bufOut.flush();
				            msg = null;
	        			}
	        			
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	        		

	        }
	    }

	}


