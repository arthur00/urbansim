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
#ifndef SIMULATOR_CONFIG_H
#define SIMULATOR_CONFIG_H

#include <string>

#include "Properties.h"

/**
 * Represents the properties of a .config simulator configuration file.
 *
 * @remarks The class is thread safe through immutable state.
 * @author Steve Eriksson steve.eriksson@pitch.se
 */ 
class SimulatorConfig : public Properties {
public:
    explicit SimulatorConfig(const std::wstring& file);
    
    std::wstring getFOM()                     const;
    std::wstring getScenarioDir()             const;
    std::wstring getLocalSettingsDesignator() const;
    std::wstring getCarsDir()                 const;
    std::wstring getFederationName()          const;
    std::wstring getFederateName()            const;
    int          getFrameRate()               const;
};

#endif
