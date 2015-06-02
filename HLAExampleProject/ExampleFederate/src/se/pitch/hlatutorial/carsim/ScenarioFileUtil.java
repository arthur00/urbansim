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
package se.pitch.hlatutorial.carsim;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


class ScenarioFileUtil {

   private ScenarioFileUtil() {
   }

   private static final String MAP = "Map";
   private static final String TOP_LEFT_LAT = "TopLeftLat";
   private static final String TOP_LEFT_LONG = "TopLeftLong";
   private static final String BOTTOM_RIGHT_LAT = "BottomRightLat";
   private static final String BOTTOM_RIGHT_LONG = "BottomRightLong";

   private static final String START_LAT = "StartLat";
   private static final String START_LONG = "StartLong";
   private static final String STOP_LAT = "StopLat";
   private static final String STOP_LONG = "StopLong";

   public static Scenario createScenario(File file) throws IOException {
      Properties properties = new Properties();
      properties.load(new FileInputStream(file));

      return new Scenario(properties.getProperty(MAP),
                          Float.parseFloat(properties.getProperty(TOP_LEFT_LAT)),
                          Float.parseFloat(properties.getProperty(TOP_LEFT_LONG)),
                          Float.parseFloat(properties.getProperty(BOTTOM_RIGHT_LAT)),
                          Float.parseFloat(properties.getProperty(BOTTOM_RIGHT_LONG)),
                          Float.parseFloat(properties.getProperty(START_LAT)),
                          Float.parseFloat(properties.getProperty(START_LONG)),
                          Float.parseFloat(properties.getProperty(STOP_LAT)),
                          Float.parseFloat(properties.getProperty(STOP_LONG)));
   }

   static class Scenario {
      private final String _map;
      private final float _topLeftLat;
      private final float _topLeftLong;
      private final float _bottomRightLat;
      private final float _bottomRightLong;

      private final float _startLat;
      private final float _startLong;
      private final float _stopLat;
      private final float _stopLong;

      Scenario(String map, float topLeftLat, float topLeftLong, float bottomRightLat, float bottomRightLong, float startLat, float startLong, float stopLat, float stopLong) {
         _map = map;
         _topLeftLat = topLeftLat;
         _topLeftLong = topLeftLong;
         _bottomRightLat = bottomRightLat;
         _bottomRightLong = bottomRightLong;
         _startLat = startLat;
         _startLong = startLong;
         _stopLat = stopLat;
         _stopLong = stopLong;
      }

      public String getMap() {
         return _map;
      }

      public float getTopLeftLat() {
         return _topLeftLat;
      }

      public float getTopLeftLong() {
         return _topLeftLong;
      }

      public float getBottomRightLat() {
         return _bottomRightLat;
      }

      public float getBottomRightLong() {
         return _bottomRightLong;
      }

      public float getStartLat() {
         return _startLat;
      }

      public float getStartLong() {
         return _startLong;
      }

      public float getStopLat() {
         return _stopLat;
      }

      public float getStopLong() {
         return _stopLong;
      }
   }
}
