package se.pitch.sushifederation.transport;

import hla.rti1516e.*;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleSetFactory;
import hla.rti1516e.FederateHandleSetFactory;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.LogicalTimeInterval;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import se.pitch.sushifederation.*;
import se.pitch.sushifederation.manager.ManagerNames;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Properties;

public final class Transport {
   //system properties used throughout
   private static String _fileSeparator = System.getProperty("file.separator");
   private static String _userDirectory = System.getProperty("user.dir");

   private TransportFrame _userInterface;
   private FederateHandle _federateHandle; // -1 when not joined
   private boolean _simulationEndsReceived;
   private FedAmbImpl _fedAmb;
   private Properties _properties;

   //barriers used to await announcement of synchronization points
   //these must be allocated here lest announcement sneak up on us
   private Barrier _readyToPopulateAnnouncementBarrier = new Barrier();
   private Barrier _readyToRunAnnouncementBarrier = new Barrier();
   private Barrier _readyToResignAnnouncementBarrier = new Barrier();

   //we use HLAfloat64Time
   private HLAfloat64TimeFactory _logicalTimeFactory = (HLAfloat64TimeFactory) LogicalTimeFactoryFactory.getLogicalTimeFactory(HLAfloat64TimeFactory.NAME);
   private LogicalTime _logicalTime;
   private LogicalTimeInterval _lookahead;
   private LogicalTimeInterval _advanceInterval; //how far to move on each time step
   private LogicalTime _targetTime;              //where we're moving to

   //things dependent on RTI implementation in use
   private RTIambassador _rti;
   public ParameterHandleValueMapFactory _parameterFactory;
   public AttributeHandleValueMapFactory _attributeFactory;
   public AttributeHandleSetFactory _attributeHandleSetFactory;
   public FederateHandleSetFactory _federateHandleSetFactory;

   //handles and handle sets
   private ObjectClassHandle _RestaurantClass;
   private ObjectClassHandle _BoatClass;
   private ObjectClassHandle _ServingClass;
   private AttributeHandle _privilegeToDeleteObjectAttribute;
   private AttributeHandle _positionAttribute;
   private AttributeHandleSet _positionAttributeAsSet;
   private AttributeHandle _typeAttribute;
   private AttributeHandle _spaceAvailableAttribute;
   private AttributeHandle _cargoAttribute;
   private AttributeHandleSet _BoatAttributes;
   private InteractionClassHandle _SimulationEndsClass;
   private InteractionClassHandle _TransferAcceptedClass;
   private ParameterHandle _servingNameParameter;

   private double _boatRate;
   private BoatTable _boatTable;     //table of our Boats
   private CallbackQueue _callbackQueue;
   private Hashtable<ObjectInstanceHandle, Serving> _servings; //key: instance handle; value: Serving

   public Transport(Properties props) {
      _federateHandle = null;  //not joined
      _boatTable = new BoatTable();
      _servings = new Hashtable();
      _callbackQueue = new CallbackQueue();
      _simulationEndsReceived = false;

      _userInterface = new TransportFrame();
      _userInterface.finishConstruction(this, _boatTable);
      _userInterface.setVisible(true);
      _userInterface.lastAdjustments();

      _properties = props;
      //System.out.println("host: " + props.get("RTI_HOST"));
      //System.out.println("port: " + props.get("RTI_PORT"));
      //System.out.println("config: " + props.get("CONFIG"));
      //create RTI implementation
      try {
         _rti = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
         _userInterface.post("RTIambassador created");
         _fedAmb = new FedAmbImpl(this, _userInterface);
         String settingsDesignator = "crcHost=" + _properties.get("RTI_HOST") + "\ncrcPort=" + _properties.get("RTI_PORT");
         _rti.connect(_fedAmb, CallbackModel.HLA_IMMEDIATE, settingsDesignator);

         System.out.println("Transport started.");
      }
      catch (Exception e) {
         _userInterface.post("Transport: constructor failed: " + e);
         _userInterface.post("You may as well exit.");
      }
   }

   public static void main(String[] args) {
      Properties props = parseArgs(args);
      loadProperties(props);
      Transport production = new Transport(props);
      production.mainThread();
   }

