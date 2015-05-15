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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


public class DataModel {

   private final List<DataModelListener> _listeners = new CopyOnWriteArrayList<DataModelListener>();

   private final Map<String, Car> _cars = new HashMap<String, Car>();


   public void addListener(DataModelListener carListener) {
      _listeners.add(carListener);
   }

   public void carAdded(Car car) {
      _cars.put(car.getIdentifier(), car);
      for (DataModelListener listener : _listeners) {
         listener.carAdded(car);
      }
   }

   public void carRemoved(String id) {
      Car car = _cars.get(id);
      _cars.remove(id);
      for (DataModelListener listener : _listeners) {
         listener.carRemoved(car);
      }
   }

   public void carNameUpdated(String id, String name) {
      _cars.get(id).setName(name);
      fireCarUpdated(id);
   }

   public void carLicensePlateNumberUpdated(String id, String licensePlateNumber) {
      _cars.get(id).setLicensePlateNumber(licensePlateNumber);
      fireCarUpdated(id);
   }

   public void carFuelTypeUpdated(String id, FuelType fuelType) {
      _cars.get(id).setFuelType(fuelType);
      fireCarUpdated(id);
   }

   public void carPositionUpdated(String id, int x, int y) {
      _cars.get(id).setX(x);
      _cars.get(id).setY(y);
      fireCarUpdated(id);
   }

   public void carFuelLevelUpdated(String id, int fuelLevel) {
      _cars.get(id).setFuelLevel(fuelLevel);
      fireCarUpdated(id);
   }

   private void fireCarUpdated(String identifier) {
      Car car = _cars.get(identifier);
      for (DataModelListener listener : _listeners) {
         listener.updated(car);
      }
   }

   public void setScenario(Scenario scenario) {
      for (DataModelListener l : _listeners) {
         l.setScenario(scenario);
      }
   }

   public void scenarioStared(float timeScaleFactor) {
      for (DataModelListener l : _listeners) {
         l.scenarioStared(timeScaleFactor);
      }
   }

   public void scenarioStopped() {
      for (DataModelListener l : _listeners) {
         l.scenarioStopped();
      }
   }
}
