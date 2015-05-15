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


public enum FuelType {
   GASOLINE("Gasoline"),
   DIESEL("Diesel"),
   ETHANOL_FLEXIBLE_FUEL("EthanolFlexibleFuel"),
   NATURAL_GAS("NaturalGas"),
   UNKNOWN("Unknown");

   private final String _name;

   private FuelType(String name) {
      _name = name;
   }

   public String getName() {
      return _name;
   }

   public static FuelType find(String name) {
      for (FuelType fuel : values()) {
         if (fuel._name.equals(name)) {
            return fuel;
         }
      }
      return UNKNOWN;
   }
}
