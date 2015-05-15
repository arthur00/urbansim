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


class CarImpl implements Car {

   private static int carCount = 0;

   private final String _identifier;

   private final String _name;
   private final String _licensePlateNumber;
   private final FuelType _fuelType;

   private final int _normalSpeed; // km/h
   private double _currentSpeed; // km/h

   private final double _litersPerKm1; // First minute of driving
   private final double _litersPerKm2; // Second minute of driving
   private final double _litersPerKm3; // Third minute and beyond

   private Position _position;

   private double _fuelLevel;

   public CarImpl(String name,
                  String licensePlateNumber,
                  FuelType fuelType,
                  int normalSpeed,
                  double litersPerKm1,
                  double litersPerKm2,
                  double litersPerKm3) {
      _identifier = "Car" + carCount;
      carCount++;
      _name = name;
      _licensePlateNumber = licensePlateNumber;
      _fuelType = fuelType;
      _normalSpeed = normalSpeed;
      _currentSpeed = normalSpeed;
      _litersPerKm1 = litersPerKm1;
      _litersPerKm2 = litersPerKm2;
      _litersPerKm3 = litersPerKm3;
   }

   public String getIdentifier() {
      return _identifier;
   }

   public String getName() {
      return _name;
   }

   public String getLicensePlateNumber() {
      return _licensePlateNumber;
   }

   public FuelType getFuelType() {
      return _fuelType;
   }

   public int getNormalSpeed() {
      return _normalSpeed;
   }

   public double getLitersPerKm1() {
      return _litersPerKm1;
   }

   public double getLitersPerKm2() {
      return _litersPerKm2;
   }

   public double getLitersPerKm3() {
      return _litersPerKm3;
   }

   public Position getPosition() {
      return _position;
   }

   public double getFuelLevel() {
      return _fuelLevel;
   }

   public void setPosition(Position position) {
      _position = position;
   }

   public void setFuelLevel(double fuelLevel) {
      _fuelLevel = fuelLevel;
   }

   public double getCurrentSpeed() {
      return _currentSpeed;
   }

   public void adjustCurrentSpeed(double change) {
      _currentSpeed = _currentSpeed + change;
   }
}
