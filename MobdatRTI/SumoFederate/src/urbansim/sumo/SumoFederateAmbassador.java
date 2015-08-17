
package urbansim.sumo;


import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.FederateHandleSet;
import hla.rti1516e.InteractionClassHandle;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.NullFederateAmbassador;
import hla.rti1516e.ObjectClassHandle;
import hla.rti1516e.ObjectInstanceHandle;
import hla.rti1516e.OrderType;
import hla.rti1516e.ParameterHandle;
import hla.rti1516e.ParameterHandleValueMap;
import hla.rti1516e.SynchronizationPointFailureReason;
import hla.rti1516e.TransportationTypeHandle;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAASCIIstring;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

public class SumoFederateAmbassador extends NullFederateAmbassador {
	//----------------------------------------------------------
		//                    STATIC VARIABLES
		//----------------------------------------------------------

		//----------------------------------------------------------
		//                   INSTANCE VARIABLES
		//----------------------------------------------------------
		private SumoFederate federate;

		// these variables are accessible in the package
		protected double federateTime        = 0.0;
		protected double federateLookahead   = 1.0;
		
		protected boolean isRegulating       = false;
		protected boolean isConstrained      = false;
		protected boolean isAdvancing        = false;
		
		protected boolean isAnnounced        = false;
		protected boolean isReadyToRun       = false;
		protected boolean registrationFailed = false;
		protected PositionCoder _positionRecordCoder;
		protected EncoderFactory _encoderFactory;
		protected boolean isRegistered = 	   false;
		public  Socket SocketElement;
		OutputStream OutElement;
		BufferedWriter bufOut;
		connectionClass connection;
		BlockingQueue receiveQueue;
		BlockingQueue sendQueue;
		

		//----------------------------------------------------------
		//                      CONSTRUCTORS
		//----------------------------------------------------------

		public SumoFederateAmbassador( SumoFederate federate, EncoderFactory encoder )
		{
			this.federate = federate;
			this._encoderFactory = encoder;
			this._positionRecordCoder = new PositionCoder(_encoderFactory);
			
			//StartConnection();

			//HLAASCIIstring test = encoder.createHLAASCIIstring("lalal");
			
		}

		//----------------------------------------------------------
		//                    INSTANCE METHODS
		//----------------------------------------------------------
		
		//Start the connection
		public void StartConnection(){
			connection = federate.GetConnection();
			receiveQueue = connection.GetInQueue();
			sendQueue = connection.GetOutQueue();
		}
		
		private void log( String message )
		{
			System.out.println( "FederateAmbassador: " + message );
		}
		
		//////////////////////////////////////////////////////////////////////////
		////////////////////////// RTI Callback Methods //////////////////////////
		//////////////////////////////////////////////////////////////////////////
		@Override
		public void synchronizationPointRegistrationFailed( String label,
		                                                    SynchronizationPointFailureReason reason )
		{
			log( "Failed to register sync point: " + label + ", reason="+reason );
			registrationFailed = true;
			isRegistered = true;
		}

		@Override
		public void synchronizationPointRegistrationSucceeded( String label )
		{
			log( "Successfully registered sync point: " + label );
			registrationFailed = false;
			isRegistered = true;
		}


		@Override
		public void announceSynchronizationPoint( String label, byte[] tag )
		{
			log( "Synchronization point announced: " + label );
			if( label.equals(SumoFederate.READY_TO_RUN) )
				this.isAnnounced = true;
		}

		@Override
		public void federationSynchronized( String label, FederateHandleSet failed )
		{
			log( "Federation Synchronized: " + label );
			if( label.equals(SumoFederate.READY_TO_RUN) )
				this.isReadyToRun = true;
		}

		/**
		 * The RTI has informed us that time regulation is now enabled.
		 */
		@Override
		public void timeRegulationEnabled( LogicalTime time )
		{
			this.federateTime = ((HLAfloat64Time)time).getValue();
			this.isRegulating = true;
		}

		@Override
		public void timeConstrainedEnabled( LogicalTime time )
		{
			this.federateTime = ((HLAfloat64Time)time).getValue();
			this.isConstrained = true;
		}

		@Override
		public void timeAdvanceGrant( LogicalTime time )
		{
			this.federateTime = ((HLAfloat64Time)time).getValue();
			this.isAdvancing = false;
		}

		@Override
		public void discoverObjectInstance( ObjectInstanceHandle theObject,
		                                    ObjectClassHandle theObjectClass,
		                                    String objectName )
		    throws FederateInternalError
		{
			log( "Discoverd Object: handle=" + theObject + ", classHandle=" +
			     theObjectClass + ", name=" + objectName );
		}

		@Override
		public void reflectAttributeValues( ObjectInstanceHandle theObject,
		                                    AttributeHandleValueMap theAttributes,
		                                    byte[] tag,
		                                    OrderType sentOrder,
		                                    TransportationTypeHandle transport,
		                                    SupplementalReflectInfo reflectInfo )
		    throws FederateInternalError
		{
				// just pass it on to the other method for printing purposes
				// passing null as the time will let the other method know it
				// it from us, not from the RTI
				reflectAttributeValues( theObject,
				                        theAttributes,
				                        tag,
				                        sentOrder,
				                        transport,
				                        null,
				                        sentOrder,
				                        reflectInfo );
		}