   //the main thread
   private void mainThread() {
      Barrier barrier;
      Object[] result;

      try {
         getConfigurationData();

         //create federation execution (if necessary) and join
         String fedexName = (String) _properties.get("FEDEX");
         URL fomURL;
         String urlString =
               (String)_properties.get("CONFIG")
                     + fedexName
                     + ".xml";
         fomURL = new URL(urlString);
         //the federation execution may already exist
         try {
            _rti.createFederationExecution(fedexName, fomURL);
            _userInterface.post("Federation execution " + fedexName + " created.");
         }
         catch (FederationExecutionAlreadyExists e) {
            _userInterface.post("Federation execution " + fedexName
                  + " already exists.");
         }
         //join federation execution
         fedexName = (String)_properties.get("FEDEX");
         _federateHandle = _rti.joinFederationExecution(
               TransportNames._federateType,
               TransportNames._federateType,
               fedexName,
               new URL[] {fomURL});
         _userInterface.post("Joined as federate " + _federateHandle);

         //do other implementation-specific things
         _parameterFactory = _rti.getParameterHandleValueMapFactory();
         _attributeFactory = _rti.getAttributeHandleValueMapFactory();
         _attributeHandleSetFactory = _rti.getAttributeHandleSetFactory();
         _federateHandleSetFactory = _rti.getFederateHandleSetFactory();

         //enable time constraint
         _userInterface.post("Enabling time constraint...");
         barrier = new Barrier();
         _fedAmb.setEnableTimeConstrainedBarrier(barrier);
         _rti.enableTimeConstrained();
         result = barrier.await();
         _logicalTime = (LogicalTime)result[0];
         _userInterface.post("...constraint enabled at " + _logicalTime);

         //enable time regulation
         _logicalTime = _logicalTimeFactory.makeTime(new Double(getProperty("Federation.initialTime")));
         _lookahead = _logicalTimeFactory.makeInterval(new Double((getProperty("Transport.lookahead"))));
         _advanceInterval = _logicalTimeFactory.makeInterval(new Double((getProperty("Transport.advanceInterval"))));
         _userInterface.post("Enabling time regulation...");
         barrier = new Barrier();
         _fedAmb.setEnableTimeRegulationBarrier(barrier);
         _rti.enableTimeRegulation(_lookahead);
         result = barrier.await();
         _logicalTime = (LogicalTime)result[0];
         _userInterface.post("...regulation enabled at " + _logicalTime);
         _userInterface.setLogicalTime(((HLAfloat64Time)_logicalTime).getValue());
         _userInterface.setTimeStateGranted();

         getHandles();
         publish();
         subscribe();

         //Transport achieves ReadyToPopulate and waits for rest of federation
         _readyToPopulateAnnouncementBarrier.await();
         _userInterface.post("Waiting for ReadyToPopulate...");
         barrier = new Barrier(ManagerNames._readyToPopulate);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToPopulate);
         barrier.await();
         _userInterface.post("...federation synchronized.");

         makeInitialInstances();

         //Transport achieves ReadyToRun and waits for rest of federation
         _readyToRunAnnouncementBarrier.await();
         _userInterface.post("Waiting for ReadyToRun...");
         barrier = new Barrier(ManagerNames._readyToRun);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToRun);
         barrier.await();
         _userInterface.post("...federation synchronized.");

         _rti.enableAsynchronousDelivery();

         //advance time in steps until SimulationEnds received
         timeLoop:
         while (!_simulationEndsReceived) {
            //advance by a step
            _targetTime = _logicalTime.add(_advanceInterval);
            _userInterface.setTimeStateAdvancing();
            _rti.timeAdvanceRequest(_targetTime);
            //chew through all the events we receive from the RTI
            boolean wasTimeAdvanceGrant;
            do {
               Callback callback = _callbackQueue.dequeue();
               wasTimeAdvanceGrant = callback.dispatch();
               if (_simulationEndsReceived) break timeLoop;
            } while (!wasTimeAdvanceGrant);
            updateInternalStateAtNewTime();
         }
         _userInterface.post("SimulationEnds received.");

