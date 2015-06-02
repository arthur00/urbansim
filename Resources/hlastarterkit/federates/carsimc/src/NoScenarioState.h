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
#ifndef NO_SCENARIO_STATE_H
#define NO_SCENARIO_STATE_H

#include "State.h"

/**
 * The NoScenarioState class only implements the loadScenario method.
 * 
 * The NoScenarioState can transition to ScenarioLoadedState.
 * 
 * @remark The class is thread safe through immutable state.
 * @see State, ScenarioLoadedState, Controller
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class NoScenarioState : public State {
public:
    NoScenarioState();
    virtual void loadScenario(const std::wstring& scenario, double initialFuelLevel, Controller* controller);
};

#endif
