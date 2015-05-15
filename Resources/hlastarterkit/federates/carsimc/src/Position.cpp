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
#include <sstream>
#include "Position.h"

using namespace std;

Position::Position() : _lat(0.0), _long(0.0) { /* void */ }

Position::Position(double latitude, double longitude) : _lat(latitude), _long(longitude) { /* void */ }

double Position::getLatitude() const { return _lat; }

double Position::getLongitude() const { return _long; }

wstring Position::wstr() const
{
    wostringstream woss;
    woss << L"{Latitude: " << getLatitude() 
        << L", Longitude: " << getLongitude() << L"}";
    return woss.str();
}

wostream& operator << (wostream& wos, const Position& position)
{
    return wos << position.wstr();
}
