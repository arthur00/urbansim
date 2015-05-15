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
#include "SimulatorConfig.h"
#include "StringUtils.h"

using namespace std;

SimulatorConfig::SimulatorConfig(const wstring& file) : Properties(file) {}

wstring SimulatorConfig::getFOM() const
{
    return getProperty(L"FOM");
}

wstring SimulatorConfig::getScenarioDir() const
{
    return getProperty(L"ScenarioDir");
}

wstring SimulatorConfig::getCarsDir() const
{
  return getProperty(L"CarsDir");
}

wstring SimulatorConfig::getLocalSettingsDesignator() const
{
    return getProperty(L"LocalSettingsDesignator");
}

wstring SimulatorConfig::getFederationName() const
{
    return getProperty(L"FederationName");
}

wstring SimulatorConfig::getFederateName() const
{
    return getProperty(L"FederateName");
}

int SimulatorConfig::getFrameRate() const
{
    return StringUtils::fromWString<int>(getProperty(L"FrameRate"));
}
