package se.pitch.sushifederation.production;

import hla.rti1516e.*;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleSetFactory;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.LogicalTimeInterval;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import se.pitch.sushifederation.*;
import se.pitch.sushifederation.manager.ManagerNames;

import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;

public final class Production {
   //system properties used throughout
   private static String _fileSeparator = System.getProperty("file.separator");
   private static String _userDirectory = System.getProperty("user.dir");

   private ProductionFrame _userInterface;
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
   private LogicalTime _logicalTime = _logicalTimeFactory.makeInitial();
   private LogicalTimeInterval _lookahead;

   //things dependent on RTI implementation in use
   private RTIambassador _rti;
   public AttributeHandleSetFactory _attributeHandleSetFactory;

   private ObjectClassHandle _boatClass;
   private ObjectClassHandle _servingClass;
   private ObjectClassHandle _chefClass;

   private AttributeHandleSet _privilegeToDeleteObjectAttributeAsSet;
   private AttributeHandle _positionAttribute;
   private AttributeHandleSet _positionAttributeAsSet;
   private AttributeHandle _typeAttribute;
   private AttributeHandle _spaceAvailableAttribute;
   private AttributeHandle _cargoAttribute;
   private AttributeHandle _chefStateAttribute;
   private AttributeHandle _servingNameAttribute;
   private AttributeHandleSet _servingAttributes;
   private AttributeHandleSet _boatAttributes;
   private AttributeHandleSet _chefAttributes;
   private InteractionClassHandle _SimulationEndsClass;
   private InteractionClassHandle _TransferAcceptedClass;
   private ParameterHandle _servingNameParameter;

   private int _nextServingSerial;   //serial to assign to next Serving registered
   private ChefTable _chefTable;     //table of our notional chefs
   private Hashtable<ObjectInstanceHandle, Serving> _servings;      //key: instance handle; value: Serving
   private Random _random;           //generator to determine sushi types
   private int _numberOfSushiTypes;
   private LogicalTimeInterval[] _manufactureTimes; //time to make sushi by type
   private Hashtable<ObjectInstanceHandle, Boat> _knownBoats;    //key: instance handle; value: Boat
   private InternalQueue _internalQueue;
   private CallbackQueue _callbackQueue;
   private double _chefsReach;

   private AttributeHandleValueMapFactory _attributeFactory;