		@Override
		public void reflectAttributeValues( ObjectInstanceHandle theObject,
		                                    AttributeHandleValueMap theAttributes,
		                                    byte[] tag,
		                                    OrderType sentOrdering,
		                                    TransportationTypeHandle theTransport,
		                                    LogicalTime time,
		                                    OrderType receivedOrdering,
		                                    SupplementalReflectInfo reflectInfo )
		    throws FederateInternalError
		{
			StringBuilder builder = new StringBuilder( "Reflection for object:" );
			// print the handle
			builder.append( " handle=" + theObject );
			// print the tag
			builder.append( ", tag=" + new String(tag) );
			// print the time (if we have it) we'll get null if we are just receiving
			// a forwarded call from the other reflect callback above
			builder.append( ", time=" + ((HLAfloat64Time)time).getValue() );
			
			// print the attribute information
			builder.append( ", attributeCount=" + theAttributes.size() );
			builder.append( "\n" );
			for( AttributeHandle attributeHandle : theAttributes.keySet() )
			{
				// print the attibute handle
				builder.append( "\tattributeHandle=" );

				// if we're dealing with Flavor, decode into the appropriate enum value
				if( attributeHandle.equals(Vehicle.position) )
				{
					builder.append( attributeHandle );
					builder.append( " (Position)" );
					builder.append( ", attributeValue=" );
					Position rec;
					try {
						rec = _positionRecordCoder.decode(theAttributes.get(attributeHandle));
						builder.append(rec.toString());
					} catch (DecoderException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
				{
					builder.append( attributeHandle );
					builder.append( " (Unknown)   " );
				}
				
				builder.append( "\n" );
			}
			
			log( builder.toString() );
		}

		@Override
		public void receiveInteraction( InteractionClassHandle interactionClass,
		                                ParameterHandleValueMap theParameters,
		                                byte[] tag,
		                                OrderType sentOrdering,
		                                TransportationTypeHandle theTransport,
		                                SupplementalReceiveInfo receiveInfo )
		    throws FederateInternalError
		{
			// just pass it on to the other method for printing purposes
			// passing null as the time will let the other method know it
			// it from us, not from the RTI
			this.receiveInteraction( interactionClass,
			                         theParameters,
			                         tag,
			                         sentOrdering,
			                         theTransport,
			                         null,
			                         sentOrdering,
			                         receiveInfo );
			
		}

		@Override
		public void receiveInteraction( InteractionClassHandle interactionClass,
		                                ParameterHandleValueMap theParameters,
		                                byte[] tag,
		                                OrderType sentOrdering,
		                                TransportationTypeHandle theTransport,
		                                LogicalTime time,
		                                OrderType receivedOrdering,
		                                SupplementalReceiveInfo receiveInfo )
		    throws FederateInternalError
		{
			
			
			HLAASCIIstring strParam = this._encoderFactory.createHLAASCIIstring(); 
			//HLAfloat64Time timeParam = (HLAfloat64Time) this._encoderFactory.createHLAfloat64BE();
			JSONObject receiveInteraction= new JSONObject();
			

			if ( interactionClass.equals(AddVehicle.handle) )
			{

				try {
					receiveInteraction.put("evt_type", "AddVehicle");
					receiveInteraction.append("tag", new String(tag));
					
					// print the time (if we have it) we'll get null if we are just receiving
					// a forwarded call from the other reflect callback above
					if( time != null )					
						receiveInteraction.put("time", ((HLAfloat64Time)time).getValue());					
					

					for( ParameterHandle parameter : theParameters.keySet() )
					{

						try {
							if (parameter.equals(CreateObject.position))							{
								receiveInteraction.put("pos",_positionRecordCoder.decode(theParameters.get(parameter)) );								
							}
							else{
								strParam.decode(theParameters.get((AttributeHandle)parameter));
								receiveInteraction.put(AddVehicle.InteractionDataByHandle.get(parameter).name.toString(), strParam.getValue().toString());
							}
						}catch (DecoderException e) {
							e.printStackTrace();				
						}						
					}

				}catch (JSONException e2) {
					e2.printStackTrace();
				}
				
				try {
					sendQueue.put(receiveInteraction);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			}
			
		}
		
							
					          
						
			 	          
			 	          
			 	      
			      

			 public Socket GetSocket(){
				 return SocketElement;
			 }
			 public void SetSocket(Socket in){
				 SocketElement = in;
			 }
			 
			 	
			
			 
			
				
						
			
		

		
		@Override
		public void removeObjectInstance( ObjectInstanceHandle theObject,
		                                  byte[] tag,
		                                  OrderType sentOrdering,
		                                  SupplementalRemoveInfo removeInfo )
		    throws FederateInternalError
		{
			log( "Object Removed: handle=" + theObject );
		}

		//----------------------------------------------------------
		//                     STATIC METHODS
		//----------------------------------------------------------
	
}
