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
package se.pitch.hlatutorial.mapviewer.hlamodule;

import se.pitch.hlatutorial.mapviewer.model.Car;
import se.pitch.hlatutorial.mapviewer.model.FuelType;
import se.pitch.hlatutorial.mapviewer.model.Position;


public interface CarListener {

   /**
    * Called when a new Car in discovered by the HlaInterface
    *
    * @param car The discovered Car
    */
   void carAdded(Car car);

   /**
    * Called when a Car is removed from the federation
    *
    * @param id the identifier for the removed car
    */
   void carRemoved(String id);

   /**
    * Called when the Name attribute of a Car is updated
    *
    * @param id   the id of the Car whose name is updated
    * @param name the new Name attribute value
    */
   void carNameUpdated(String id, String name);

   /**
    * Called when the LicensePlateNumber attribute of a Car is updated
    *
    * @param id                 the id of the Car whose name is updated
    * @param licensePlateNumber the new LicensePlateNumber attribute value
    */
   void carLicensePlateNumberUpdated(String id, String licensePlateNumber);

   /**
    * Called when the FuelType attribute of a Car is updated
    *
    * @param id       the id of the Car whose name is updated
    * @param fuelType the new FuelType attribute value
    */
   void carFuelTypeUpdated(String id, FuelType fuelType);

   /**
    * Called when the Position attribute of a Car is updated
    *
    * @param id       the id of the Car whose name is updated
    * @param position the new Position attribute value
    */
   void carPositionUpdated(String id, Position position);

   /**
    * Called when the FuelLevel attribute of a Car is updated
    *
    * @param id        the id of the Car whose name is updated
    * @param fuelLevel the new FuelLevel attribute value
    */
   void carFuelLevelUpdated(String id, int fuelLevel);
}
