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
package se.pitch.hlatutorial.carsim.carsimulator;

import se.pitch.hlatutorial.carsim.model.Car;
import se.pitch.hlatutorial.carsim.model.Position;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


class CarSimulatorImpl implements CarSimulator {

   private final Map<String, Car> _cars = new HashMap<String, Car>();

   private Position _goalPosition;

   private int _timeSinceStartInMs = 0;

   private boolean _running = true;

   public void advanceTime(int timeStepMs) throws NoScenarioException {
      if (!_running) {
         throw new NoScenarioException();
      }

      for (Car car : _cars.values()) {
         updateSpeed(car);

         double possibleDistance = (float)LatLongHelper.distance(car.getPosition(), _goalPosition);

         float timeSinceStartInS = ((float)_timeSinceStartInMs) / 1000f;
         float timeStepInS = ((float)timeStepMs) / 1000f;
         double consumedFuel;
         float timeStep1 = Math.min(60, timeSinceStartInS + timeStepInS) - Math.min(60, timeSinceStartInS);
         double currentSpeed = car.getCurrentSpeed();
         double km1 = (currentSpeed / 3600f) * timeStep1;
         double litersPerKm1 = car.getLitersPerKm1();
         if (litersPerKm1 * km1 > car.getFuelLevel()) {
            km1 = car.getFuelLevel() / litersPerKm1;
            if (km1 < 0.1) {
               km1 = 0;
            }
         }
         if (km1 > possibleDistance) {
            km1 = possibleDistance;
         }
         consumedFuel = km1 * litersPerKm1;


         float timeStep2 = Math.max(60, Math.min(120, timeSinceStartInS + timeStepInS)) - Math.min(120, Math.max(timeSinceStartInS, 60));
         double km2 = (currentSpeed / 3600f) * timeStep2;
         double litersPerKm2 = car.getLitersPerKm2();
         if (litersPerKm2 * km2 > currentSpeed - consumedFuel) {
            km2 = (car.getFuelLevel() - consumedFuel) / litersPerKm2;
            if (km2 < 0.1) {
               km2 = 0;
            }
         }
         if (km1 + km2 > possibleDistance) {
            km2 = possibleDistance - km1;
         }
         consumedFuel += km2 * litersPerKm2;

         float timeStep3 = Math.max(timeSinceStartInS + timeStepInS, 120) - Math.max(timeSinceStartInS, 120);
         double km3 = (currentSpeed / 3600f) * timeStep3;
         double litersPerKm3 = car.getLitersPerKm3();
         if (litersPerKm3 * km3 > car.getFuelLevel() - consumedFuel) {
            km3 = (car.getFuelLevel() - consumedFuel) / litersPerKm3;
            if (km3 < 0.1) {
               km3 = 0;
            }
         }
         if (km1 + km2 + km3 > possibleDistance) {
            km3 = possibleDistance - km1 - km2;
         }

         car.setPosition(LatLongHelper.newPosition(car.getPosition(), _goalPosition, km1 + km2 + km3));
         car.setFuelLevel(car.getFuelLevel() - km1 * litersPerKm1 - km2 * litersPerKm2 - km3 * litersPerKm3);
      }
      _timeSinceStartInMs += timeStepMs;
   }

   private void updateSpeed(Car car) {
      double speedChange = Math.random() * 5;
      int normalSpeed = car.getNormalSpeed();
      if (normalSpeed - car.getCurrentSpeed() > 20) {
         car.adjustCurrentSpeed(speedChange);
      } else if (normalSpeed - car.getCurrentSpeed() < -20) {
         car.adjustCurrentSpeed(-speedChange);
      } else if (Math.random() < 0.5) {
         car.adjustCurrentSpeed(speedChange);
      } else {
         car.adjustCurrentSpeed(-speedChange);
      }
   }

   public void simulateCar(Car car) {
      _cars.put(car.getIdentifier(), car);
   }

   public Collection<Car> getCars() {
      return _cars.values();
   }

   public Car getCar(String identifier) {
      return _cars.get(identifier);
   }

   public void setScenario(Position start, Position goal, int initialFuelLevel) {
      _goalPosition = goal;
      _running = false;
      _timeSinceStartInMs = 0;
      for (Car car : _cars.values()) {
         car.setPosition(start);
         car.setFuelLevel(initialFuelLevel);
      }
      _goalPosition = goal;
      _running = true;
   }
}
