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
#include <iostream>

#include <RTI/RTIambassadorFactory.h>

#include "HLAmodule.h"
#include "StringUtils.h"
#include "SimulatorConfig.h"

using namespace std;
using namespace rti1516e;

HLAmodule::HLAmodule(const SimulatorConfig& config) 
    : _connected(false),
    _localSettingsDesignator(config.getLocalSettingsDesignator()),
    _federateName           (config.getFederateName()),
    _federationName         (config.getFederationName()),
    _FOM                    (config.getFOM())
{ /* void */ }

HLAmodule::~HLAmodule() throw() { disconnect(); }

// Connect to the Federation and publish and subscribe to all objects and interactions.
// The services used are:
//  - Federation management
//  - Declaration management
//  - Object management
void HLAmodule::connect() throw (rti1516e::Exception)
{
    boost::unique_lock<boost::mutex> lock(_connectionMutex);

    // Get an RTI ambassador
    try {
        boost::shared_ptr<RTIambassadorFactory> rtiAmbassadorFactory(new RTIambassadorFactory());
        _rtiAmbassador = rtiAmbassadorFactory->createRTIambassador();
    } catch (const rti1516e::Exception&) { 
        throw; 
    }

    vector<wstring> FOMmoduleUrls;
    FOMmoduleUrls.push_back(_FOM);
    
    // Connect to the RTI
    try {
        _rtiAmbassador->connect(*this, HLA_IMMEDIATE, _localSettingsDesignator);
    } catch (const rti1516e::Exception&) { 
        throw; 
    }

    // Always try and destroy the federation execution.
    // There might be an old federation execution running with no federates
    // so we do this to start with a clean slate. 
    // We do this because we have no special federate that has the responsibility
    // to create the federation execution. Make sure to catch the exception!
     try {
         _rtiAmbassador->destroyFederationExecution(_federationName);

         // we don't have to handle these exceptions
     } catch (const rti1516e::FederatesCurrentlyJoined&) {
     } catch (const rti1516e::FederationExecutionDoesNotExist&) {
     } catch (const rti1516e::RTIinternalError&) {
     }

    // Always try and create the federation execution.
    // We do this because we have no special federate that has the responsibility
    // to create the federation execution. Make sure to catch the exception!
     try {
          _rtiAmbassador->createFederationExecutionWithMIM(_federationName, FOMmoduleUrls, L"", L"");
     } catch (const FederationExecutionAlreadyExists&) {
         // the federation execution already exist so ignore
     } catch (const rti1516e::Exception&) { 
         throw;
     } 
        

     // Join the federation. If the federate name is taken add a sequence 
     // number to it to make it unique and try again. 
     bool joined = false;
     int sequenceNumber = 1;
     wstring uniqueName = _federateName;

     while (!joined) {
         try {
             FederateHandle federateHandle = _rtiAmbassador->joinFederationExecution( 
                 uniqueName, 
                 L"Car simulator",
                 _federationName,
                 FOMmoduleUrls
            );

             joined = true;
             _federateName = uniqueName;

         } catch (const rti1516e::FederateNameAlreadyInUse&) {
             uniqueName = _federateName + L"-" + StringUtils::toWString(sequenceNumber);
             ++sequenceNumber;
         } 
     }

     // Declare the intent to publish and subscribe to objects, attributes
     // and interactions.
     try {
        getHandles();
        publishCar();
        publishInteractions();
        subscribeInteractions();
        _connected = true;
     } catch (const rti1516e::Exception&) { 
         throw; 
     }
}

void HLAmodule::disconnect()
{
    boost::unique_lock<boost::mutex> lock(_connectionMutex);

    if (_connected) 
    { 
        // When we disconnect from the federation execution we want to: 
        //  - Cancel any pending ownership acquisitions
        //  - Delete the objects created by us
        //  - Divest any owned attributes
        try {
            _rtiAmbassador->resignFederationExecution(CANCEL_THEN_DELETE_THEN_DIVEST);

            // we don't have to handle these exceptions
        } catch (const rti1516e::InvalidResignAction&) {
        } catch (const rti1516e::OwnershipAcquisitionPending&) {
        } catch (const rti1516e::FederateOwnsAttributes&) {
        } catch (const rti1516e::FederateNotExecutionMember&) {
        } catch (const rti1516e::NotConnected&) {
        } catch (const rti1516e::CallNotAllowedFromWithinCallback&) {
        } catch (const rti1516e::RTIinternalError&) {
        }
        
        // Just as we try and create the federation execution we will try and 
        // destroy it since we cannot know if we're the last one to resign.
        try {
            _rtiAmbassador->destroyFederationExecution(_federationName);

            // we don't have to handle these exceptions
        } catch (const rti1516e::FederatesCurrentlyJoined&) {
        } catch (const rti1516e::FederationExecutionDoesNotExist&) {
        } catch (const rti1516e::NotConnected&) {
        } catch (const rti1516e::RTIinternalError&) {
        }

        // Finally disconnect from the federation
        try {
            _rtiAmbassador->disconnect();

            // we don't have to handle these exceptions
        } catch (const rti1516e::FederateIsExecutionMember&) {
        } catch (const rti1516e::CallNotAllowedFromWithinCallback&) {
        } catch (const rti1516e::RTIinternalError&) {
        }
        
        _connected = false;
    }
}

