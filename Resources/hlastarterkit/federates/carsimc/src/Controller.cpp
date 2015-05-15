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
#include <boost/algorithm/string.hpp>
#include <boost/thread/mutex.hpp>
#include <boost/filesystem.hpp>

#include "Controller.h"
#include "NoScenarioState.h"
#include "SimulatorConfig.h"
#include "CarProperties.h"
#include "StringUtils.h"

using namespace std;

Controller::Controller(CarFactoryPtr carFactory, 
                       SimulatorPtr simulator, 
                       HLAmodulePtr hlaModule, 
                       StatePtr startState) 
    : _carFactory(carFactory), 
      _simulator(simulator), 
      _hlaModule(hlaModule), 
      _state(startState) 
{
    /* void */
}

void Controller::start() 
{
    try
    {
        CarPtrMap cars = _carFactory->getCars(); 
        _simulator->addCars(cars);
        _simulator->addListener(this);
        _hlaModule->addListener(this);
        _hlaModule->connect();
        _hlaModule->addCars(cars);
        readEvalPrint();
    }
    catch (const rti1516e::Exception& e)
    {
        wcerr << L"Controller received an exception during initialization: " 
              << e.what() << endl;
        receivedQuit(); // exit
    }
}

void Controller::updateCar(int id, CarPtr car)
{
    _hlaModule->updateCar(id, *car);
}

void Controller::changeState(StatePtr state)
{
    boost::unique_lock<boost::recursive_mutex> lock(_stateMutex);
    _state->disable(this);
    _state = state;
    _state->enable(this);
}


///////////////////////////////////
// InteractionListener interface //
///////////////////////////////////

void Controller::receivedStart(double timeScaleFactor) 
{
    boost::unique_lock<boost::recursive_mutex> lock(_stateMutex); 
    _state->start(timeScaleFactor, this);
}

void Controller::receivedStop() 
{
    boost::unique_lock<boost::recursive_mutex> lock(_stateMutex); 
    _state->stop(this);
}

void Controller::receivedLoadScenario(const std::wstring& scenario, double initialFuelLevel) 
{
    boost::unique_lock<boost::recursive_mutex> lock(_stateMutex); 
    _state->loadScenario(scenario, initialFuelLevel, this);
}

void Controller::receivedQuit() 
{
    boost::lock_guard<boost::recursive_mutex> lock(_stateMutex); 
    _state->quit(this);
    _hlaModule->disconnect();
}



/////////////////////////////////
// SimulatorListener interface //
/////////////////////////////////

void Controller::stoppedSimulating(Simulator* simulator)
{ 
    /* no op */
}

void Controller::startedSimulating(Simulator* simulator)
{
    /* no op */ 
}

void Controller::updatedState(Simulator* simulator)
{
    const CarPtrMap& cars = simulator->getCars();
    CarPtrMap::const_iterator iter(cars.begin());

    for (; iter != cars.end(); ++iter) {
        _hlaModule->updateCar(iter->first, *(iter->second) );
    }
}



///////////////////////////
// User input and output //
///////////////////////////

void Controller::readEvalPrint()
{
    wcout << _hlaModule->getFederateName() << L" has joined. Ready to receive scenario." << endl;
    string input;
    while (true)
    {
        wcout << L"Press Q at any time to quit." << endl
             << L"> ";
        getline(cin, input);
        boost::algorithm::trim(input);
        boost::algorithm::to_lower(input);

        if (boost::algorithm::equals(input, "q")) {
            receivedQuit();
            break;
        } else {
            wcout << L"Unknown command: '" << StringUtils::str2wstr(input) << L"'" << endl;
        }
    }
}

void Controller::printScenarioLoaded(const wstring& name) const
{
    wcout << L"\b\b" << _hlaModule->getFederateName() << " has loaded scenario " << name  << endl
          << L"The following cars are standing by at the starting point:" << endl;
        
    CarPtrMap cars = _simulator->getCars();
    CarPtrMap::iterator iter(cars.begin());
        for (; iter != cars.end(); ++iter) {
            wcout << (iter->second)->getName() << endl;
        }

        wcout << L"> " << flush;
}

void Controller::printStopSimulating() const
{
    wcout << L"\b\b" << _hlaModule->getFederateName() << L" has stopped simulating." << endl
         << L"> " << flush;
}

void Controller::printStartSimulating() const
{
    CarPtrMap cars = _simulator->getCars();
    wcout << L"\b\b" << _hlaModule->getFederateName() << L" is now simulating "<< cars.size() << L" cars." << endl
          << L"> " << flush;
}
