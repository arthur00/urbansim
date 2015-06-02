package urbansim.sumo;

import urbansim.sumo.SumoFederate;
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
import hla.rti1516e.encoding.HLAinteger16BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;

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
		protected PositionCoder _positionRecordCoder;
		protected EncoderFactory _encoderFactory;

		//----------------------------------------------------------
		//                      CONSTRUCTORS
		//----------------------------------------------------------

		public SumoFederateAmbassador( SumoFederate federate, EncoderFactory encoder )
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
		
		//////////////////////////////////////////////////////////////////////////
		////////////////////////// RTI Callback Methods //////////////////////////
		//////////////////////////////////////////////////////////////////////////
		@Override
		public void synchronizationPointRegistrationFailed( String label,
		                                                    SynchronizationPointFailureReason reason )
		{
			log( "Failed to register sync point: " + label + ", reason="+reason );
		}

		@Override
		public void synchronizationPointRegistrationSucceeded( String label )
		{
			log( "Successfully registered sync point: " + label );
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
			StringBuilder builder = new StringBuilder( "Interaction Received:" );
			HLAASCIIstring strParam = this._encoderFactory.createHLAASCIIstring(); 
			
			// print the handle
			builder.append( " handle=" + interactionClass );
			if( interactionClass.equals(AddVehicle.handle) )
			{
				builder.append( " (VehicleHandle)" );
			}
			
			// print the tag
			builder.append( ", tag=" + new String(tag) );
			// print the time (if we have it) we'll get null if we are just receiving
			// a forwarded call from the other reflect callback above
			if( time != null )
			{
				builder.append( ", time=" + ((HLAfloat64Time)time).getValue() );
			}
			
			// print the parameter information
			builder.append( ", parameterCount=" + theParameters.size() );
			builder.append( "\n" );
			for( ParameterHandle parameter : theParameters.keySet() )
			{
				// print the parameter handle
				builder.append( "\tparamHandle=" );
				builder.append( parameter );
				try {
					strParam.decode(theParameters.get(parameter));
					// print the parameter value
					builder.append( ", paramValue=" );
					builder.append( strParam.getValue() );
				} catch (DecoderException e) {
					// TODO Auto-generated catch block
					builder.append("Couldn't read!");
				}
				builder.append( "\n" );
			}

			log( builder.toString() );
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