   public Production(Properties props) {
      _federateHandle = null;  //not joined
      _nextServingSerial = 0;
      _servings = new Hashtable<ObjectInstanceHandle, Serving>();
      _knownBoats = new Hashtable<ObjectInstanceHandle, Boat>();
      _chefTable = new ChefTable();
      _internalQueue = new InternalQueue();
      _callbackQueue = new CallbackQueue();
      _random = new Random();
      _simulationEndsReceived = false;

      _userInterface = new ProductionFrame();
      _userInterface.finishConstruction(this, _chefTable);
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

         System.out.println("Production started.");
      }
      catch (Exception e) {
         _userInterface.post("Production: constructor failed: " + e);
         _userInterface.post("You may as well exit.");
      }
   }

   public static void main(String[] args) {
      Properties props = parseArgs(args);
      loadProperties(props);
      Production production = new Production(props);
      production.mainThread();
   }

   //called when we get a time advance grant
   private void checkInternalQueue()
         throws RTIexception
   {
      while (_internalQueue.getTimeAtHead().compareTo(_logicalTime) <= 0 ) {
         InternalEvent event = _internalQueue.dequeue();
         /*
       _userInterface.post("Dequeued internal event at " + _logicalTime
         + ", chef: " + event.getChef());
         */
         if (event != null) {
            event.dispatch();
         }
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
         String urlString = _properties.get("CONFIG") + fedexName + ".xml";
         fomURL = new URL(urlString);
         //the federation execution may already exist
         try {
            _rti.createFederationExecution(fedexName, fomURL);
            _userInterface.post("Federation execution " + fedexName + " created.");
         }
         catch (FederationExecutionAlreadyExists e) {
            _userInterface.post("Federation execution " + fedexName + " already exists.");
         }
         //join federation execution
         fedexName = (String)_properties.get("FEDEX");
         _federateHandle = _rti.joinFederationExecution(ProductionNames._federateType,
                                                        ProductionNames._federateType,
               fedexName,
                                                       new URL[]{fomURL});
         _userInterface.post("Joined as federate " + _federateHandle);

         _attributeFactory = _rti.getAttributeHandleValueMapFactory();
         _attributeHandleSetFactory = _rti.getAttributeHandleSetFactory();

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
         _lookahead = _logicalTimeFactory.makeInterval(new Double((getProperty("Production.lookahead"))));
         _userInterface.post("Enabling time regulation...");
         barrier = new Barrier();
         _fedAmb.setEnableTimeRegulationBarrier(barrier);
         _rti.enableTimeRegulation(_lookahead);
         result = barrier.await();
         _logicalTime = ((LogicalTime)result[0]);
         _userInterface.post("...regulation enabled at " + _logicalTime);
         _userInterface.setLogicalTime(((HLAfloat64Time)_logicalTime).getValue());
         _userInterface.setTimeStateGranted();

         getHandles();
         publish();
         subscribe();

         //Production achieves ReadyToPopulate and waits for rest of federation
         _readyToPopulateAnnouncementBarrier.await();
         _userInterface.post("Waiting for ReadyToPopulate...");
         barrier = new Barrier(ManagerNames._readyToPopulate);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToPopulate);
         barrier.await();
         _userInterface.post("...federation synchronized.");

         //Production federate makes initial instances as it makes chefs
         makeInitialInstances();

         //Production achieves ReadyToRun and waits for rest of federation
         _readyToRunAnnouncementBarrier.await();
         _userInterface.post("Waiting for ReadyToRun...");
         barrier = new Barrier(ManagerNames._readyToRun);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToRun);
         barrier.await();
         _userInterface.post("...federation synchronized.");

         _rti.enableAsynchronousDelivery();

         //advance time until SimulationEnds received
         timeLoop:
         while (!_simulationEndsReceived) {
            _userInterface.setTimeStateAdvancing();
            LogicalTime timeToMoveTo = _internalQueue.getTimeAtHead();
            timeToMoveTo = _logicalTime.add(_logicalTimeFactory.makeInterval(1));
            //_userInterface.post("NER to " + timeToMoveTo);
            _rti.timeAdvanceRequest(timeToMoveTo);
            boolean wasTimeAdvanceGrant;
            do {
               Callback callback = _callbackQueue.dequeue();
               wasTimeAdvanceGrant = callback.dispatch();
               if (_simulationEndsReceived) break timeLoop;
            } while (!wasTimeAdvanceGrant);
            updateInternalStateAtNewTime();
         }
         _userInterface.post("SimulationEnds received.");

         //Production achieves ReadyToResign and waits for rest of federation
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
      _servingClass = _rti.getObjectClassHandle(RestaurantNames._ServingClassName);
      _boatClass = _rti.getObjectClassHandle(RestaurantNames._BoatClassName);
      ObjectClassHandle actorClass = _rti.getObjectClassHandle(RestaurantNames._ActorClassName);
      _chefClass = _rti.getObjectClassHandle(RestaurantNames._ChefClassName);
      AttributeHandle privilegeToDeleteObjectAttribute = _rti.getAttributeHandle(
            restaurantClass,
            RestaurantNames._privilegeToDeleteObjectAttributeName);
      _positionAttribute = _rti.getAttributeHandle(
            restaurantClass,
            RestaurantNames._positionAttributeName);
      _typeAttribute = _rti.getAttributeHandle(
            _servingClass,
            RestaurantNames._typeAttributeName);
      _spaceAvailableAttribute = _rti.getAttributeHandle(
            _boatClass,
            RestaurantNames._spaceAvailableAttributeName);
      _cargoAttribute = _rti.getAttributeHandle(
            _boatClass,
            RestaurantNames._cargoAttributeName);
      _servingNameAttribute = _rti.getAttributeHandle(
            actorClass,
            RestaurantNames._servingNameAttributeName);
      _chefStateAttribute = _rti.getAttributeHandle(
            _chefClass,
            RestaurantNames._chefStateAttributeName);

      _privilegeToDeleteObjectAttributeAsSet = _attributeHandleSetFactory.create();
      _privilegeToDeleteObjectAttributeAsSet.add(privilegeToDeleteObjectAttribute);

      _positionAttributeAsSet = _attributeHandleSetFactory.create();
      _positionAttributeAsSet.add(_positionAttribute);

      _servingAttributes = _attributeHandleSetFactory.create();
      _servingAttributes.add(privilegeToDeleteObjectAttribute);
      _servingAttributes.add(_positionAttribute);
      _servingAttributes.add(_typeAttribute);

      _boatAttributes = _attributeHandleSetFactory.create();
      _boatAttributes.add(_positionAttribute);
      _boatAttributes.add(_spaceAvailableAttribute);
      _boatAttributes.add(_cargoAttribute);

      _chefAttributes = _attributeHandleSetFactory.create();
      _chefAttributes.add(_positionAttribute);
      _chefAttributes.add(_chefStateAttribute);
      _chefAttributes.add(_servingNameAttribute);

      _SimulationEndsClass =
            _rti.getInteractionClassHandle(ManagerNames._SimulationEndsClassName);
      _TransferAcceptedClass =
            _rti.getInteractionClassHandle(RestaurantNames._TransferAcceptedClassName);

      _servingNameParameter = _rti.getParameterHandle(
            _TransferAcceptedClass,
            RestaurantNames._servingNameParameterName);
   }

   private void getConfigurationData() {
      //get manfacture times for sushi types
      _numberOfSushiTypes = Integer.parseInt(getProperty("Federation.Sushi.numberOfTypes"));
      _manufactureTimes = new LogicalTimeInterval[_numberOfSushiTypes];
      for (int i = 0; i < _numberOfSushiTypes; ++i) {
         String timeString = getProperty("Federation.Sushi.meanManufactureTime."+i);
         double time = new Double((timeString));
         _manufactureTimes[i] = _logicalTimeFactory.makeInterval(time);
      }
   }

   private void publish()
         throws RTIexception
   {
      //publish Serving class because we register them
      _rti.publishObjectClassAttributes(_servingClass, _servingAttributes);
      //publish Chef class because we register them
      _rti.publishObjectClassAttributes(_chefClass, _chefAttributes);
   }

   private void subscribe()
         throws RTIexception
   {
      //subscribe to Boat (less privToDelete)
      _rti.subscribeObjectClassAttributes(_boatClass, _boatAttributes);

      _rti.subscribeInteractionClass(_SimulationEndsClass);
      _rti.subscribeInteractionClass(_TransferAcceptedClass);
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
      _chefsReach = new Double(getProperty("Production.chef.reach"));
      int numberOfChefs = Integer.parseInt(getProperty("Production.numberOfChefs"));
      for (int serial = 0; serial < numberOfChefs; ++serial) {
         //register Chef instance
         String chefName = "C_" + _federateHandle + "_" + serial;

         try {
            _reservationComplete = false;
            synchronized (_reservationSemaphore) {
               _rti.reserveObjectInstanceName(chefName);
               while(!_reservationComplete) {
                  try {
                     _reservationSemaphore.wait();
                  } catch (InterruptedException e) {
                     //ignore
                  }
               }
            }
            if (!_reservationSucceeded) {
               throw new RTIexception("Failed to reserve name: " + chefName);
            }
         } catch (IllegalName e) {
            throw new RTIexception("Illegal Name: " + chefName);
         }

         ObjectInstanceHandle chefHandle = _rti.registerObjectInstance(_chefClass, chefName);
         //add to local table
         String prop = "Production.chef.position." + serial;
         double position = new Double((getProperty(prop)));
         _chefTable.add(
               chefHandle,
               chefName,
               position,
               Chef.State.MAKING_SUSHI,
               "",
               null); //serving created later
         //update Chef attribute values
         AttributeHandleValueMap sa = _attributeFactory.create(3);
         sa.put(_positionAttribute, _chefTable.getFullPosition(serial).encode());
         sa.put(_chefStateAttribute, IntegerAttribute.encode(_chefTable.getState(serial).getOrdinal()));
         sa.put(_servingNameAttribute, InstanceName.encode(_chefTable.getServingName(serial)));
         LogicalTime sendTime = _logicalTime.add(_lookahead);
         _rti.updateAttributeValues(chefHandle, sa, null, sendTime);
         //what kind of sushi?
         int type = Math.abs(_random.nextInt()) % _numberOfSushiTypes;
         //put event on internal queue
         LogicalTime eventTime = _logicalTime.add(_manufactureTimes[type]);
         _internalQueue.enqueue(new FinishMakingSushiEvent(eventTime, serial, type));
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

   private void updateChefs()
         throws RTIexception
   {
      LogicalTime sendTime = _logicalTime.add(_lookahead);
      _chefTable.updateChefs(
            sendTime,
            _attributeFactory,
            _positionAttribute,
            _servingNameAttribute,
            _chefStateAttribute,
            _rti);
   }

   private void updateInternalStateAtNewTime()
         throws RTIexception
   {
      checkInternalQueue();
      updateChefs();
      updateServings();
   }

   private void updateServings()
         throws RTIexception
   {
      LogicalTime sendTime = _logicalTime.add(_lookahead);
      Enumeration e = _servings.elements();
      while (e.hasMoreElements()) {
         Serving serving = (Serving)e.nextElement();
         AttributeHandleValueMap sa = _attributeFactory.create(2);
         boolean needToUpdate = false;
         if (serving.getPositionState() == AttributeState.OWNED_INCONSISTENT) {
            sa.put(_positionAttribute, serving.getPosition().encode());
            needToUpdate = true;
            serving.setPositionState(AttributeState.OWNED_CONSISTENT);
         }
         if (serving.getTypeState() == AttributeState.OWNED_INCONSISTENT) {
            sa.put(_typeAttribute, IntegerAttribute.encode(serving.getType()));
            needToUpdate = true;
            serving.setTypeState(AttributeState.OWNED_CONSISTENT);
         }
         if (needToUpdate) {
            _rti.updateAttributeValues(serving.getHandle(), sa, null, sendTime);
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
         String defaultRTIhost = java.net.InetAddress.getLocalHost().getHostName();
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
      catch (java.net.UnknownHostException e) {
         System.out.println("Production.parseArgs: default arguments failed: " + e);
         System.exit(1);
      }
      return props;
   }

   //load other properties from URL
   private static void loadProperties(Properties props) {
      try {
         //form URL for properties
         String urlString = props.getProperty("CONFIG")
               + props.getProperty("FEDEX")
               + ".props";
         URL propsURL = new URL(urlString);
         props.load(propsURL.openStream());
      }
      catch (Exception e) {
         System.out.println("Production failed to load properties: " + e);
         System.exit(1);
      }
   }

   //represents one event on the internal queue
   //This class is defined within Production so the dispatch routines
   //of its subclasses have the context of Production
   public abstract class InternalEvent {
      protected LogicalTime _time;
      protected int _chef;  //serial, not instance handle
      protected int _sushiType; //type of sushi we're making

      public LogicalTime getTime() { return _time; }
      public int getChef() { return _chef; }
      public int getType() { return _sushiType; }
      public abstract void dispatch()
            throws RTIexception;
   }

   public final class FinishMakingSushiEvent extends InternalEvent {
      public FinishMakingSushiEvent(LogicalTime time, int chef, int type) {
         _time = time;
         _chef = chef;
         _sushiType = type;
      }

      public void dispatch()
            throws RTIexception
      {
         //chef has finished making; register new Serving
         String servingName = "S_" + _federateHandle + "_" + _nextServingSerial++;

         try {
            _reservationComplete = false;
            synchronized (_reservationSemaphore) {
               _rti.reserveObjectInstanceName(servingName);
               while(!_reservationComplete) {
                  try {
                     _reservationSemaphore.wait();
                  } catch (InterruptedException e) {
                     //ignore
                  }
               }
            }
            if (!_reservationSucceeded) {
               throw new RTIexception("Failed to reserve name: " + servingName);
            }
         } catch (IllegalName e) {
            throw new RTIexception("Illegal Name: " + servingName);
         }

         ObjectInstanceHandle servingHandle = _rti.registerObjectInstance(_servingClass, servingName);
         //add new instance to our collection
         Serving serving = new Serving();
         serving.setPrivilegeToDeleteObjectState(AttributeState.OWNED_CONSISTENT);
         serving.setHandle(servingHandle);
         serving.setName(servingName);
         Position pos = new Position(
               _chefTable.getPosition(_chef), //Serving starts where chef is
               OffsetEnum.INBOARD_CANAL);
         serving.setPosition(pos);
         serving.setPositionState(AttributeState.OWNED_INCONSISTENT);
         serving.setType(_sushiType);
         serving.setTypeState(AttributeState.OWNED_INCONSISTENT);
         serving.setPrivilegeToDeleteObjectState(AttributeState.OWNED_CONSISTENT);
         _servings.put(servingHandle, serving);
         //update chef's state and serving
         _chefTable.setState(_chef, Chef.State.LOOKING_FOR_BOAT);
         _chefTable.setServing(_chef, servingHandle);
         _chefTable.setServingName(_chef, servingName);
      }
   } //end FinishMakingSushiEvent

   //represents one callback coming from RTI
   //This class is defined within Production so the dispatch routines
   //of its subclasses have the context of Production
   public abstract class Callback {
      //returns true if event was a grant
      public abstract boolean dispatch() throws RTIexception;
   }

   //represents one callback that is an event (carries a time)
   public abstract class ExternalEvent extends Callback {
      protected LogicalTime _time;

      public LogicalTime getTime() { return _time; }
      //returns true if event was a grant
      public abstract boolean dispatch()
            throws RTIexception;
   }

   public void queueAttributeOwnershipDivestitureNotification(
         ObjectInstanceHandle object) {
      Callback callback = new AODNcallback(object);
      _callbackQueue.enqueue(callback);
   }

   public final class AODNcallback extends Callback {
      private ObjectInstanceHandle _object;

      public AODNcallback(
            ObjectInstanceHandle object) {
         _object = object;
      }

      public boolean dispatch() {
         try {
            //record transfer of ownership
            Serving serving = (_servings.get(_object));
            if (serving == null) throw new ProductionInternalError(
                  "Serving " + _object + " not known");
            serving.setPositionState(AttributeState.NOT_REFLECTED); //we're not subscribed
            //_userInterface.post("AODN Serving " + serving._name);
            _rti.confirmDivestiture(_object, _positionAttributeAsSet, new byte[0]);
         }
         catch (ProductionInternalError e) {
            _userInterface.post("ERROR AODN: " + e.getMessage());
         } catch (RTIexception e) {
            _userInterface.post("ERROR reqDivConf: " + e);
         }
         return false;
      }
   } //end AttributeOwnershipDivestitureNotificationCallback

   public void queueDiscoverObjectInstance(
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
            if (!_objectClass.equals(_boatClass)) throw new ProductionInternalError(
                  "instance " + _instanceHandle + "(" + _instanceName + ") of class "
                        + _objectClass + " not a Boat!");
            Boat newBoat = new Boat();
            newBoat.setHandle(_instanceHandle);
            newBoat.setName(_instanceName);
            newBoat.setPrivilegeToDeleteObjectState(AttributeState.NOT_REFLECTED);
            newBoat.setPositionState(AttributeState.DISCOVERED);
            newBoat.setSpaceAvailableState(AttributeState.DISCOVERED);
            newBoat.setCargoState(AttributeState.DISCOVERED);
            _knownBoats.put(_instanceHandle, newBoat);
            _userInterface.post("Discovered Boat " + _instanceHandle + "(" + _instanceName + ")");
         }
         catch (ProductionInternalError e) {
            _userInterface.post("ERROR discoverObjectInstance: " + e.getMessage());
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
            Serving serving = _servings.get(_object);
            if (serving == null) {
               throw new RTIexception("Instance " + _object + " not known.");
            }
            AttributeHandleValueMap sa = _attributeFactory.create(2);
            for (AttributeHandle handle : _attributes) {
               if (handle.equals(_positionAttribute)) {
                  if (serving.getPositionState() != AttributeState.OWNED_INCONSISTENT
                        && serving.getPositionState() != AttributeState.OWNED_CONSISTENT) throw new AttributeNotOwned(
                        "Position attribute not owned for instance " + _object);
                  sa.put(_positionAttribute, serving.getPosition().encode());
                  serving.setPositionState(AttributeState.OWNED_CONSISTENT);
               }
               else if (handle.equals(_typeAttribute)) {
                  if (serving.getTypeState() != AttributeState.OWNED_INCONSISTENT
                        && serving.getTypeState() != AttributeState.OWNED_CONSISTENT) throw new AttributeNotOwned(
                        "Type attribute not owned for instance " + _object);
                  sa.put(_typeAttribute, IntegerAttribute.encode(serving.getType()));
                  serving.setTypeState(AttributeState.OWNED_CONSISTENT);
               }
               else throw new RTIexception("Attribute " + handle
                        + " not known for instance " + _object);
            }
//            System.out.println("logicalTime: " + _logicalTime);
            LogicalTime sendTime = _logicalTime.add(_lookahead).add(_logicalTimeFactory.makeInterval(1));
//            System.out.println("sendTime:" + sendTime);
            _rti.updateAttributeValues(serving.getHandle(), sa, null, sendTime);
         } catch (RTIexception e) {
            _userInterface.post("ERROR provideAttributeValueUpdate: " + e);
         }
         return false;
      }
   } //end ProvideAttributeValueUpdateCallback

   public void queueReceiveInteractionCallback(InteractionClassHandle interactionClass)
   {
      Callback callback = new ReceiveInteractionCallback(interactionClass);
      _callbackQueue.enqueue(callback);
   }

   public final class ReceiveInteractionCallback extends Callback {
      InteractionClassHandle _class;

      public ReceiveInteractionCallback(
            InteractionClassHandle interactionClass)
      {
         _class = interactionClass;
      }

      public boolean dispatch()
            throws RTIexception
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

   public void queueReceiveInteractionEvent(
         InteractionClassHandle interactionClass,
         ParameterHandleValueMap parameters,
         LogicalTime time)
   {
      ExternalEvent event = new ReceiveInteractionEvent(
            interactionClass,
            parameters,
            time);
      _callbackQueue.enqueue(event);
   }

   public final class ReceiveInteractionEvent extends ExternalEvent {
      InteractionClassHandle _class;
      ParameterHandleValueMap _parameters;

      public ReceiveInteractionEvent(
            InteractionClassHandle interactionClass,
            ParameterHandleValueMap parameters,
            LogicalTime time)
      {
         _time = time;
         _class = interactionClass;
         _parameters = parameters;
      }

      public boolean dispatch()
            throws RTIexception
      {
         try {
            if (!_class.equals(_TransferAcceptedClass)) {
               throw new ProductionInternalError("interaction of unknown class " + _class);
            }
            //retrieve serving name from parameter
            String servingName = InstanceName.decode(_parameters.get(_servingNameParameter));
            ObjectInstanceHandle object = _rti.getObjectInstanceHandle(servingName);
            int chefSerial = _chefTable.getChefForServing(object);
            if (chefSerial < 0) throw new ProductionInternalError("TransferAccepted for"
                  + " serving " + servingName + " not held by any chef");
            if (_chefTable.getState(chefSerial) != Chef.State.WAITING_TO_TRANSFER) throw new
                  ProductionInternalError("TransferAccepted for serving " + servingName
                  + " held by chef serial " + chefSerial + "which was in state "
                  + _chefTable.getState(chefSerial));
            _userInterface.post("TransferAccepted: " + servingName + " from chef serial "
                  + chefSerial + " " + _time);
            //change state of chef
            _chefTable.setState(chefSerial, Chef.State.MAKING_SUSHI);
            _chefTable.setServing(chefSerial, null);
            _chefTable.setServingName(chefSerial, "");
            _chefTable.setBoatHandle(chefSerial, null);
            //schedule production of another serving
            //what kind of sushi?
            int type = Math.abs(_random.nextInt()) % _numberOfSushiTypes;
            //put event on internal queue
            LogicalTime eventTime = _logicalTime.add(_manufactureTimes[type]);
            _internalQueue.enqueue(new FinishMakingSushiEvent(eventTime, chefSerial, type));
         }
         catch (ProductionInternalError e) {
            _userInterface.post("ERROR receiveInteraction dispatch: " + e.getMessage());
         } catch (CouldNotDecode e) {
            _userInterface.post("ERROR could not decode: " + e);
         } catch (RTIexception e) {
            _userInterface.post("ERROR receiveInteraction dispatch: " + e);
         }
         return false;
      }
   } //end ReceiveInteractionEvent

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
            ObjectClassHandle objectClassHandle = _rti.getKnownObjectClassHandle(_object);
            if (!objectClassHandle.equals(_boatClass)) throw new ProductionInternalError (
                  "unexpected class " + objectClassHandle + " instance " + _object);
            //find the Boat in our collection
            Boat boat = _knownBoats.get(_object);
            if (boat == null) throw new ProductionInternalError ("unknown Boat " + _object);
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
               else throw new ProductionInternalError("unknown attribute " + handle);
            }
            //_userInterface.post("Reflect " + boat._name + " at " + _time);
            //if the boat is empty, see if a chef wants to put something on it
            if (boat.isSpaceAvailable()) doWeWantToLoadThisBoat(boat);
            //should we cancel attempt to load this boat?
            checkLoadingCancellation(boat);
         }
         catch (ProductionInternalError e) {
            _userInterface.post("ERROR: reflectAttributeValues: " + e.getMessage());
         }
         catch (CouldNotDecode e) {
            _userInterface.post("ERROR: reflectAttributeValues: " + e);
         }
         return false;
      }

      private void doWeWantToLoadThisBoat(Boat boat) throws RTIexception
      {
         int chefCount = _chefTable.getRowCount();
         for (int chef = 0; chef < chefCount; ++chef) {
            if (_chefTable.getState(chef) == Chef.State.LOOKING_FOR_BOAT
                  && _chefTable.isInReach(chef, boat.getPosition().getAngle(), _chefsReach)) {
               String boatName = _rti.getObjectInstanceName(_object);
               /*
             _userInterface.post("Chef " + chef + " attempting to load boat "
               + boatName);
               */
               //change state of chef
               _chefTable.setState(chef, Chef.State.WAITING_TO_TRANSFER);
               _chefTable.setBoatHandle(chef, boat.getHandle());
               //begin negotiated divestiture of Serving position attribute
               //user-supplied tag contains Boat's instance name
               _rti.negotiatedAttributeOwnershipDivestiture(
                     _chefTable.getServing(chef),
                     _positionAttributeAsSet,
                     boatName.getBytes());
               //break;
            }
         }
      }

      private void checkLoadingCancellation(Boat boat) throws RTIexception
      {
         int chefCount = _chefTable.getRowCount();
         for (int chef = 0; chef < chefCount; ++chef) {
            try {
               if (_chefTable.getState(chef) == Chef.State.WAITING_TO_TRANSFER
                     && boat.getHandle().equals(_chefTable.getBoatHandle(chef))
                     && !_chefTable.isInReach(chef, boat.getPosition().getAngle(), _chefsReach)) {
                  String boatName = _rti.getObjectInstanceName(_object);
                  _userInterface.post("Chef " + chef + " cancelling attempt to load boat "
                        + boatName);
                  //cancel the negotiated transfer of Serving position attribute
                  _rti.cancelNegotiatedAttributeOwnershipDivestiture(
                        _chefTable.getServing(chef),
                        _positionAttributeAsSet);
                  //change state of Chef
                  _chefTable.setState(chef, Chef.State.LOOKING_FOR_BOAT);
                  _chefTable.setBoatHandle(chef, null);
               }
            }
            catch (AttributeNotOwned e) {
               //this might occur if the RTI has already transferred ownership of
               //the attribute but we haven't yet acted on the
               //attributeOwnershipDivestitureNotification, in which case we merely skip
               //the change of state and assume the AODN will catch up with us
               _userInterface.post("Attempted to cancel divestiture of Serving position"
                     + " attribute when RTI had already transferred it, chef: "
                     + chef + " serving: " + _chefTable.getServing(chef));
            }
         }
      }
   } //end ReflectAttributeValuesEvent

   public void queueRemoveObjectInstanceCallback(
         ObjectInstanceHandle objectHandle)
   {
      Callback callback = new RemoveObjectInstanceCallback(
            objectHandle);
      _callbackQueue.enqueue(callback);
   }

   public final class RemoveObjectInstanceCallback extends Callback {
      ObjectInstanceHandle _object;

      public RemoveObjectInstanceCallback(
            ObjectInstanceHandle objectHandle)
      {
         _object = objectHandle;
      }

      public boolean dispatch()
            throws RTIexception
      {
         Serving serving = _servings.get(_object);
         if (serving == null) throw new RTIexception("Object " + _object + " not known");
         //_userInterface.post("Serving " + serving._name + " being removed");
         //Hashtables are synchronized
         _servings.remove(_object);
         return false;
      }
   } //end RemoveObjectInstanceCallback

   public void queueRequestAttributeOwnershipReleaseCallback(
         ObjectInstanceHandle objectHandle,
         AttributeHandleSet attributes,
         byte[] tag)
   {
      Callback callback = new RAORcallback(
            objectHandle,
            attributes,
            tag);
      _callbackQueue.enqueue(callback);
   }

   public final class RAORcallback extends Callback {
      ObjectInstanceHandle _object;
      AttributeHandleSet _attributes;
      byte[] _tag;

      public RAORcallback(
            ObjectInstanceHandle objectHandle,
            AttributeHandleSet attributes,
            byte[] tag)
      {
         _object = objectHandle;
         _attributes = attributes;
         _tag = tag;
      }

      public boolean dispatch()
            throws RTIexception
      {
         try {
            /*
          _userInterface.post("Req for attr ownership rel, instance " + _object
            + "(" + _rti.getObjectInstanceName(_object) + ") attrs "
            + _attributes);
            */
            if (!_rti.getKnownObjectClassHandle(_object).equals(_servingClass)) throw new
                  ProductionInternalError("Instance " + _object + "is of class "
                  + _rti.getKnownObjectClassHandle(_object));
            Serving serving = _servings.get(_object);
            if (serving == null) throw new RTIexception("instance " + _object
                  + " not known.");
            if (_attributes.equals(_privilegeToDeleteObjectAttributeAsSet)) {
               if (serving.getPrivilegeToDeleteObjectState() != AttributeState.OWNED_CONSISTENT) {
                  throw new AttributeNotOwned("privilegeToDeleteObject of instance " + _object + " not owned.");
               }
               AttributeHandleSet released = _rti.attributeOwnershipDivestitureIfWanted(_object,
                     _privilegeToDeleteObjectAttributeAsSet);
               if (!released.equals(_privilegeToDeleteObjectAttributeAsSet)) {
                  throw new ProductionInternalError("attributes " + released + " were released, not " + _privilegeToDeleteObjectAttributeAsSet);
               }
               serving.setPrivilegeToDeleteObjectState(AttributeState.DISCOVERED);
            }
            else {
               throw new RTIexception("attributes " + _attributes+ " of instance " + _object + " not known.");
            }
         }
         catch (ProductionInternalError e) {
            _userInterface.post("ERROR requestAttributeOwnershipRelease: "+ e.getMessage());
         }
         catch (RTIexception e) {
            _userInterface.post("ERROR requestAttributeOwnershipRelease: " + e);
         }
         return false;
      }
   } //end RequestAttributeOwnershipReleaseCallback
}
