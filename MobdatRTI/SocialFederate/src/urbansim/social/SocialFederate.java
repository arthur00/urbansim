package urbansim.social;

import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.RtiFactoryFactory;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAASCIIstring;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.json.*;
import org.portico.lrc.services.object.msg.SendInteraction;

public class SocialFederate {
	//----------------------------------------------------------
	//                    STATIC VARIABLES
	//----------------------------------------------------------
	/** The number of times we will update our attributes and send an interaction */
	public static final int ITERATIONS = 200000;

	/** The sync point all federates will sync up on before starting */
	public static final String READY_TO_RUN = "ReadyToRun";
	



	//----------------------------------------------------------
	//                   INSTANCE VARIABLES
	//----------------------------------------------------------
	private RTIambassador rtiamb; // Eh como se fosse o controlador do Social Federate
	private SocialFederateAmbassador fedamb;  // created when we connect   //Eh o responsavel por receber os dados do RTI.
	private HLAfloat64TimeFactory timeFactory; // set when we join
	protected EncoderFactory encoderFactory;     // set when we join
	protected PositionCoder _positionRecordCoder;
	public final String host = "localhost";
	public final int socketPort = 8081;
	public Socket socketElement;
	BlockingQueue receiveQueue;
	BlockingQueue sendQueue;
	ConnectionClass connection;
	//----------------------------------------------------------
	//                      CONSTRUCTORS
	//----------------------------------------------------------

	//----------------------------------------------------------
	//                    INSTANCE METHODS
	//----------------------------------------------------------
	/**
	 * This is just a helper method to make sure all logging it output in the same form
	 */
	private void log( String message )
	{
		System.out.println( "SumoFederate   : " + message );
	}

