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
package se.pitch.hlatutorial.mapviewer;


class LatLongHelper {

   private final double _topLeftLat;
   private final double _topLeftLong;
   private final double _bottomRightLat;
   private final double _bottomRightLong;

   private final int _width;
   private final int _height;

   LatLongHelper(double topLeftLat, double topLeftLong, double bottomRightLat, double bottomRightLong, int width, int height) {
      _topLeftLat = topLeftLat;
      _topLeftLong = topLeftLong;
      _bottomRightLat = bottomRightLat;
      _bottomRightLong = bottomRightLong;

      _width = width;
      _height = height;
   }

   int convertLatToY(double latitude) {
      return (int)(_height / (_bottomRightLat - _topLeftLat) * (latitude - _topLeftLat));
   }


   int convertLongToX(double longitude) {
      return (int)((_width / (_bottomRightLong - _topLeftLong)) * (longitude - _topLeftLong));
   }

}
