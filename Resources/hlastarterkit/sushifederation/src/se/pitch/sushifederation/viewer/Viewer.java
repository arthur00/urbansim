
//Title:        Viewer Federate for Book
//Version:      
//Copyright:    Copyright (c) 1998
//Author:       Frederick Kuhl
//Company:      The MITRE corporation
//Description:  Your description


package se.pitch.sushifederation.viewer;

import hla.rti1516e.*;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleSetFactory;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.LogicalTimeInterval;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.exceptions.CouldNotDecode;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64TimeFactory;
import se.pitch.sushifederation.*;
import se.pitch.sushifederation.manager.ManagerNames;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Properties;

public final class Viewer {
   //system properties used throughout
   private static String _fileSeparator = System.getProperty("file.separator");
   private static String _userDirectory = System.getProperty("user.dir");

   private ViewerFrame _userInterface;
   private FederateHandle _federateHandle; // -1 when not joined
   private boolean _simulationEndsReceived;
   private FedAmbImpl _fedAmb;
   private String _fedexName;
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
   public ParameterHandleValueMapFactory _parametersFactory;
   public AttributeHandleValueMapFactory _attributesFactory;
   public AttributeHandleSetFactory _attributeHandleSetFactory;
   public FederateHandleSetFactory _federateHandleSetFactory;

   //handles and handle sets
   private ObjectClassHandle _RestaurantClass;
   private ObjectClassHandle _BoatClass;
   private ObjectClassHandle _ServingClass;
   private ObjectClassHandle _ActorClass;
   private ObjectClassHandle _ChefClass;
   private ObjectClassHandle _DinerClass;
   private AttributeHandle _positionAttribute;
   private AttributeHandle _typeAttribute;
   private AttributeHandle _spaceAvailableAttribute;
   private AttributeHandle _cargoAttribute;
   private AttributeHandle _chefStateAttribute;
   private AttributeHandle _dinerStateAttribute;
   private AttributeHandle _servingNameAttribute;
   private AttributeHandleSet _ServingAttributes;
   private AttributeHandleSet _BoatAttributes;
   private AttributeHandleSet _ChefAttributes;
   private AttributeHandleSet _DinerAttributes;
   private InteractionClassHandle _SimulationEndsClass;

   private int _nextServingSerial;   //serial to assign to next Serving registered
   private Hashtable<ObjectInstanceHandle, Chef> _chefs;         //key: instance handle; value: Chef
   private Hashtable<ObjectInstanceHandle, Serving> _servings;      //key: instance handle; value: Serving
   private Hashtable<ObjectInstanceHandle, Boat> _boats;         //key: instance handle; value: Boat
   private Hashtable<ObjectInstanceHandle, Diner> _diners;        //key: instance handle; value: Diners
   private int _numberOfSushiTypes;
   private CallbackQueue _callbackQueue;

