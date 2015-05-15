package se.pitch.sushifederation.consumption;

import hla.rti1516e.*;
import hla.rti1516e.exceptions.CouldNotDecode;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.IllegalName;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import se.pitch.sushifederation.*;
import se.pitch.sushifederation.manager.ManagerNames;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

public final class Consumption {
   //system properties used throughout
   private static String _fileSeparator = System.getProperty("file.separator");
   private static String _userDirectory = System.getProperty("user.dir");

   private ConsumptionFrame _userInterface;
   private FederateHandle _federateHandle; // -1 when not joined
   private boolean _simulationEndsReceived;
   private FedAmbImpl _fedAmb;
   private Properties _properties;
   private int _servingsConsumed;
   private int _servingsToConsume; //target to end simulation

   //barriers used to await announcement of synchronization points
   //these must be allocated here lest announcement sneak up on us
   private Barrier _readyToPopulateAnnouncementBarrier = new Barrier();
   private Barrier _readyToRunAnnouncementBarrier = new Barrier();
   private Barrier _readyToResignAnnouncementBarrier = new Barrier();

   //we use HLAfloat64Time
   private HLAfloat64TimeFactory _logicalTimeFactory = (HLAfloat64TimeFactory) LogicalTimeFactoryFactory.getLogicalTimeFactory(HLAfloat64TimeFactory.NAME);
   private LogicalTime _logicalTime;
   private LogicalTimeInterval _lookahead;

   //things dependent on RTI implementation in use
   private RTIambassador _rti;
   public ParameterHandleValueMapFactory _parametersFactory;
   public AttributeHandleValueMapFactory _attributesFactory;
   public AttributeHandleSetFactory _attributeHandleSetFactory;
   public FederateHandleSetFactory _federateHandleSetFactory;

   private ObjectClassHandle _BoatClass;
   private ObjectClassHandle _ServingClass;
   private ObjectClassHandle _DinerClass;
   private AttributeHandleSet _privilegeToDeleteObjectAttributeAsSet;
   private AttributeHandle _positionAttribute;
   private AttributeHandleSet _positionAttributeAsSet;
   private AttributeHandle _spaceAvailableAttribute;
   private AttributeHandle _cargoAttribute;
   private AttributeHandle _dinerStateAttribute;
   private AttributeHandle _servingNameAttribute;
   private AttributeHandleSet _BoatAttributes;
   private AttributeHandleSet _DinerAttributes;
   private InteractionClassHandle _SimulationEndsClass;

   private DinerTable _dinerTable;     //table of our notional chefs
   private HashMap<ObjectInstanceHandle, Serving> _servings;      //key: instance handle; value: Serving
   private HashMap<ObjectInstanceHandle, Boat> _knownBoats;    //key: instance handle; value: Boat
   private InternalQueue _internalQueue;
   private CallbackQueue _callbackQueue;
   private double _dinersReach;
   private LogicalTimeInterval[] _consumptionTimes;

