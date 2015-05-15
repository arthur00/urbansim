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
#ifndef RUNNING_STATE_H
#define RUNNING_STATE_H

#include "State.h"

/**
 * The RunningState class handles all State actions and contains a simulation
 * loop thread.
 * 
 * The RunningState can transition to ScenarioLoadedState
 *
 * @remark The class is not thread safe.
 * @see State, ScenarioLoadedState, Controller
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class RunningState : public State {
public:
    RunningState();
    virtual void stop(Controller* controller);
    virtual void loadScenario(const std::wstring& scenario, double initialFuelLevel, Controller* controller);
    virtual void quit(Controller* controller);
    virtual void enable(Controller* controller);
};

#endif