void HLAmodule::addListener(InteractionListener* listener)
{
    boost::unique_lock<boost::mutex> lock(_listenersMutex);
    _interactionListeners.insert(listener);
}

void HLAmodule::removeListener(InteractionListener* listener)
{
    boost::unique_lock<boost::mutex> lock(_listenersMutex);
    _interactionListeners.erase(listener);
}

wstring HLAmodule::getFederateName() const
{
    return _federateName;
}

// Adding a car involves:
// - Registering the object using the registerObjectInstance method
// - Updating the newly registered car's attributes using the 
//   updateAttributeValues method
void HLAmodule::addCar(const Car& car) throw (rti1516e::Exception)
{
        // May throw rti1516e::Exception, won't leave data structures in inconsistent state
        ObjectInstanceHandle carHandle = _rtiAmbassador->registerObjectInstance(_objectClassCarHandle);
        AttributeHandleValueMap attributeMap;
    {
        boost::unique_lock<boost::mutex> lock(_carMutex);

        // Map car id to the object instance handle and cache the car's state
        int id = car.getID();
        _localCars.insert(HLAcar(id, carHandle));
        HLAcarState state(car);
        _carStateCache[id] = state;

        _hlaCarNameEncoder.set(car.getName());	 
        attributeMap[_nameAttributeHandle] = _hlaCarNameEncoder.encode(); 

        _hlaCarLicensePlateEncoder.set(car.getLicensePlate());
        attributeMap[_licensePlateAttributeHandle] = _hlaCarLicensePlateEncoder.encode(); 
        attributeMap[_fuelTypeAttributeHandle]     = _hlaCarFuelTypeEncoder.encode(car.getFuelType());
    }

    // Updating attribute values may throw an exception but we don't have to handle that.
    // We assume that the values will get updated soon enough through updated Car state or
    // by getting a request for values from another federate.
    try {
        _rtiAmbassador->updateAttributeValues(carHandle, attributeMap, VariableLengthData());
    } catch (const rti1516e::Exception&) {
        // Internal data structures are still valid so no clean up is performed
    }

}

void HLAmodule::addCars(const CarPtrMap& cars) throw (rti1516e::Exception)
{
    CarPtrMap::const_iterator car_iter(cars.begin()); 
    for (; car_iter != cars.end(); ++car_iter) {
        addCar(*(car_iter->second));
    }
}

// When removing a Car object we have to make sure it's removed from the
// federation aswell.
void HLAmodule::removeCar(int id) throw (rti1516e::Exception, std::out_of_range)
{
    ObjectInstanceHandle carHandle;
    {
        boost::unique_lock<boost::mutex> lock(_carMutex);
        carHandle = _localCars.left.at(id);
        _localCars.left.erase(id);
    }
    _rtiAmbassador->deleteObjectInstance(carHandle, VariableLengthData());
}

// Updating a car involves calling updateAttributeValues for the given car object using
// the RTI Ambassador.
void HLAmodule::updateCar(int id, const Car& car) throw (rti1516e::EncoderException)
{
    AttributeHandleValueMap attributeMap;
    ObjectInstanceHandle carInstanceHandle;

    {
        boost::unique_lock<boost::mutex> lock(_carMutex);
        _carStateCache[id] = HLAcarState(car);
        carInstanceHandle = _localCars.left.at(id);
    }

    // position
    attributeMap[_positionAttributeHandle] = _hlaCarPositionEncoder.encode(car.getPosition());

    // fuel level
    _hlaCarFuelLevelEncoder.set(static_cast<int>(car.getFuelLevel()));
    attributeMap[_fuelLevelAttributeHandle] = _hlaCarFuelLevelEncoder.encode(); // EncoderException

    // If the update fails we assume that the next update will succeed
    try {
        _rtiAmbassador->updateAttributeValues(carInstanceHandle, attributeMap, VariableLengthData());
    } catch (const rti1516e::Exception&) {
        // Internal data structures are still valid so no clean up is performed
    }
}

