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
#ifndef CAR_H
#define CAR_H

#include <string>
#include <map>

#include <boost/shared_ptr.hpp>
#include <boost/thread/mutex.hpp>

#include "Position.h"
#include "FuelType.h"
#include "CarProperties.h"

class Car;
typedef boost::shared_ptr<Car> CarPtr;
typedef std::map<int, CarPtr> CarPtrMap;

/**
 * The Car object can be moved towards given Position objects and simulates fuel consumption.
 *
 * @remark The class is thread safe through immutable state and mutex locking.
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class Car {
public:	
    Car(int id, const CarProperties&);

    Car(int id, 
        const std::wstring& name, 
        const std::wstring& licensePlate, 
        double fuelLevel, 
        FuelTypePtr fuelType, 
        PositionPtr position,
        double speed,
        double litersPer100km1,
        double litersPer100km2,
        double litersPer100km3);	

    Car(const Car& other);

    /**
     * Get the ID of the Car.
     *
     * @return the ID 
     */
    int getID() const;

    /**
     * Get the name of the Car.
     * This is typically the model of the Car.
     *
     * @return the name
     */
    std::wstring getName() const; 

    /**
     * Get the Car's license plate number.
     *
     * @return the license plate number
     */
    std::wstring getLicensePlate() const;

    /**
     * Get the current fuel level in liters.
     * Shall be a positive value.
     *
     * @return the fuel level
     */
    double getFuelLevel() const;

    /**
     * Get the Car's fuel type.
     *
     * @return a shared pointer to a fuel type object
     */
    FuelTypePtr getFuelType() const;

    /**
     * Get the Car's current position.
     *
     * @return a shared pointer to a position object
     */
    PositionPtr getPosition()  const;

    /**
     * Get the current speed of the car.
     * The value shall be positive and the unit is kilometer per hour.
     *
     * @return the speed in kilometer per hour
     */
    double getSpeed() const;

    /**
     * Get the wide string representation of the Car object.
     *
     * @return the Car as wstring
     */
    std::wstring wstr() const;

    /**
     * Set the Car's fuel level.
     * The value shall be positive.
     *
     * @param level - fuel level in liters
     */
    void setFuelLevel(double level);

    /**
     * Set the Car's position.
     *
     * @param position - a shared pointer to a position object
     */
    void setPosition(PositionPtr position);

    /**
     * Set the Car's speed.
     * The value shall be positive and in kilometers per hour.
     *
     * @param speed - the speed in kilometers per hour
     */
    void setSpeed(double speed);

    /**
     * Set the Car's state to drive or stop.
     *
     * @param shouldDrive - true if the Car should drive
     */
    void setDriving(bool shouldDrive);

    /**
     * Check if the Car is currently driving.
     * 
     * @return true if the Car is driving or false if it has stopped
     */
    bool isDriving() const;

    /**
     * Tell the Car to drive towards a certain destination during the 
     * given time.
     *
     * @param duration - how long in milliseconds the Car should drive
     * @param destination - the position to drive towards
     */
    void drive(double duration, PositionPtr destination);

private:

    /**
     * Randomize the Car's speed based on the current speed.
     */
    void randomizeSpeed();

private:
    const int            _id;
    const std::wstring   _name;
    const std::wstring   _licensePlate;
    double               _fuelLevel; // guarded by _attributeMutex
    const FuelTypePtr    _fuelType;
    PositionPtr          _position;   // guarded by _attributeMutex
    double               _speed;      // guarded by _attributeMutex
    const double         _baseSpeed;
    const double         _litersPer100km1;
    const double         _litersPer100km2;
    const double         _litersPer100km3;
    bool                 _driving;     // guarded by _drivingMutex
    mutable boost::mutex _attributeMutex;
    mutable boost::mutex _drivingMutex;
    static const double  DISTANCE_THRESHOLD;
};

std::wostream& operator << (std::wostream& os, const Car& car);

#endif

