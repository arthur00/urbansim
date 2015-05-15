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
#ifndef POSITION_H
#define POSITION_H

#include <iostream>

#include <boost/shared_ptr.hpp>

class Position;
typedef boost::shared_ptr<Position> PositionPtr;

/**
 * The Position class represents a position using latitude and longitude.
 * 
 * Latitude and Longitude is given in degrees.
 *
 * @remark The class is thread safe through immutable state.
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class Position {
public:
    Position();
    Position(double latitude, double longitude);

    /**
     * Get latitude in degrees
     *
     * @return degrees
     */
    double getLatitude()  const;

    /**
     * Get longitude in degrees
     *
     * @return degrees
     */
    double getLongitude() const;

    /**
     * Get the string representation of the Position.
     *
     * @return the Position as wstring
     */
    std::wstring wstr()   const;

private:
    const double _lat; // in degrees
    const double _long;
};

// helper function
std::wostream& operator << (std::wostream& wostream, const Position& position);

#endif

