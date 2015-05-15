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
#include <exception>

#include <boost/timer.hpp>

#include "Simulator.h"

using namespace std;

const int Simulator::MILLISECOND = 1000;


Simulator::Simulator(const SimulatorConfig& configuration) 
    : _frameRate(configuration.getFrameRate()), 
    _scenarioDir(configuration.getScenarioDir()),
    _timeScaleFactor(1.0),
    _simulating(false)
{ 
    /* void */ 
}

Simulator::~Simulator()
{ 
    /* void */ 
}

void Simulator::simulate()
{
    if (_frameRate <= 0) {
        throw (new runtime_error("Framerate should be a positive integer. Check simulator configuration."));
    }

    unsigned int totalLoopTime = MILLISECOND / _frameRate; // number of milliseconds the loop should execute
    unsigned int advanceTimeStep;
    {
        boost::unique_lock<boost::mutex> lock(_timeScaleFactorMutex);
        advanceTimeStep = static_cast<unsigned int>(totalLoopTime * _timeScaleFactor); 
    }

    boost::unique_lock<boost::mutex> lock(_simLoopMutex); 
    while (isSimulating()) {
        boost::timer timer;

        advanceTime(advanceTimeStep);

        // notify the listeners
        SimulatorListenerSet::iterator iter(_simulatorListeners.begin());
        for (; iter != _simulatorListeners.end(); ++iter) {
            (*iter)->updatedState(this);
        }

        double elapsedTime = timer.elapsed() * MILLISECOND;
        int64_t sleepTime = static_cast<int64_t>(totalLoopTime - elapsedTime);
        
        if (sleepTime < 0) continue; // do not sleep, we're a bit late

        try {
            boost::this_thread::sleep(boost::posix_time::milliseconds(sleepTime));
        } catch(const boost::thread_interrupted&) {
            // might be a spurious interrupt, loop again and check isSimulating()
        }
    }
}

void Simulator::advanceTime(unsigned int timeStep)
{ 
    boost::unique_lock<boost::mutex> lock(_carsMutex);
    // really big counter so we shouldn't have to worry about wrapping
    _scenarioTime += timeStep; 
        
    CarPtrMap::iterator iter(_cars.begin()); 
    for (; iter != _cars.end(); ++iter) {
        (iter->second)->drive(timeStep, _scenario->getEndPosition());
    }
}

void Simulator::loadScenario(ScenarioPtr scenario, double initialFuelLevel)
{	
    {
        boost::unique_lock<boost::mutex> lock(_carsMutex);

        // Reset all Cars' states according to the scenario
        CarPtrMap::iterator iter(_cars.begin()); 
        for (; iter != _cars.end(); ++iter) { 
            (iter->second)->setFuelLevel(initialFuelLevel);
            (iter->second)->setPosition(scenario->getStartPosition());
            (iter->second)->setDriving(true);
        }
        _scenarioTime = 0;
    }

    boost::unique_lock<boost::mutex> lock(_scenarioMutex);
    _scenario = scenario;
}

bool Simulator::isSimulating() const
{
    boost::unique_lock<boost::mutex> lock(_simulatingMutex);
    return _simulating;
}

void Simulator::setSimulating(bool shouldSimulate)
{
    {
        boost::unique_lock<boost::mutex> lock(_simulatingMutex);
        _simulating = shouldSimulate;
    }

    {
        boost::unique_lock<boost::mutex> lock(_threadMutex);
        if (shouldSimulate) {
            if (_thread && _thread->joinable()) { // already running
                return;
            } else {
                _thread = ThreadPtr(new boost::thread(&Simulator::simulate, this));
            }
        
        } else if (_thread) {
            _thread->interrupt();
            _thread->join(); 
        }
    }
}

bool Simulator::loadScenario(const std::wstring& scenario, double initialFuelLevel)
{
    ScenarioPtr ptr(new Scenario(_scenarioDir + L"/" + scenario + L".scenario"));
    if (ptr->failedToLoad()) {
        return false;
    } else {
        loadScenario(ptr, initialFuelLevel);
        return true;
    }
}

bool Simulator::hasScenario() const
{ 
    boost::unique_lock<boost::mutex> lock(_scenarioMutex);
    return (_scenario? true : false);
}

void Simulator::addListener(SimulatorListener* listener)
{
    boost::unique_lock<boost::mutex> lock(_listenersMutex);
    _simulatorListeners.insert(listener);
}

void Simulator::removeListener(SimulatorListener* listener)
{
    boost::unique_lock<boost::mutex> lock(_listenersMutex);
    _simulatorListeners.erase(listener);
}

void Simulator::addCars(const CarPtrMap& cars)
{ 
    boost::unique_lock<boost::mutex> lock(_carsMutex);
    
    CarPtrMap::const_iterator car_iter(cars.begin());
    for (; car_iter != cars.end(); ++car_iter) {
        int id(car_iter->first);
        CarPtr car(new Car(*(car_iter->second)));
        _cars[id] = car;
    }
}

CarPtrMap Simulator::getCars()
{
    boost::unique_lock<boost::mutex> lock(_carsMutex);
    return _cars; // copy the map
}

CarPtr Simulator::getCar(int id)
{
    boost::unique_lock<boost::mutex> lock(_carsMutex);
    return _cars[id];
}

void Simulator::setTimeScaleFactor(double factor)
{
    boost::unique_lock<boost::mutex> lock(_timeScaleFactorMutex);
    _timeScaleFactor = factor;
}
