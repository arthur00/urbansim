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


public class Position {

   private final double _latitude;
   private final double _longitude;

   public Position(double latitude, double longitude) {
      _latitude = latitude;
      _longitude = longitude;
   }

   public double getLatitude() {
      return _latitude;
   }

   public double getLongitude() {
      return _longitude;
   }

   @Override
   public String toString() {
      return "Position[" + _latitude + ", " + _longitude + "]";
   }
}