//
// Get the handles for objects and interactions.
// This method is not thread safe but should only be called once during initialization.
//
void HLAmodule::getHandles() 
     throw (
        FederateNotExecutionMember, 
        NotConnected,
        RTIinternalError, 
        NameNotFound, 
        InvalidInteractionClassHandle, 
        InvalidObjectClassHandle) 
{
    // Object class handle
    _objectClassCarHandle = _rtiAmbassador->getObjectClassHandle(L"Car");

    // Attribute handles
    _positionAttributeHandle     = _rtiAmbassador->getAttributeHandle(_objectClassCarHandle, L"Position");
    _fuelLevelAttributeHandle    = _rtiAmbassador->getAttributeHandle(_objectClassCarHandle, L"FuelLevel");
    _nameAttributeHandle         = _rtiAmbassador->getAttributeHandle(_objectClassCarHandle, L"Name");
    _licensePlateAttributeHandle = _rtiAmbassador->getAttributeHandle(_objectClassCarHandle, L"LicensePlateNumber");
    _fuelTypeAttributeHandle     = _rtiAmbassador->getAttributeHandle(_objectClassCarHandle, L"FuelType");

    // Interaction class handles Lab D-4 (Sending and receiving interactions)
    _loadScenarioInteractionClassHandle         = _rtiAmbassador->getInteractionClassHandle(L"LoadScenario");
    _scenarioLoadFailureInteractionClassHandle  = _rtiAmbassador->getInteractionClassHandle(L"ScenarioLoadFailure");
    _scenarioLoadedInteractionClassHandle       = _rtiAmbassador->getInteractionClassHandle(L"ScenarioLoaded");
    _startInteractionClassHandle                = _rtiAmbassador->getInteractionClassHandle(L"Start");
    _stopInteractionClassHandle                 = _rtiAmbassador->getInteractionClassHandle(L"Stop");

    // Interaction parameter handles
    _scenarionNameParameterHandle     = _rtiAmbassador->getParameterHandle(_loadScenarioInteractionClassHandle,        L"ScenarioName");
    _initialFuelAmountParameterHandle = _rtiAmbassador->getParameterHandle(_loadScenarioInteractionClassHandle,        L"InitialFuelAmount");
    _timeScaleFactorParameterHandle   = _rtiAmbassador->getParameterHandle(_startInteractionClassHandle,               L"TimeScaleFactor");
    _federateNameParameterHandle      = _rtiAmbassador->getParameterHandle(_scenarioLoadedInteractionClassHandle,      L"FederateName");
    _errorMessageParameterHandle      = _rtiAmbassador->getParameterHandle(_scenarioLoadFailureInteractionClassHandle, L"ErrorMessage");
    _errorFederateNameParameterHandle = _rtiAmbassador->getParameterHandle(_scenarioLoadFailureInteractionClassHandle, L"FederateName");
}

//
// Publish Car object attributes
// This method is not thread safe but should only be called once during initialization!
//
void HLAmodule::publishCar() 
      throw (
         FederateNotExecutionMember, 
         RestoreInProgress, 
         SaveInProgress, 
         NotConnected, 
         RTIinternalError, 
         AttributeNotDefined, 
         ObjectClassNotDefined)
{
    AttributeHandleSet carAttributes;
    carAttributes.insert(_positionAttributeHandle);
    carAttributes.insert(_fuelLevelAttributeHandle);
    carAttributes.insert(_nameAttributeHandle);
    carAttributes.insert(_licensePlateAttributeHandle);
    carAttributes.insert(_fuelTypeAttributeHandle);

    _rtiAmbassador->publishObjectClassAttributes(_objectClassCarHandle, carAttributes); 
}

//
// Subscribe to interactions.
// This method is not thread safe but should only be called once during initialization!
//
void HLAmodule::subscribeInteractions() 
       throw (
       FederateNotExecutionMember, 
       RestoreInProgress,
       SaveInProgress, 
       NotConnected, 
       RTIinternalError, 
       InteractionClassNotDefined, 
       FederateServiceInvocationsAreBeingReportedViaMOM)
{
    _rtiAmbassador->subscribeInteractionClass(_startInteractionClassHandle);
    _rtiAmbassador->subscribeInteractionClass(_stopInteractionClassHandle);
    _rtiAmbassador->subscribeInteractionClass(_loadScenarioInteractionClassHandle);
}

