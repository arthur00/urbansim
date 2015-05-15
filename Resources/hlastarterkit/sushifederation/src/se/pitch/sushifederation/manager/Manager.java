package se.pitch.sushifederation.manager;

import hla.rti1516e.*;
import hla.rti1516e.AttributeHandleSet;
import hla.rti1516e.AttributeHandleSetFactory;
import hla.rti1516e.LogicalTime;
import hla.rti1516e.LogicalTimeInterval;
import hla.rti1516e.RTIambassador;
import hla.rti1516e.ResignAction;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAunicodeString;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Properties;

public final class Manager {
   //system properties used throughout
   private static String _fileSeparator = System.getProperty("file.separator");
   private static String _userDirectory = System.getProperty("user.dir");

   private FederateTable _federateTable;
   private CallbackQueue _callbackQueue;
   private ManagerFrame _userInterface;
   private FederateHandle _federateHandle; // -1 when not joined
   private FedAmbImpl _fedAmb;
   private String _fedexName;
   private Properties _properties;
   private int _federateClassHandle;
   //we use HLAfloat64Time
   private HLAfloat64TimeFactory _logicalTimeFactory = (HLAfloat64TimeFactory) LogicalTimeFactoryFactory.getLogicalTimeFactory(HLAfloat64TimeFactory.NAME);
   private LogicalTime _logicalTime;
   private LogicalTimeInterval _lookahead;
   private LogicalTimeInterval _advanceInterval; //how far to move on each time step
   private long _advanceIntervalAsMillis;        //ditto as milliseconds
   private LogicalTime _targetTime;              //where we're moving to
   private double _timeAdvanceRate;              //rate of advance relative to wall time
   private ToggleBarrier _pausedBarrier;         //mediate pausing Manager
   private boolean _simulationEndsReceived;
   private ToggleBarrier _timeAdvanceBarrier;
   private int _numberOfFederatesToAwait;

   //handles
   private ObjectClassHandle _FederateClass;
   private AttributeHandle _FederateHandleAttribute;
   private AttributeHandle _FederateTypeAttribute;
   private AttributeHandle _FederateHostAttribute;
   private AttributeHandleSet _federateAttributesSet;
   private InteractionClassHandle _SimulationEndsClass;

   //things dependent on RTI implementation in use
   private RTIambassador _rti;
   public ParameterHandleValueMapFactory _parametersFactory;
   public AttributeHandleValueMapFactory _attributesFactory;
   public AttributeHandleSetFactory _attributeHandleSetFactory;
   public FederateHandleSetFactory _federateHandleSetFactory;
   private EncoderFactory _encoderFactory;

   public Manager(Properties props) {
      _federateHandle = null;  //not joined
      _federateTable = new FederateTable();
      _callbackQueue = new CallbackQueue();
      _pausedBarrier = new ToggleBarrier();
      _timeAdvanceBarrier = new ToggleBarrier();
      _userInterface = new ManagerFrame(_pausedBarrier);
      _userInterface.finishConstruction(this, _federateTable);
      _userInterface.setVisible(true);
      _userInterface.lastAdjustments();

      _properties = props;
      //System.out.println("host: " + props.get("RTI_HOST"));
      //System.out.println("port: " + props.get("RTI_PORT"));
      //System.out.println("config: " + props.get("CONFIG"));
      //create RTI implementation
      try {
         _rti = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
         _encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
         _userInterface.post("RTIambassador created");
         _fedAmb = new FedAmbImpl(this, _userInterface);
         _simulationEndsReceived = false;
         String settingsDesignator = "crcHost=" + _properties.get("RTI_HOST") + "\ncrcPort=" + _properties.get("RTI_PORT");
         _rti.connect(_fedAmb, CallbackModel.HLA_IMMEDIATE, settingsDesignator);

         System.out.println("Manager started.");
      }
      catch (Exception e) {
         _userInterface.post("Manager: constructor failed: " + e);
         _userInterface.post("You may as well exit.");
      }
   }

   public static void main(String[] args) {
      Properties props = parseArgs(args);
      loadProperties(props);
      Manager manager = new Manager(props);
      manager.mainThread();
   }

