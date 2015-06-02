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
#ifndef SCENARIO_H
#define SCENARIO_H

#include <boost/shared_ptr.hpp>

#include "Properties.h"
#include "Position.h"

class Scenario;
typedef boost::shared_ptr<Scenario> ScenarioPtr;

/**
 * Represents the properties of a .scenario configuration file.
 * 
 * @remarks The class is thread safe through immutable state.
 * @author Steve Eriksson steve.eriksson@pitch.se
 */ 
class Scenario : public Properties {
public:
    explicit Scenario(const std::wstring& file);
    PositionPtr getStartPosition() const;
    PositionPtr getEndPosition()   const; 
};

#endif