   public Consumption(Properties props) {
      _federateHandle = null;  //not joined
      _servings = new HashMap<ObjectInstanceHandle, Serving>();
      _knownBoats = new HashMap<ObjectInstanceHandle, Boat>();
      _dinerTable = new DinerTable();
      _internalQueue = new InternalQueue();
      _callbackQueue = new CallbackQueue();
      _simulationEndsReceived = false;
      _servingsConsumed = 0;

      _userInterface = new ConsumptionFrame();
      _userInterface.finishConstruction(this, _dinerTable);
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

         System.out.println("Consumption started.");
      }
      catch (Exception e) {
         _userInterface.post("Consumption: constructor failed: " + e);
         _userInterface.post("You may as well exit.");
      }
   }

   public static void main(String[] args) {
      Properties props = parseArgs(args);
      loadProperties(props);
      Consumption consumption = new Consumption(props);
      consumption.mainThread();
   }

   //called when we get a time advance grant
   private void checkInternalQueue()
         throws RTIexception
   {
      while (_internalQueue.getTimeAtHead().compareTo(_logicalTime) <= 0) {
         InternalEvent event = _internalQueue.dequeue();
         /*
       _userInterface.post("Dequeued internal event at " + event.getTime()
         + ", diner: " + event.getDiner());
         */
         if (event != null) {
            event.dispatch();
         }
      }
   }
   
   public void checkEndOfSimulation()
         throws RTIexception
   {
      if (_servingsConsumed >= _servingsToConsume) {
         ParameterHandleValueMap sp = _parametersFactory.create(0);
         _rti.sendInteraction(_SimulationEndsClass, sp, null);
         _simulationEndsReceived = true;
      }
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
               _properties.get("CONFIG")
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
               ConsumptionNames._federateType,
               ConsumptionNames._federateType,
               fedexName,
               new URL[] {fomURL} );
         _userInterface.post("Joined as federate " + _federateHandle);

         //do other implementation-specific things
         _parametersFactory = _rti.getParameterHandleValueMapFactory();
         _attributesFactory = _rti.getAttributeHandleValueMapFactory();
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
         _logicalTime = _logicalTimeFactory.makeTime(new Double((getProperty("Federation.initialTime"))));
         _lookahead = _logicalTimeFactory.makeInterval(new Double((getProperty("Consumption.lookahead"))));
         _servingsToConsume = Integer.parseInt(getProperty("Federation.servingsToConsume"));
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

         //Consumption achieves ReadyToPopulate and waits for rest of federation
         _readyToPopulateAnnouncementBarrier.await();
         _userInterface.post("Waiting for ReadyToPopulate...");
         barrier = new Barrier(ManagerNames._readyToPopulate);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToPopulate);
         barrier.await();
         _userInterface.post("...federation synchronized.");

         makeInitialInstances();

         //Consumption achieves ReadyToRun and waits for rest of federation
         _readyToRunAnnouncementBarrier.await();
         _userInterface.post("Waiting for ReadyToRun...");
         barrier = new Barrier(ManagerNames._readyToRun);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToRun);
         barrier.await();
         _userInterface.post("...federation synchronized.");

         _rti.enableAsynchronousDelivery();

         //advance time until ending criterion satisfied
         while (!_simulationEndsReceived) {
            _userInterface.setTimeStateAdvancing();
            LogicalTime timeToMoveTo = _internalQueue.getTimeAtHead();
            if (timeToMoveTo == null) {
               timeToMoveTo = _logicalTime.add(_lookahead);
            }
            timeToMoveTo = _logicalTime.add(_logicalTimeFactory.makeInterval(1));
            //_userInterface.post("NER to " + timeToMoveTo);
            _rti.timeAdvanceRequest(timeToMoveTo);
            //process all the events & callbacks we receive from the RTI
            boolean wasTimeAdvanceGrant;
            do {
               Callback callback = _callbackQueue.dequeue();
               wasTimeAdvanceGrant = callback.dispatch();
               //_userInterface.post("After dispatch " + _logicalTime);
            } while (!wasTimeAdvanceGrant);
            updateInternalStateAtNewTime();
            if (_simulationEndsReceived) {
               break;
            }
            //process callbacks not requiring advance while in granted state
            while (!_dinerTable.isTimeAdvanceRequired()) {
               Callback callback = _callbackQueue.dequeue();
               callback.dispatch();
            }
         }
         _userInterface.post("Ending criterion satisfied.");

         //Consumption achieves ReadyToResign and waits for rest of federation
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
      ObjectClassHandle restaurantClass = _rti.getObjectClassHandle(RestaurantNames._RestaurantClassName);
      _ServingClass = _rti.getObjectClassHandle(RestaurantNames._ServingClassName);
      _BoatClass = _rti.getObjectClassHandle(RestaurantNames._BoatClassName);
      ObjectClassHandle actorClass = _rti.getObjectClassHandle(RestaurantNames._ActorClassName);
      _DinerClass = _rti.getObjectClassHandle(RestaurantNames._DinerClassName);
      AttributeHandle privilegeToDeleteObjectAttribute = _rti.getAttributeHandle(
            restaurantClass,
            RestaurantNames._privilegeToDeleteObjectAttributeName);
      _positionAttribute = _rti.getAttributeHandle(
            restaurantClass,
            RestaurantNames._positionAttributeName);
      _spaceAvailableAttribute = _rti.getAttributeHandle(
            _BoatClass,
            RestaurantNames._spaceAvailableAttributeName);
      _cargoAttribute = _rti.getAttributeHandle(
            _BoatClass,
            RestaurantNames._cargoAttributeName);
      _servingNameAttribute = _rti.getAttributeHandle(
            actorClass,
            RestaurantNames._servingNameAttributeName);
      _dinerStateAttribute = _rti.getAttributeHandle(
            _DinerClass,
            RestaurantNames._dinerStateAttributeName);

      _privilegeToDeleteObjectAttributeAsSet = _attributeHandleSetFactory.create();
      _privilegeToDeleteObjectAttributeAsSet.add(privilegeToDeleteObjectAttribute);

      _positionAttributeAsSet = _attributeHandleSetFactory.create();
      _positionAttributeAsSet.add(_positionAttribute);

      _BoatAttributes = _attributeHandleSetFactory.create();
      _BoatAttributes.add(_positionAttribute);
      _BoatAttributes.add(_spaceAvailableAttribute);
      _BoatAttributes.add(_cargoAttribute);

      _DinerAttributes = _attributeHandleSetFactory.create();
      _DinerAttributes.add(_positionAttribute);
      _DinerAttributes.add(_dinerStateAttribute);
      _DinerAttributes.add(_servingNameAttribute);

      _SimulationEndsClass =
            _rti.getInteractionClassHandle(ManagerNames._SimulationEndsClassName);
   }

   private void publish()
         throws RTIexception
   {
      //publish Serving position because we'll update it
      _rti.publishObjectClassAttributes(_ServingClass, _positionAttributeAsSet);
      //publish Diner class because we register them
      _rti.publishObjectClassAttributes(_DinerClass, _DinerAttributes);
      //publish the interaction because it's our job to send it
      _rti.publishInteractionClass(_SimulationEndsClass);
   }

   private void subscribe()
         throws RTIexception
   {
      //subscribe to Boat (less privToDelete)
      _rti.subscribeObjectClassAttributes(_BoatClass, _BoatAttributes);
      _rti.subscribeObjectClassAttributes(_ServingClass, _positionAttributeAsSet);
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
      _dinersReach = new Double(getProperty("Consumption.diner.reach"));
      int numberOfDiners = Integer.parseInt(getProperty("Consumption.numberOfDiners"));
      _consumptionTimes = new LogicalTimeInterval[numberOfDiners];
      for (int serial = 0; serial < numberOfDiners; ++serial) {
         //register Diner instance
         String dinerName = "D_" + _federateHandle + "_" + serial;

         try {
            _reservationComplete = false;
            synchronized (_reservationSemaphore) {
               _rti.reserveObjectInstanceName(dinerName);
               while(!_reservationComplete) {
                  try {
                     _reservationSemaphore.wait();
                  } catch (InterruptedException e) {
                     //ignore
                  }
               }
            }
            if (!_reservationSucceeded) {
               throw new RTIexception("Failed to reserve name: " + dinerName);
            }
         } catch (IllegalName e) {
            throw new RTIexception("Illegal Name: " + dinerName);
         }

         ObjectInstanceHandle dinerHandle = _rti.registerObjectInstance(_DinerClass, dinerName);
         //add to local table
         String prop = "Consumption.diner.position." + serial;
         double position = new Double(getProperty(prop));
         _dinerTable.add(
               dinerHandle,
               dinerName,
               position,
               Diner.State.LOOKING_FOR_FOOD,
               "",
               null);
         //update Diner attribute values
         AttributeHandleValueMap sa = _attributesFactory.create(3);
         sa.put(_positionAttribute, _dinerTable.getFullPosition(serial).encode());
         sa.put(_dinerStateAttribute, IntegerAttribute.encode(_dinerTable.getState(serial).getOrdinal()));
         sa.put(_servingNameAttribute, InstanceName.encode(_dinerTable.getServingName(serial)));
         LogicalTime sendTime = _logicalTime.add(_lookahead);
         _rti.updateAttributeValues(dinerHandle, sa, null, sendTime);
         //collect its consumption time
         prop = "Consumption.diner.meanConsumptionTime." + serial;
         double time = new Double(getProperty(prop));
         _consumptionTimes[serial] = _logicalTimeFactory.makeInterval(time);
      }
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

   private void getConfigurationData() {
   }

   private void updateInternalStateAtNewTime()
         throws RTIexception
   {
      checkInternalQueue();
      updateDiners();
      updateServings();
      checkEndOfSimulation();
   }

   private void updateDiners()
         throws RTIexception
   {
      LogicalTime sendTime = _logicalTime.add(_lookahead);
      _dinerTable.updateDiners(
            sendTime,
            _attributesFactory,
            _positionAttribute,
            _servingNameAttribute,
            _dinerStateAttribute,
            _rti);
   }

   private void updateServings()
         throws RTIexception
   {
      LogicalTime sendTime = _logicalTime.add(_lookahead);

      for (ObjectInstanceHandle handle : new HashSet<ObjectInstanceHandle>(_servings.keySet())) {
         Serving serving = _servings.get(handle);
         if (serving.getPrivilegeToDeleteObjectState() == AttributeState.OWNED_INCONSISTENT) {
            _rti.deleteObjectInstance(serving.getHandle(), null, sendTime);
            _servings.remove(serving.getHandle());
            _userInterface.post("Deleted serving " + serving.getHandle());
            //update count to end simulation
            _servingsConsumed++;
         }
         else if (serving.getPositionState() == AttributeState.OWNED_INCONSISTENT) {
            AttributeHandleValueMap sa = _attributesFactory.create(1);
            sa.put(_positionAttribute, serving.getPosition().encode());
            _rti.updateAttributeValues(serving.getHandle(), sa, null, sendTime);
            serving.setPositionState(AttributeState.OWNED_CONSISTENT);
         }
      }
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
         System.out.println("Consumption.parseArgs: default arguments failed: " + e);
         System.exit(1);
      }
      return props;
   }

   //load other properties from URL
   private static void loadProperties(Properties props) {
      try {
         //form URL for properties
         String urlString =
               props.getProperty("CONFIG")
                     + props.getProperty("FEDEX")
                     + ".props";
         URL propsURL = new URL(urlString);
         props.load(propsURL.openStream());
      }
      catch (Exception e) {
         System.out.println("Consumption failed to load properties: " + e);
         System.exit(1);
      }
   }

   //represents one event on the internal queue
   //This class is defined within Consumption so the dispatch routines
   //of its subclasses have the context of Consumption
   public abstract class InternalEvent {
      protected LogicalTime _time;
      protected int _diner;  //serial, not instance handle

      public LogicalTime getTime() { return _time; }
      public int getDiner() { return _diner; }
      public abstract void dispatch()
            throws RTIexception;
   }

   public final class FinishEatingSushiEvent extends InternalEvent {
      public FinishEatingSushiEvent(LogicalTime time, int diner) {
         _time = time;
         _diner = diner;
      }

      public void dispatch()
            throws RTIexception
      {
         //diner has finished eating; prepare to delete Serving
         //update diner's state
         _dinerTable.setState(_diner, Diner.State.PREPARING_TO_DELETE_SERVING);
         //begin transfer of privilegeToDeleteObject for Serving
         _rti.attributeOwnershipAcquisition(
               _dinerTable.getServing(_diner),
               _privilegeToDeleteObjectAttributeAsSet,
               null);
      }
   } //end FinishEatingSushiEvent

   //represents one callback coming from RTI
   //This class is defined within Production so the dispatch routines
   //of its subclasses have the context of Consumption
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
         ObjectInstanceHandle object,
         AttributeHandleSet attributes) {
      Callback callback = new AOANcallback(
            object,
            attributes);
      _callbackQueue.enqueue(callback);
   }

   public final class AOANcallback extends Callback {
      private ObjectInstanceHandle _object;
      private AttributeHandleSet _attributes;

      public AOANcallback(
            ObjectInstanceHandle object,
            AttributeHandleSet attributes) {
         _object = object;
         _attributes = attributes;
      }

      public boolean dispatch() {
         try {
            //is the object a Serving?
            ObjectClassHandle objectClass = _rti.getKnownObjectClassHandle(_object);
            if (!objectClass.equals(_ServingClass)) {
               throw new RTIexception("Object not Serving class, class: " + objectClass);
            }
            if (_attributes.equals(_positionAttributeAsSet)) {
               transferServingToDiner();
            }
            else if (_attributes.equals(_privilegeToDeleteObjectAttributeAsSet)) {
               finishDestroyingServing();
            }
            else throw new RTIexception("unexpected attribute set: " + _attributes);
         }
         catch (ConsumptionInternalError e) {
            _userInterface.post("ERROR AOAN: " + e.getMessage());
         }
         catch (RTIexception e) {
            _userInterface.post("ERROR AOAN: " + e);
         }
         return false;
      }

      private void transferServingToDiner()
            throws RTIexception, ConsumptionInternalError
      {
         int dinerSerial = _dinerTable.getDinerForServing(_object);
         if (dinerSerial < 0) throw new ConsumptionInternalError("acquisition notification for"
               + " serving " + _object + " not wanted by any diner");
         if (_dinerTable.getState(dinerSerial) != Diner.State.ACQUIRING) throw new
               ConsumptionInternalError("acquisition notification for serving " + _object
               + " wanted by diner serial " + dinerSerial + "which was in state "
               + _dinerTable.getState(dinerSerial));
         _userInterface.post("Transferring serving " + _dinerTable.getServingName(dinerSerial)
               + " to diner " + _dinerTable.getName(dinerSerial));
         //change state of diner
         _dinerTable.setState(dinerSerial, Diner.State.EATING);
         _dinerTable.setBoatHandle(dinerSerial, null);
         //record transfer of ownership
         Serving serving = _servings.get(_object);
         if (serving == null) throw new ConsumptionInternalError(
               "Serving " + _object + " not known");
         serving.setPrivilegeToDeleteObjectState(AttributeState.NOT_REFLECTED);
         serving.setTypeState(AttributeState.NOT_REFLECTED);
         serving.setPositionState(AttributeState.OWNED_INCONSISTENT);
         serving.setPosition(new Position(
               _dinerTable.getPosition(dinerSerial),
               OffsetEnum.OUTBOARD_CANAL));
         //schedule consumption of the serving
         LogicalTime eventTime = _logicalTime;
         eventTime = eventTime.add(_consumptionTimes[dinerSerial]);
         _internalQueue.enqueue(new FinishEatingSushiEvent(eventTime, dinerSerial));
         //_userInterface.post("Internal queue: " + _internalQueue);
      }

      private void finishDestroyingServing()
            throws RTIexception, ConsumptionInternalError
      {
         int dinerSerial = _dinerTable.getDinerForServing(_object);
         if (dinerSerial < 0) throw new ConsumptionInternalError("acquisition notification for"
               + " serving " + _object + " not being chewed by any diner");
         //_userInterface.post("Marking serving " + _object + " for deletion");
         if (_dinerTable.getState(dinerSerial) != Diner.State.PREPARING_TO_DELETE_SERVING) throw new
               ConsumptionInternalError("acquisition notification for serving " + _object
               + " wanted by diner serial " + dinerSerial + "which was in state "
               + _dinerTable.getState(dinerSerial));
         //change state of diner
         _dinerTable.setState(dinerSerial, Diner.State.LOOKING_FOR_FOOD);
         _dinerTable.setServing(dinerSerial, null);
         _dinerTable.setServingName(dinerSerial, "");
         //mark Serving for deletion after time grant
         Serving serving = _servings.get(_object);
         if (serving == null) throw new ConsumptionInternalError(
               "Serving " + _object + " not known");
         serving.setPrivilegeToDeleteObjectState(AttributeState.OWNED_INCONSISTENT);
      }
   } //end AttributeOwnershipAcquisitionNotificationCallback

   public void queueAttributeOwnershipAcquisitionUnavailableCallback(
         ObjectInstanceHandle object,
         AttributeHandleSet attributes) {
      Callback callback = new AOAUcallback(
            object,
            attributes);
      _callbackQueue.enqueue(callback);
   }

   public final class AOAUcallback extends Callback {
      private ObjectInstanceHandle _object;
      private AttributeHandleSet _attributes;

      public AOAUcallback(
            ObjectInstanceHandle object,
            AttributeHandleSet attributes) {
         _object = object;
         _attributes = attributes;
      }

      public boolean dispatch() {
         try {
            //is the object a Serving?
            ObjectClassHandle objectClass = _rti.getKnownObjectClassHandle(_object);
            if (!objectClass.equals(_ServingClass)) throw new RTIexception(
                  "Object not Serving class, class: " + objectClass);
            if (_attributes.equals(_positionAttributeAsSet)) {
               returnDejectedlyToLooking();
            }
            else throw new RTIexception("unexpected attribute set: " + _attributes);
         }
         catch (ConsumptionInternalError e) {
            _userInterface.post("ERROR attrOwnUnav: " + e.getMessage());
         }
         catch (RTIexception e) {
            _userInterface.post("ERROR attrOwnUnav: " + e);
         }
         return false;
      }

      private void returnDejectedlyToLooking()
            throws RTIexception, ConsumptionInternalError
      {
         int dinerSerial = _dinerTable.getDinerForServing(_object);
         if (dinerSerial < 0) throw new ConsumptionInternalError("attr own unav for"
               + " serving " + _object + " not wanted by any diner");
         if (_dinerTable.getState(dinerSerial) != Diner.State.ACQUIRING) throw new
               ConsumptionInternalError("attr own unav for serving " + _object
               + " wanted by diner serial " + dinerSerial + "which was in state "
               + _dinerTable.getState(dinerSerial));
         _userInterface.post("Failed to get serving " + _dinerTable.getServingName(dinerSerial)
               + " to diner " + _dinerTable.getName(dinerSerial));
         //change state of diner
         _dinerTable.setState(dinerSerial, Diner.State.LOOKING_FOR_FOOD);
         _dinerTable.setServing(dinerSerial, null);
         _dinerTable.setServingName(dinerSerial, "");
         _dinerTable.setBoatHandle(dinerSerial, null);
      }
   } //end AttributeOwnershipAcquisitionUnavailableCallback

   public void queueDiscoverObjectInstanceCallback(
         ObjectInstanceHandle handle,
         ObjectClassHandle objectClass,
         String name) {
      Callback callback = new DiscoverObjectInstanceCallback(
            handle,
            objectClass,
            name);
      _callbackQueue.enqueue(callback);
   }

   public final class DiscoverObjectInstanceCallback
         extends Callback
   {
      private ObjectInstanceHandle _instanceHandle;
      private ObjectClassHandle _objectClass;
      private String _instanceName;

      public DiscoverObjectInstanceCallback(
            ObjectInstanceHandle handle,
            ObjectClassHandle objectClass,
            String name) {
         _instanceHandle = handle;
         _objectClass = objectClass;
         _instanceName = name;
      }

      public boolean dispatch() {
         try {
            if (_objectClass.equals(_ServingClass)) {
               // Don't do anything for servings
               return false;
            }
            if (!_objectClass.equals(_BoatClass)) {
               throw new ConsumptionInternalError(
                     "instance " + _instanceHandle + "(" + _instanceName + ") of class " + _objectClass
                           + " not a Boat!");
            }
            Boat newBoat = new Boat();
            newBoat.setHandle(_instanceHandle);
            newBoat.setName(_instanceName);
            newBoat.setPrivilegeToDeleteObjectState(AttributeState.DISCOVERED);
            newBoat.setPositionState(AttributeState.DISCOVERED);
            newBoat.setSpaceAvailableState(AttributeState.DISCOVERED);
            newBoat.setCargoState(AttributeState.DISCOVERED);
            _knownBoats.put(_instanceHandle, newBoat);
            _userInterface.post("Discovered Boat " + _instanceHandle + "(" + _instanceName + ")");
         }
         catch (ConsumptionInternalError e) {
            _userInterface.post("discoverObjectInstance: " + e.getMessage());
         }
         return false;
      }
   } //end DiscoverObjectInstanceCallback

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
            Object entry = _servings.get(_object);
            if (entry != null) updateServing((Serving)entry);
            else throw new RTIexception("Instance " + _object + " not known.");
         }
         catch (RTIexception e) {
            _userInterface.post("ERROR provideAttributeValueUpdate: " + e);
         }
         return false;
      }

      private void updateServing(Serving serving)
            throws RTIexception
      {
         AttributeHandleValueMap sa = _attributesFactory.create(2);
         for (AttributeHandle handle : _attributes) {
            if (handle.equals(_positionAttribute)) {
               if (serving.getPositionState() != AttributeState.OWNED_CONSISTENT
                     && serving.getPositionState() != AttributeState.OWNED_INCONSISTENT) {
                  throw new RTIexception("Position attribute not owned for instance " + _object);
               }
               sa.put(_positionAttribute, serving.getPosition().encode());
            }
            else throw new RTIexception("Attribute " + handle
                  + " not known for instance " + _object);
         }
         LogicalTime sendTime = _logicalTime.add(_lookahead).add(_logicalTimeFactory.makeInterval(1));
         _rti.updateAttributeValues(serving.getHandle(), sa, null, sendTime);
         if (serving.getPositionState() == AttributeState.OWNED_INCONSISTENT)
            serving.setPositionState(AttributeState.OWNED_CONSISTENT);
      }
   } //end ProvideAttributeValueUpdateCallback

   public void queueReflectAttributeValuesEvent(
         LogicalTime time,
         ObjectInstanceHandle objectHandle,
         AttributeHandleValueMap attributes,
         byte[] tag)
   {
      ExternalEvent event = new ReflectAttributeValuesEvent(
            time,
            objectHandle,
            attributes,
            tag);
      _callbackQueue.enqueue(event);
   }

   public final class ReflectAttributeValuesEvent extends ExternalEvent {
      ObjectInstanceHandle _object;
      AttributeHandleValueMap _attributes;
      byte[] _tag;

      public ReflectAttributeValuesEvent(
            LogicalTime time,
            ObjectInstanceHandle objectHandle,
            AttributeHandleValueMap attributes,
            byte[] tag)
      {
         _time = time;
         _object = objectHandle;
         _attributes = attributes;
         _tag = tag;
      }

      public boolean dispatch()
            throws RTIexception
      {
         try {
            //this had better be a Boat; that's all we're subscribed to
            ObjectClassHandle objectClass = _rti.getKnownObjectClassHandle(_object);
            if (objectClass.equals(_ServingClass)) {
               return false;
            }
            if (!objectClass.equals(_BoatClass)) {
               throw new ConsumptionInternalError("unexpected class " + objectClass + " instance " + _object);
            }
            //find the Boat in our collection
            Boat boat = _knownBoats.get(_object);
            if (boat == null) throw new ConsumptionInternalError("unknown Boat " + _object);
            //decode and store new attribute values
            for (AttributeHandle handle : _attributes.keySet()) {
               if (handle.equals(_positionAttribute)) {
                  boat.setPosition(new Position(_attributes.get(handle)));
                  boat.setPositionState(AttributeState.REFLECTED); //we have data
               }
               else if (handle.equals(_spaceAvailableAttribute)) {
                  boat.setSpaceAvailable(SpaceAvailable.decode(_attributes.get(handle)));
                  boat.setSpaceAvailableState(AttributeState.REFLECTED); //we have data
               }
               else if (handle.equals(_cargoAttribute)) {
                  boat.setCargo(InstanceName.decode(_attributes.get(handle)));
                  boat.setCargoState(AttributeState.REFLECTED); //we have data
               }
               else throw new ConsumptionInternalError("unknown attribute " + handle);
            }
            if (!boat.isSpaceAvailable()) {
               boat.setServing(_rti.getObjectInstanceHandle(boat.getCargo()));
            }
            /*
          _userInterface.post("Reflected boat " + _rti.getObjectInstanceName(_object)
            + " position " + boat._position + " at " + _time);
            */
            //if the boat is loaded, see if a diner wants to take its Serving it
            if (!boat.isSpaceAvailable()) doWeWantToEmptyThisBoat(boat);
         }
         catch (ConsumptionInternalError e) {
            _userInterface.post("ERROR: reflectAttributeValues: " + e.getMessage());
         }
         catch (CouldNotDecode e) {
            _userInterface.post("ERROR: reflectAttributeValues: " + e);
         }
         return false;
      }

      private void doWeWantToEmptyThisBoat(Boat boat)
            throws RTIexception
      {
         int dinerCount = _dinerTable.getRowCount();
         for (int diner = 0; diner < dinerCount; ++diner) {
            if (_dinerTable.getState(diner) == Diner.State.LOOKING_FOR_FOOD
                  && _dinerTable.isInReach(diner, boat.getPosition().getAngle(), _dinersReach)) {
               /*
             _userInterface.post("Diner " + diner + " attempting to take from boat "
               + boatName);
               */
               //change state of diner
               _dinerTable.setState(diner, Diner.State.ACQUIRING);
               _dinerTable.setServing(diner, boat.getServing());
               _dinerTable.setServingName(diner, boat.getCargo());
               _dinerTable.setBoatHandle(diner, boat.getHandle());
               //remember Serving
               boat.setServing(_rti.getObjectInstanceHandle(boat.getCargo()));
               Serving serving = new Serving();
               serving.setHandle(boat.getServing());
               serving.setName(boat.getCargo());
               serving.setPositionState(AttributeState.NOT_REFLECTED);
               serving.setPrivilegeToDeleteObjectState(AttributeState.NOT_REFLECTED);
               serving.setTypeState(AttributeState.NOT_REFLECTED);
               _servings.put(boat.getServing(), serving);
               //begin acquisition of Serving
               _rti.attributeOwnershipAcquisitionIfAvailable (
                     boat.getServing(),
                     _positionAttributeAsSet);
            }
         }
      }
   } //end ReflectAttributeValuesEvent
}
