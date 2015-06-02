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

import hla.rti1516e.exceptions.RTIexception;
import se.pitch.hlatutorial.mapviewer.gui.MapFrame;
import se.pitch.hlatutorial.mapviewer.hlamodule.CarListener;
import se.pitch.hlatutorial.mapviewer.hlamodule.HlaInterface;
import se.pitch.hlatutorial.mapviewer.hlamodule.InteractionListener;
import se.pitch.hlatutorial.mapviewer.model.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


public class MapViewer {

   private LatLongHelper _latLongHelper;
   
   private final DataModel _dataModel = new DataModel();

   public MapViewer() {

      try {
         UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Throwable e) {
         System.err.println("Failed to set Look & Feel: " + e.toString());
      }

      final MapFrame frame = new MapFrame(_dataModel);
      frame.setVisible(true);

      final SimulationConfig config;
      try {
         config = new SimulationConfig("Simulation.config");
      } catch (IOException e) {
         System.out.println("Could not read Simulation.config");
         return;
      }

      final HlaInterface hlaInterface = HlaInterface.Factory.newInterface();
      frame.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            try {
               hlaInterface.stop();
               System.exit(0);
            } catch (RTIexception ignored) {
            }
         }
      });

      hlaInterface.addInteractionListener(new InteractionListener() {
         public void receivedSetScenario(String scenarioName, int initialFuelLevel) {
            String scenarioPath = config.getScenarioDir() + File.separator + scenarioName + ".scenario";
            try {
               ScenarioFileUtil.Scenario scenario = ScenarioFileUtil.createScenario(new File(scenarioPath));
               String imagePath = config.getScenarioDir() + File.separator + scenario.getMap();
               try {
                  BufferedImage image = ImageIO.read(new File(imagePath));

                  _latLongHelper = new LatLongHelper(scenario.getTopLeftLat(),
                                                     scenario.getTopLeftLong(),
                                                     scenario.getBottomRightLat(),
                                                     scenario.getBottomRightLong(),
                                                     image.getWidth(),
                                                     image.getHeight());
                  Point start = new Point(_latLongHelper.convertLongToX(scenario.getStartLong()), _latLongHelper.convertLatToY(scenario.getStartLat()));
                  Point goal = new Point(_latLongHelper.convertLongToX(scenario.getStopLong()), _latLongHelper.convertLatToY(scenario.getStopLat()));
                  _dataModel.setScenario(new Scenario(image, start, goal, initialFuelLevel));
                  System.out.println("Loaded scenario " + scenarioName);
                  try {
                     hlaInterface.sendScenarioLoaded();
                  } catch (RTIexception e) {
                     System.out.println("Failed to send ScenarioLoaded interaction");
                  }
                  return;
               } catch (IOException e) {
                  System.out.println("Failed to load scenario image: " + scenarioName + " from " + imagePath);
               }
            } catch (IOException e) {
               System.out.println("Failed to load scenario: " + scenarioName + " from " + scenarioPath);
            }
            try {
               hlaInterface.sendScenarioLoadFailure("Failed to read scenario from disk.");
            } catch (RTIexception rtIexception) {
               System.out.println("Failed to send ScenarioLoadFailure interaction");
            }
         }

         public void receivedStart(float timeScaleFactor) {
            _dataModel.scenarioStared(timeScaleFactor);
         }

         public void receivedStop() {
            _dataModel.scenarioStopped();
         }
      });

       hlaInterface.addCarListener( new CarListener() {
         public void carAdded(Car car) {
            _dataModel.carAdded(car);
         }

         public void carRemoved(String id) {
            _dataModel.carRemoved(id);
         }

         public void carNameUpdated(String id, String name) {
            _dataModel.carNameUpdated(id, name);
         }

         public void carLicensePlateNumberUpdated(String id, String licensePlateNumber) {
            _dataModel.carLicensePlateNumberUpdated(id, licensePlateNumber);
         }

         public void carFuelTypeUpdated(String id, FuelType fuelType) {
            _dataModel.carFuelTypeUpdated(id, fuelType);
         }


          public void carPositionUpdated(String id, Position position) {
            if (_latLongHelper != null) {
               _dataModel.carPositionUpdated(id, _latLongHelper.convertLongToX(position.getLongitude()), _latLongHelper.convertLatToY(position.getLatitude()));
            }
         }

         public void carFuelLevelUpdated(String id, int fuelLevel) {
            _dataModel.carFuelLevelUpdated(id, fuelLevel);
         }
      });

      try {
         hlaInterface.start(config.getLocalSettingsDesignator(), config.getFom(), config.getFederationName(), config.getFederateName());
      } catch (RTIexception e) {
         System.out.println("Could not connect to the RTI using the local settings designator \"" + config.getLocalSettingsDesignator() + "\"");
         e.printStackTrace();
         System.exit(0);
      }
   }

   public static void main(String[] args) {
      new MapViewer();
   }
}
