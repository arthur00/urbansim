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
package se.pitch.hlatutorial.carsim.model;


public interface Car {

   String getIdentifier();

   /**
    * Get the name of the Car
    *
    * @return the name
    */
   String getName();

   /**
    * Get the license plate number
    *
    * @return the license plate number
    */
   String getLicensePlateNumber();

   /**
    * Get the fuel type
    *
    * @return the fuel type
    */
   FuelType getFuelType();

   /**
    * Get the normal speed of the car
    *
    * @return the speed
    */
   int getNormalSpeed();

   /**
    * Get the current speed of the car
    *
    * @return the speed
    */
   double getCurrentSpeed();

   void adjustCurrentSpeed(double change);

   double getLitersPerKm1();

   double getLitersPerKm2();

   double getLitersPerKm3();

   /**
    * Get the current position of the Car
    *
    * @return Position of the Car
    */
   Position getPosition();

   public void setPosition(Position position);

   /**
    * Get the current fuel level of the car
    *
    * @return Fuel level of the Car
    */
   double getFuelLevel();

   void setFuelLevel(double fuelLevel);

}
