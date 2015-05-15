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
#ifndef LAT_LONG_HELPER_H
#define LAT_LONG_HELPER_H

#include <boost/math/constants/constants.hpp>

#include "Position.h"


/**
 * A class with utility methods for calculating latitude/longitude 
 * positions and the distance between them.
 * 
 * @author Åsa Wihlborg asa.wihlborg@pitch.se
 * @author Steve Eriksson steve.eriksson@pitch.se         
 */ 
class LatLongHelper {
public:
    /**
     * Create a new Position object.
     *
     * A new Position is created based on a starting position, a desired goal
     * position and a maximum travel distance from the starting position.
     * 
     * @param origin      - a Position object designating the starting point
     * @param destination - a Position object designating the desired goal
     * @param distance    - the total amount of kilometers from the original
     *                      position the new position is allowed to be
     * @return a shared pointer to the new Position object
     */
    static PositionPtr newPosition(PositionPtr origin, PositionPtr destination, double distance);

    /**
     * Calculate the distance in kilometer between two Position objects.
     *
     * @return the distance in kilometers
     */
    static double calcDistance(PositionPtr pos1, PositionPtr pos2);


    LatLongHelper() {}

    /**
     * Create a new Position based on two lat/long points and a distance. 
     *
     * @param lat1     - the latitude of the current position in degrees
     * @param long1    - the longitude of the current position in degrees
     * @param lat2     - the latitude of the desired goal in degrees
     * @param long2    - the longitude of the desired goal in degrees
     * @param distance - the total amount of kilometers from the original
     *                   position the new position is allowed to be
     * @return a shared pointer to the new Position object 
     */
    static PositionPtr newPosition(double lat1, double long1, double lat2, double long2, double distance);

    /**
     * Create a new Position based on a lat/long point, a maximum distance and a bearing.
     *
     * @param currentLat  - the latitude of the current position in degrees
     * @param currentLong - the longitude of the current position in degrees
     * @param distance    - the total amount of kilometers from the original
     *                      position the new position is allowed to be
     * @param bearing     - the bearing to use when calculating the new position in degrees
     * @return a shared pointer to the new Position object 
     */
    static PositionPtr newPosition(double currentLat, double currentLong, double distance, double bearing);

    static double toRad(double degree);
    static double toDeg(double radian);

    /**
     * Calculate the bearing between two lat/long points.
     *	 
     * @param lat1  - the latitude of the first position in degrees
     * @param long1 - the longitude of the first position in degrees
     * @param lat2  - the latitude of the second position in degrees
     * @param long2 - the longitude of the second position in degrees
     * @return the bearing in degrees
     */
    static double calcBearing(double lat1, double long1, double lat2, double long2);

    /**
     * Calculate the distance between two lat/long points.
     *	 
     * @param lat1  - the latitude of the first position in degrees
     * @param long1 - the longitude of the first position in degrees
     * @param lat2  - the latitude of the second position in degrees
     * @param long2 - the longitude of the second position in degrees
     * @return the distance in kilometers
     */
    static double calcDistance(double lat1, double long1, double lat2, double long2);

private:
    static const double EARTH_RADIUS; // kilometers
    static const double PI;
    static const double DEG2RAD;
    static const double RAD2DEG;
};

#endif
