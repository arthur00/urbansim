/*
 * Copyright (C) 2012  Pitch Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.pitch.hlatutorial.mapviewer.hlamodule;

import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.*;
import se.pitch.hlatutorial.mapviewer.model.Car;
import se.pitch.hlatutorial.mapviewer.model.FuelType;
import se.pitch.hlatutorial.mapviewer.model.Position;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


class HlaInterfaceImpl extends NullFederateAmbassador implements HlaInterface {
   private RTIambassador _ambassador;

   private String _federateName = "";
   private String _federationName;

   private final List<InteractionListener> _listeners = new CopyOnWriteArrayList<InteractionListener>();

   private final List<CarListener> _carListeners = new CopyOnWriteArrayList<CarListener>();

   private HLAunicodeStringCoder _unicodeStringCoder;
   private HLAunicodeStringCoder _callbackUnicodeStringCoder;
   private FuelInt32Coder _callbackFuelInt32Coder;
   private FuelTypeEnum8Coder _callbackFuelTypeEnum8Coder;
   private PositionRecordCoder _callbackPositionRecordCoder;
   private TimeScaleFactorFloat32Coder _callbackTimeScaleFactorCoder;

   private InteractionClassHandle _startInteractionClassHandle;
   private ParameterHandle _timeScaleFactorParameterHandle;
   private InteractionClassHandle _stopInteractionClassHandle;

   private InteractionClassHandle _loadScenarioInteractionClassHandle;
   private ParameterHandle _scenarioNameParameterHandle;
   private ParameterHandle _initialFuelAmountParameterHandle;

   private InteractionClassHandle _scenarioLoadedInteractionClassHandle;
   private ParameterHandle _scenarioLoadedFederateNameParameterHandle;

   private InteractionClassHandle _scenarioLoadFailureInteractionClassHandle;
   private ParameterHandle _scenarioLoadFailureFederateNameParameterHandle;
   private ParameterHandle _scenarioLoadFailureErrorMessageParameterHandle;

   private ObjectClassHandle _objectClassCarHandle;
   private AttributeHandle _attributePositionHandle;
   private AttributeHandle _attributeFuelLevelHandle;
   private AttributeHandle _nameAttributeHandle;
   private AttributeHandle _licensePlateNumberAttributeHandle;
   private AttributeHandle _fuelTypeAttributeHandle;

   private final CarMapping _localCars = new CarMapping();


   public HlaInterfaceImpl() {
   }

   public void start(String localSettingsDesignator, String fomPath, String federationName, String federateName) throws ConnectionFailed, InvalidLocalSettingsDesignator, RTIinternalError, NotConnected, ErrorReadingFDD, CouldNotOpenFDD, InconsistentFDD, RestoreInProgress, SaveInProgress, FederateServiceInvocationsAreBeingReportedViaMOM {
      RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
      _ambassador = rtiFactory.getRtiAmbassador();

      EncoderFactory encoderFactory = rtiFactory.getEncoderFactory();
      _unicodeStringCoder = new HLAunicodeStringCoder(encoderFactory);
      _callbackUnicodeStringCoder = new HLAunicodeStringCoder(encoderFactory);
      _callbackFuelInt32Coder = new FuelInt32Coder(encoderFactory);
      _callbackFuelTypeEnum8Coder = new FuelTypeEnum8Coder(encoderFactory);
      _callbackPositionRecordCoder = new PositionRecordCoder(encoderFactory);
      _callbackTimeScaleFactorCoder = new TimeScaleFactorFloat32Coder(encoderFactory);

      try {
         _ambassador.connect(this, CallbackModel.HLA_IMMEDIATE, localSettingsDesignator);
      } catch (AlreadyConnected ignored) {
      } catch (UnsupportedCallbackModel e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (CallNotAllowedFromWithinCallback e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }

      _federationName = federationName;
      try {
         _ambassador.destroyFederationExecution(federationName);
      } catch (FederatesCurrentlyJoined ignored) {
      } catch (FederationExecutionDoesNotExist ignored) {
      }

      File fddFile = new File(fomPath);
      URL url = null;
      try {
         url = fddFile.toURI().toURL();
      } catch (MalformedURLException ignored) {
      }
      try {
         _ambassador.createFederationExecution(federationName, url);
      } catch (FederationExecutionAlreadyExists ignored) {
      }

      try {
         boolean joined = false;
         String federateNameSuffix = "";
         int federateNameIndex = 1;
         while (!joined) {
            try {
               _ambassador.joinFederationExecution(federateName + federateNameSuffix, "MapViewer", federationName, new URL[] {url});
               joined = true;
               _federateName = federateName + federateNameSuffix;
            } catch (FederateNameAlreadyInUse e) {
               federateNameSuffix = "-" + federateNameIndex++;
            }
         }
      } catch (FederateAlreadyExecutionMember ignored) {
      } catch (CouldNotCreateLogicalTimeFactory e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (FederationExecutionDoesNotExist e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (CallNotAllowedFromWithinCallback e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }

      try {
         getHandles();

         subscribeInteractions();
         publishInteractions();

         subscribeCar();
      } catch (FederateNotExecutionMember e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   public void stop() throws RTIinternalError {
      try {
         try {
            _ambassador.resignFederationExecution(ResignAction.CANCEL_THEN_DELETE_THEN_DIVEST);
         } catch (FederateNotExecutionMember ignored) {
         } catch (FederateOwnsAttributes e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
         } catch (OwnershipAcquisitionPending e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
         } catch (CallNotAllowedFromWithinCallback e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
         } catch (InvalidResignAction e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
         }

         if (_federationName != null) {
            try {
               _ambassador.destroyFederationExecution(_federationName);
            } catch (FederatesCurrentlyJoined ignored) {
            } catch (FederationExecutionDoesNotExist ignored) {
            }
         }

         try {
            _ambassador.disconnect();
         } catch (FederateIsExecutionMember e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
         } catch (CallNotAllowedFromWithinCallback e) {
            throw new RTIinternalError("HlaInterfaceFailure", e);
         }
      } catch (NotConnected ignored) {
      }
   }

   private void getHandles() throws RTIinternalError, FederateNotExecutionMember, NotConnected {
      try {
         _objectClassCarHandle = _ambassador.getObjectClassHandle("Car");
         _attributePositionHandle = _ambassador.getAttributeHandle(_objectClassCarHandle, "Position");
         _attributeFuelLevelHandle = _ambassador.getAttributeHandle(_objectClassCarHandle, "FuelLevel");
         _nameAttributeHandle = _ambassador.getAttributeHandle(_objectClassCarHandle, "Name");
         _licensePlateNumberAttributeHandle = _ambassador.getAttributeHandle(_objectClassCarHandle, "LicensePlateNumber");
         _fuelTypeAttributeHandle = _ambassador.getAttributeHandle(_objectClassCarHandle, "FuelType");

         _loadScenarioInteractionClassHandle = _ambassador.getInteractionClassHandle("LoadScenario");
         _scenarioNameParameterHandle = _ambassador.getParameterHandle(_loadScenarioInteractionClassHandle, "ScenarioName");
         _initialFuelAmountParameterHandle = _ambassador.getParameterHandle(_loadScenarioInteractionClassHandle, "InitialFuelAmount");

         _startInteractionClassHandle = _ambassador.getInteractionClassHandle("Start");
         _timeScaleFactorParameterHandle = _ambassador.getParameterHandle(_startInteractionClassHandle, "TimeScaleFactor");
         _stopInteractionClassHandle = _ambassador.getInteractionClassHandle("Stop");

         _scenarioLoadedInteractionClassHandle = _ambassador.getInteractionClassHandle("ScenarioLoaded");
         _scenarioLoadedFederateNameParameterHandle = _ambassador.getParameterHandle(_scenarioLoadedInteractionClassHandle, "FederateName");

         _scenarioLoadFailureInteractionClassHandle = _ambassador.getInteractionClassHandle("ScenarioLoadFailure");
         _scenarioLoadFailureFederateNameParameterHandle = _ambassador.getParameterHandle(_scenarioLoadFailureInteractionClassHandle, "FederateName");
         _scenarioLoadFailureErrorMessageParameterHandle = _ambassador.getParameterHandle(_scenarioLoadFailureInteractionClassHandle, "ErrorMessage");
      } catch (NameNotFound e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InvalidInteractionClassHandle e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InvalidObjectClassHandle e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   private void subscribeCar() throws FederateNotExecutionMember, RestoreInProgress, SaveInProgress, NotConnected, RTIinternalError {
      try {
         AttributeHandleSet carAttributes = _ambassador.getAttributeHandleSetFactory().create();
         carAttributes.add(_attributePositionHandle);
         carAttributes.add(_attributeFuelLevelHandle);
         carAttributes.add(_nameAttributeHandle);
         carAttributes.add(_licensePlateNumberAttributeHandle);
         carAttributes.add(_fuelTypeAttributeHandle);
         _ambassador.subscribeObjectClassAttributes(_objectClassCarHandle, carAttributes);
      } catch (AttributeNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (ObjectClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   private void subscribeInteractions() throws FederateNotExecutionMember, RestoreInProgress, SaveInProgress, NotConnected, RTIinternalError, FederateServiceInvocationsAreBeingReportedViaMOM {
      try {
         _ambassador.subscribeInteractionClass(_startInteractionClassHandle);
         _ambassador.subscribeInteractionClass(_stopInteractionClassHandle);
         _ambassador.subscribeInteractionClass(_loadScenarioInteractionClassHandle);
      } catch (InteractionClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   private void publishInteractions() throws FederateNotExecutionMember, RestoreInProgress, SaveInProgress, NotConnected, RTIinternalError {
      try {
         _ambassador.publishInteractionClass(_scenarioLoadedInteractionClassHandle);
         _ambassador.publishInteractionClass(_scenarioLoadFailureInteractionClassHandle);
      } catch (InteractionClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   @Override
   public void connectionLost(String faultDescription) throws FederateInternalError {
      System.out.println("Lost Connection because: " + faultDescription);
   }


   /*
    * Interactions
    */

   public void addInteractionListener(InteractionListener listener) {
      _listeners.add(listener);
   }

   public void removeInteractionListener(InteractionListener listener) {
      _listeners.add(listener);
   }

   public void sendScenarioLoaded() throws FederateNotExecutionMember, NotConnected, RestoreInProgress, SaveInProgress, RTIinternalError {
      try {
         ParameterHandleValueMap theParameters = _ambassador.getParameterHandleValueMapFactory().create(1);
         theParameters.put(_scenarioLoadedFederateNameParameterHandle, _unicodeStringCoder.encode(_federateName));
         _ambassador.sendInteraction(_scenarioLoadedInteractionClassHandle, theParameters, null);
      } catch (InteractionClassNotPublished e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InteractionParameterNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InteractionClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   public void sendScenarioLoadFailure(String errorMessage) throws FederateNotExecutionMember, NotConnected, RestoreInProgress, SaveInProgress, RTIinternalError {
      try {
         ParameterHandleValueMap theParameters = _ambassador.getParameterHandleValueMapFactory().create(2);
         theParameters.put(_scenarioLoadFailureFederateNameParameterHandle, _unicodeStringCoder.encode(_federateName));
         theParameters.put(_scenarioLoadFailureErrorMessageParameterHandle, _unicodeStringCoder.encode(errorMessage));
         _ambassador.sendInteraction(_scenarioLoadFailureInteractionClassHandle, theParameters, null);
      } catch (InteractionClassNotPublished e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InteractionParameterNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InteractionClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   @Override
   public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters, byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
      receiveInteraction(interactionClass, theParameters);
   }

   @Override
   public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters, byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, LogicalTime theTime, OrderType receivedOrdering, SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
      receiveInteraction(interactionClass, theParameters);
   }

   @Override
   public void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters, byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, LogicalTime theTime, OrderType receivedOrdering, MessageRetractionHandle retractionHandle, SupplementalReceiveInfo receiveInfo) throws FederateInternalError {
      receiveInteraction(interactionClass, theParameters);
   }

   private void receiveInteraction(InteractionClassHandle interactionClass, ParameterHandleValueMap theParameters) {
      if (interactionClass.equals(_startInteractionClassHandle)) {
         try {
            float timeScaleFactor = _callbackTimeScaleFactorCoder.decode(theParameters.get(_timeScaleFactorParameterHandle));
            for (InteractionListener listener : _listeners) {
               listener.receivedStart(timeScaleFactor);
            }
         } catch (DecoderException e) {
            System.out.println("Failed to decode parameters for Start interaction");
            e.printStackTrace();
         }
      } else if (interactionClass.equals(_stopInteractionClassHandle)) {
         for (InteractionListener listener : _listeners) {
            listener.receivedStop();
         }
      } else if (interactionClass.equals(_loadScenarioInteractionClassHandle)) {
         try {
            String destination = _callbackUnicodeStringCoder.decode(theParameters.get(_scenarioNameParameterHandle));
            int initialFuelAmount = _callbackFuelInt32Coder.decode(theParameters.get(_initialFuelAmountParameterHandle));
            for (InteractionListener listener : _listeners) {
               listener.receivedSetScenario(destination, initialFuelAmount);
            }
         } catch (DecoderException e) {
            System.out.println("Failed to decode parameters for SetScenario interaction");
            e.printStackTrace();
         }
      }
   }


   /*
    * Objects
    */

   public void addCarListener(CarListener listener) {
      _carListeners.add(listener);
   }

   public void removeCarListener(CarListener listener) {
      _carListeners.remove(listener);
   }

   @Override
   public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName) throws FederateInternalError {
      Car car = new Car();
      synchronized (_localCars) {
         _localCars.put(theObject, car.getIdentifier());
      }
      for (CarListener listener : _carListeners) {
         listener.carAdded(car);
      }
   }

   @Override
   public void discoverObjectInstance(ObjectInstanceHandle theObject, ObjectClassHandle theObjectClass, String objectName, FederateHandle producingFederate) throws FederateInternalError {
      discoverObjectInstance(theObject, theObjectClass, objectName);
   }

   @Override
   public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes, byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, SupplementalReflectInfo reflectInfo) throws FederateInternalError {
      reflectAttributeValues(theObject, theAttributes);
   }

   @Override
   public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes, byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, LogicalTime theTime, OrderType receivedOrdering, SupplementalReflectInfo reflectInfo) throws FederateInternalError {
      reflectAttributeValues(theObject, theAttributes);
   }

   @Override
   public void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes, byte[] userSuppliedTag, OrderType sentOrdering, TransportationTypeHandle theTransport, LogicalTime theTime, OrderType receivedOrdering, MessageRetractionHandle retractionHandle, SupplementalReflectInfo reflectInfo) throws FederateInternalError {
      reflectAttributeValues(theObject, theAttributes);
   }

   private void reflectAttributeValues(ObjectInstanceHandle theObject, AttributeHandleValueMap theAttributes) {
      String id;
      synchronized (_localCars) {
         id = _localCars.translate(theObject);
      }
      if (id != null) {
         if (theAttributes.containsKey(_nameAttributeHandle)) {
            try {
               String name = _callbackUnicodeStringCoder.decode(theAttributes.get(_nameAttributeHandle));
               for (CarListener listener : _carListeners) {
                  listener.carNameUpdated(id, name);
               }
            } catch (DecoderException e) {
               System.out.println("Failed to decode Name attribute");
               e.printStackTrace();
            }
         }
         if (theAttributes.containsKey(_licensePlateNumberAttributeHandle)) {
            try {
               String licensePlateNumber = _callbackUnicodeStringCoder.decode(theAttributes.get(_licensePlateNumberAttributeHandle));
               for (CarListener listener : _carListeners) {
                  listener.carLicensePlateNumberUpdated(id, licensePlateNumber);
               }
            } catch (DecoderException e) {
               System.out.println("Failed to decode LicensePlateNumber attribute");
               e.printStackTrace();
            }
         }
         if (theAttributes.containsKey(_fuelTypeAttributeHandle)) {
            try {
               FuelType fuelType = _callbackFuelTypeEnum8Coder.decode(theAttributes.get(_fuelTypeAttributeHandle));
               for (CarListener listener : _carListeners) {
                  listener.carFuelTypeUpdated(id, fuelType);
               }
            } catch (DecoderException e) {
               System.out.println("Failed to decode FuelType attribute");
               e.printStackTrace();
            }
         }
         if (theAttributes.containsKey(_attributePositionHandle)) {
            try {
               Position rec = _callbackPositionRecordCoder.decode(theAttributes.get(_attributePositionHandle));
               for (CarListener listener : _carListeners) {
                  listener.carPositionUpdated(id, new Position(rec.getLatitude(), rec.getLongitude()));
               }
            } catch (DecoderException e) {
               System.out.println("Failed to decode Position attribute");
               e.printStackTrace();
            }
         }
         if (theAttributes.containsKey(_attributeFuelLevelHandle)) {
            try {
               int fuelLevel = _callbackFuelInt32Coder.decode(theAttributes.get(_attributeFuelLevelHandle));
               for (CarListener listener : _carListeners) {
                  listener.carFuelLevelUpdated(id, fuelLevel);
               }
            } catch (DecoderException e) {
               System.out.println("Failed to decode FuelLevel attribute");
               e.printStackTrace();
            }
         }
      } else {
         System.out.println("Received reflect for unknown car");
      }
   }

   @Override
   public void removeObjectInstance(ObjectInstanceHandle theObject, byte[] userSuppliedTag, OrderType sentOrdering, SupplementalRemoveInfo removeInfo) throws FederateInternalError {
      removeObjectInstance(theObject);
   }

   @Override
   public void removeObjectInstance(ObjectInstanceHandle theObject, byte[] userSuppliedTag, OrderType sentOrdering, LogicalTime theTime, OrderType receivedOrdering, SupplementalRemoveInfo removeInfo) throws FederateInternalError {
      removeObjectInstance(theObject);
   }

   @Override
   public void removeObjectInstance(ObjectInstanceHandle theObject, byte[] userSuppliedTag, OrderType sentOrdering, LogicalTime theTime, OrderType receivedOrdering, MessageRetractionHandle retractionHandle, SupplementalRemoveInfo removeInfo) throws FederateInternalError {
      removeObjectInstance(theObject);
   }

   private void removeObjectInstance(ObjectInstanceHandle theObject) {
      String id = null;
      synchronized (_localCars) {
         if (_localCars.hasCar(theObject)) {
            id = _localCars.translate(theObject);
            _localCars.remove(theObject);
         }
      }
      if (id != null) {
         for (CarListener listener : _carListeners) {
            listener.carRemoved(id);
         }
      }
   }
}
