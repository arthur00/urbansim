package urbansim.social;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;

import hla.rti13.java1.ReflectedAttributes;
import hla.rti1516e.AttributeHandle;
import hla.rti1516e.AttributeHandleValueMap;
import hla.rti1516e.FederateHandle;
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

import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;

public class SocialFederateAmbassador extends NullFederateAmbassador 

{
	//----------------------------------------------------------
		//                    STATIC VARIABLES
		//----------------------------------------------------------

		
		//----------------------------------------------------------
		//                   INSTANCE VARIABLES
		//----------------------------------------------------------
		private SocialFederate federate;

		// these variables are accessible in the package
		protected double federateTime        = 0.0;
		protected double federateLookahead   = 1.0;
		
		protected boolean isRegulating       = false;
		protected boolean isConstrained      = false;
		protected boolean isAdvancing        = false;
		
		protected boolean isAnnounced        = false;
		protected boolean isReadyToRun       = false;
		protected boolean registrationFailed = false;
		protected boolean isRegistered = 	   false;
		
		protected PositionCoder _positionRecordCoder;
		protected EncoderFactory _encoderFactory;

		public  Socket socketElement;
		OutputStream OutElement;
		BufferedWriter bufOut;
		ConnectionClass connection;
		BlockingQueue receiveQueue;
		BlockingQueue sendQueue;
		//----------------------------------------------------------
		//                      CONSTRUCTORS
		//----------------------------------------------------------

		public SocialFederateAmbassador( SocialFederate federate, EncoderFactory encoder )
		{
			this.federate = federate;
			this._encoderFactory = encoder;
			this._positionRecordCoder = new PositionCoder(_encoderFactory);

			//HLAASCIIstring test = encoder.createHLAASCIIstring("lalal");
			
		}
		


		//----------------------------------------------------------
		//                    INSTANCE METHODS
		//----------------------------------------------------------
		private void log( String message )
		{
			System.out.println( "FederateAmbassador: " + message );
		}
		
		public void startConnection() throws IOException 
		{
			connection = federate.getConnection();
			receiveQueue = connection.GetInQueue();
			sendQueue = connection.GetOutQueue();
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
			if( label.equals(SocialFederate.READY_TO_RUN) )
				this.isAnnounced = true;
		}

		@Override
		public void federationSynchronized( String label, FederateHandleSet failed )
		{
			log( "Federation Synchronized: " + label );
			if( label.equals(SocialFederate.READY_TO_RUN) )
				this.isReadyToRun = true;
		}

		/**
		 * The RTI has informed us that time regulation is now enabled.
		 */
		/*
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

		*/
		