         //Transport achieves ReadyToResign and waits for rest of federation
         _readyToResignAnnouncementBarrier.await();
         _userInterface.post("Waiting for ReadyToResign...");
         barrier = new Barrier(ManagerNames._readyToResign);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToResign);
         barrier.await();
         _userInterface.post("...federation synchronized.");
         _rti.resignFederationExecution(ResignAction.DELETE_OBJECTS_THEN_DIVEST);
         _federateHandle = null;
         _userInterface.post("All done.");

      }
      catch (Exception e) {
         _userInterface.post("Exception in main thread: " + e);
         e.printStackTrace();
      }
   }

   private void getHandles()
         throws RTIexception
   {
      _RestaurantClass = _rti.getObjectClassHandle(RestaurantNames._RestaurantClassName);
      _ServingClass = _rti.getObjectClassHandle(RestaurantNames._ServingClassName);
      _BoatClass = _rti.getObjectClassHandle(RestaurantNames._BoatClassName);
      _privilegeToDeleteObjectAttribute = _rti.getAttributeHandle(
            _RestaurantClass,
            RestaurantNames._privilegeToDeleteObjectAttributeName);
      _positionAttribute = _rti.getAttributeHandle(
            _RestaurantClass,
            RestaurantNames._positionAttributeName);
      _typeAttribute = _rti.getAttributeHandle(
            _ServingClass,
            RestaurantNames._typeAttributeName);
      _spaceAvailableAttribute = _rti.getAttributeHandle(
            _BoatClass,
            RestaurantNames._spaceAvailableAttributeName);
      _cargoAttribute = _rti.getAttributeHandle(
            _BoatClass,
            RestaurantNames._cargoAttributeName);

      _positionAttributeAsSet = _attributeHandleSetFactory.create();
      _positionAttributeAsSet.add(_positionAttribute);

      _BoatAttributes = _attributeHandleSetFactory.create();
      _BoatAttributes.add(_privilegeToDeleteObjectAttribute);
      _BoatAttributes.add(_positionAttribute);
      _BoatAttributes.add(_spaceAvailableAttribute);
      _BoatAttributes.add(_cargoAttribute);

      _SimulationEndsClass =
            _rti.getInteractionClassHandle(ManagerNames._SimulationEndsClassName);
      _TransferAcceptedClass =
            _rti.getInteractionClassHandle(RestaurantNames._TransferAcceptedClassName);

      _servingNameParameter = _rti.getParameterHandle(
            _TransferAcceptedClass,
            RestaurantNames._servingNameParameterName);
   }

   private void publish()
         throws RTIexception
   {
      //publish Boat (including privToDelete)
      _rti.publishObjectClassAttributes(_BoatClass, _BoatAttributes);
      //publish Serving class position attribute because we update it
      _rti.publishObjectClassAttributes(_ServingClass, _positionAttributeAsSet);
      //publish interaction because we send it
      _rti.publishInteractionClass(_TransferAcceptedClass);
   }

   private void subscribe()
         throws RTIexception
   {
      _rti.subscribeObjectClassAttributes(_ServingClass, _positionAttributeAsSet);
      _rti.subscribeInteractionClass(_SimulationEndsClass);
   }

   private volatile boolean _reservationComplete;
   private volatile boolean _reservationSucceeded;
   private final Object _reservationSemaphore = new Object();

   public final void objectInstanceNameReservationSucceeded()
   {
      synchronized (_reservationSemaphore) {
         _reservationComplete = true;
         _reservationSucceeded = true;
         _reservationSemaphore.notifyAll();
      }
   }

   public final void objectInstanceNameReservationFailed()
   {
      synchronized (_reservationSemaphore) {
         _reservationComplete = true;
         _reservationSucceeded = false;
         _reservationSemaphore.notifyAll();
      }
   }

   private void makeInitialInstances()
         throws RTIexception
   {
      _boatRate = (new Double((getProperty("Transport.Boats.rate")))).doubleValue();
      int numberOfBoats = Integer.parseInt(getProperty("Transport.numberOfBoats"));
      for (int serial = 0; serial < numberOfBoats; ++serial) {
         String prop = "Transport.Boat.position." + serial;
         double position = (new Double((getProperty(prop)))).doubleValue();
         String instanceName = "B_" + _federateHandle + "_" + serial;

         try {
            _reservationComplete = false;
            synchronized (_reservationSemaphore) {
               _rti.reserveObjectInstanceName(instanceName);
               while(!_reservationComplete) {
                  try {
                     _reservationSemaphore.wait();
                  } catch (InterruptedException e) {
                     //ignore
                  }
               }
            }
            if (!_reservationSucceeded) {
               throw new RTIexception("Failed to reserve name: " + instanceName);
            }
         } catch (IllegalName e) {
            throw new RTIexception("Illegal Name: " + instanceName);
         }

         ObjectInstanceHandle handle = _rti.registerObjectInstance(_BoatClass, instanceName);
         _boatTable.add(
               handle,
               instanceName,
               position,
               Boat.State.EMPTY,
               null,
               "");
         updateBoatBySerial(serial);
      }
   }

   private void moveBoats()
         throws RTIexception
   {
      try {
         int boatCount = _boatTable.getRowCount();
         for (int serial = 0; serial < boatCount; ++serial) {
            //update table
            double positionAsDouble = _boatTable.getPositionBySerial(serial);
            positionAsDouble += _boatRate
                  * ((HLAfloat64Interval)_advanceInterval).getValue();
            if (positionAsDouble >= 360.0) positionAsDouble -= 360.0;
            _boatTable.setPositionBySerial(serial, positionAsDouble);
            //update Boat attributes
            updateBoatBySerial(serial);
            //if necessary, update value for Serving carried on Boat
            if (_boatTable.getStateBySerial(serial) == Boat.State.LOADED) {
               ObjectInstanceHandle servingHandle = _boatTable.getServingBySerial(serial);
               updateServing(servingHandle, positionAsDouble);
            }
         }
      }
      catch (TransportInternalError e) {
         _userInterface.post("moveBoats: " + e.getMessage());
      }
   }

   private void updateServing(ObjectInstanceHandle servingHandle, double positionAsDouble)
         throws RTIexception, TransportInternalError
   {
      try {
         Serving serving = _servings.get(servingHandle);
         if (serving == null) throw new TransportInternalError("Serving "
               + servingHandle + " not owned.");
         Position position = new Position(positionAsDouble, OffsetEnum.ON_CANAL);
         AttributeHandleValueMap sa = _attributeFactory.create(1);
         sa.put(_positionAttribute, position.encode());
         LogicalTime sendTime = _logicalTime.add(_lookahead).add(_advanceInterval);
         _rti.updateAttributeValues(servingHandle, sa, null, sendTime);
      } catch (AttributeNotOwned e) {
         //the RTI may transfer ownership away before before we can adjust state
         //write informative message and expect us to catch up later
         _userInterface.post("Lost ownership of serving " + servingHandle
               + " before AODN arrived");
      }
   }

   private void updateBoatBySerial(int serial)
         throws RTIexception
   {
      updateBoat(_boatTable.getHandleBySerial(serial));
   }

   private void updateBoat(ObjectInstanceHandle handle)
         throws RTIexception
   {
      //update attribute values for Boat
      int serial = _boatTable.getSerialByHandle(handle);
      Position position = new Position(
            _boatTable.getPositionBySerial(serial),
            OffsetEnum.ON_CANAL);
      boolean spaceAvailable;
      String cargo;
      ObjectInstanceHandle servingHandle;
      if (_boatTable.getStateBySerial(serial) == Boat.State.LOADED) {
         spaceAvailable = false;
         servingHandle = _boatTable.getServingBySerial(serial);
         cargo = _rti.getObjectInstanceName(servingHandle);
      }
      else {
         spaceAvailable = true;
         cargo = "";
      }
      AttributeHandleValueMap sa = _attributeFactory.create(3);
      sa.put(_positionAttribute, position.encode());
      sa.put(_spaceAvailableAttribute, SpaceAvailable.encode(spaceAvailable));
      sa.put(_cargoAttribute, InstanceName.encode(cargo));
      LogicalTime sendTime = _logicalTime.add(_lookahead).add(_advanceInterval);
      _rti.updateAttributeValues(
            _boatTable.getHandleBySerial(serial),
            sa,
            null,
            sendTime);
      /*
    _userInterface.post("Updating boat " + _boatTable.getNameBySerial(serial)
      + " at time " + sendTime);
      */
   }

   //federate ambassador calls this when it gets an announcement
   public void recordSynchronizationPointAnnouncement(String label) {
      if (label.equals(ManagerNames._readyToPopulate))
         _readyToPopulateAnnouncementBarrier.lower(null);
      else if (label.equals(ManagerNames._readyToRun))
         _readyToRunAnnouncementBarrier.lower(null);
      else if (label.equals(ManagerNames._readyToResign))
         _readyToResignAnnouncementBarrier.lower(null);
      else
         _userInterface.post("INFO: unexpected sync point announced: " + label);
   }

   private void updateInternalStateAtNewTime()
         throws RTIexception
   {
      //we're at the new logical time: update the Boats
      moveBoats();
   }

   private void getConfigurationData() {
   }

   //defined so that a missing property doesn't cause a crash later
   private String getProperty(String name) {
      String value = (String)_properties.get(name);
      if (value == null) {
         System.err.println("Property " + name + " not defined; exiting.");
         System.exit(1);
      }
      return value;
   }

   //used by UI
   public boolean isJoined() { return _federateHandle != null; }

   private static Properties parseArgs(String args[]) {
      Properties props = new Properties();
      //default values
      try {
         //host name for Central RTI Component
         String defaultRTIhost = InetAddress.getLocalHost().getHostName();
         String cmdHost = System.getProperty("RTI_HOST");
         //System.out.println("Cmd line had " + cmdHost);
         if (cmdHost == null) props.put("RTI_HOST", defaultRTIhost);
         else props.put("RTI_HOST", cmdHost);

         //port number for Central RTI component
         String defaultRTIport = "8989";
         String cmdPort = System.getProperty("RTI_PORT");
         //System.out.println("Cmd line had " + cmdPort);
         if (cmdPort == null) props.put("RTI_PORT", defaultRTIport);
         else props.put("RTI_PORT", cmdPort);

         //form URL
         String urlString = System.getProperty("CONFIG",
               "file:" + _userDirectory + _fileSeparator + "config" + _fileSeparator);
         props.put("CONFIG", urlString);
         //System.out.println("Config URL: " + props.get("CONFIG"));

         //federation execution name
         String defaultFedExName = "SushiRestaurant";
         String cmdFedExName = System.getProperty("FEDEX");
         if (cmdFedExName == null) props.put("FEDEX", defaultFedExName);
         else props.put("FEDEX", cmdFedExName);
      }
      catch (UnknownHostException e) {
         System.out.println("Transport.parseArgs: default arguments failed: " + e);
         System.exit(1);
      }
      return props;
   }

   //load other properties from URL
   private static void loadProperties(Properties props) {
      try {
         //form URL for properties
         String urlString =
               (String)props.get("CONFIG")
                     + (String)props.get("FEDEX")
                     + ".props";
         URL propsURL = new URL(urlString);
         props.load(propsURL.openStream());
      }
      catch (Exception e) {
         System.out.println("Transport failed to load properties: " + e);
         System.exit(1);
      }
   }

   //represents one callback coming from RTI
   //This class is defined within Production so the dispatch routines
   //of its subclasses have the context of Production
   public abstract class Callback {
      //returns true if event was a grant
      public abstract boolean dispatch()
            throws RTIexception;
   }

   //represents one callback that is an event (carries a time)
   public abstract class ExternalEvent extends Callback {
      protected LogicalTime _time;

      public LogicalTime getTime() { return _time; }
      //returns true if event was a grant
      public abstract boolean dispatch()
            throws RTIexception;
   }

   public void queueAttributeOwnershipAcquisitionNotificationCallback(
         ObjectInstanceHandle theObject,
         AttributeHandleSet theAttributes)
   {
      Callback callback = new AOANcallback(
            theObject,
            theAttributes);
      _callbackQueue.enqueue(callback);
   }

   public final class AOANcallback extends Callback {
      private ObjectInstanceHandle _object;
      private AttributeHandleSet _attributes;

      public AOANcallback(
            ObjectInstanceHandle theObject,
            AttributeHandleSet theAttributes)
      {
         _object = theObject;
         _attributes = theAttributes;
      }

      public boolean dispatch() {
         try {
            //is the object a Serving?
            ObjectClassHandle objectClass = _rti.getKnownObjectClassHandle(_object);
            if (!objectClass.equals(_ServingClass)) {
               throw new AttributeAcquisitionWasNotRequested("not Serving class: " + objectClass);
            }
            //is it the position attribute?
            if (!_attributes.equals(_positionAttributeAsSet)) throw new
                  AttributeAcquisitionWasNotRequested("not position: " + _attributes);
            int boatSerial = _boatTable.getBoatForServing(_object);
            ObjectInstanceHandle boatHandle = _boatTable.getHandleBySerial(boatSerial);
            if (boatSerial < 0) throw new TransportInternalError(
                  "No boat was waiting for Serving " + _object);
            String servingName = _rti.getObjectInstanceName(_object);
            _userInterface.post("Acquired serving " + servingName);
            //change Serving state
            Serving serving = new Serving();
            serving.setHandle(_object);
            serving.setName(servingName);
            Position newServingPosition = new Position(
                  _boatTable.getPositionBySerial(boatSerial),
                  OffsetEnum.ON_CANAL);
            serving.setPosition(newServingPosition);
            serving.setPositionState(AttributeState.OWNED_INCONSISTENT);
            _servings.put(_object, serving);
            //change Boat state
            _boatTable.setState(boatHandle, Boat.State.LOADED);
            _boatTable.setCargoBySerial(boatSerial, servingName);
            _boatTable.setServing(boatHandle, _object);
            _boatTable.setSpaceAvailableBySerial(boatSerial, false);
            //update Boat attributes
            AttributeHandleValueMap sa = _attributeFactory.create(2);
            sa.put(_spaceAvailableAttribute, SpaceAvailable.encode(false));
            sa.put(_cargoAttribute, InstanceName.encode(servingName));
            LogicalTime sendTime = _logicalTime.add(_lookahead).add(_advanceInterval);
             _rti.updateAttributeValues(boatHandle, sa, null, sendTime);
            //update Serving position attribute
            sa = _attributeFactory.create(1);
            sa.put(_positionAttribute, newServingPosition.encode());
            _rti.updateAttributeValues(_object, sa, null, sendTime);
            //make Serving position available for acquisition by Consumption
            _rti.negotiatedAttributeOwnershipDivestiture(_object, _attributes, null);
         }
         catch (TransportInternalError e) {
            _userInterface.post("ERROR AttrOwnAcqNotif: " + e.getMessage());
         }
         catch (RTIexception e) {
            _userInterface.post("ERROR AttrOwnAcqNotif: " + e);
         }
         return false;
      }
   } //end AttributeOwnershipAcquisitionNotificationCallback

   public void queueAttributeOwnershipDivestitureNotificationCallback(
         ObjectInstanceHandle theObject,
         AttributeHandleSet theAttributes)
   {
      Callback callback = new AODNcallback(
            theObject,
            theAttributes);
      _callbackQueue.enqueue(callback);
   }

   public final class AODNcallback extends Callback {
      private ObjectInstanceHandle _object;
      private AttributeHandleSet _attributes;

      public AODNcallback(
            ObjectInstanceHandle theObject,
            AttributeHandleSet theAttributes)
      {
         _object = theObject;
         _attributes = theAttributes;
      }

      public boolean dispatch() {
         try {
            //is the object a Serving?
            ObjectClassHandle objectClass = _rti.getKnownObjectClassHandle(_object);
            if (!objectClass.equals(_ServingClass)) throw new
                  AttributeAcquisitionWasNotRequested("not Serving class: " + objectClass);
            //is it the position attribute?
            if (!_attributes.equals(_positionAttributeAsSet)) throw new
                  AttributeAcquisitionWasNotRequested("not position: " + _attributes);
            int boatSerial = _boatTable.getBoatForServing(_object);
            ObjectInstanceHandle boatHandle = _boatTable.getHandleBySerial(boatSerial);
            if (boatSerial < 0) throw new TransportInternalError(
                  "No boat was carrying Serving " + _object);
            //was the Boat in the right state?
            if (_boatTable.getStateBySerial(boatSerial) != Boat.State.LOADED) throw new
                  TransportInternalError("Boat " + boatHandle + " not loaded");
            String servingName = _rti.getObjectInstanceName(_object);
            _userInterface.post("Divesting serving " + servingName);
            //change Serving state
            Serving serving = _servings.get(_object);
            if (serving == null) {
               throw new RTIexception("Serving " + _object + " not known to federate");
            }
            _servings.remove(_object);
            //change Boat state
            _boatTable.setState(boatHandle, Boat.State.EMPTY);
            _boatTable.setCargoBySerial(boatSerial, "");
            _boatTable.setSpaceAvailableBySerial(boatSerial, true);
            _boatTable.setServing(boatHandle, null);
            //update Boat attributes
            AttributeHandleValueMap sa = _attributeFactory.create(2);
            sa.put(_spaceAvailableAttribute, SpaceAvailable.encode(true));
            sa.put(_cargoAttribute, InstanceName.encode(""));
            LogicalTime sendTime = _logicalTime.add(_lookahead).add(_advanceInterval);
            _rti.updateAttributeValues(boatHandle, sa, null, sendTime);
            _rti.attributeOwnershipDivestitureIfWanted(_object, _attributes);
         }
         catch (TransportInternalError e) {
            _userInterface.post("ERROR AttrOwnDivNotif: " + e.getMessage());
         }
         catch (RTIexception e) {
            _userInterface.post("ERROR AttrOwnDivNotif: " + e);
         }
         return false;
      }
   } //end AttributeOwnershipDivestitureNotificationCallback

   public void queueAttributeOwnershipUnavailableCallback(
         ObjectInstanceHandle theObject,
         AttributeHandleSet theAttributes)
   {
      Callback callback = new AttributeOwnershipUnavailableCallback(
            theObject,
            theAttributes);
      _callbackQueue.enqueue(callback);
   }

   public final class AttributeOwnershipUnavailableCallback extends Callback {
      private ObjectInstanceHandle _object;
      private AttributeHandleSet _attributes;

      public AttributeOwnershipUnavailableCallback(
            ObjectInstanceHandle theObject,
            AttributeHandleSet theAttributes)
      {
         _object = theObject;
         _attributes = theAttributes;
      }

      public boolean dispatch() {
         try {
            //we'll assume the object is a Serving and the attribute is position
            int boatSerial = _boatTable.getBoatForServing(_object);
            ObjectInstanceHandle boatHandle = _boatTable.getHandleBySerial(boatSerial);
            if (boatSerial < 0) throw new TransportInternalError(
                  "No boat was waiting for Serving " + _object);
            String servingName = _rti.getObjectInstanceName(_object);
            _userInterface.post("Serving " + servingName + " was unvailable");
            //change Boat state
            _boatTable.setState(boatHandle, Boat.State.EMPTY);
            _boatTable.setCargoBySerial(boatSerial, "");
            _boatTable.setServing(boatHandle, null);
            _boatTable.setSpaceAvailableBySerial(boatSerial, true);
         }
         catch (TransportInternalError e) {
            _userInterface.post("ERROR AttrOwnUnav: " + e.getMessage());
         }
         catch (RTIexception e) {
            _userInterface.post("ERROR AttrOwnUnav: " + e);
         }
         return false;
      }
   } //end AttributeOwnershipUnavailableCallback

   public void queueGrantEvent(LogicalTime time) {
      ExternalEvent event = new GrantEvent(time);
      _callbackQueue.enqueue(event);
   }

   public final class GrantEvent extends ExternalEvent {

      public GrantEvent(LogicalTime time) {
         _time = time;
      }

      //this dispatch doesn't do as much as the others because actions upon
      //grant are handled differently.
      public boolean dispatch() {
         _logicalTime = _time;
         //_userInterface.post("...granted to " + _logicalTime);
         _userInterface.setLogicalTime(((HLAfloat64Time)_logicalTime).getValue());
         _userInterface.setTimeStateGranted();
         return true;
      }
   } //end GrantEvent

   public void queueProvideAttributeValueUpdateCallback(
         ObjectInstanceHandle objectHandle,
         AttributeHandleSet theAttributes)
   {
      Callback callback = new ProvideAttributeValueUpdateCallback(
            objectHandle,
            theAttributes);
      _callbackQueue.enqueue(callback);
   }

   public final class ProvideAttributeValueUpdateCallback extends Callback {
      ObjectInstanceHandle _object;
      AttributeHandleSet _attributes;

      public ProvideAttributeValueUpdateCallback(
            ObjectInstanceHandle objectHandle,
            AttributeHandleSet attributes)
      {
         _object = objectHandle;
         _attributes = attributes;
      }

      public boolean dispatch()
            throws RTIexception
      {
         try {
            Serving serving = _servings.get(_object);
            if (serving != null) {
               AttributeHandleValueMap sa = _attributeFactory.create(1);
               for (AttributeHandle h : _attributes) {
                  if (h == _positionAttribute) {
                     if (serving.getPositionState() != AttributeState.OWNED_INCONSISTENT
                           && serving.getPositionState() != AttributeState.OWNED_CONSISTENT) throw new RTIexception(
                           "Position attribute not owned for instance " + _object);
                     sa.put(_positionAttribute, serving.getPosition().encode());
                     serving.setPositionState(AttributeState.OWNED_CONSISTENT);
                  }
                  else throw new RTIexception("Attribute " + h
                        + " not known for instance " + _object);
               }
               LogicalTime sendTime = _logicalTime.add(_lookahead).add(_advanceInterval);
               _rti.updateAttributeValues(serving.getHandle(), sa, null, sendTime);
            }
            else {
               //this will throw ArrayIndexOutOfBoundsException if not found
               int serial = _boatTable.getSerialByHandle(_object);
               //we'll update all attributes of Boat: it's easy & respects I/F spec
               updateBoat(_object);
            }
         }
         catch (ArrayIndexOutOfBoundsException e) {
            throw new RTIexception("Instance " + _object + " not known.");
         }
         catch (RTIexception e) {
            _userInterface.post("ERROR provideAttributeValueUpdate: " + e);
         }
         return false;
      }
   } //end ProvideAttributeValueUpdateCallback

   public void queueReceiveInteractionCallback(
         InteractionClassHandle interactionClass)
   {
      Callback callback = new ReceiveInteractionCallback(
            interactionClass);
      _callbackQueue.enqueue(callback);
   }

   public final class ReceiveInteractionCallback extends Callback {
      InteractionClassHandle _class;

      public ReceiveInteractionCallback(
            InteractionClassHandle interactionClass)
      {
         _class = interactionClass;
      }

      public boolean dispatch() throws RTIexception
      {
         if (!_class.equals(_SimulationEndsClass)) {
            _userInterface.post("ERROR unexpected interaction class " + _class);
            throw new RTIexception("class not " + _SimulationEndsClass);
         }
         //updates of booleans are atomic: not synch problem
         _simulationEndsReceived = true;
         return false;
      }
   } //end ReceiveInteractionCallback

   public void queueRequestAttributeOwnershipAssumptionCallback(
         ObjectInstanceHandle theObject,
         AttributeHandleSet theAttributes,
         byte[] userSuppliedTag)
   {
      Callback callback = new RAOAcallback(
            theObject,
            theAttributes,
            userSuppliedTag);
      _callbackQueue.enqueue(callback);
   }

   public final class RAOAcallback extends Callback {
      private ObjectInstanceHandle _object;
      private AttributeHandleSet _attributes;
      private byte[] _tag;

      public RAOAcallback(
            ObjectInstanceHandle theObject,
            AttributeHandleSet theAttributes,
            byte[] userSuppliedTag)
      {
         _object = theObject;
         _attributes = theAttributes;
         _tag = userSuppliedTag;
      }

      public boolean dispatch() {
         String tagString = new String(_tag);
         try {
            //the tag should contain the name of a Boat instance
            ObjectInstanceHandle boatHandle = _rti.getObjectInstanceHandle(tagString);
            int boatSerial = _boatTable.getSerialByHandle(boatHandle);
            //we're going to assume that the object is a Serving and the
            //attribute set contains position
            String objectName = _rti.getObjectInstanceName(_object);
            if (_boatTable.getStateBySerial(boatSerial) == Boat.State.EMPTY) {
               /*
             _userInterface.post("Boat " + boatHandle + " agrees to acquire serving"
               + objectName);
               */
               //we'll accept this offer: send interaction
               ParameterHandleValueMap sp = _parameterFactory.create(1);
               sp.put(_servingNameParameter, InstanceName.encode(objectName));
               LogicalTime sendTime = _logicalTime.add(_lookahead).add(_advanceInterval);
               _rti.sendInteraction(
                     _TransferAcceptedClass,
                     sp,
                     null,
                     sendTime);
               //now ask for ownership of the attribute
               _rti.attributeOwnershipAcquisitionIfAvailable(
                     _object,
                     _positionAttributeAsSet);
               //update internal state of Boat
               _boatTable.setState(boatHandle, Boat.State.AWAITING_SERVING);
               //record the Serving so we know later which Boat it goes with
               _boatTable.setServing(boatHandle, _object);
            } else {
               //otherwise, we don't reply to request to assume
               _userInterface.post("Boat " + boatHandle + " spurns offer of serving "
                     + _rti.getObjectInstanceName(_object));
            }
         }
         catch (ArrayIndexOutOfBoundsException e) {
            _userInterface.post("ERROR ReqAttrOwnAssump: tag " + tagString
                  + " corresponds to no known Boat");
         }
         catch (RTIexception e) {
            _userInterface.post("ERROR ReqAttrOwnAssump: " + e);
         }
         return false;
      }
   } //end RequestAttributeOwnershipAssumptionCallback
}
