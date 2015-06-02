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
#ifndef CONTROLLER_H
#define CONTROLLER_H

#include <string>

#include <boost/thread/mutex.hpp>

#include "Simulator.h"
#include "HLAmodule.h"
#include "State.h"
#include "NoScenarioState.h"
#include "InteractionListener.h"
#include "SimulatorListener.h"
#include "CarFactory.h"

/**
 * The Controller manages communication between the Simulator, HLA module and the user. 
 *
 * The Controller class is responsible for managing communication between the Simulator, 
 * HLA module and the user. The Controller can transition between different states during
 * runtime.
 *
 * @see State, Simulator, HLAmodule, CarFactory
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class Controller : public InteractionListener, SimulatorListener {
public:
    explicit Controller(CarFactoryPtr carFactory, 
                        SimulatorPtr simulator, 
                        HLAmodulePtr hlaModule, 
                        StatePtr startState = StatePtr(new NoScenarioState()));

    /**
     * Initialize and start the Controller.
     *
     * The Car objects will be created and added to both the Simulator and
     * the HLA module. The Controller will register itself as a listener to
     * the Simulator and the HLA module.
     */
    void start();

    /**
     * Read input from the user, evaluate the input and print the result
     * of the given input.
     */
    void readEvalPrint();

    /**
     * Update a specific Car object.
     * The provided Car object's state is used for the update.
     *
     * @param id - id of the car to update
     * @param car - a shared pointer to a Car which state will be used
     */
    void updateCar(int id, CarPtr car);

    // InteractionListener interface
    virtual void receivedStart(double timeScaleFactor);
    virtual void receivedStop();
    virtual void receivedQuit();
    virtual void receivedLoadScenario(const std::wstring& scenario, double initialFuelLevel);

    // SimulatorListener interface
    virtual void stoppedSimulating(Simulator* simulator) ;
    virtual void startedSimulating(Simulator* simulator);
    virtual void updatedState(Simulator* simulator);

private:

    /**
     * Change the Controller's current state.
     *
     * @param state - a shared pointer to the State object to switch to
     */
    void changeState(StatePtr state);

    /**
     * Tell the user that the scenario has been loaded.
     *
     * @param name - the name of the loaded scenario
     */
    void printScenarioLoaded(const std::wstring& name) const;

    /**
     * Tell the user that the simulation has stopped.
     */
    void printStopSimulating() const;

    /**
     * Tell the user that the simulation has started.
     */
    void printStartSimulating() const;

private:
    CarFactoryPtr          _carFactory;
    SimulatorPtr           _simulator;
    HLAmodulePtr           _hlaModule;
    StatePtr               _state; // guarded by _stateMutex
    mutable boost::recursive_mutex _stateMutex; 

    friend class State;
    friend class NoScenarioState;
    friend class ScenarioLoadedState;
    friend class RunningState;
};

#endif
