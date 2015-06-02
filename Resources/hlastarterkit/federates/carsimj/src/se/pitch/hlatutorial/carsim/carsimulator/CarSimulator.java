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


public interface CarSimulator {

   /**
    * Adds a car to the simulation
    *
    * @param carConfiguration A collection of parameters describing a car
    */
   public void simulateCar(Car carConfiguration);

   /**
    * Advance that time in the simulation and update all cars
    *
    * @param timeStepMs the number of milli seconds to advance
    */
   void advanceTime(int timeStepMs) throws NoScenarioException;

   /**
    * Get a car
    *
    * @param identifier The car identifier
    */
   Car getCar(String identifier);

   /**
    * Get all simulated Cars
    *
    * @return An collection of Cars
    */
   Collection<Car> getCars();

   /**
    * Set a scenario
    *
    * @param start            The start position of the cars
    * @param goal             The goal position
    * @param initialFuelLevel The initial fuel level in the cars
    */
   void setScenario(Position start, Position goal, int initialFuelLevel);

   public static class Factory {
      public static CarSimulator newSimulator() {
         return new CarSimulatorImpl();
      }
   }
}
