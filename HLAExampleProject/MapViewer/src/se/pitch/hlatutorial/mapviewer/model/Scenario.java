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

import java.awt.*;
import java.awt.image.BufferedImage;


public class Scenario {

   private final BufferedImage _map;

   private final Point _startPosition;
   private final Point _goalPosition;

   private final int _initialFuelAmount;

   public Scenario(BufferedImage map, Point startPosition, Point goalPosition, int initialFuelAmount) {
      _map = map;
      _startPosition = startPosition;
      _goalPosition = goalPosition;
      _initialFuelAmount = initialFuelAmount;
   }

   public BufferedImage getMap() {
      return _map;
   }

   public Point getStartPosition() {
      return _startPosition;
   }

   public Point getGoalPosition() {
      return _goalPosition;
   }

   public int getInitialFuelAmount() {
      return _initialFuelAmount;
   }
}