	/**
	 * This method will block until the user presses enter
	 */
	private void waitForUser()
	{
		log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
		BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
		try
		{
			reader.readLine();
		}
		catch( Exception e )
		{
			log( "Error while waiting for user input: " + e.getMessage() );
			e.printStackTrace();
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	////////////////////////// Main Simulation Method /////////////////////////
	///////////////////////////////////////////////////////////////////////////
	/**
	 * This is the main simulation loop. It can be thought of as the main method of
	 * the federate. For a description of the basic flow of this federate, see the
	 * class level comments
	 */
	public void runFederate( String federateName ) throws Exception
	{
		/////////////////////////////////////////////////
		// 1 & 2. create the RTIambassador and Connect //
		/////////////////////////////////////////////////
		log( "Creating RTIambassador" );
		rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
		encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
		_positionRecordCoder = new PositionCoder(encoderFactory);
		// connect
		log( "Connecting..." );
		fedamb = new SocialFederateAmbassador( this, encoderFactory);
		rtiamb.connect( fedamb, CallbackModel.HLA_IMMEDIATE );

		//////////////////////////////
		// 3. create the federation //
		//////////////////////////////
		log( "Creating Federation..." );
		// We attempt to create a new federation with the first three of the
		// restaurant FOM modules covering processes, food and drink
		try
		{
			URL[] modules = new URL[]{
			    (new File("fom/UrbanSim.xml")).toURI().toURL(),
			};
			
			rtiamb.createFederationExecution( "UrbanSimFederation", modules );
			log( "Created Federation" );
		}
		catch( FederationExecutionAlreadyExists exists )
		{
			log( "Didn't create federation, it already existed" );
		}
		catch( MalformedURLException urle )
		{
			log( "Exception loading one of the FOM modules from disk: " + urle.getMessage() );
			urle.printStackTrace();
			return;
		}
		
		////////////////////////////
		// 4. join the federation //
		////////////////////////////
		URL[] joinModules = new URL[]{
		    (new File("fom/UrbanSim.xml")).toURI().toURL()
		};
		
		rtiamb.joinFederationExecution( federateName,            // name for the federate
		                                "SocialSimulation",   // federate type
		                                "UrbanSimFederation",     // name of federation
		                                joinModules );           // modules we want to add

		log( "Joined Federation as " + federateName );
		
		// cache the time factory for easy access
		this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();

		////////////////////////////////
		// 5. announce the sync point //
		////////////////////////////////
		// announce a sync point to get everyone on the same page. if the point
		// has already been registered, we'll get a callback saying it failed,
		// but we don't care about that, as long as someone registered it
		rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
		startConnection();
		fedamb.startConnection();
		
		// wait until the point is announced
		while( fedamb.isAnnounced == false || fedamb.isRegistered == false )
		{
			Thread.sleep(200);
		}

		// WAIT FOR USER TO KICK US OFF
		// So that there is time to add other federates, we will wait until the
		// user hits enter before proceeding. That was, you have time to start
		if (!fedamb.registrationFailed)
			waitForUser();
		
		///////////////////////////////////////////////////////
		// 6. achieve the point and wait for synchronization //
		///////////////////////////////////////////////////////
		// tell the RTI we are ready to move past the sync point and then wait
		// until the federation has synchronized on
		rtiamb.synchronizationPointAchieved( READY_TO_RUN );
		log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
		while( fedamb.isReadyToRun == false )
		{
			Thread.sleep(500);
		}

		/////////////////////////////
		// 7. enable time policies //
		/////////////////////////////
		// in this section we enable/disable all time policies
		// note that this step is optional!
		
		/*
		enableTimePolicy();
		log( "Time Policy Enabled" );
		*/
		
		//////////////////////////////
		// 8. publish and subscribe //
		//////////////////////////////
		// in this section we tell the RTI of all the data we are going to
		// produce, and all the data we want to know about
		publishAndSubscribe();
		log( "Published and Subscribed" );

		/////////////////////////////////////
		// 10. do the main simulation loop //
		/////////////////////////////////////
		// here is where we do the meat of our work. in each iteration, we will
		// update the attribute values of the object we registered, and will
		// send an interaction.
		
		// Wait for pub-sub to propagate.
		
		/*
		Thread.sleep(1000);
		for( int i = 0; i < ITERATIONS; i++ )
		{
			sendInteraction();
			advanceTime( 1.0 );
			Thread.sleep(2000);
		}
		*/
		
		//TODO RECEIVE COMMANDS (SOCKETS, JSON - SERVER).
        String fromClient;
        Map<String,Object> map = new HashMap<String,Object>(); //Cria um hashmap baseado em duas strings
        ObjectMapper mapper = new ObjectMapper(); 	// Cria o Objeto mapper
        
        boolean run = true;
        while(run) 
        {
        	System.out.println("While do Socket");
            if((fromClient = receiveQueue.take().toString()) != null)
            {
            	System.out.println("From Client: " + fromClient);
                try
                {            	
                	map = mapper.readValue(fromClient, new TypeReference<HashMap<String,Object>>(){}); //map recebera o HASHMAP criado a partir do JSON
                }
                catch(Exception e)
                {
                	e.printStackTrace();
                }
                
                if(map.containsValue("AddVehicle"))
                {
                	AddVehicleInteraction addVehicleInteraction = new AddVehicleInteraction();
                	//System.out.println(map);
                    ObjectMapper mapperIn = new ObjectMapper(); // Cria o Objeto mapper
                    try
                    {
                    	addVehicleInteraction = mapperIn.readValue(fromClient, AddVehicleInteraction.class);
                    }catch(JsonGenerationException e)
                    {
                    	e.printStackTrace();
                    }
                    //System.out.println("AEEEEEW!  " + addVehicleInteraction);  
                    sendInteraction(addVehicleInteraction);
                }
                //server.close();
                //run = false;
            }

         }
  
		

		////////////////////////////////////
		// 12. resign from the federation //
		////////////////////////////////////
		rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
		log( "Resigned from Federation" );

		////////////////////////////////////////
		// 13. try and destroy the federation //
		////////////////////////////////////////
		// NOTE: we won't die if we can't do this because other federates
		//       remain. in that case we'll leave it for them to clean up
		try
		{
			rtiamb.destroyFederationExecution( "UrbanSimFederation" );
			log( "Destroyed Federation" );
		}
		catch( FederationExecutionDoesNotExist dne )
		{
			log( "No need to destroy federation, it doesn't exist" );
		}
		catch( FederatesCurrentlyJoined fcj )
		{
			log( "Didn't destroy federation, federates still joined" );
		}
	}
	
	public void startConnection() throws IOException 
	{
		connection = new ConnectionClass(host, socketPort);
		receiveQueue = connection.GetInQueue();
		sendQueue = connection.GetOutQueue();
		socketElement = connection.GetSocket();
	}
	
	public ConnectionClass getConnection()
	{
		return connection;
	}

	////////////////////////////////////////////////////////////////////////////
	////////////////////////////// Helper Methods //////////////////////////////
	////////////////////////////////////////////////////////////////////////////
	/**
	 * This method will attempt to enable the various time related properties for
	 * the federate
	 */
	
	/*
	
	private void enableTimePolicy() throws Exception
	{
		// NOTE: Unfortunately, the LogicalTime/LogicalTimeInterval create code is
		//       Portico specific. You will have to alter this if you move to a
		//       different RTI implementation. As such, we've isolated it into a
		//       method so that any change only needs to happen in a couple of spots 
		HLAfloat64Interval lookahead = timeFactory.makeInterval( fedamb.federateLookahead );
		
		
		
		////////////////////////////
		// enable time regulation //
		////////////////////////////
		this.rtiamb.enableTimeRegulation( lookahead );

		// tick until we get the callback
		while( fedamb.isRegulating == false )
		{
			Thread.sleep(500);
		}
		
		/////////////////////////////
		// enable time constrained //
		/////////////////////////////
		this.rtiamb.enableTimeConstrained();
		
		// tick until we get the callback
		while( fedamb.isConstrained == false )
		{
			Thread.sleep(500);
		}
	}
	
	*/
	
	/**
	 * This method will inform the RTI about the types of data that the federate will
	 * be creating, and the types of data we are interested in hearing about as other
	 * federates produce it.
	 */
	
	
	private void publishAndSubscribe() throws RTIexception
	{
		Vehicle.handle = rtiamb.getObjectClassHandle( "HLAobjectRoot.Vehicle" );
		Vehicle.position = rtiamb.getAttributeHandle( Vehicle.handle, "Position" );

		AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
		attributes.add( Vehicle.position );
		rtiamb.subscribeObjectClassAttributes( Vehicle.handle, attributes );

		AddVehicle.handle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.AddVehicle" );
		AddVehicle.vname = rtiamb.getParameterHandle(AddVehicle.handle, "VehicleName");
		AddVehicle.vtype = rtiamb.getParameterHandle(AddVehicle.handle, "VehicleType");
		AddVehicle.dname = rtiamb.getParameterHandle(AddVehicle.handle, "DestinationName");
		AddVehicle.source = rtiamb.getParameterHandle(AddVehicle.handle, "Source");
		
		DeleteObject.handle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.DeleteObject" );
		DeleteObject.vid =  rtiamb.getParameterHandle(DeleteObject.handle, "ID");
		
		rtiamb.publishInteractionClass( AddVehicle.handle );
		rtiamb.subscribeInteractionClass( DeleteObject.handle );
	}
	
	private void sendInteraction(AddVehicleInteraction object) throws RTIexception
	{
		//////////////////////////
		// send the interactions //
		//////////////////////////
		ParameterHandleValueMap parameters = rtiamb.getParameterHandleValueMapFactory().create(4);
		HLAASCIIstring vehicleName = encoderFactory.createHLAASCIIstring(object.getVname());
		HLAASCIIstring vehicleType = encoderFactory.createHLAASCIIstring(object.getVtype());
		HLAASCIIstring destinationName = encoderFactory.createHLAASCIIstring(object.getSname());
		HLAASCIIstring destinationSource = encoderFactory.createHLAASCIIstring(object.getDname());
		parameters.put(AddVehicle.vname, vehicleName.toByteArray());
		parameters.put(AddVehicle.vtype, vehicleType.toByteArray());
		parameters.put(AddVehicle.dname, destinationName.toByteArray());
		parameters.put(AddVehicle.source, destinationSource.toByteArray());
		
 		HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );
		rtiamb.sendInteraction( AddVehicle.handle, parameters, generateTag(), time );
	}

	/**
	 * This method will request a time advance to the current time, plus the given
	 * timestep. It will then wait until a notification of the time advance grant
	 * has been received.
	 * @throws InterruptedException 
	 */
	private void advanceTime( double timestep ) throws RTIexception, InterruptedException
	{
		// request the advance
		fedamb.isAdvancing = true;
		HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime + timestep );
		rtiamb.timeAdvanceRequest( time );
		
		// wait for the time advance to be granted. ticking will tell the
		// LRC to start delivering callbacks to the federate
		while( fedamb.isAdvancing )
		{
			Thread.sleep(200);
		}
	}

	private byte[] generateTag()
	{
		return ("(timestamp) "+System.currentTimeMillis()).getBytes();
	}
	
	public static void main(String[] args) {
		String federateName = "SocialFederate";
		if( args.length != 0 )
		{
			federateName = args[0];
		}
		
		try
		{
			new SocialFederate().runFederate( federateName );
		}
		catch( Exception rtie )
		{
			// an exception occurred, just log the information and exit
			rtie.printStackTrace();
		}
	}
	
	

}
