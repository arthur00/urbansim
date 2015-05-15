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
#include <cmath>
#include "LatLongHelper.h"

const double LatLongHelper::EARTH_RADIUS = 6372.7976;
const double LatLongHelper::PI = boost::math::constants::pi<double>();
const double LatLongHelper::DEG2RAD = PI / 180.0;
const double LatLongHelper::RAD2DEG = 180.0 / PI;

PositionPtr LatLongHelper::newPosition(PositionPtr position, PositionPtr destination, double distance)
{
    return newPosition(
        position->getLatitude(), 
        position->getLongitude(), 
        destination->getLatitude(), 
        destination->getLongitude(), 
        distance);
}

PositionPtr LatLongHelper::newPosition(double lat1, double long1, double lat2, double long2, double distance)
{
    if (calcDistance(lat1, long1, lat2, long2) <= distance) {
        return PositionPtr( new Position(lat2, long2) );
    } else {
        double bearing = calcBearing(lat1, long1, lat2, long2);
        return newPosition(lat1, long1, distance, bearing);
    }
}

PositionPtr LatLongHelper::newPosition(double currentLat, double currentLong, double distance, double bearing)
{
    double dr = distance/EARTH_RADIUS;
    double latRad1 = toRad(currentLat);
    double longRad1 = toRad(currentLong);

    double destinationLatRad = asin(sin(latRad1) * cos(dr) + cos(latRad1) * sin(dr) * cos(toRad(bearing)));
    double destinationLongRad = longRad1 + atan2(sin(toRad(bearing)) * sin(dr) *cos(latRad1),
        cos(dr) - sin(latRad1) * sin(destinationLatRad));
    return PositionPtr(new Position(toDeg(destinationLatRad), toDeg(destinationLongRad)));
}

double LatLongHelper::toRad(double degree)
{
    return degree * DEG2RAD;
}

double LatLongHelper::toDeg(double radian)
{
    return radian * RAD2DEG;
}

double LatLongHelper::calcBearing(double lat1, double long1, double lat2, double long2)
{
    double latRad1  = toRad(lat1);  // currentLat  * (_pi/180);
    double longRad1 = toRad(long1); // currentLong * (_pi/180);
    double latRad2  = toRad(lat2);  // goalLat  * (_pi/180);
    double longRad2 = toRad(long2); // goalLong * (_pi/180);

    double dLongRad = longRad2 - longRad1;

    double yRad = sin(dLongRad) * cos(latRad2);
    double xRad = cos(latRad1) * sin(latRad2) - sin(latRad1) * cos(latRad2) *cos(dLongRad);

    double bearing = toDeg(atan2(yRad, xRad)) + 360.0;

    return fmod(bearing, 360.0);
}

double LatLongHelper::calcDistance(PositionPtr pos1, PositionPtr pos2)
{
    return calcDistance(
        pos1->getLatitude(), 
        pos1->getLongitude(), 
        pos2->getLatitude(), 
        pos2->getLongitude());
}

double LatLongHelper::calcDistance(double lat1, double long1, double lat2, double long2)
{
    return acos(sin(toRad(lat1)) * sin(toRad(lat2)) +
            cos(toRad(lat1))     * cos(toRad(lat2)) *
            cos(toRad(long2)     - toRad(long1)))   * EARTH_RADIUS;
}
