package urbansim.sumo;

import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.CallbackModel;
import hla.rti1516e.ObjectInstanceHandle;
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
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

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
			rtiamb.connect( fedamb, CallbackModel.HLA_EVOKED );

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
			                                "TrafficSimulation",   // federate type
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
			// wait until the point is announced
			while( fedamb.isAnnounced == false || fedamb.isRegistered == false )
			{
				rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
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
				rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
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

			/////////////////////////////////////
			// 9. register an object to update //
			/////////////////////////////////////
			ObjectInstanceHandle objectHandle = registerObject();
			log( "Registered Object, handle=" + objectHandle );
			
			
			Thread.sleep(1000);
			/////////////////////////////////////
			// 10. do the main simulation loop //
			/////////////////////////////////////
			// here is where we do the meat of our work. in each iteration, we will
			// update the attribute values of the object we registered, and will
			// send an interaction.
			for( int i = 0; i < ITERATIONS; i++ )
			{
				// 9.1 update the attribute values of the instance //
				updateAttributeValues( objectHandle );
				advanceTime( 1.0 );
				
				Thread.sleep(100);

				// 9.2 send an interaction
				sendInteraction();
			}

			//////////////////////////////////////
			// 11. delete the object we created //
			//////////////////////////////////////
			deleteObject( objectHandle );
			log( "Deleted Object, handle=" + objectHandle );

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
			this.rtiamb.enableTimeConstrained();
			
			// tick until we get the callback
			while( fedamb.isConstrained == false )
			{
				rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
			}
		}
		
		/**
		 * This method will inform the RTI about the types of data that the federate will
		 * be creating, and the types of data we are interested in hearing about as other
		 * federates produce it.
		 */
		private void publishAndSubscribe() throws RTIexception
		{
			///////////////////////////////////////////////
			// publish all attributes of Food.Drink.Soda //
			///////////////////////////////////////////////
			// before we can register instance of the object class Food.Drink.Soda and
			// update the values of the various attributes, we need to tell the RTI
			// that we intend to publish this information

			// get all the handle information for the attributes of Food.Drink.Soda
			Vehicle.handle = rtiamb.getObjectClassHandle( "HLAobjectRoot.Vehicle" );
			Vehicle.position = rtiamb.getAttributeHandle( Vehicle.handle, "Position" );
			Vehicle.vname = rtiamb.getAttributeHandle( Vehicle.handle, "VehicleName" );
			Vehicle.vtype = rtiamb.getAttributeHandle( Vehicle.handle, "VehicleType" );
			Vehicle.velocity = rtiamb.getAttributeHandle( Vehicle.handle, "Velocity" );
			Vehicle.angle = rtiamb.getAttributeHandle( Vehicle.handle, "Angle" );
			// package the information into a handle set
			AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
			attributes.add( Vehicle.position );
			
			// do the actual publication
			rtiamb.publishObjectClassAttributes( Vehicle.handle, attributes );

			////////////////////////////////////////////////////
			// subscribe to all attributes of Food.Drink.Soda //
			////////////////////////////////////////////////////
			// we also want to hear about the same sort of information as it is
			// created and altered in other federates, so we need to subscribe to it
			rtiamb.subscribeObjectClassAttributes( Vehicle.handle, attributes );

			//////////////////////////////////////////////////////////
			// publish the interaction class FoodServed.DrinkServed //
			//////////////////////////////////////////////////////////
			// we want to send interactions of type FoodServed.DrinkServed, so we need
			// to tell the RTI that we're publishing it first. We don't need to
			// inform it of the parameters, only the class, making it much simpler
			AddVehicle.handle = rtiamb.getInteractionClassHandle( "HLAinteractionRoot.AddVehicle" );
			DeleteObject.handle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.DeleteObject");
			CreateObject.handle = rtiamb.getInteractionClassHandle("HLAinteractionRoot.CreateObject");
			
			/////////////////////////////////////////////////////////
			// subscribe to the FoodServed.DrinkServed interaction //
			/////////////////////////////////////////////////////////
			// we also want to receive other interaction of the same type that are
			// sent out by other federates, so we have to subscribe to it first
			rtiamb.subscribeInteractionClass( AddVehicle.handle );
			
			DeleteObject.vid = rtiamb.getParameterHandle(DeleteObject.handle, "ID");
			rtiamb.publishInteractionClass(DeleteObject.handle);
			
			CreateObject.vid = rtiamb.getParameterHandle(CreateObject.handle, "ID");
			CreateObject.vtype = rtiamb.getParameterHandle(CreateObject.handle, "VehicleType");
			CreateObject.pos = rtiamb.getParameterHandle(CreateObject.handle, "Position");
			rtiamb.publishInteractionClass(CreateObject.handle);
			
		}
		
		/**
		 * This method will register an instance of the Soda class and will
		 * return the federation-wide unique handle for that instance. Later in the
		 * simulation, we will update the attribute values for this instance
		 */
		private ObjectInstanceHandle registerObject() throws RTIexception
		{
			return rtiamb.registerObjectInstance( Vehicle.handle );
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
		private void updateAttributeValues( ObjectInstanceHandle objectHandle ) throws RTIexception
		{
			///////////////////////////////////////////////
			// create the necessary container and values //
			///////////////////////////////////////////////
			// create a new map with an initial capacity - this will grow as required
			AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(1);
			
			// create the collection to store the values in, as you can see
			// this is quite a lot of work. You don't have to use the encoding
			// helpers if you don't want. The RTI just wants an arbitrary byte[]

			// generate the value for the number of cups (same as the timestep)
			Position pos = new Position(fedamb.federateTime,fedamb.federateTime,25.5);
	        attributes.put(Vehicle.position, _positionRecordCoder.encode(pos));
			
			// note that if you want to associate a particular timestamp with the
			// update. here we send another update, this time with a timestamp:
	        rtiamb.updateAttributeValues( objectHandle, attributes, generateTag());
			//HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );
			//rtiamb.updateAttributeValues( objectHandle, attributes, generateTag(), time );
			//log( "X,Y,Z: " + pos.toString());
		}
		
		/**
		 * This method will send out an interaction of the type FoodServed.DrinkServed. Any
		 * federates which are subscribed to it will receive a notification the next time
		 * they tick(). This particular interaction has no parameters, so you pass an empty
		 * map, but the process of encoding them is the same as for attributes.
		 */
		private void sendInteraction() throws RTIexception
		{
			//////////////////////////
			// send the interaction //
			//////////////////////////
			ParameterHandleValueMap creObjParam = rtiamb.getParameterHandleValueMapFactory().create(3);
			HLAASCIIstring crevID = encoderFactory.createHLAASCIIstring("Departing Vehicle");
			HLAASCIIstring crevType = encoderFactory.createHLAASCIIstring("Awesome Type");
			Position pos = new Position(fedamb.federateTime, fedamb.federateTime, 25.5);

			creObjParam.put(CreateObject.vid, crevID.toByteArray());
			creObjParam.put(CreateObject.vtype, crevType.toByteArray());
			creObjParam.put(CreateObject.pos, _positionRecordCoder.encode(pos));
			
			
			ParameterHandleValueMap delObjParam = rtiamb.getParameterHandleValueMapFactory().create(1);
			HLAASCIIstring delvID = encoderFactory.createHLAASCIIstring("Arrived Vehicle");
			delObjParam.put(DeleteObject.vid, delvID.toByteArray());
			
			
			// if you want to associate a particular timestamp with the
			// interaction, you will have to supply it to the RTI. Here
			// we send another interaction, this time with a timestamp:
			HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead );
			rtiamb.sendInteraction( CreateObject.handle, creObjParam, generateTag());
			rtiamb.sendInteraction( DeleteObject.handle, delObjParam, generateTag());
		}

		/**
		 * This method will request a time advance to the current time, plus the given
		 * timestep. It will then wait until a notification of the time advance grant
		 * has been received.
		 */
		private void advanceTime( double timestep ) throws RTIexception
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
				rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
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

		private byte[] generateTag()
		{
			return ("(timestamp) "+System.currentTimeMillis()).getBytes();
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
