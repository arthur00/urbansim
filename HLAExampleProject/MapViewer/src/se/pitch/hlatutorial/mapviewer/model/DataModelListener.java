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
package se.pitch.hlatutorial.mapviewer.model;


public interface DataModelListener {

   /**
    * This callback will be called when a Car is added to the DataModel
    *
    * @param car The new Car
    */
   void carAdded(Car car);

   /**
    * This callback will be called when a Car is removed from the DataModel
    *
    * @param car the car
    */
   void carRemoved(Car car);

   /**
    * This callback will be called when a Car is updated
    *
    * @param car the car
    */
   void updated(Car car);

   /**
    * This callback will be called when a new Scenario is loaded
    *
    * @param scenario the scenario
    */
   void setScenario(Scenario scenario);

   /**
    * This callback will be called when a Scenario is stared
    *
    * @param timeScaleFactor the time scale factor
    */
   void scenarioStared(float timeScaleFactor);

   /**
    * This callback will be called when a Scenario is stopped
    */
   void scenarioStopped();
}