//
// Publish interactions.
// This method is not thread safe but should only be called once during initialization!
//
void HLAmodule::publishInteractions()
    throw (
    FederateNotExecutionMember,
    RestoreInProgress,
    SaveInProgress,
    NotConnected,
    RTIinternalError,
    InteractionClassNotDefined) 
{
    _rtiAmbassador->publishInteractionClass(_scenarioLoadedInteractionClassHandle);
    _rtiAmbassador->publishInteractionClass(_scenarioLoadFailureInteractionClassHandle);
}

void HLAmodule::sendScenarioLoadedInteraction() 
        throw (
           InteractionClassNotPublished,
       InteractionParameterNotDefined,
       InteractionClassNotDefined,
       SaveInProgress,
       RestoreInProgress,
       FederateNotExecutionMember,
       NotConnected,
       RTIinternalError)

{
    ParameterHandleValueMap parameterMap;
    _hlaFederateNameEncoder.set(_federateName);
    parameterMap[_federateNameParameterHandle] = _hlaFederateNameEncoder.encode();
    _rtiAmbassador->sendInteraction(_scenarioLoadedInteractionClassHandle, parameterMap, VariableLengthData());
}

void HLAmodule::sendScenarioLoadFailureInteraction(const wstring& errorMessage)
        throw (
       InteractionClassNotPublished,
       InteractionParameterNotDefined,
       InteractionClassNotDefined,
       SaveInProgress,
       RestoreInProgress,
       FederateNotExecutionMember,
       NotConnected,
       RTIinternalError)
{
    _hlaErrorMessageEncoder.set(errorMessage);
    ParameterHandleValueMap parameterMap;
    parameterMap[_errorFederateNameParameterHandle] = _hlaFederateNameEncoder.encode();
    parameterMap[_errorMessageParameterHandle]      = _hlaErrorMessageEncoder.encode();
    _rtiAmbassador->sendInteraction(_scenarioLoadFailureInteractionClassHandle, parameterMap, VariableLengthData());
}


//////////////////////////////////////////////
// Federate Ambassador interface definition //
//////////////////////////////////////////////

 void HLAmodule::receiveInteraction (
         InteractionClassHandle theInteraction,
         const ParameterHandleValueMap& theParameterValues,
         const VariableLengthData& theUserSuppliedTag,
         OrderType sentOrder,
         TransportationType theType,
         SupplementalReceiveInfo theReceiveInfo)
         throw (FederateInternalError)
{
    receiveInteraction(theInteraction, theParameterValues);
}

void HLAmodule::receiveInteraction (
         InteractionClassHandle theInteraction,
         const ParameterHandleValueMap& theParameterValues,
         const VariableLengthData& theUserSuppliedTag,
         OrderType sentOrder,
         const LogicalTime& theTime,
         OrderType receivedOrder,
         SupplementalReceiveInfo theReceiveInfo)
         throw (FederateInternalError)
{
    receiveInteraction(theInteraction, theParameterValues);
}

void HLAmodule::receiveInteraction (
         InteractionClassHandle theInteraction,
         const ParameterHandleValueMap& theParameterValues,
         const VariableLengthData& theUserSuppliedTag,
         OrderType sentOrder,
         TransportationType theType,
         const LogicalTime& theTime,
         OrderType receivedOrder,
         MessageRetractionHandle theHandle,
         SupplementalReceiveInfo theReceiveInfo)
         throw (FederateInternalError)
{
    receiveInteraction(theInteraction, theParameterValues);
}

