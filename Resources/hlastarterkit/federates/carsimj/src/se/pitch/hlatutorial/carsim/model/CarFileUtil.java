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
package se.pitch.hlatutorial.carsim.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class CarFileUtil {

   private CarFileUtil() {
   }

   private static final String NAME = "Name";
   private static final String LICENSE_PLATE = "LicensePlate";
   private static final String FUEL_TYPE = "FuelType";
   private static final String NORMAL_SPEED = "NormalSpeed";
   private static final String LITERS_PER_100_KM_1 = "LitersPer100km1";
   private static final String LITERS_PER_100_KM_2 = "LitersPer100km2";
   private static final String LITERS_PER_100_KM_3 = "LitersPer100km3";

   public static CarImpl createCar(File file) throws IOException {
      Properties properties = new Properties();
      properties.load(new FileInputStream(file));

      return new CarImpl(properties.getProperty(NAME),
                         properties.getProperty(LICENSE_PLATE),
                         FuelType.find(properties.getProperty(FUEL_TYPE)),
                         Integer.parseInt(properties.getProperty(NORMAL_SPEED)),
                         ((double)Integer.parseInt(properties.getProperty(LITERS_PER_100_KM_1))) / 100.0,
                         ((double)Integer.parseInt(properties.getProperty(LITERS_PER_100_KM_2))) / 100.0,
                         ((double)Integer.parseInt(properties.getProperty(LITERS_PER_100_KM_3))) / 100.0);
   }
}
