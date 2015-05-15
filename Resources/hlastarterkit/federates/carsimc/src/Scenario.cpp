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
#include "Scenario.h"
#include "StringUtils.h"

using namespace std;

Scenario::Scenario(const wstring& file) : Properties(file) {}

PositionPtr Scenario::getStartPosition() const 
{
    double startLat = StringUtils::fromWString<double>(getProperty(L"StartLat"));
    double startLong = StringUtils::fromWString<double>(getProperty(L"StartLong"));
    return PositionPtr(new Position(startLat, startLong));
}

PositionPtr Scenario::getEndPosition() const 
{
    double stopLat = StringUtils::fromWString<double>(getProperty(L"StopLat"));
    double stopLong = StringUtils::fromWString<double>(getProperty(L"StopLong"));
    return PositionPtr(new Position(stopLat, stopLong));
}
