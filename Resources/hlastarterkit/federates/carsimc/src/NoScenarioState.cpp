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

#include "NoScenarioState.h"
#include "ScenarioLoadedState.h"
#include "Controller.h"
#include "Car.h"

using namespace std;

NoScenarioState::NoScenarioState() 
{
    /* void */ 
}

void NoScenarioState::loadScenario(const wstring& scenario, double initialFuelLevel, Controller* controller)
{
    bool loadSucceeded = (controller->_simulator)->loadScenario(scenario, initialFuelLevel); 

    if (loadSucceeded) {
        CarPtrMap cars = (controller->_simulator)->getCars();
        
      CarPtrMap::const_iterator iter(cars.begin());
        for (; iter != cars.end(); ++iter) {
            (controller->_hlaModule)->updateCar(iter->first, *(iter->second) );
        }

        (controller->_hlaModule)->sendScenarioLoadedInteraction();
        controller->printScenarioLoaded(scenario);
        changeState(controller, StatePtr(new ScenarioLoadedState()));
    } else {
        (controller->_hlaModule)->sendScenarioLoadFailureInteraction(L"CarSimC failed to load scenario " + scenario);
    }
}