   public Viewer(Properties props) {
      _federateHandle = null;  //not joined
      _properties = props;
      _servings = new Hashtable<ObjectInstanceHandle, Serving>();
      _boats = new Hashtable<ObjectInstanceHandle, Boat>();
      _chefs = new Hashtable<ObjectInstanceHandle, Chef>();
      _diners = new Hashtable<ObjectInstanceHandle, Diner>();
      _callbackQueue = new CallbackQueue();
      _simulationEndsReceived = false;

      _userInterface = new ViewerFrame();
      _userInterface.finishConstruction(
            this,
            _chefs,
            _servings,
            _boats,
            _diners);
      _userInterface.setVisible(true);
      _userInterface.lastAdjustments();

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


         System.out.println("Viewer started.");
      }
      catch (Exception e) {
         _userInterface.post("Viewer: constructor failed: " + e);
         _userInterface.post("You may as well exit.");
      }
   }

   public static void main(String[] args) {
      Properties props = parseArgs(args);
      loadProperties(props);
      Viewer production = new Viewer(props);
      production.mainThread();
   }

   //the main thread
   private void mainThread() {
      Barrier barrier;
      Object[] result;

      try {
         getConfigurationData();

         //create federation execution (if necessary) and join
         _fedexName = (String)_properties.get("FEDEX");
         URL fomURL;
         String urlString =
               (String)_properties.get("CONFIG")
                     + _fedexName
                     + ".xml";
         fomURL = new URL(urlString);
         //the federation execution may already exist
         try {
            _rti.createFederationExecution(_fedexName, fomURL);
            _userInterface.post("Federation execution " + _fedexName + " created.");
         }
         catch (FederationExecutionAlreadyExists e) {
            _userInterface.post("Federation execution " + _fedexName
                  + " already exists.");
         }
         //join federation execution
         _fedexName = (String)_properties.get("FEDEX");
         _federateHandle = _rti.joinFederationExecution(
               ViewerNames._federateType,
               ViewerNames._federateType,
               _fedexName,
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
         _lookahead = _logicalTimeFactory.makeInterval(new Double((getProperty("Viewer.lookahead"))));
         _advanceInterval = _logicalTimeFactory.makeInterval(new Double((getProperty("Viewer.advanceInterval"))));
         _userInterface.post("Enabling time regulation...");
         barrier = new Barrier();
         _fedAmb.setEnableTimeRegulationBarrier(barrier);
         _rti.enableTimeRegulation(_lookahead);
         result = barrier.await();
         _logicalTime = (LogicalTime)result[0];
         _userInterface.post("...regulation enabled at " + _logicalTime);

         getHandles();
         subscribe();

         //Viewer achieves ReadyToPopulate and waits for rest of federation
         _readyToPopulateAnnouncementBarrier.await();
         _userInterface.post("Waiting for ReadyToPopulate...");
         barrier = new Barrier(ManagerNames._readyToPopulate);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToPopulate);
         barrier.await();
         _userInterface.post("...federation synchronized.");

         //Viewer achieves ReadyToRun and waits for rest of federation
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
            _rti.timeAdvanceRequest(_targetTime);
            //chew through all the events we receive from the RTI
            boolean wasTimeAdvanceGrant;
            do {
               Callback callback = _callbackQueue.dequeue();
               wasTimeAdvanceGrant = callback.dispatch();
               if (_simulationEndsReceived) break timeLoop;
            } while (!wasTimeAdvanceGrant);
         }
         _userInterface.post("SimulationEnds received.");

         //Viewer achieves ReadyToResign and waits for rest of federation
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
      _ActorClass = _rti.getObjectClassHandle(RestaurantNames._ActorClassName);
      _ChefClass = _rti.getObjectClassHandle(RestaurantNames._ChefClassName);
      _DinerClass = _rti.getObjectClassHandle(RestaurantNames._DinerClassName);
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
      _servingNameAttribute = _rti.getAttributeHandle(
            _ActorClass,
            RestaurantNames._servingNameAttributeName);
      _chefStateAttribute = _rti.getAttributeHandle(
            _ChefClass,
            RestaurantNames._chefStateAttributeName);
      _dinerStateAttribute = _rti.getAttributeHandle(
            _DinerClass,
            RestaurantNames._dinerStateAttributeName);

      _ServingAttributes = _attributeHandleSetFactory.create();
      _ServingAttributes.add(_positionAttribute);
      _ServingAttributes.add(_typeAttribute);

      _BoatAttributes = _attributeHandleSetFactory.create();
      _BoatAttributes.add(_positionAttribute);
      _BoatAttributes.add(_spaceAvailableAttribute);
      _BoatAttributes.add(_cargoAttribute);

      _ChefAttributes = _attributeHandleSetFactory.create();
      _ChefAttributes.add(_positionAttribute);
      _ChefAttributes.add(_chefStateAttribute);
      _ChefAttributes.add(_servingNameAttribute);

      _DinerAttributes = _attributeHandleSetFactory.create();
      _DinerAttributes.add(_positionAttribute);
      _DinerAttributes.add(_dinerStateAttribute);
      _DinerAttributes.add(_servingNameAttribute);

      _SimulationEndsClass =
            _rti.getInteractionClassHandle(ManagerNames._SimulationEndsClassName);
   }

   private void subscribe()
         throws RTIexception
   {
      _rti.subscribeObjectClassAttributesPassively(_BoatClass, _BoatAttributes);
      _rti.subscribeObjectClassAttributesPassively(_ServingClass, _ServingAttributes);
      _rti.subscribeObjectClassAttributesPassively(_ChefClass, _ChefAttributes);
      _rti.subscribeObjectClassAttributesPassively(_DinerClass, _DinerAttributes);

      _rti.subscribeInteractionClass(_SimulationEndsClass);
   }

   private void getConfigurationData()
   {
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

   //defined so that a missing property doesn't cause a crash later
   public String getProperty(String name) {
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
         System.out.println("Viewer.parseArgs: default arguments failed: " + e);
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
         
         File propsFile = new File("config" + _fileSeparator + props.getProperty("FEDEX") + ".props");
         System.out.println(propsFile.getAbsolutePath());
         loadProperties(propsFile, props);
      }
      catch (Exception e) {
         System.out.println("Viewer failed to load properties: " + e);
         System.exit(1);
      }
   }

   private static void loadProperties(File file, Properties props) throws IOException {
      InputStream in = null;
      try {
         in = new FileInputStream(file);
         props.load(in);
      } finally {
         if (in != null) {
            in.close();
         }
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
         if (_objectClass.equals(_BoatClass)) {
            Boat newBoat = new Boat();
            newBoat.setHandle(_instanceHandle);
            newBoat.setName(_instanceName);
            newBoat.setPrivilegeToDeleteObjectState(AttributeState.NOT_REFLECTED);
            newBoat.setPositionState(AttributeState.DISCOVERED);
            newBoat.setSpaceAvailableState(AttributeState.DISCOVERED);
            newBoat.setCargoState(AttributeState.DISCOVERED);
            _boats.put(_instanceHandle, newBoat);
            //_userInterface.post("Discovered Boat " + _instanceHandle + "(" + _instanceName + ")");
         }
         else if (_objectClass.equals(_ServingClass)) {
            Serving newServing = new Serving();
            newServing.setHandle(_instanceHandle);
            newServing.setName(_instanceName);
            newServing.setPrivilegeToDeleteObjectState(AttributeState.NOT_REFLECTED);
            newServing.setPositionState(AttributeState.DISCOVERED);
            newServing.setTypeState(AttributeState.DISCOVERED);
            _servings.put(_instanceHandle, newServing);
            //_userInterface.post("Discovered Serving " + _instanceHandle + "(" + _instanceName + ")");
         }
         else if (_objectClass.equals(_ChefClass)) {
            Chef newChef = new Chef();
            newChef.setHandle(_instanceHandle);
            newChef.setName(_instanceName);
            newChef.setPrivilegeToDeleteObjectState(AttributeState.NOT_REFLECTED);
            newChef.setPositionState(AttributeState.DISCOVERED);
            newChef.setStateState(AttributeState.DISCOVERED);
            newChef.setServingNameState(AttributeState.DISCOVERED);
            _chefs.put(_instanceHandle, newChef);
            //_userInterface.post("Discovered Chef " + _instanceHandle + "(" + _instanceName + ")");
         }
         else if (_objectClass.equals(_DinerClass)) {
            Diner newDiner = new Diner();
            newDiner.setHandle(_instanceHandle);
            newDiner.setName(_instanceName);
            newDiner.setPrivilegeToDeleteObjectState(AttributeState.NOT_REFLECTED);
            newDiner.setPositionState(AttributeState.DISCOVERED);
            newDiner.setStateState(AttributeState.DISCOVERED);
            newDiner.setServingNameState(AttributeState.DISCOVERED);
            _diners.put(_instanceHandle, newDiner);
            //_userInterface.post("Discovered Diner " + _instanceHandle + "(" + _instanceName + ")");
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
         _userInterface.updateView();
         return true;
      }
   } //end GrantEvent

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
            ObjectClassHandle objectClass = _rti.getKnownObjectClassHandle(_object);
            if (objectClass.equals(_BoatClass)) updateBoat();
            else if (objectClass.equals(_ServingClass)) updateServing();
            else if (objectClass.equals(_ChefClass)) updateChef();
            else if (objectClass.equals(_DinerClass)) updateDiner();
            else throw new ViewerInternalError (
                     "unexpected class " + objectClass + " instance " + _object);
         }
         catch (ViewerInternalError e) {
            _userInterface.post("ERROR: reflectAttributeValues: " + e.getMessage());
         }
         catch (CouldNotDecode e) {
            _userInterface.post("ERROR: reflectAttributeValues: " + e);
         }
         return false;
      }

      private void updateBoat()
            throws ViewerInternalError, CouldNotDecode
      {
         //find the Boat in our collection
         Boat boat = _boats.get(_object);
         if (boat == null) throw new ViewerInternalError ("unknown Boat " + _object);
         //decode and store new attribute values
         for (AttributeHandle handle : _attributes.keySet()) {
            if (handle.equals(_positionAttribute)) {
               boat.setPosition(new Position(_attributes.get(handle)));
               boat.setPositionState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " boat " + _object + " posn " + boat._position);
            }
            else if (handle.equals(_spaceAvailableAttribute)) {
               boat.setSpaceAvailable(SpaceAvailable.decode(_attributes.get(handle)));
               boat.setSpaceAvailableState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " boat " + _object + " sp avail " + boat._spaceAvailable);
            }
            else if (handle.equals(_cargoAttribute)) {
               boat.setCargo(InstanceName.decode(_attributes.get(handle)));
               boat.setCargoState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " boat " + _object + " cargo " + boat._cargo);
            }
            else throw new ViewerInternalError("unknown attribute " + handle);
         }
      }

      private void updateServing()
            throws ViewerInternalError, CouldNotDecode
      {
         //find the Serving in our collection
         Serving serving = _servings.get(_object);
         if (serving == null) throw new ViewerInternalError ("unknown Serving " + _object);
         //decode and store new attribute values
         for (AttributeHandle handle : _attributes.keySet()) {
            if (handle.equals(_positionAttribute)) {
               serving.setPosition(new Position(_attributes.get(handle)));
               serving.setPositionState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " Svg " + _object + " posn " + serving._position);
            }
            else if (handle.equals(_typeAttribute)) {
               serving.setType(IntegerAttribute.decode(_attributes.get(handle)));
               serving.setTypeState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " Svg " + _object + " type " + serving._type);
            }
            else throw new ViewerInternalError("unknown attribute " + handle);
         }
      }

      private void updateChef()
            throws ViewerInternalError, CouldNotDecode
      {
         //find the Chef in our collection
         Chef chef = _chefs.get(_object);
         if (chef == null) throw new ViewerInternalError ("unknown Chef " + _object);
         //decode and store new attribute values
         for (AttributeHandle handle : _attributes.keySet()) {
            if (handle.equals(_positionAttribute)) {
               chef.setPosition(new Position(_attributes.get(handle)));
               chef.setPositionState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " Chef " + _object + " posn " + chef._position);
            }
            else if (handle.equals(_chefStateAttribute)) {
               chef.setState(Chef.State.find(IntegerAttribute.decode(_attributes.get(handle))));
               chef.setStateState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " Chef " + _object + " state " + chef._state);
            }
            else if (handle.equals(_servingNameAttribute)) {
               chef.setServingName(InstanceName.decode(_attributes.get(handle)));
               chef.setServingNameState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " Chef " + _object + " svg name " + chef._servingName);
            }
            else throw new ViewerInternalError("unknown attribute " + handle);
         }
      }

      private void updateDiner()
            throws ViewerInternalError, CouldNotDecode
      {
         //find the Diner in our collection
         Diner diner = _diners.get(_object);
         if (diner == null) throw new ViewerInternalError ("unknown Diner " + _object);
         //decode and store new attribute values
         for (AttributeHandle handle : _attributes.keySet()) {
            if (handle.equals(_positionAttribute)) {
               diner.setPosition(new Position(_attributes.get(handle)));
               diner.setPositionState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " Diner " + _object + " posn " + diner._position);
            }
            else if (handle.equals(_dinerStateAttribute)) {
               diner.setState(Diner.State.find(IntegerAttribute.decode(_attributes.get(handle))));
               diner.setStateState(AttributeState.REFLECTED); //we have data
               // _userInterface.post("Refl " + _time + " Diner " + _object + " state " + diner._state);
            }
            else if (handle.equals(_servingNameAttribute)) {
               diner.setServingName(InstanceName.decode(_attributes.get(handle)));
               diner.setServingNameState(AttributeState.REFLECTED); //we have data
               //_userInterface.post("Refl " + _time + " Diner " + _object + " svg name " + diner._servingName);
            }
            else throw new ViewerInternalError("unknown attribute " + handle);
         }
      }
   } //end ReflectAttributeValuesEvent

   public void queueRemoveObjectInstanceEvent(
         LogicalTime time,
         ObjectInstanceHandle objectHandle)
   {
      ExternalEvent event = new RemoveObjectInstanceEvent(
            time,
            objectHandle);
      _callbackQueue.enqueue(event);
   }

   public final class RemoveObjectInstanceEvent extends ExternalEvent {
      LogicalTime _time;
      ObjectInstanceHandle _object;

      public RemoveObjectInstanceEvent(
            LogicalTime time,
            ObjectInstanceHandle objectHandle)
      {
         _time = time;
         _object = objectHandle;
      }

      public boolean dispatch()
            throws RTIexception
      {
         Object entry;
         if ((entry = _servings.get(_object)) != null) {
            _servings.remove(_object);
            _userInterface.post("Serving " + ((Serving)entry).getName() + " being removed at " + _time);
         }
         else if ((entry = _boats.get(_object)) != null) {
            _boats.remove(_object);
            _userInterface.post("Boat " + ((Boat)entry).getName() + " being removed at " + _time);
         }
         else if ((entry = _chefs.get(_object)) != null) {
            _chefs.remove(_object);
            _userInterface.post("Chef " + ((Chef)entry).getName() + " being removed at " + _time);
         }
         else if ((entry = _diners.get(_object)) != null) {
            _diners.remove(_object);
            _userInterface.post("Diner " + ((Diner)entry).getName() + " being removed at " + _time);
         }
         else {
            throw new RTIexception("Object " + _object + " not known");
         }
         return false;
      }
   } //end RemoveObjectInstanceEvent
}