		@Override
		public void timeAdvanceGrant( LogicalTime time )
		{
			this.federateTime = ((HLAfloat64Time)time).getValue();
			this.isAdvancing = false;
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
			JSONObject json = new JSONObject();
			StringBuilder builder = new StringBuilder( "Reflection for object:" );			
			// print the handle
			//builder.append( " handle=" + theObject );		//!	
			json.put("handle", theObject);
			// print the tag
			//builder.append( ", tag=" + new String(tag) ); //!
			json.put("tag", new String(tag));
			// print the time (if we have it) we'll get null if we are just receiving
			// a forwarded call from the other reflect callback above
			if (time != null)
				//builder.append( ", time=" + ((HLAfloat64Time)time).getValue() ); //!
				json.put("time",((HLAfloat64Time)time).getValue());
			else
				//builder.append( ", time=???" ); //!
				json.put("time","???");
			
			// print the attribute information
			//builder.append( ", attributeCount=" + theAttributes.size() ); //!
			json.put("attributeCount", theAttributes.size());
			builder.append( "\n" );
			for( AttributeHandle attributeHandle : theAttributes.keySet() )
			{
				// print the attibute handle
				//builder.append( "\tattributeHandle=" ); //!

				// if we're dealing with Flavor, decode into the appropriate enum value
				if( attributeHandle.equals(Vehicle.position) )
				{
					//builder.append( attributeHandle ); //!
					json.put("Attribute Handle", attributeHandle);
					//builder.append( " (Position)" );
					// builder.append( ", attributeValue=" ); //!
					Position rec;
					try {
						rec = _positionRecordCoder.decode(theAttributes.get(attributeHandle));
						//builder.append(rec.toString()); //!
						json.put("attributeValue",rec.toString());
					} catch (DecoderException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				else
				{
					//builder.append( attributeHandle );
					//builder.append( " (Unknown)   " );
					json.put("attributeHandle", "Unknown");
				}
				
				// builder.append( "\n" ); //!
			      /*try (OutputStreamWriter out = new OutputStreamWriter(soc.getOutputStream(), StandardCharsets.UTF_8)) 	              
			      {
			          out.write(json.toString());
			          System.out.println(json.toString());
			      } catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
*/			}
			
			
			//log( builder.toString() );


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
			JSONObject json = new JSONObject();
			StringBuilder builder = new StringBuilder( "Interaction Received:" );
			HLAASCIIstring strParam = this._encoderFactory.createHLAASCIIstring(); 
			
			// print the handle
			// builder.append( " handle=" + interactionClass ); //!
			json.put("handle", interactionClass);
			
			/*
			if ( interactionClass.equals(AddVehicle.handle) )
			{
				builder.append( " (VehicleHandle)" );
			}
			else if ( interactionClass.equals(CreateObject.handle) )
			{
				builder.append( " (CreateObject)" );
			}
			else if ( interactionClass.equals(DeleteObject.handle) )
			{
				builder.append( " (DeleteObject)" );
			}
			*/ //!
			
			// print the tag
			// builder.append( ", tag=" + new String(tag) ); //!
			json.put("tag",new String(tag));
			// print the time (if we have it) we'll get null if we are just receiving
			// a forwarded call from the other reflect callback above
			if( time != null )
			{
				// builder.append( ", time=" + ((HLAfloat64Time)time).getValue() ); //!
				json.put("time",((HLAfloat64Time)time).getValue());
			}
			
			// print the parameter information
			//builder.append( ", parameterCount=" + theParameters.size() ); //!
			//builder.append( "\n" ); //!
			json.put("parameterCount", theParameters.size());
			for( ParameterHandle parameter : theParameters.keySet() )
			{
				// print the parameter handle
				//builder.append( "\tparamHandle=" ); //!
				//builder.append( parameter ); //!
				json.put("ParamHandle",parameter);
				try {
					if (parameter.equals(CreateObject.pos))
					{
						//builder.append( ", paramValue=" ); //!
						//builder.append(_positionRecordCoder.decode(theParameters.get(parameter))); //!
						json.put("paramValue",_positionRecordCoder.decode(theParameters.get(parameter)));
					}
					else
					{
						strParam.decode(theParameters.get(parameter));
						// print the parameter value
						//builder.append( ", paramValue=" ); //!
						//builder.append( strParam.getValue() ); //!
						json.put("paramValue",strParam.getValue());
					}
				} 
				catch (DecoderException e) {
					//builder.append("Couldn't read!");
					json.put("message", "Couldn't read");
				}
				//builder.append( "\n" );
			}
			
			try
			{
				sendQueue.put(json);
			}catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		

		@Override
		public void removeObjectInstance( ObjectInstanceHandle theObject,
		                                  byte[] tag,
		                                  OrderType sentOrdering,
		                                  SupplementalRemoveInfo removeInfo )
		    throws FederateInternalError
		{
			//log( "Object Removed: handle=" + theObject );.
			JSONObject json = new JSONObject();
			log( "Object Removed:");
			json.put("handle", theObject);
/*		      try (OutputStreamWriter out = new OutputStreamWriter(soc.getOutputStream(), StandardCharsets.UTF_8)) 	              
		      {
		          out.write(json.toString());
		          System.out.println(json.toString());
		      } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
		}
		
		@Override
	   public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName) throws FederateInternalError 
	   {	
			JSONObject json = new JSONObject();
			log( "Discovered Object:");
			json.put("handle", theObject);
			json.put("classHandle", theObjectClass);
			json.put("name", objectName);
			log(json.toString());
		      /*try (OutputStreamWriter out = new OutputStreamWriter(soc.getOutputStream(), StandardCharsets.UTF_8)) 	              
		      {
		          out.write(json.toString());
		          //System.out.println(json.toString());
		      } catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				//log( "Discoverd Object: handle=" + theObject + ", classHandle=" +
			    //theObjectClass + ", name=" + objectName );
*/		}
			

	   @Override
	   public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName, FederateHandle producingFederate) throws FederateInternalError
	   {
	    discoverObjectInstance(theObject, theObjectClass, objectName);
	   }

		//----------------------------------------------------------
		//                     STATIC METHODS
		//----------------------------------------------------------

		
		//----------------------------------------------------------
		//                    MAIN METHOD
		//----------------------------------------------------------		 
}
