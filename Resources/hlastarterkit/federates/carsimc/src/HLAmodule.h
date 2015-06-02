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
#ifndef HLA_MODULE_H
#define HLA_MODULE_H

#include <map>

#include <RTI/NullFederateAmbassador.h>
#include <RTI/time/HLAinteger64TimeFactory.h>
#include <RTI/time/HLAinteger64Time.h>
#include <RTI/time/HLAinteger64Interval.h>
#include <RTI/encoding/BasicDataElements.h>
#include <RTI/encoding/HLAvariantRecord.h>
#include <RTI/encoding/HLAfixedRecord.h>
#include <RTI/RTIambassador.h>

#include <boost/shared_ptr.hpp>
#include <boost/bimap.hpp>

#include "Car.h"
#include "InteractionListener.h"
#include "SimulatorConfig.h"
#include "HLAposition.h"
#include "HLAfuelType.h"

class HLAmodule;
class HLAcarState;

typedef boost::shared_ptr<HLAmodule> HLAmodulePtr;
typedef boost::bimap<int, rti1516e::ObjectInstanceHandle> HLAcarMap; 
typedef HLAcarMap::value_type HLAcar;
typedef std::map<int, HLAcarState> HLAcarStateMap; 
typedef std::set<InteractionListener*> InteractionListenerSet;

/**
 * An HLA interface module for a Car simulator.
 * 
 * The HLAmodule implements all HLA functionality needed for connecting
 * a Car simulator to a federation. The class communicates with the federation
 * via an RTI ambassador and Federate ambassador.
 *
 * @remarks The class is thread safe through mutex locking.
 * @see SimulatorConfig, Car, InteractionListener
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class HLAmodule : public rti1516e::NullFederateAmbassador {
public:
    explicit HLAmodule(const SimulatorConfig& config);
    virtual ~HLAmodule() throw();

    /**
     * Connect to the RTI and join or create the federation execution.
     */
    virtual void connect() throw (rti1516e::Exception);

    /**
     * Disconnect from the RTI and perform cleanup.
     *
     * Cleanup includes:
     * - Cancel any pending ownership acquisitions
     * - Delete the objects created by us
     * - Divest any owned attributes
     * - Try and destroy the federation execution if this federate is the last
     */
    virtual void disconnect();

    /**
     * Add a Car to the federation.
     *
     * A mapping is created between the Car object in the simulator and
     * the Car object we register with the RTI.
     *
     * @param car - the Car object to add.
     */
    virtual void addCar(const Car& car) throw (rti1516e::Exception);

    /**
     * Add more than one car to the federation.
     *
     * @param cars - a map with shared pointers to Car objects.
     */
    virtual void addCars(const CarPtrMap& cars) throw (rti1516e::Exception);

    /**
     * Remove a car from the federation.
     *
     * The mapping performed in addCar is removed.
     *
     * @param id - the identifier of the Car.
     */
    virtual void removeCar(int id) throw (rti1516e::Exception, std::out_of_range);

    /**
     * Update the attribute values of a Car.
     *
     * @param id    - the identifier of the Car.
     * @param state - the Car with the new state.
     */
    virtual void updateCar(int id, const Car& car) throw (rti1516e::EncoderException);
    
    /**
     * Add an interaction listener.
     *
     * @param listener - pointer to an InteractionListener object.
     */
    virtual void addListener(InteractionListener* listener);
    
    /**
     * Remove an interaction listener.
     *
     * @param listener - pointer to an InteractionListener object.
     */
    virtual void removeListener(InteractionListener* listener);

    /**
     * Get the name of the federate created by this module.
     * @return the name of the created federate
     */
    std::wstring getFederateName() const;

    /**
     * Send a ScenarioLoaded interaction to the federation.
     */
    virtual void sendScenarioLoadedInteraction() 
        throw (
        rti1516e::InteractionClassNotPublished,
        rti1516e::InteractionParameterNotDefined,
        rti1516e::InteractionClassNotDefined,
        rti1516e::SaveInProgress,
        rti1516e::RestoreInProgress,
        rti1516e::FederateNotExecutionMember,
        rti1516e::NotConnected,
        rti1516e::RTIinternalError);

    /**
     * Send a ScenarioLoadFailureInteraction to the federation.
     *
     * @param errorMessage - the message to use as the interaction parameter.
     */
    virtual void sendScenarioLoadFailureInteraction(const std::wstring& errorMessage) 
        throw (
        rti1516e::InteractionClassNotPublished,
        rti1516e::InteractionParameterNotDefined,
        rti1516e::InteractionClassNotDefined,
        rti1516e::SaveInProgress,
        rti1516e::RestoreInProgress,
        rti1516e::FederateNotExecutionMember,
        rti1516e::NotConnected,
        rti1516e::RTIinternalError);
  
    // FederateAmbassador interface
    virtual void receiveInteraction(rti1516e::InteractionClassHandle theInteraction,
                                    const rti1516e::ParameterHandleValueMap& theParameterValues,
                                    const rti1516e::VariableLengthData& theUserSuppliedTag,
                                    rti1516e::OrderType sentOrder,
                                    rti1516e::TransportationType theType,
                                    rti1516e::SupplementalReceiveInfo theReceiveInfo)
        throw (
        rti1516e::FederateInternalError);

    virtual void receiveInteraction(rti1516e::InteractionClassHandle theInteraction,
                                    const rti1516e::ParameterHandleValueMap& theParameterValues,
                                    const rti1516e::VariableLengthData& theUserSuppliedTag,
                                    rti1516e::OrderType sentOrder,
                                    const rti1516e::LogicalTime& theTime,
                                    rti1516e::OrderType receivedOrder,
                                    rti1516e::SupplementalReceiveInfo theReceiveInfo)
        throw (
        rti1516e::FederateInternalError);

    virtual void receiveInteraction(rti1516e::InteractionClassHandle theInteraction,
                                    const rti1516e::ParameterHandleValueMap& theParameterValues,
                                    const rti1516e::VariableLengthData& theUserSuppliedTag,
                                    rti1516e::OrderType sentOrder,
                                    rti1516e::TransportationType theType,
                                    const rti1516e::LogicalTime& theTime,
                                    rti1516e::OrderType receivedOrder,
                                    rti1516e::MessageRetractionHandle theHandle,
                                    rti1516e::SupplementalReceiveInfo theReceiveInfo)
         throw (
         rti1516e::FederateInternalError);

    virtual void provideAttributeValueUpdate(rti1516e::ObjectInstanceHandle theObject, 
                                             const rti1516e::AttributeHandleSet& theAttributes, 
                                             const rti1516e::VariableLengthData& theUserSuppliedTag) 
        throw (
        rti1516e::FederateInternalError);

