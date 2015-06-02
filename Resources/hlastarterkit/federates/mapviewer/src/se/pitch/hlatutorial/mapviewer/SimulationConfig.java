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


class SimulationConfig {

   private static final String LOCAL_SETTINGS_DESIGNATOR = "localSettingsDesignator";
   private static final String FEDERATION_NAME = "federationName";
   private static final String FEDERATE_NAME = "federateName";

   private static final String SCENARIO_DIR = "scenarioDir";
   private static final String FOM = "fom";

   private final String _localSettingsDesignator;
   private final String _federationName;
   private final String _federateName;

   private final String _scenarioDir;
   private final String _fom;

   public SimulationConfig(String fileName) throws IOException {
      this(new File(fileName));
   }

   public SimulationConfig(File file) throws IOException {
      Properties properties = new Properties();
      properties.load(new FileInputStream(file));

      _localSettingsDesignator = properties.getProperty(LOCAL_SETTINGS_DESIGNATOR, "");
      _federationName = properties.getProperty(FEDERATION_NAME, "HLA Tutorial");
      _federateName = properties.getProperty(FEDERATE_NAME, "CarSimJ");

      _scenarioDir = properties.getProperty(SCENARIO_DIR, ".");
      _fom = properties.getProperty(FOM, "FuelEconomyBase.xml");
   }

   public String getLocalSettingsDesignator() {
      return _localSettingsDesignator;
   }

   public String getFederationName() {
      return _federationName;
   }

   public String getFederateName() {
      return _federateName;
   }

   public String getScenarioDir() {
      return _scenarioDir;
   }

   public String getFom() {
      return _fom;
   }
}
