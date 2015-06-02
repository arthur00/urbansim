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


public class Car {

   private static class ColorCreator {
      private int i = 0;
      private final Color[] colors = new Color[] {
            new Color(142, 225, 223),
            new Color(228, 154, 182),
            new Color(82, 224, 171),
            new Color(234, 228, 134),
            new Color(247, 176, 70),
            new Color(141, 137, 231),
            new Color(66, 163, 61),
            new Color(168, 94, 168)};

      Color getNextColor() {
         if (i == colors.length) {
            i = 0;
         }
         return colors[i++];
      }
   }

   private final static ColorCreator _colorCreator = new ColorCreator();

   private static int carCount = 0;

   private final String _identifier;

   private String _name;
   private String _licensePlateNumber;
   private FuelType _fuelType;

   private int _fuelLevel;

   private int _x = -50;
   private int _y = -50;

   private final Color _color;

   public Car() {
      _identifier = "Car" + carCount;
      carCount++;
      _color = _colorCreator.getNextColor();
   }

   public Car(String name, String licensePlateNumber, FuelType fuelType, int fuelLevel, int x, int y) {
      this();
      _name = name;
      _licensePlateNumber = licensePlateNumber;
      _fuelType = fuelType;
      _fuelLevel = fuelLevel;
      _x = x;
      _y = y;
   }

   public String getIdentifier() {
      return _identifier;
   }

   public String getName() {
      return _name;
   }

   public String getLicensePlateNumber() {
      return _licensePlateNumber;
   }

   public FuelType getFuelType() {
      if (_fuelType == null) {
         return FuelType.UNKNOWN;
      }
      return _fuelType;
   }

   public int getFuelLevel() {
      return _fuelLevel;
   }

   public int getX() {
      return _x;
   }

   public int getY() {
      return _y;
   }

   public Color getColor() {
      return _color;
   }

   public void setName(String name) {
      _name = name;
   }

   public void setLicensePlateNumber(String licensePlateNumber) {
      _licensePlateNumber = licensePlateNumber;
   }

   public void setFuelType(FuelType fuelType) {
      _fuelType = fuelType;
   }

   public void setFuelLevel(int fuelLevel) {
      _fuelLevel = fuelLevel;
   }

   public void setX(int x) {
      _x = x;
   }

   public void setY(int y) {
      _y = y;
   }
}
