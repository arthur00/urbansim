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

import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import se.pitch.hlatutorial.mapviewer.model.FuelType;


class FuelTypeEnum8Coder {
   private enum FuelTypeEnum8 {
      UNKNOWN(0), GASOLINE(1), DIESEL(2), ETHANOL_FLEXIBLE_FUEL(3), NATURAL_GAS(4);

      private final int _value;

      private FuelTypeEnum8(int value) {
         _value = value;
      }

      public int getValue() {
         return _value;
      }

      public static FuelTypeEnum8 find(int value) {
         for (FuelTypeEnum8 fuelType : values()) {
            if (fuelType.getValue() == value) {
               return fuelType;
            }
         }
         return FuelTypeEnum8.UNKNOWN;
      }
   }

   private final HLAinteger32BE _coder;

   FuelTypeEnum8Coder(EncoderFactory encoderFactory) {
      _coder = encoderFactory.createHLAinteger32BE();
   }

   byte[] encode(FuelType fuelType) {
      _coder.setValue(translate(fuelType).getValue());
      return _coder.toByteArray();
   }

   FuelType decode(byte[] bytes) throws DecoderException {
      _coder.decode(bytes);
      return translate(FuelTypeEnum8.find(_coder.getValue()));
   }

   private static FuelType translate(FuelTypeEnum8 fuelType) {
      switch (fuelType) {
         case GASOLINE:
            return FuelType.GASOLINE;
         case DIESEL:
            return FuelType.DIESEL;
         case ETHANOL_FLEXIBLE_FUEL:
            return FuelType.ETHANOL_FLEXIBLE_FUEL;
         case NATURAL_GAS:
            return FuelType.NATURAL_GAS;
      }
      return FuelType.UNKNOWN;
   }

   private static FuelTypeEnum8 translate(FuelType fuelType) {
      switch (fuelType) {
         case GASOLINE:
            return FuelTypeEnum8.GASOLINE;
         case DIESEL:
            return FuelTypeEnum8.DIESEL;
         case ETHANOL_FLEXIBLE_FUEL:
            return FuelTypeEnum8.ETHANOL_FLEXIBLE_FUEL;
         case NATURAL_GAS:
            return FuelTypeEnum8.NATURAL_GAS;
      }
      return FuelTypeEnum8.UNKNOWN;
   }
}