   //the main thread
   private void mainThread() {
      Barrier barrier;
      Object[] result;
      try {
         //create federation execution (if necessary) and join
         _fedexName = (String)_properties.get("FEDEX");
         URL fomURL;
         String urlString = _properties.get("CONFIG")
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
         _federateHandle = _rti.joinFederationExecution(
               ManagerNames._federateType,
               ManagerNames._federateType,
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
         _lookahead = _logicalTimeFactory.makeInterval(new Double((getProperty("Manager.lookahead"))));
         double advanceIntervalAsDouble = new Double((getProperty("Manager.advanceInterval")));
         _advanceInterval = _logicalTimeFactory.makeInterval(advanceIntervalAsDouble);
         _timeAdvanceRate = new Double((getProperty("Manager.rate")));
         _advanceIntervalAsMillis = (new Double(
               (advanceIntervalAsDouble / _timeAdvanceRate) * 1000.0)).longValue();
         _userInterface.post("Enabling time regulation...");
         barrier = new Barrier();
         _fedAmb.setEnableTimeRegulationBarrier(barrier);
         _rti.enableTimeRegulation(_lookahead);
         result = barrier.await();
         _logicalTime = (LogicalTime)result[0];
         _userInterface.post("...regulation enabled at " + _logicalTime);
         _userInterface.setLogicalTime(((HLAfloat64Time)_logicalTime).getValue());
         _userInterface.setTimeStateGranted();

         //register synchronization points
         _userInterface.post("Registering " + ManagerNames._readyToPopulate + "...");
         barrier = new Barrier(ManagerNames._readyToPopulate);
         _fedAmb.setSynchronizationPointRegistrationSucceededBarrier(barrier);
         _rti.registerFederationSynchronizationPoint(ManagerNames._readyToPopulate, null);
         barrier.await();
         _userInterface.post("...registration succeeded.");

         _userInterface.post("Registering " + ManagerNames._readyToRun + "...");
         barrier = new Barrier(ManagerNames._readyToRun);
         _fedAmb.setSynchronizationPointRegistrationSucceededBarrier(barrier);
         _rti.registerFederationSynchronizationPoint(ManagerNames._readyToRun, null);
         barrier.await();
         _userInterface.post("...registration succeeded.");

         _userInterface.post("Registering " + ManagerNames._readyToResign + "...");
         barrier = new Barrier(ManagerNames._readyToResign);
         _fedAmb.setSynchronizationPointRegistrationSucceededBarrier(barrier);
         _rti.registerFederationSynchronizationPoint(ManagerNames._readyToResign, null);
         barrier.await();
         _userInterface.post("...registration succeeded.");

         getHandles();
         subscribe();

         _rti.enableAsynchronousDelivery();

         _userInterface.post("Waiting for all federates to join...");
         _numberOfFederatesToAwait = Integer.parseInt(
               getProperty("Manager.numberOfFederatesToAwait"));
         while (_federateTable.getRowCount() < _numberOfFederatesToAwait) {
            Callback callback = _callbackQueue.dequeue();
            boolean ignore = callback.dispatch();
            _federateTable.updateFederates(_rti, _federateAttributesSet);
         }
         _userInterface.post("...done.");

         //Manager achieves ReadyToPopulate and waits for rest of federation
         _userInterface.post("Waiting for ReadyToPopulate...");
         barrier = new Barrier(ManagerNames._readyToPopulate);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToPopulate);
         barrier.await();
         _userInterface.post("...federation synchronized.");

         //Manager achieves ReadyToRun and waits for rest of federation
         _userInterface.post("Waiting for ReadyToRun...");
         barrier = new Barrier(ManagerNames._readyToRun);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToRun);
         barrier.await();
         _userInterface.post("...federation synchronized.");
         _federateTable.updateFederates(_rti, _federateAttributesSet);

         //advance time in steps until interrupted with SimulationEnds interaction
         _userInterface.post("Adv int (ms): " + _advanceIntervalAsMillis);
         long wallClockWhenGranted;
         long wallClockAtWhichToRequestAdvance = System.currentTimeMillis();
         timeLoop:
         while (!_simulationEndsReceived) {
            //wait till we're not paused
            _pausedBarrier.await();
            wallClockWhenGranted = System.currentTimeMillis();
            long sleepTime = wallClockAtWhichToRequestAdvance + _advanceIntervalAsMillis
                  - wallClockWhenGranted;
            _userInterface.post("Sleeping for " + sleepTime);
            if (sleepTime > 0) {
               try {
                  Thread.sleep(sleepTime);
               }
               catch (InterruptedException e) {
                  // ignore
               }
            }
            //advance by a step
            _targetTime = _logicalTime.add(_advanceInterval);
            _userInterface.setTimeStateAdvancing();
            wallClockAtWhichToRequestAdvance += _advanceIntervalAsMillis;
            _rti.timeAdvanceRequest(_targetTime);
            boolean wasTimeAdvanceGrant;
            do {
               Callback callback = _callbackQueue.dequeue();
               wasTimeAdvanceGrant = callback.dispatch();
               if (_simulationEndsReceived) break timeLoop;
            } while (!wasTimeAdvanceGrant);
            _federateTable.updateFederates(_rti, _federateAttributesSet);
         }
         _userInterface.post("SimulationEnds received.");

         //Manager achieves ReadyToResign and waits for rest of federation
         _userInterface.post("Waiting for ReadyToResign...");
         barrier = new Barrier(ManagerNames._readyToResign);
         _fedAmb.setFederationSynchronizedBarrier(barrier);
         _rti.synchronizationPointAchieved(ManagerNames._readyToResign);
         barrier.await();
         _userInterface.post("...federation synchronized.");

         //wait until other federates have resigned
         while (_federateTable.getRowCount() > 1) {
            Callback callback = _callbackQueue.dequeue();
            boolean ignore = callback.dispatch();
         }

         _rti.resignFederationExecution(ResignAction.DELETE_OBJECTS_THEN_DIVEST);
         _federateHandle = null;
         _rti.destroyFederationExecution(_fedexName);
         _userInterface.post("All done.");
      }
      catch (Exception e) {
         _userInterface.post("Exception in main thread: " + e);
         e.printStackTrace();
      }
   }

   private void getHandles() throws RTIexception {
      _FederateClass = _rti.getObjectClassHandle(ManagerNames._FederateClassName);
      _FederateHandleAttribute = _rti.getAttributeHandle(
            _FederateClass,
            ManagerNames._FederateHandleAttributeName);
      _FederateTypeAttribute = _rti.getAttributeHandle(
            _FederateClass,
            ManagerNames._FederateTypeAttributeName);
      _FederateHostAttribute = _rti.getAttributeHandle(
            _FederateClass,
            ManagerNames._FederateHostAttributeName);

      _federateAttributesSet = _attributeHandleSetFactory.create();
      _federateAttributesSet.add(_FederateHandleAttribute);
      _federateAttributesSet.add(_FederateTypeAttribute);
      _federateAttributesSet.add(_FederateHostAttribute);

      _SimulationEndsClass =
            _rti.getInteractionClassHandle(ManagerNames._SimulationEndsClassName);
   }

   private void subscribe()
         throws RTIexception
   {
      //subscribe to MOM attributes
      _rti.subscribeObjectClassAttributes(_FederateClass, _federateAttributesSet);
      //subscribe to ending interaction
      _rti.subscribeInteractionClass(_SimulationEndsClass);
   }

   //represents one callback coming from RTI
   //This class is defined within Manager so the dispatch routines
   //of its subclasses have the context of Manager
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
         try {
            if (!_objectClass.equals(_FederateClass)) throw new ManagerInternalError(
                  "instance " + _instanceHandle + "(" + _instanceName + ") of class " + _objectClass);
            _federateTable.add(_instanceHandle, _instanceName);
            //FederateTable.updateFederates() will elicit updates for these values
            //This will be called on time grant.
         }
         catch (ManagerInternalError e) {
            _userInterface.post("ERROR: discoverObjectInstance: " + e);
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

   public void queueReflectAttributeValuesCallback(
         ObjectInstanceHandle objectHandle,
         AttributeHandleValueMap attributes,
         byte[] tag)
   {
      Callback callback = new ReflectAttributeValuesCallback(
            objectHandle,
            attributes,
            tag);
      _callbackQueue.enqueue(callback);
   }

   public final class ReflectAttributeValuesCallback extends Callback {
      ObjectInstanceHandle _object;
      AttributeHandleValueMap _attributes;
      byte[] _tag;

      public ReflectAttributeValuesCallback(
            ObjectInstanceHandle objectHandle,
            AttributeHandleValueMap attributes,
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
            for (AttributeHandle handle : _attributes.keySet()) {
               byte[] value = _attributes.get(handle);
               if (handle.equals(_FederateHandleAttribute)) {
                  String result = "[";
                  for (short s : value) {
                     result += " " + s;
                  }
                  result += " ]";
                  _federateTable.setFederateHandle(
                        _object,
                        String.valueOf(result));
               } else {
                  HLAunicodeString decoder = _encoderFactory.createHLAunicodeString();
                  decoder.decode(value);
                  String str  = decoder.getValue();

                  if (handle.equals(_FederateTypeAttribute)) {
                     _federateTable.setFederateType(
                           _object,
                           str);
                  }
                  else if (handle.equals(_FederateHostAttribute)) {
                     _federateTable.setFederateHost(
                           _object,
                           str);
                  }
                  else throw new ManagerInternalError("unknown attribute " + handle);
               }
            }
         }
         catch (DecoderException e) {
            _userInterface.post("ERROR reflectAttributeValues: " + e.getMessage());
         }
         catch (ManagerInternalError e) {
            _userInterface.post("ERROR reflectAttributeValues: " + e.getMessage());
         }
         return false;
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
      {
         _federateTable.remove(_object);
         return false;
      }
   } //end RemoveObjectInstanceCallback

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
         System.out.println("Manager.parseArgs: default arguments failed: " + e);
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
         System.out.println("Manager failed to load properties: " + e);
         System.exit(1);
      }
   }
}
