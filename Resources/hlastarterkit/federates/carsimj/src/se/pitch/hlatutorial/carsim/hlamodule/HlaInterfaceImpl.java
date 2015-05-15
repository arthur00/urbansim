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
package se.pitch.hlatutorial.carsim.hlamodule;

import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.*;
import se.pitch.hlatutorial.carsim.Controller;
import se.pitch.hlatutorial.carsim.model.Car;
import se.pitch.hlatutorial.carsim.model.Position;

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

   private FuelInt32Coder _fuelInt32Coder;
   private FuelInt32Coder _callbackFuelInt32Coder;
   private FuelTypeEnum8Coder _fuelTypeEnum8Coder;
   private FuelTypeEnum8Coder _callbackFuelTypeEnum8Coder;
   private PositionRecordCoder _positionRecordCoder;
   private PositionRecordCoder _callbackPositionRecordCoder;
   private HLAunicodeStringCoder _unicodeStringCoder;
   private HLAunicodeStringCoder _callbackUnicodeStringCoder;
   private TimeScaleFactorFloat32Coder _timeScaleFactorFloat32Coder;

   private InteractionClassHandle _startInteractionClassHandle;
   private ParameterHandle _startTimeScaleFactorParameterHandle;
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
   private final Controller _controller;


   public HlaInterfaceImpl(Controller controller) {
      _controller = controller;
   }

   public void addInteractionListener(InteractionListener listener) {
      _listeners.add(listener);
   }

   public void removeInteractionListener(InteractionListener listener) {
      _listeners.add(listener);
   }

   public void start(String localSettingsDesignator, String fomPath, String federationName, String federateName) throws RestoreInProgress, SaveInProgress, NotConnected, FederateServiceInvocationsAreBeingReportedViaMOM, RTIinternalError, ConnectionFailed, InvalidLocalSettingsDesignator, ErrorReadingFDD, CouldNotOpenFDD, InconsistentFDD {
      RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
      _ambassador = rtiFactory.getRtiAmbassador();

      EncoderFactory encoderFactory = rtiFactory.getEncoderFactory();
      _fuelInt32Coder = new FuelInt32Coder(encoderFactory);
      _callbackFuelInt32Coder = new FuelInt32Coder(encoderFactory);
      _fuelTypeEnum8Coder = new FuelTypeEnum8Coder(encoderFactory);
      _callbackFuelTypeEnum8Coder = new FuelTypeEnum8Coder(encoderFactory);
      _positionRecordCoder = new PositionRecordCoder(encoderFactory);
      _callbackPositionRecordCoder = new PositionRecordCoder(encoderFactory);
      _unicodeStringCoder = new HLAunicodeStringCoder(encoderFactory);
      _callbackUnicodeStringCoder = new HLAunicodeStringCoder(encoderFactory);
      _timeScaleFactorFloat32Coder = new TimeScaleFactorFloat32Coder(encoderFactory);

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
               _ambassador.joinFederationExecution(federateName + federateNameSuffix, "CarSimJ", federationName, new URL[] {url});
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

         publishCar();
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
         _startTimeScaleFactorParameterHandle = _ambassador.getParameterHandle(_startInteractionClassHandle, "TimeScaleFactor");
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

   private void publishCar() throws FederateNotExecutionMember, NotConnected, RestoreInProgress, SaveInProgress, RTIinternalError {
      try {
         AttributeHandleSet carAttributes = _ambassador.getAttributeHandleSetFactory().create();
         carAttributes.add(_attributePositionHandle);
         carAttributes.add(_attributeFuelLevelHandle);
         carAttributes.add(_nameAttributeHandle);
         carAttributes.add(_licensePlateNumberAttributeHandle);
         carAttributes.add(_fuelTypeAttributeHandle);

         _ambassador.publishObjectClassAttributes(_objectClassCarHandle, carAttributes);
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
         float timeScaleFactor;
         try {
            timeScaleFactor = _timeScaleFactorFloat32Coder.decode(theParameters.get(_startTimeScaleFactorParameterHandle));
         } catch (DecoderException e) {
            System.out.println("Could not decode parameter TimeScaleFactor in Start interaction");
            e.printStackTrace();
            return;
         }
         for (InteractionListener listener : _listeners) {
            listener.receivedStart(timeScaleFactor);
         }
      } else if (interactionClass.equals(_stopInteractionClassHandle)) {
         for (InteractionListener listener : _listeners) {
            listener.receivedStop();
         }
      } else if (interactionClass.equals(_loadScenarioInteractionClassHandle)) {
         String scenarioName;
         int initialFuelAmount;
         try {
            scenarioName = _callbackUnicodeStringCoder.decode(theParameters.get(_scenarioNameParameterHandle));
            initialFuelAmount = _callbackFuelInt32Coder.decode(theParameters.get(_initialFuelAmountParameterHandle));
         } catch (DecoderException e) {
            System.out.println("Could not decode parameters in LoadScenario interaction ");
            e.printStackTrace();
            return;
         }
         for (InteractionListener listener : _listeners) {
            listener.receivedSetScenario(scenarioName, initialFuelAmount);
         }
      }
   }


   /*
    * Objects
    */

   public void createCar(Car car) throws FederateNotExecutionMember, RestoreInProgress, SaveInProgress, NotConnected, RTIinternalError {
      try {
         ObjectInstanceHandle carHandle = _ambassador.registerObjectInstance(_objectClassCarHandle);
         _localCars.put(carHandle, car.getIdentifier());
         AttributeHandleValueMap attributeMap = _ambassador.getAttributeHandleValueMapFactory().create(3);
         attributeMap.put(_nameAttributeHandle, _unicodeStringCoder.encode(car.getName()));
         attributeMap.put(_licensePlateNumberAttributeHandle, _unicodeStringCoder.encode(car.getLicensePlateNumber()));
         attributeMap.put(_fuelTypeAttributeHandle, _fuelTypeEnum8Coder.encode(car.getFuelType()));
         _ambassador.updateAttributeValues(carHandle, attributeMap, null);
      } catch (ObjectClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (ObjectInstanceNotKnown e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (AttributeNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (AttributeNotOwned e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (ObjectClassNotPublished e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   public void removeCar(Car car) throws FederateNotExecutionMember, RestoreInProgress, SaveInProgress, NotConnected, RTIinternalError {
      try {
         _ambassador.deleteObjectInstance(_localCars.translate(car.getIdentifier()), null);
      } catch (DeletePrivilegeNotHeld e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (ObjectInstanceNotKnown e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
      _localCars.remove(car.getIdentifier());
   }

   public void updateCar(Car car) throws FederateNotExecutionMember, NotConnected, RestoreInProgress, SaveInProgress, RTIinternalError {
      try {
         AttributeHandleValueMap attributeMap = _ambassador.getAttributeHandleValueMapFactory().create(2);
         attributeMap.put(_attributePositionHandle, _positionRecordCoder.encode(car.getPosition()));
         attributeMap.put(_attributeFuelLevelHandle, _fuelInt32Coder.encode((int)Math.round(car.getFuelLevel())));
         _ambassador.updateAttributeValues(_localCars.translate(car.getIdentifier()), attributeMap, null);
      } catch (AttributeNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (ObjectInstanceNotKnown e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (AttributeNotOwned e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   @Override
   public void provideAttributeValueUpdate(ObjectInstanceHandle theObject, AttributeHandleSet theAttributes, byte[] userSuppliedTag) throws FederateInternalError {
      String id = _localCars.translate(theObject);
      if (id != null) {
         Car car = _controller.getCar(id);
         if (car != null) {
            try {
               AttributeHandleValueMap attributeMap = _ambassador.getAttributeHandleValueMapFactory().create(5);

               attributeMap.put(_nameAttributeHandle, _callbackUnicodeStringCoder.encode(car.getName()));
               attributeMap.put(_licensePlateNumberAttributeHandle, _callbackUnicodeStringCoder.encode(car.getLicensePlateNumber()));
               attributeMap.put(_fuelTypeAttributeHandle, _callbackFuelTypeEnum8Coder.encode(car.getFuelType()));
               Position pos = car.getPosition();
               if (pos != null) {
                  attributeMap.put(_attributePositionHandle, _callbackPositionRecordCoder.encode(pos));
                  attributeMap.put(_attributeFuelLevelHandle, _callbackFuelInt32Coder.encode((int)Math.round(car.getFuelLevel())));
               }
               _ambassador.updateAttributeValues(theObject, attributeMap, null);
            } catch (RTIexception e) {
               System.out.println("Could not provide car update ");
               e.printStackTrace();
            }
         }
      }
   }
}