void HLAmodule::receiveInteraction(
    InteractionClassHandle theInteraction, 
    const ParameterHandleValueMap& theParameterValues) 
    throw (FederateInternalError)
{
    boost::unique_lock<boost::mutex> lock(_listenersMutex);

    // Check which type of interaction we received

    if (theInteraction == _startInteractionClassHandle) {
        ParameterHandleValueMap::const_iterator timeScaleFactorParam(theParameterValues.find(_timeScaleFactorParameterHandle));

      if (timeScaleFactorParam == theParameterValues.end()) {
            cerr << "HLAmodule::receiveInteraction LoadScenario: All necessary parameters not set" << endl;
        } else {
            _hlaTimeScaleFactorDecoder.decode(timeScaleFactorParam->second);
            double timeScaleFactor = _hlaTimeScaleFactorDecoder.get();

            InteractionListenerSet::const_iterator iter(_interactionListeners.begin());
            for (; iter != _interactionListeners.end(); ++iter) {
                (*iter)->receivedStart(timeScaleFactor);
            }
        }

    } else if (theInteraction == _stopInteractionClassHandle) {
        InteractionListenerSet::const_iterator iter(_interactionListeners.begin());
      for (; iter != _interactionListeners.end(); ++iter) {
            (*iter)->receivedStop();
        }

    } else if (theInteraction == _loadScenarioInteractionClassHandle) {	
        ParameterHandleValueMap::const_iterator scenarioParam(theParameterValues.find(_scenarionNameParameterHandle));
        ParameterHandleValueMap::const_iterator fuelParam(theParameterValues.find(_initialFuelAmountParameterHandle));

      if (fuelParam == theParameterValues.end() || scenarioParam == theParameterValues.end()) {
            wstring errorMessage = L"CarSimC LoadScenario: All necessary parameters not set";
            sendScenarioLoadFailureInteraction(errorMessage);
            wcerr << errorMessage << endl;
            return;
        } else {
            _hlaScenarioNameDecoder.decode(scenarioParam->second);
            _hlaCarFuelLevelDecoder.decode(fuelParam->second);
            wstring name = _hlaScenarioNameDecoder.get();
            double fuelLevel = _hlaCarFuelLevelDecoder.get();

            InteractionListenerSet::const_iterator iter(_interactionListeners.begin());
         for (; iter != _interactionListeners.end(); ++iter) {
                (*iter)->receivedLoadScenario(name, fuelLevel);
            }
        }

    } else {
        cerr << "HLAmodule::receiveInteraction got unknown interaction" << endl;
    }
}

// The RTI may ask us to provide the latest attribute values for a car object.
void HLAmodule::provideAttributeValueUpdate (
    ObjectInstanceHandle theObject, 
    const AttributeHandleSet& theAttributes, 
    const VariableLengthData& theUserSuppliedTag) 
    throw (FederateInternalError)
{
    AttributeHandleValueMap attributeMap;

    {
        boost::unique_lock<boost::mutex> lock(_carMutex);
        HLAcarMap::right_const_iterator car_iter = _localCars.right.find(theObject);

        // This should not happen, it means we lost a reference to the object instance
        if (car_iter == _localCars.right.end()) { 
            wcerr << L"Error in HLAmodule::provideAttributeUpdate: Objecthandle: " 
                  << theObject 
                  << L" not found!" 
                  << endl;
            return; 
        } 

        // Get the last known state of the Car with the given id
        HLAcarState& state = _carStateCache[car_iter->second];

        // Only send values for the requested attributes
        AttributeHandleSet::iterator nameIter(theAttributes.find(_nameAttributeHandle));
        if (nameIter != theAttributes.end()) {
            _hlaCarNameEncoder.set(state.name);
            attributeMap[_nameAttributeHandle] = _hlaCarNameEncoder.encode();
        }

        AttributeHandleSet::iterator positionIter(theAttributes.find(_positionAttributeHandle));
        if (positionIter != theAttributes.end()) {
            attributeMap[_positionAttributeHandle] = _hlaCarPositionEncoder.encode(state.position);
        }

        AttributeHandleSet::iterator fuelLevelIter(theAttributes.find(_fuelLevelAttributeHandle));
        if (fuelLevelIter != theAttributes.end()) {
            _hlaCarFuelLevelEncoder.set(static_cast<int>(state.fuelLevel));
            attributeMap[_fuelLevelAttributeHandle] = _hlaCarFuelLevelEncoder.encode();
        }

        AttributeHandleSet::iterator fuelTypeIter(theAttributes.find(_fuelTypeAttributeHandle));
        if (fuelTypeIter != theAttributes.end()) {
            attributeMap[_fuelTypeAttributeHandle] = _hlaCarFuelTypeEncoder.encode(state.fuelType);
        }

        AttributeHandleSet::iterator licensePlateIter(theAttributes.find(_licensePlateAttributeHandle));
        if (licensePlateIter != theAttributes.end()) {
            _hlaCarLicensePlateEncoder.set(state.licensePlate);
            attributeMap[_licensePlateAttributeHandle] = _hlaCarLicensePlateEncoder.encode();
        }
    } // end lock

    // Send the attribute values
    _rtiAmbassador->updateAttributeValues(theObject, attributeMap, VariableLengthData());
}