private:

    /**
     * Get the handles for all objects and interactions this federate will use.
     *
     * Before any Interactions or Objects and their attributes can be used, the
     * federate must have their HLA handles. The handles are received from the 
     * RTI.
     * This method should only be used once during initialization and is NOT
     * thread safe!
     */
    void getHandles() 
        throw (
        rti1516e::FederateNotExecutionMember, 
        rti1516e::NotConnected, 
        rti1516e::RTIinternalError, 
        rti1516e::NameNotFound, 
        rti1516e::InvalidInteractionClassHandle, 
        rti1516e::InvalidObjectClassHandle);

    /**
     * Publish the Car attributes.
     *
     * Before any attributes updates can be performed the federate must tell
     * the RTI that it will send attribute values to the federation.
     * This method should only be used once during initialization and is NOT
     * thread safe!
     */
    void publishCar() 
        throw (
        rti1516e::FederateNotExecutionMember, 
        rti1516e::RestoreInProgress, 
        rti1516e::SaveInProgress, 
        rti1516e::NotConnected, 
        rti1516e::RTIinternalError, 
        rti1516e::AttributeNotDefined, 
        rti1516e::ObjectClassNotDefined);

    /**
     * Subscribe to Interaction classes.
     *
     * Before any interactions can be received the federate must subscribe
     * to them.
     * This method should only be used once during initialization and is NOT
     * thread safe!
     */
    void subscribeInteractions() 
        throw (
        rti1516e::FederateNotExecutionMember, 
        rti1516e::RestoreInProgress, 
        rti1516e::SaveInProgress, 
        rti1516e::NotConnected, 
        rti1516e::RTIinternalError, 
        rti1516e::InteractionClassNotDefined, 
        rti1516e::FederateServiceInvocationsAreBeingReportedViaMOM);

    /**
     * Publish the Interaction classes.
     *
     * Before any interactions can be sent the federate must tell
     * the RTI what interactions it will send to the federation.
     * This method should only be used once during initialization and is NOT
     * thread safe!
     */
    void publishInteractions() 
        throw (
        rti1516e::FederateNotExecutionMember, 
        rti1516e::RestoreInProgress, 
        rti1516e::SaveInProgress, 
        rti1516e::NotConnected, 
        rti1516e::RTIinternalError, 
        rti1516e::InteractionClassNotDefined);

    /**
     * Receive an Interaction.
     *
     * The method belongs to the FederateAmbassador interface.
     * This class inherits several overloaded receiveInteraction methods
     * but they are all dispatch to this implementation.
     */
    void receiveInteraction(rti1516e::InteractionClassHandle theInteraction, 
                            const rti1516e::ParameterHandleValueMap& theParameterValues) 
        throw (rti1516e::FederateInternalError);

