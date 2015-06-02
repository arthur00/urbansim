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
                          Double.parseDouble(properties.getProperty(TOP_LEFT_LAT)),
                          Double.parseDouble(properties.getProperty(TOP_LEFT_LONG)),
                          Double.parseDouble(properties.getProperty(BOTTOM_RIGHT_LAT)),
                          Double.parseDouble(properties.getProperty(BOTTOM_RIGHT_LONG)),
                          Double.parseDouble(properties.getProperty(START_LAT)),
                          Double.parseDouble(properties.getProperty(START_LONG)),
                          Double.parseDouble(properties.getProperty(STOP_LAT)),
                          Double.parseDouble(properties.getProperty(STOP_LONG)));
   }

   static class Scenario {
      private final String _map;
      private final double _topLeftLat;
      private final double _topLeftLong;
      private final double _bottomRightLat;
      private final double _bottomRightLong;

      private final double _startLat;
      private final double _startLong;
      private final double _stopLat;
      private final double _stopLong;

      Scenario(String map, double topLeftLat, double topLeftLong, double bottomRightLat, double bottomRightLong, double startLat, double startLong, double stopLat, double stopLong) {
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

      public double getTopLeftLat() {
         return _topLeftLat;
      }

      public double getTopLeftLong() {
         return _topLeftLong;
      }

      public double getBottomRightLat() {
         return _bottomRightLat;
      }

      public double getBottomRightLong() {
         return _bottomRightLong;
      }

      public double getStartLat() {
         return _startLat;
      }

      public double getStartLong() {
         return _startLong;
      }

      public double getStopLat() {
         return _stopLat;
      }

      public double getStopLong() {
         return _stopLong;
      }
   }
}
