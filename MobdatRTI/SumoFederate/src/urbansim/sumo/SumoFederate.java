package urbansim.sumo;
import urbansim.*;
                                             
import hla.rti1516.jlc.HLAfloat32BE;
import hla.rti1516.jlc.HLAfloat64BE;
import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.ParameterHandle;
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
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;


import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

//Fucionality of Sumo Federate 
// 1 - Receive from the socket the information(Receive Work)
// 2 - Treats this information and make sure that the information is not broken, if it is resolve the problem(Receive work)
// 3 - Store in the Queue (Receive work)
// 4 - pop from the Queue to precess the information ( Sumo Federate)
// 5 - Create 2 dictionaries to trace all objects sent from Sumo - byId byHandle ( Sumo Federate)
// 6 - Define what kind of event and send to RTI ( Sumo Federate)

// 1 - Receive from RTI ( Sumo Ambassador )
// 2 - Transfor to Json Object ( Sumo Ambassador )
// 3 - Place in a Queue( Sumo Ambassador)
// 4 - Send via Socket (Send Work)



/* Proximos passos 

1 - editar angle e velocity para o array de 4 eleemtos - esta definido como sera no Sumofederate
mas nao na FOM
2 - 




public class SumoFederate {
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
	private RTIambassador rtiamb;
	private SumoFederateAmbassador fedamb;  // created when we connect
	private HLAfloat64TimeFactory timeFactory; // set when we join
	protected EncoderFactory encoderFactory;     // set when we join
	protected PositionCoder _positionRecordCoder;
	connectionClass conection;
	BlockingQueue receiveQueue;
	BlockingQueue sendQueue;
	public  Socket SocketElement;
	public final int socketPort = 23456;
	public final String host = "localhost";
	// = // StartConnection();
	//public final Socket client =  StartConnection();

	
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
			fedamb = new SumoFederateAmbassador( this, encoderFactory);
			rtiamb.connect( fedamb, CallbackModel.HLA_IMMEDIATE);

			//////////////////////////////
			// 3. create the federation //
			//////////////////////////////
			
			//Socket b = fedamb.StartConnection1();
			log("Stating socket Connection");
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
			                                "TrafficSimulation",   // federate type
			                                "UrbanSimFederation",     // name of federation
			                                joinModules );           // modules we want to add

			log( "Joined Federation as " + federateName );
			
			// cache the time factory for easy access
			this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();
			
			////////////////////////////////
			// 5. announce the sync point //
			////////////////////////////////
			
			
			StartConnection();
			fedamb.StartConnection();
			log("Connected with the Socket");
			// announce a sync point to get everyone on the same page. if the point
			// has already been registered, we'll get a callback saying it failed,
			// but we don't care about that, as long as someone registered it
			rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
			// wait until the point is announced
			while( fedamb.isAnnounced == false || fedamb.isRegistered == false )
			{
				Thread.sleep(200);
			}

			// WAIT FOR USER TO KICK US OFF
			// So that there is time to add other federates, we will wait until the
			// user hits enter before proceeding. That was, you have time to start
			// other federates.
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
				Thread.sleep(200);
			}

			/////////////////////////////
			// 7. enable time policies //
			/////////////////////////////
			// in this section we enable/disable all time policies
			// note that this step is optional!
			enableTimePolicy();
			log( "Time Policy Enabled" );

			//////////////////////////////
			// 8. publish and subscribe //
			//////////////////////////////
			// in this section we tell the RTI of all the data we are going to
			// produce, and all the data we want to know about
			publishAndSubscribe();
			log( "Published and Subscribed" );
			///////////////////////////////
			// 8.1 Create objects layout //
			//////////////////////////////
			//Vehicle vehicle = new Vehicle();
			//TrafficLight trafficLight = new TrafficLight();
			//log( "Veicle layout created");
			//log( "TrafficLight layout created");
			
			
			
			
			
			/////////////////////////////////////
			// 9. register an object to update //
			/////////////////////////////////////
			
			//ObjectInstanceHandle objectHandle = registerObject();
			//log( "Registered Object, handle=" + objectHandle );
			
			
			//Thread.sleep(1000);
			///////////////////////////////////////////
			// 10. Send Interactions/Register Objects//
			//////////////////////////////////////////
			/*
			for (Map.Entry<String, Object> entry : map.entrySet())
			{
			    System.out.println(entry.getKey() + "/" + entry.getValue());
			}*/
			
			
			

			//String that store the income information from the socket
			
			   
			String fromClient ;
			// 2 Dictionaries that store all the elements created by the Sumo, 
			//1 Dictionary sotore the information with the key the ID that Sumo give to its elements
			// and the other use the Handle that the RTI give to the Sumo Federation
			Map<Integer,ObjectReferencesByID> objectHashMapByID = new HashMap<Integer,ObjectReferencesByID>();
			Map<ObjectInstanceHandle,ObjectReferencesByHandle> objectHashMapByHandle = new HashMap<ObjectInstanceHandle,ObjectReferencesByHandle>();
			Map<String,Object> map;
			ObjectMapper mapper;
			//vatiables used to store the current  ID and handle to strore in the Dictionaries
			ObjectReferencesByID objectReferencesByID;
			ObjectReferencesByHandle objectReferencesByHandle;
				   
            boolean run = true;
	        while(run) 
	        { 
	            if((fromClient = receiveQueue.take().toString()) != null){
	            	System.out.println("fromClient: "+fromClient);
	            	//Transform the map sent by socket in a hash map
	            	//keep the same structure of a json object
		            map = new HashMap<String,Object>(); //Cria um hashmap baseado em duas strings 
		            mapper = new ObjectMapper(); // Cria o Objeto mapper
		            
		            
		            try{
		            	//map recebera o HASHMAP criado a partir do JSON
		            	map = mapper.readValue(fromClient, new TypeReference<HashMap<String,Object>>(){}); }
		            catch(Exception e){            
		            	e.printStackTrace();}
		            
		            /*-----------------check all the possible messages that SumoConnector can send -------------*/
		            
		            	
		            
		            
		            if(map.containsValue("InductionLoop"))
		            	sendInteraction(map);   
		            
		            if(map.containsValue("DeleteObject"))         	
				           sendInteraction(map); 
		            	
		            
		            if(map.containsValue("CreateObject"))		            	
		            	sendInteraction(map); 		            	
		            
		            	            	            
		            
		            // The program uses the same message "TrafficlightInstance" when sumo order to create another object
		            // or to update an object of  TrafficLight
		            if(map.containsValue("TrafficlightInstance")){
		            	ObjectInstanceHandle objectHandle = null;
		            	//Check if the object already exist in the list 
		            	//If the ID is Given and it is in the dictionary the Connector wants to update an element, if not, create a new element and insert in the Dictionary
		            	if(!objectHashMapByID.containsKey((Integer) map.get("id"))){
		            		//Register TrafficLightsInstance object 
		            		objectHandle = registerObject(map);
		            		System.out.println("11111");
		            		//Insert to the Dictionary the object TrafficLight
		            		objectReferencesByID = new ObjectReferencesByID(objectHandle, "TrafficLight" );
		            		objectReferencesByHandle = new ObjectReferencesByHandle((Integer) map.get("id"),"TrafficLight");
		            		objectHashMapByID.put((Integer) map.get("id"), objectReferencesByID);
		            		objectHashMapByHandle.put(objectHandle,objectReferencesByHandle);		  	            		

		            	}
		            	else{
		            		
		            		//take the handle gave by the sumo connector
		            		objectHandle = (ObjectInstanceHandle) objectHashMapByID.get(map.get("id")).handle;
		            	}
		            	
		            	try{
		            		updateAttributeValues(map,objectHandle);
		            	}catch(Exception e){
		            		log("Wrong input to TrafficlightInstance");
		            	}	 
		            }
		            
		            // The program uses the same message "VehicleInstance" when sumo order to create another object
		            // or to update an object of  Vehicle
		           if(map.containsValue("VehicleInstance")){
		            	ObjectInstanceHandle objectHandle = null;
		            	//Check if the object already exist in the list 
		            	//If the ID is Given and it is in the dictionary the Connector wants to update an element, if not, create a new element and insert in the Dictionary
		            	if(!objectHashMapByID.containsKey((Integer) map.get("id"))){
		            		//Register TrafficLightsInstance object 
		            		objectHandle = registerObject(map);
		            		objectReferencesByID = new ObjectReferencesByID(objectHandle, "Vehicle" );
		            		objectReferencesByHandle = new ObjectReferencesByHandle((Integer) map.get("id"),"TrafficLight");
		            		objectHashMapByID.put((Integer) map.get("id"), objectReferencesByID);
		            		objectHashMapByHandle.put(objectHandle,objectReferencesByHandle);
		            		
		            	}
		            	else{
		            		//take the handle gave by the sumo connector
		            		objectHandle = (ObjectInstanceHandle) objectHashMapByID.get(map.get("id")).handle;
		            	}
		            	

		            		updateAttributeValues(map,objectHandle);
		            	            	 
		            }
	            }
	        }
			
	        
			//////////////////////////////////////
			// 11. delete the object we created //
			//////////////////////////////////////
			//deleteObject( objectHandle );
			//log( "Deleted Object, handle=" + objectHandle );

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
		
		////////////////////////////////////////////////////////////////////////////
		////////////////////////////// Helper Methods //////////////////////////////
		////////////////////////////////////////////////////////////////////////////
		/**
		 * This method will attempt to enable the various time related properties for
		 * the federate
		 */
		
	
		
		private void enableTimePolicy() throws Exception
		{
			// NOTE: Unfortunately, the LogicalTime/LogicalTimeInterval create code is
			//       Portico specific. You will have to alter this if you move to a
			//       different RTI implementation. As such, we've isolated it into a
			//       method so that any change only needs to happen in a couple of spots 
			/////////////////////////////
			// enable time constrained //
			/////////////////////////////
			
			
			
			HLAfloat64Interval lookahead = timeFactory.makeInterval( fedamb.federateLookahead );			
			////////////////////////////
			// enable time regulation //
			////////////////////////////
			this.rtiamb.enableTimeRegulation( lookahead );
		    /////////////////////////////
			// enable time constrained //
			/////////////////////////////
			this.rtiamb.enableTimeConstrained();
			
	
			
			/*this.rtiamb.enableTimeConstrained();
			  
			 
			
			// tick until we get the callback
			while( fedamb.isConstrained == false )
			{
				rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
			}*/
		}
		
		/**
		 * This method will inform the RTI about the types of data that the federate will
		 * be creating, and the types of data we are interested in hearing about as other
		 * federates produce it.
		 */
		private void publishAndSubscribe() throws RTIexception
		{
			
			
			// before we can register instance of the object class and
			// update the values of the various attributes, we need to tell the RTI
			// that we intend to publish this information
			
			//Hash maps used to designed the correct information to the Dictionary inside each class
			//This dictionaries store the type, name and handle of each attribute 
			Map<ParameterHandle,InteractionData> interactionDataByHandle;
			Map<String,ObjectData> objectDataByName;
			Map<AttributeHandle,ObjectData> ObjectDataByHandle;
			
			/*------------------------------InductionLoop Interaction -------------------------*/
					
			// get all the handle information for the attributes of the class
			InductionLoop.handle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.InductionLoop");
			InductionLoop.id = rtiamb.getParameterHandle(InductionLoop.handle, "ID");
			InductionLoop.count = rtiamb.getParameterHandle(InductionLoop.handle, "Count");
						
			//create the Dictionary that store the handle of the attributes as the key to a object that stores 
			//the handle, type and its name.
			//The objective is to know its type and name when necessary to any convention
			interactionDataByHandle = new HashMap<ParameterHandle,InteractionData>();
			interactionDataByHandle.put(InductionLoop.id , new InteractionData(InductionLoop.id ,"id", "string"));
			interactionDataByHandle.put(InductionLoop.count , new InteractionData(InductionLoop.count ,"id", "intenger"));
			InductionLoop.InteractionDataByHandle = interactionDataByHandle;
			
			
			rtiamb.publishInteractionClass(InductionLoop.handle);
			/*----------------------------- DeleteObject Interaction --------------------------*/
			
			// get all the handle information for the attributes of the class
			DeleteObject.handle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.DeleteObject");
			DeleteObject.vid = rtiamb.getParameterHandle(DeleteObject.handle, "ID");
			
			//create the Dictionary that store the handle of the attributes as the key to a object that stores 
			//the handle, type and its name.
			//The objective is to know its type and name when necessary to any convention
			interactionDataByHandle = new HashMap<ParameterHandle,InteractionData>();
			interactionDataByHandle.put(DeleteObject.vid , new InteractionData(CreateObject.vid ,"id", "string"));
			DeleteObject.InteractionDataByHandle = interactionDataByHandle;
			
			rtiamb.publishInteractionClass(DeleteObject.handle);
			
			/*----------------------------- AddVehicle Interaction-----------------------------*/
			
			// get all the handle information for the attributes of the class
			AddVehicle.handle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.AddVehicle" );
			AddVehicle.vname = rtiamb.getParameterHandle(AddVehicle.handle, "VehicleName");
			AddVehicle.vtype = rtiamb.getParameterHandle(AddVehicle.handle, "VehicleType");
			AddVehicle.dname = rtiamb.getParameterHandle(AddVehicle.handle, "DestinationName");
			AddVehicle.source = rtiamb.getParameterHandle(AddVehicle.handle, "Source");
			
			//create the Dictionary that store the handle of the attributes as the key to a object that stores 
			//the handle, type and its name.
			//The objective is to know its type and name when necessary to any convention
			interactionDataByHandle = new HashMap<ParameterHandle,InteractionData>();
			interactionDataByHandle.put(AddVehicle.vname, new InteractionData(AddVehicle.vname,"vname", "string"));
			interactionDataByHandle.put(AddVehicle.vtype, new InteractionData(AddVehicle.vtype,"vtype", "string"));
			interactionDataByHandle.put(AddVehicle.dname, new InteractionData(AddVehicle.dname,"dname", "string"));
			interactionDataByHandle.put(AddVehicle.source, new InteractionData(AddVehicle.source,"sorce", "string"));
			AddVehicle.InteractionDataByHandle = interactionDataByHandle;
			
			
			rtiamb.subscribeInteractionClass(AddVehicle.handle);
			
			/* ----------------------------- Create Object Interaction-----------------------------*/
			
			// get all the handle information for the attributes of the class
			CreateObject.handle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.CreateObject" );
			CreateObject.vid = rtiamb.getParameterHandle(urbansim.sumo.CreateObject.handle, "ID");
			CreateObject.vtype = rtiamb.getParameterHandle(urbansim.sumo.CreateObject.handle, "VehicleType");
			CreateObject.position = rtiamb.getParameterHandle(urbansim.sumo.CreateObject.handle, "Position");
			
			//create the Dictionary that store the handle of the attributes as the key to a object that stores 
			//the handle, type and its name.
			//The objective is to know its type and name when necessary to any convention
			interactionDataByHandle = new HashMap<ParameterHandle,InteractionData>();
			interactionDataByHandle.put(CreateObject.vid , new InteractionData(CreateObject.vid ,"id", "string"));
			interactionDataByHandle.put(CreateObject.vtype , new InteractionData(CreateObject.vtype ,"vtype", "string"));
			interactionDataByHandle.put(CreateObject.position  , new InteractionData(CreateObject.position  ,"position", "position"));
			CreateObject.InteractionDataByHandle = interactionDataByHandle;
			
			
			rtiamb.publishInteractionClass( CreateObject.handle );
			/* ----------------------------- Vehicle Object -----------------------------*/
			// get all the handle information for the attributes of the class
			System.out.println("SUMO  first- "+Vehicle.vtype);
			Vehicle.handle = rtiamb.getObjectClassHandle( "HLAobjectRoot.Vehicle" );
			System.out.println("SUMO  Second- "+Vehicle.vtype);
			Vehicle.position = rtiamb.getAttributeHandle( Vehicle.handle, "Position" );
			Vehicle.vname = rtiamb.getAttributeHandle( Vehicle.handle, "VehicleName" );
			Vehicle.vtype = rtiamb.getAttributeHandle( Vehicle.handle, "VehicleType" );
			Vehicle.velocity = rtiamb.getAttributeHandle( Vehicle.handle, "Velocity" );
			Vehicle.angle = rtiamb.getAttributeHandle( Vehicle.handle, "Angle" );
			
			//create the Dictionary that store the handle of the attributes as the key to a object that stores 
			//the handle, type and its name.
			//The objective is to know its type and name when necessary to any convention
			objectDataByName = new HashMap<String,ObjectData>();	
			objectDataByName.put("pos",new ObjectData(Vehicle.position,"position","position"));
			objectDataByName.put("angle",new ObjectData(Vehicle.angle,"angle","float"));
			objectDataByName.put("velocity",new ObjectData(Vehicle.velocity,"velocity","float"));
			objectDataByName.put("vname",new ObjectData(Vehicle.vname,"vname","String"));
			objectDataByName.put("vtype",new ObjectData(Vehicle.vtype,"vtype","String"));
			Vehicle.ObjectDataByName = objectDataByName;
			
			//create the Dictionary that store the handle of the attributes as the key to a object that stores 
			//the handle, type and its name.
			//The objective is to know its type and name when necessary to any convention
			ObjectDataByHandle = new HashMap<AttributeHandle,ObjectData>();	
			ObjectDataByHandle.put(Vehicle.position,new ObjectData(Vehicle.position,"position","position"));
			ObjectDataByHandle.put(Vehicle.angle,new ObjectData(Vehicle.angle,"angle","float"));
			ObjectDataByHandle.put(Vehicle.velocity,new ObjectData(Vehicle.velocity,"velocity","float"));
			ObjectDataByHandle.put(Vehicle.vname,new ObjectData(Vehicle.vname,"vname","String"));
			ObjectDataByHandle.put(Vehicle.vtype,new ObjectData(Vehicle.vtype,"vtype","String"));
			Vehicle.ObjectDataByHandle = ObjectDataByHandle;
			
			// package the information into a handle set
			AttributeHandleSet attributesVehicle = rtiamb.getAttributeHandleSetFactory().create();
			attributesVehicle.add( Vehicle.position );
			attributesVehicle.add(Vehicle.vname);
			attributesVehicle.add(Vehicle.vtype);
			attributesVehicle.add(Vehicle.velocity);
			attributesVehicle.add(Vehicle.angle);
			
			
			// we also want to hear about the same sort of information as it is
			// created and altered in other federates, so we need to subscribe to it
			rtiamb.publishObjectClassAttributes( Vehicle.handle, attributesVehicle  );
			rtiamb.subscribeObjectClassAttributes( Vehicle.handle, attributesVehicle  );
			
			/* ----------------------------- TrafficLight Object -----------------------------*/
			TrafficLight.handle = rtiamb.getObjectClassHandle( "HLAobjectRoot.TrafficLight" );
			TrafficLight.id = rtiamb.getAttributeHandle( TrafficLight.handle, "ID" );
			TrafficLight.position = rtiamb.getAttributeHandle( TrafficLight.handle, "Position" );
			TrafficLight.status= rtiamb.getAttributeHandle( TrafficLight.handle, "Status" );
			//package the information into a handle set
			AttributeHandleSet attributesTrafficLight = rtiamb.getAttributeHandleSetFactory().create();
			attributesTrafficLight.add( TrafficLight.id );
			attributesTrafficLight.add( TrafficLight.status );
			attributesTrafficLight.add( TrafficLight.id );
			
			ObjectDataByHandle = new HashMap<AttributeHandle,ObjectData>();	
			ObjectDataByHandle.put(TrafficLight.position,new ObjectData(TrafficLight.position,"position","position"));
			ObjectDataByHandle.put(TrafficLight.id,new ObjectData(TrafficLight.id,"id","intenger"));
			ObjectDataByHandle.put(TrafficLight.status,new ObjectData(TrafficLight.status,"status","string"));
			TrafficLight.ObjectDataByHandle = ObjectDataByHandle;
			
			objectDataByName = new HashMap<String,ObjectData>();	
			objectDataByName .put("position",new ObjectData(TrafficLight.position,"position","position"));
			objectDataByName .put("id",new ObjectData(TrafficLight.id,"id","intenger"));
			objectDataByName .put("status",new ObjectData(TrafficLight.status,"status","string"));
			TrafficLight.ObjectDataByName = objectDataByName;
			
			
			// we also want to hear about the same sort of information as it is
			// created and altered in other federates, so we need to subscribe to it
			rtiamb.publishObjectClassAttributes(TrafficLight.handle, attributesTrafficLight);
			rtiamb.subscribeObjectClassAttributes( TrafficLight.handle, attributesTrafficLight );
			
			
		}
		
		
		/**
		 * This method will register an instance of the Soda class and will
		 * return the federation-wide unique handle for that instance. Later in the
		 * simulation, we will update the attribute values for this instance
		 */
		private ObjectInstanceHandle registerObject(Map<String,Object> map) throws RTIexception
		{
			if(map.containsValue("TrafficlightInstance")){
				return rtiamb.registerObjectInstance( TrafficLight.handle );
			}
			else if((map.containsValue("VehicleInstance"))){
				return rtiamb.registerObjectInstance( Vehicle.handle );
			}
			return null;
		}
		
		/**
		 * This method will update all the values of the given object instance. It will
		 * set the flavour of the soda to a random value from the options specified in
		 * the FOM (Cola - 101, Orange - 102, RootBeer - 103, Cream - 104) and it will set
		 * the number of cups to the same value as the current time.
		 * <p/>
		 * Note that we don't actually have to update all the attributes at once, we
		 * could update them individually, in groups or not at all!
		 */
		private void updateAttributeValues( Map<String,Object> map ,ObjectInstanceHandle objectHandle ) throws RTIexception
		{
	/*
			
			for (Map.Entry<String, Object> entry : map.entrySet())
			{
			    System.out.println(entry.getKey() + "/" + entry.getValue());
			}*/
		    //Check by the name of the event with object should be updated
			if(map.containsValue("VehicleInstance")){
				
				//create the a bundle of elements to send to the RTI, this bundle has size of: Number of elements sent by the socket -2 
				// because 2 of the fields will not be used by the object -   evt_type and id
				AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(map.size()-2);

				//For each possible attribute for the class, check if it is in the map, if so put inside the blundle 
				
				
				if(map.containsKey("position")){
					ArrayList array = (ArrayList) map.get("position");
					Position pos = new Position((double)array.get(0),(double)array.get(1),(double)array.get(2));
					attributes.put(Vehicle.position, _positionRecordCoder.encode(pos));}
				
				
				if(map.containsKey("angle")){
					hla.rti1516e.encoding.HLAfloat64BE angle =  encoderFactory.createHLAfloat64BE((double) map.get("angle"));
					attributes.put(Vehicle.angle, angle.toByteArray());}

					//ArrayList array = (ArrayList) map.get("position");
			       		//Position angle  = new Position((double)array.get(0),(double)array.get(1),(double)array.get(2));

					//attributes.put(Vehicle.angle, _positionRecordCoder.encode(angle));}
				


				if(map.containsKey("velocity")){
					hla.rti1516e.encoding.HLAfloat64BE velocity =   encoderFactory.createHLAfloat64BE((double) map.get("velocity"));
					attributes.put(Vehicle.velocity, velocity.toByteArray());}
					//ArrayList array  = (ArrayList) map.get("velocity");
			       		//Position velocity  = new Position((double)array.get(0),(double)array.get(1),(double)array.get(2));

					//attributes.put(Vehicle.velocity, _positionRecordCoder.encode(velocity));}
				
				if(map.containsKey("vname")){
					HLAASCIIstring vName = encoderFactory.createHLAASCIIstring((String) map.get("vname"));
					attributes.put(Vehicle.vname, vName.toByteArray());}
				
				
				if(map.containsKey("vtype")){
					HLAASCIIstring vType = encoderFactory.createHLAASCIIstring((String) map.get("vtype"));
					attributes.put(Vehicle.vname, vType.toByteArray());}
				
				
				//send to RTI the new values of the attributes
				rtiamb.updateAttributeValues( objectHandle, attributes, generateTag());							

			}
			
			
			if(map.containsValue("TrafficLigthInstance")){

				//create the a bundle of elements to send to the RTI, this bundle has size of: Number of elements sent by the socket -2 
				// because 2 of the fields will not be used by the object -   evt_type and id
				AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(map.size()-2);
				
			
				//For each possible attribute for the class, check if it is in the map, if so put inside the blundle 
				
				if(map.containsKey("pos")){
					ArrayList array = (ArrayList) map.get("pos");
					Position pos = new Position((double)array.get(0),(double)array.get(1),(double)array.get(2));
					attributes.put(TrafficLight.position, _positionRecordCoder.encode(pos));}
				
				
				if(map.containsKey("status")){
					HLAASCIIstring vName = encoderFactory.createHLAASCIIstring((String) map.get("status"));
					attributes.put(TrafficLight.status, vName.toByteArray());}
				
				
				if(map.containsKey("id")){
					HLAASCIIstring vType = encoderFactory.createHLAASCIIstring((String) map.get("id"));
					attributes.put(TrafficLight.id, vType.toByteArray());}
				
				
				//send to RTI the new values of the attributes
				rtiamb.updateAttributeValues( objectHandle, attributes, generateTag());							
				
			}
		}
		
		
		//Send  the interactions to the RTI
		private void sendInteraction(Map<String,Object> map) throws RTIexception
		{

					   
			if(map.containsValue("CreateObject")){
				
		    		System.out.println("event Create"); 
				
				ParameterHandleValueMap creObjParam = rtiamb.getParameterHandleValueMapFactory().create(3);
				HLAASCIIstring crevID = encoderFactory.createHLAASCIIstring((String) map.get("vid"));
				HLAASCIIstring crevType = encoderFactory.createHLAASCIIstring((String) map.get("vtype"));
				ArrayList array = (ArrayList) map.get("pos");
				Position pos;				
				pos = new Position((double)array.get(0),(double)array.get(1),(double)array.get(2));
							
				creObjParam.put(CreateObject.vid, crevID.toByteArray());
				creObjParam.put(CreateObject.vtype, crevType.toByteArray());
				creObjParam.put(CreateObject.position, _positionRecordCoder.encode(pos));
				HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );
				rtiamb.sendInteraction( CreateObject.handle, creObjParam, generateTag());
			}
			

			if(map.containsValue("DeleteObject")){
				
		    		System.out.println("event Delete"); 
				ParameterHandleValueMap delObjParam = rtiamb.getParameterHandleValueMapFactory().create(1);
				HLAASCIIstring delvID = encoderFactory.createHLAASCIIstring((String) map.get("vid"));
				delObjParam.put(DeleteObject.vid, delvID.toByteArray());
			
				HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );
				rtiamb.sendInteraction( DeleteObject.handle, delObjParam, generateTag());
			}
				
			if(map.containsValue("InductionLoop")){
				ParameterHandleValueMap inductionLoop = rtiamb.getParameterHandleValueMapFactory().create(2);
				HLAASCIIstring indID = encoderFactory.createHLAASCIIstring((String) map.get("id"));
				HLAASCIIstring indCount = encoderFactory.createHLAASCIIstring((String) map.get("count"));
				
	
				inductionLoop.put(InductionLoop.id, indID.toByteArray());
				inductionLoop.put(InductionLoop.count, indCount.toByteArray());
				
				System.out.println("wooooow "+ InductionLoop.id);
				HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );
				rtiamb.sendInteraction( InductionLoop.handle , inductionLoop, generateTag());
			}
			
			
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
			log("Time is " + time.toString());
			while( fedamb.isAdvancing )
			{
				Thread.sleep(200);
			}
		}

		/**
		 * This method will attempt to delete the object instance of the given
		 * handle. We can only delete objects we created, or for which we own the
		 * privilegeToDelete attribute.
		 */
		private void deleteObject( ObjectInstanceHandle handle ) throws RTIexception
		{
			rtiamb.deleteObjectInstance( handle, generateTag() );
		}

		private byte[] generateTag(){
			return ("(timestamp) "+System.currentTimeMillis()).getBytes();
		}
	
		
		public void StartConnection(){
			
			conection = new connectionClass(host,socketPort);
			receiveQueue = conection.GetInQueue();
			sendQueue = conection.GetOutQueue();
			SocketElement = conection.GetSocket();
		}
		
		public connectionClass GetConnection(){
			return conection;
		}
		
		
		
		
	public static void main(String[] args) {
		// get a federate name, use "exampleFederate" as default
		String federateName = "SumoFederate";
		if( args.length != 0 )
		{
			federateName = args[0];
		}
		
		try
		{
			// run the example federate
			new SumoFederate().runFederate( federateName );
		}
		catch( Exception rtie )
		{
			// an exception occurred, just log the information and exit
			rtie.printStackTrace();
		}
	}
	
	

}