private:
    std::auto_ptr<rti1516e::RTIambassador> _rtiAmbassador;
    InteractionListenerSet _interactionListeners; // guarded by _listenersMutex;
    bool _connected; // guarded by _connectionMutex

    HLAcarMap      _localCars;     // guarded by _carMutex
    HLAcarStateMap _carStateCache; // guarded by _carMutex

    // RTI connection info
    const std::wstring _localSettingsDesignator;
    std::wstring       _federateName; // may only be altered during setup
    const std::wstring _federationName;
    const std::wstring _FOM;

    // Encoders/Decoders shall not be accessed by multiple threads simultaneously
    rti1516e::HLAunicodeString _hlaCarNameEncoder;
    rti1516e::HLAunicodeString _hlaFederateNameEncoder;
    rti1516e::HLAunicodeString _hlaScenarioNameEncoder;
    rti1516e::HLAunicodeString _hlaScenarioNameDecoder;
    rti1516e::HLAunicodeString _hlaErrorMessageEncoder;
    rti1516e::HLAunicodeString _hlaCarLicensePlateEncoder;
    rti1516e::HLAfloat32BE     _hlaTimeScaleFactorEncoder;
    rti1516e::HLAfloat32BE     _hlaTimeScaleFactorDecoder;
    rti1516e::HLAinteger32BE   _hlaCarFuelLevelEncoder;
    rti1516e::HLAinteger32BE   _hlaCarFuelLevelDecoder;
    HLAposition                _hlaCarPositionEncoder;
    HLAfuelType                _hlaCarFuelTypeEncoder;

    // Handles are set once during initialization 
    // and shall not be changed after that
    rti1516e::InteractionClassHandle _startInteractionClassHandle;
    rti1516e::InteractionClassHandle _stopInteractionClassHandle;
    rti1516e::InteractionClassHandle _loadScenarioInteractionClassHandle;
    rti1516e::ParameterHandle        _scenarionNameParameterHandle;
    rti1516e::ParameterHandle        _initialFuelAmountParameterHandle;
    rti1516e::ParameterHandle        _federateNameParameterHandle;
    rti1516e::ParameterHandle        _timeScaleFactorParameterHandle;
    rti1516e::ParameterHandle        _errorMessageParameterHandle;
    rti1516e::ParameterHandle        _errorFederateNameParameterHandle;
    rti1516e::InteractionClassHandle _scenarioLoadedInteractionClassHandle;
    rti1516e::InteractionClassHandle _scenarioLoadFailureInteractionClassHandle;
    rti1516e::ObjectClassHandle      _objectClassCarHandle;
    rti1516e::AttributeHandle        _positionAttributeHandle;
    rti1516e::AttributeHandle        _fuelLevelAttributeHandle;
    rti1516e::AttributeHandle		 _fuelTypeAttributeHandle;
    rti1516e::AttributeHandle        _nameAttributeHandle;
    rti1516e::AttributeHandle        _licensePlateAttributeHandle;

    mutable boost::mutex _carMutex;
    mutable boost::mutex _listenersMutex;
    mutable boost::mutex _connectionMutex;
};

// Used for caching car state from the simulator
class HLAcarState {
public:
    HLAcarState() { /* void */ }
    HLAcarState(const Car& car) :
        id           (car.getID()),
        name         (car.getName()),
        licensePlate (car.getLicensePlate()),
        fuelLevel    (car.getFuelLevel()),
        fuelType     (car.getFuelType()),
        position     (car.getPosition()),
        speed        (car.getSpeed()) 
    { /* void */ }

public:
    int          id;
    std::wstring name; 
    std::wstring licensePlate;
    double       fuelLevel;
    FuelTypePtr  fuelType;
    PositionPtr  position;
    double       speed;
};

#endif

