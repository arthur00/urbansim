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
#ifndef SIMULATOR_H
#define SIMULATOR_H

#include <set>
#include <stdint.h>

#include <boost/shared_ptr.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/thread.hpp>

#include "Scenario.h"
#include "Car.h"
#include "SimulatorListener.h"
#include "SimulatorConfig.h"

class Simulator;
typedef boost::shared_ptr<Simulator> SimulatorPtr;
typedef std::set<SimulatorListener*> SimulatorListenerSet;
typedef boost::shared_ptr<boost::thread> ThreadPtr;

/**
 * Simulates the fuel consumption of cars.
 * 
 * The Simulator simulates the fuel consumption of Cars. 
 * The Simulator loads a Scenario description and sets an initial fuel amount
 * in each Car before simulation can begin.
 *
 * @remark The class is thread safe by using mutex locking.
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class Simulator {
public:
    Simulator(const SimulatorConfig& configuration);
    ~Simulator();

    /**
     * Load a Scenario and set the initial fuel level.
     * 
     * The method assumes a shared pointer to a correctly loaded Scenario 
     * object.
     *
     * @param scenario - a shared pointer to a functional Scenario object
     * @param initialFuelLevel - the fuel level all cars should start with
     */
    void loadScenario(ScenarioPtr scenario, double initialFuelLevel);

    /**
     * Create a Scenario object with the given name and set the initial fuel 
     * level.
     *
     * If it's not possible to create a Scenario object base on the provided 
     * name the method will return false.
     *
     * @param scenario         - the name of the scenario to load
     * @param initialFuelLevel - the fuel level all cars should start with
     * @return true if it was possible to create and load a Scenario object
     */
    bool loadScenario(const std::wstring& scenario, double initialFuelLevel);

    /**
     * Check if the Simulator has loaded a Scenario object
     *
     * @return true if there is a loaded Scenario object
     */
    bool hasScenario() const;

    /**
    * Add all supplied Car objects to the Simulator.
    * Car objects are copied.
    *
    * @param cars - a map with zero or more Car objects
    */
    void addCars(const CarPtrMap& cars);

    /**
     * Get a map with shared pointers to the Car objects.
     *
     */
    CarPtrMap getCars();

    /**
     * Get a shared pointer to the Car object with provided ID.
     *
     * @param id - the ID of the desired Car object
     */
    CarPtr getCar(int id);

    /**
     * Add a listener to the Simulator.
     * When the objects that are being simulated changes state all listeners
     * will be notified.
     *
     * @param listener - a pointer to a SimulatorListener
     */
    void addListener(SimulatorListener* listener);

    /**
     * Remove a listener from the Simulator.
     * The user has to provide the same pointer to the object that was used
     * when registering the listener with addListener(SimulatorListener*).
     * 
     * @param listener - a pointer to the listener to remove
     */
    void removeListener(SimulatorListener* listener);

    /**
     * Check whether the Simulator is running the simulation loop.
     * 
     * @return true if running the simulation loop
     */
    bool isSimulating() const;

    /**
     * Start or stop the simulation loop.
     * When given a parameter true a thread is created to run the simulation
     * loop. If the parameter is false the simulation loop will stop and the
     * thread it runs in will be destroyed.
     *
     * @param shouldSimulate - true will start the simulation loop
     *                         false will stop it
     */
    void setSimulating(bool shouldSimulate);

    /**
     * Set the time scale factor used in the simulation.
     * The time scale can make the simulation run faster or slower than 
     * wall clock time. Only positive values shall be used.
     *
     * @param factor - the time scale factor as a positive double float value
     */
    void setTimeScaleFactor(double factor);

private:

    /**
     * The simulation loop.
     * All cars' states will be updated, the scenario time will be advanced
     * and listeners to the Simulator will be notified.
     */
    void simulate();

    /**
     * Advance the time in the simulation.
     * This will update the state of all simulated Cars.
     * The timestep shall be a positive value.
     *
     * @param timeStep - the time in milliseconds to advance
     */
    virtual void  advanceTime(unsigned int timeStep);

private:
    ScenarioPtr        _scenario;        // guarded by _scenarioMutex
    CarPtrMap          _cars;            // guarded by _carsMutex
    const unsigned int _frameRate;
    const std::wstring _scenarioDir;
    double             _timeScaleFactor; // guarded by _timeScaleFactorMutex
    uint64_t           _scenarioTime;    // guarded by _carsMutex
    bool               _simulating;      // guarded by _simulatingMutex
    ThreadPtr          _thread;          // guarded by _threadMutex

    SimulatorListenerSet _simulatorListeners; // guarded by _listenersMutex;

    mutable boost::mutex _listenersMutex;
    mutable boost::mutex _simLoopMutex;
    mutable boost::mutex _timeScaleFactorMutex;
    mutable boost::mutex _simulatingMutex;
    mutable boost::mutex _scenarioMutex;
    mutable boost::mutex _carsMutex;
    mutable boost::mutex _threadMutex;

    static const int MILLISECOND;
};

#endif
