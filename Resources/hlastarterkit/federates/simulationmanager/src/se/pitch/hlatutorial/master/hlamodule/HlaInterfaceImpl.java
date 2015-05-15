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
package se.pitch.hlatutorial.master.hlamodule;

import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.exceptions.*;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


class HlaInterfaceImpl extends NullFederateAmbassador implements HlaInterface {
   private RTIambassador _ambassador;

   private ParameterHandleValueMapFactory _parameterFactory;

   private String _federationName;

   private HLAunicodeStringCoder _unicodeStringCoder;
   private HLAunicodeStringCoder _callbackUnicodeStringCoder;
   private FuelInt32Coder _fuelInt32Coder;
   private TimeScaleFactorFloat32Coder _timeScaleFactorFloat32Coder;

   private final List<InteractionListener> _listeners = new CopyOnWriteArrayList<InteractionListener>();

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

   public HlaInterfaceImpl() {
   }

   public void start(String localSettingsDesignator, String fomPath, String federationName, String federateName) throws FederateNotExecutionMember, RestoreInProgress, SaveInProgress, NotConnected, FederateServiceInvocationsAreBeingReportedViaMOM, RTIinternalError, ConnectionFailed, InvalidLocalSettingsDesignator, ErrorReadingFDD, CouldNotOpenFDD, InconsistentFDD {
      _federationName = federationName;

      RtiFactory rtiFactory = RtiFactoryFactory.getRtiFactory();
      _ambassador = rtiFactory.getRtiAmbassador();

      EncoderFactory encoderFactory = rtiFactory.getEncoderFactory();
      _unicodeStringCoder = new HLAunicodeStringCoder(encoderFactory);
      _callbackUnicodeStringCoder = new HLAunicodeStringCoder(encoderFactory);
      _fuelInt32Coder = new FuelInt32Coder(encoderFactory);
      _timeScaleFactorFloat32Coder = new TimeScaleFactorFloat32Coder(encoderFactory);

      try {
         _ambassador.connect(this, CallbackModel.HLA_IMMEDIATE, localSettingsDesignator);
      } catch (AlreadyConnected ignored) {
      } catch (UnsupportedCallbackModel e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (CallNotAllowedFromWithinCallback e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }

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
               _ambassador.joinFederationExecution(federateName + federateNameSuffix, "SimulationManager", federationName, new URL[] {url});
               joined = true;
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

      _parameterFactory = _ambassador.getParameterHandleValueMapFactory();

      try {
         getHandles();

         subscribeInteractions();
         publishInteractions();
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
         }
      } catch (NotConnected ignored) {
      } catch (CallNotAllowedFromWithinCallback e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   private void getHandles() throws RTIinternalError, FederateNotExecutionMember, NotConnected {
      try {
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
      }
   }

   private void subscribeInteractions() throws FederateNotExecutionMember, RestoreInProgress, SaveInProgress, NotConnected, RTIinternalError, FederateServiceInvocationsAreBeingReportedViaMOM {
      try {
         _ambassador.subscribeInteractionClass(_scenarioLoadedInteractionClassHandle);
         _ambassador.subscribeInteractionClass(_scenarioLoadFailureInteractionClassHandle);
      } catch (InteractionClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   private void publishInteractions() throws FederateNotExecutionMember, RestoreInProgress, SaveInProgress, NotConnected, RTIinternalError {
      try {
         _ambassador.publishInteractionClass(_startInteractionClassHandle);
         _ambassador.publishInteractionClass(_stopInteractionClassHandle);
         _ambassador.publishInteractionClass(_loadScenarioInteractionClassHandle);
      } catch (InteractionClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
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

   public void sendLoadScenario(String scenarioName, int initialFuelAmount) throws FederateNotExecutionMember, NotConnected, RestoreInProgress, SaveInProgress, RTIinternalError {
      try {
         ParameterHandleValueMap parameters = _parameterFactory.create(2);
         parameters.put(_scenarioNameParameterHandle, _unicodeStringCoder.encode(scenarioName));
         parameters.put(_initialFuelAmountParameterHandle, _fuelInt32Coder.encode(initialFuelAmount));
         _ambassador.sendInteraction(_loadScenarioInteractionClassHandle, parameters, null);
      } catch (InteractionClassNotPublished e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InteractionParameterNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InteractionClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   public void sendStart(float timeScaleFactor) throws FederateNotExecutionMember, NotConnected, RestoreInProgress, SaveInProgress, RTIinternalError {
      try {
         ParameterHandleValueMap parameters = _parameterFactory.create(1);
         parameters.put(_startTimeScaleFactorParameterHandle, _timeScaleFactorFloat32Coder.encode(timeScaleFactor));
         _ambassador.sendInteraction(_startInteractionClassHandle, parameters, null);
      } catch (InteractionClassNotPublished e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InteractionParameterNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      } catch (InteractionClassNotDefined e) {
         throw new RTIinternalError("HlaInterfaceFailure", e);
      }
   }

   public void sendStop() throws FederateNotExecutionMember, NotConnected, RestoreInProgress, SaveInProgress, RTIinternalError {
      try {
         _ambassador.sendInteraction(_stopInteractionClassHandle, _parameterFactory.create(0), null);
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
      if (interactionClass.equals(_scenarioLoadedInteractionClassHandle)) {
         String federateName;
         try {
            federateName = _callbackUnicodeStringCoder.decode(theParameters.get(_scenarioLoadedFederateNameParameterHandle));
         } catch (DecoderException e) {
            System.out.println("Failed to decode parameter FederateName in ScenarioLoaded interaction");
            e.printStackTrace();
            return;
         }
         for (InteractionListener listener : _listeners) {
            listener.receivedScenarioLoadedInteraction(federateName);
         }
      } else if (interactionClass.equals(_scenarioLoadFailureInteractionClassHandle)) {
         String federateName;
         try {
            federateName = _callbackUnicodeStringCoder.decode(theParameters.get(_scenarioLoadFailureFederateNameParameterHandle));
         } catch (DecoderException e) {
            System.out.println("Failed to decode parameter FederateName in ScenarioLoadFailure interaction");
            e.printStackTrace();
            return;
         }
         String errorMessage;
         try {
            errorMessage = _callbackUnicodeStringCoder.decode(theParameters.get(_scenarioLoadFailureErrorMessageParameterHandle));
         } catch (DecoderException e) {
            System.out.println("Failed to decode parameter ErrorMessage in ScenarioLoadFailure interaction");
            e.printStackTrace();
            return;
         }
         for (InteractionListener listener : _listeners) {
            listener.receivedScenarioLoadFailureInteraction(federateName, errorMessage);
         }
      }
   }
}
