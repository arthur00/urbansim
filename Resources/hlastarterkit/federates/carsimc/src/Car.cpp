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
#include <memory>
#include <cstdlib>

#include "Car.h"
#include "LatLongHelper.h"

using namespace std;

const double  Car::DISTANCE_THRESHOLD = 0.1;

Car::Car(int id, const CarProperties& properties)
    : _id(id), 
      _name(properties.getName()),
      _licensePlate(properties.getLicensePlate()), 
      _fuelLevel(0.0),
      _fuelType(properties.getFuelType()), 
      _position(PositionPtr(new Position(0.0, 0.0))),
      _speed(properties.getNormalSpeed()),
      _baseSpeed(_speed),
      _litersPer100km1(properties.getLitersPer100km1()),
      _litersPer100km2(properties.getLitersPer100km2()),
      _litersPer100km3(properties.getLitersPer100km3()),
      _driving(true)
{ /*void */ }

Car::Car(int id, 
        const std::wstring& name, 
        const std::wstring& licensePlate, 
        double fuelLevel, 
        FuelTypePtr fuelType, 
        PositionPtr position,
        double speed,
        double litersPer100km1,
        double litersPer100km2,
        double litersPer100km3)
    : _id(id), 
      _name(name),
      _licensePlate(licensePlate), 
      _fuelLevel(fuelLevel),
      _fuelType(fuelType), 
      _position(position),
      _speed(speed),
      _baseSpeed(speed),
      _litersPer100km1(litersPer100km1),
      _litersPer100km2(litersPer100km2),
      _litersPer100km3(litersPer100km3),
      _driving(true)
{ /* void */ }

Car::Car(const Car& other)
    : _id(other._id), 
      _name(other._name),
      _licensePlate(other._licensePlate), 
      _fuelLevel(other._fuelLevel),
      _fuelType(other._fuelType), 
      _position(other._position),
      _speed(other._speed),
      _baseSpeed(other._baseSpeed),
      _litersPer100km1(other._litersPer100km1),
      _litersPer100km2(other._litersPer100km2),
      _litersPer100km3(other._litersPer100km3),
      _driving(other._driving)
{ /* void */ }

int Car::getID() const 
{ 
    return _id; 
}

wstring Car::getName() const 
{ 
    return _name; 
}

wstring Car::getLicensePlate() const 
{ 
    return _licensePlate; 
}

double Car::getFuelLevel() const 
{ 
    boost::unique_lock<boost::mutex> lock(_attributeMutex);
    return _fuelLevel; 
}

FuelTypePtr Car::getFuelType() const 
{ 
    return _fuelType;
}

PositionPtr Car::getPosition() const 
{ 
    boost::unique_lock<boost::mutex> lock(_attributeMutex);
    return _position; 
}

double Car::getSpeed() const
{
    boost::unique_lock<boost::mutex> lock(_attributeMutex);
    return _speed;
}

void Car::setFuelLevel(double level)
{
    boost::unique_lock<boost::mutex> lock(_attributeMutex);
    _fuelLevel = level;
}

void Car::setPosition(PositionPtr position)
{
    boost::unique_lock<boost::mutex> lock(_attributeMutex);
    _position = position;
}

void Car::setSpeed(double speed)
{
    boost::unique_lock<boost::mutex> lock(_attributeMutex);
    _speed = speed;
}

bool Car::isDriving() const
{
    boost::unique_lock<boost::mutex> lock(_drivingMutex);
    return _driving;
}

void Car::setDriving(bool shouldDrive)
{
    boost::unique_lock<boost::mutex> lock(_drivingMutex);
    _driving = shouldDrive;
}

void Car::drive(double duration, PositionPtr destination)
{
    if (isDriving()) {	
        randomizeSpeed();

        double fuelLevel = getFuelLevel();
        double maxDistance = _speed * (duration / 1000.0 / 3600.0); // millis to hours	
        double maxFuelConsumption = (_litersPer100km3 / 100) * maxDistance;

        if (maxFuelConsumption > _fuelLevel) {
            maxDistance = fuelLevel / (_litersPer100km3 / 100);
            if (maxDistance < DISTANCE_THRESHOLD) {
                wcout << L"\b\bCar " << _licensePlate << L" has run out of fuel"  << endl
                     << L"> ";
                setDriving(false);
                return;
            }
        }
        
        PositionPtr oldPos =_position;
        PositionPtr newPos = LatLongHelper::newPosition(oldPos, destination, maxDistance);
        _position = newPos;
    
        double distanceDriven =  LatLongHelper::calcDistance(oldPos, newPos);
        double consumed = _litersPer100km3 * distanceDriven / 100.0;
    
        setFuelLevel(fuelLevel - consumed);

        double distanceToGoal = LatLongHelper::calcDistance(newPos, destination);
        if (distanceToGoal < DISTANCE_THRESHOLD) {
            wcout << L"\b\bCar " << _licensePlate << L" has reached its destination"  << endl
                 << L"> ";
            setDriving(false);
        }
    }
}

wstring Car::wstr() const 
{
    boost::unique_lock<boost::mutex> lock(_attributeMutex);

    wostringstream woss;
    woss << L"Name: "            << _name 
         << L", License plate: " << _licensePlate
         << L", Fuel level: "    << _fuelLevel
         << L", Fuel type: "     << *_fuelType
         << L", Position: "      << *_position
         << L", Speed: "         << _speed;
    return woss.str();
}



/////////////////////
// Private methods //
/////////////////////

void Car::randomizeSpeed()
{
    boost::unique_lock<boost::mutex> lock(_attributeMutex);
    double speedChange = rand() % 6; // 0-5

     if (_baseSpeed -_speed > 20) {
        _speed += speedChange;
     } else if (_speed - _baseSpeed > 20) { 
        _speed += -speedChange;
     } else if (speedChange < 3) {
        _speed += speedChange;
     } else {
        _speed += -speedChange;
     }
}

//////////////////////
// helper functions //
//////////////////////

wostream& operator << (wostream& wos, const Car& car)
{
    return wos << car.wstr();
}

