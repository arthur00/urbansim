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

import se.pitch.hlatutorial.carsim.model.Position;


class LatLongHelper {

   private static final float EARTH_RADIUS = 6372.7976f; // km

   public static Position newPosition(Position position, Position destination, double km) {
      return newPosition(position.getLatitude(), position.getLongitude(), destination.getLatitude(), destination.getLongitude(), km);
   }

   private static Position newPosition(double lat1, double long1, double lat2, double long2, double km) {
      if (distance(lat1, long1, lat2, long2) <= km) {
         return (new Position(lat2, long2));
      }
      double bearing = bearing(lat1, long1, lat2, long2);
      return newPosition(lat1, long1, km, bearing);
   }

   private static Position newPosition(double currentLat, double currentLong, double km, double bearing) {
      double dr = km / EARTH_RADIUS;
      double lat1 = toRad(currentLat);
      double long1 = toRad(currentLong);

      double destinationLat = Math.asin(Math.sin(lat1) * Math.cos(dr) + Math.cos(lat1) * Math.sin(dr) * Math.cos(toRad(bearing)));
      double destinationLong = long1 + Math.atan2(Math.sin(toRad(bearing)) * Math.sin(dr) * Math.cos(lat1),
                                                  Math.cos(dr) - Math.sin(lat1) * Math.sin(destinationLat));
      return new Position(toDeg(destinationLat), toDeg(destinationLong));
   }

   private static double toRad(double deg) {
      return deg * (Math.PI / 180);
   }

   private static double toDeg(double rad) {
      return rad * (180 / Math.PI);
   }

   private static double bearing(double currentLat, double currentLong, double goalLat, double goalLong) {
      double lat1 = currentLat * (Math.PI / 180);
      double long1 = currentLong * (Math.PI / 180);
      double lat2 = goalLat * (Math.PI / 180);
      double long2 = goalLong * (Math.PI / 180);

      double dLong = long2 - long1;
      double y = Math.sin(dLong) * Math.cos(lat2);
      double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLong);
      return (Math.atan2(y, x) * (180 / Math.PI) + 360) % 360;
   }

   public static double distance(Position pos1, Position pos2) {
      return distance(pos1.getLatitude(), pos1.getLongitude(), pos2.getLatitude(), pos2.getLongitude());
   }

   private static double distance(double lat1, double long1, double lat2, double long2) {
      return Math.acos(Math.sin(toRad(lat1)) * Math.sin(toRad(lat2)) +
                       Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
                       Math.cos(toRad(long2) - toRad(long1))) * EARTH_RADIUS;
   }
}
