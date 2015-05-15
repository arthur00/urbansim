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
#ifndef STATE_H
#define STATE_H

#include <string>

#include <boost/shared_ptr.hpp>

#include "Scenario.h"

class Controller;
class State;
typedef boost::shared_ptr<State> StatePtr;

/**
 * The State class is abstract and implements default behavior for all 
 * states in the Controller.
 *
 * @see Controller
 * @author Steve Eriksson steve.eriksson@pitch.se
 */ 
class State {
public:
    virtual ~State();
    virtual void start(double timeScaleFactor, Controller* controller);
    virtual void stop(Controller* controller);
    virtual void quit(Controller* controller);
    virtual void enable(Controller* controller);
    virtual void disable(Controller* controller);
    virtual void loadScenario(const std::wstring& scenario, double initialFuelLevel, Controller* controller);

protected:
    State(); 
    void changeState(Controller* controller, StatePtr state);
};

#endif 
