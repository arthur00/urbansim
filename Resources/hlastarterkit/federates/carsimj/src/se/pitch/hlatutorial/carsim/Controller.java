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

import hla.rti1516e.exceptions.RTIexception;
import se.pitch.hlatutorial.carsim.carsimulator.CarSimulator;
import se.pitch.hlatutorial.carsim.carsimulator.NoScenarioException;
import se.pitch.hlatutorial.carsim.hlamodule.HlaInterface;
import se.pitch.hlatutorial.carsim.hlamodule.InteractionListener;
import se.pitch.hlatutorial.carsim.model.Car;
import se.pitch.hlatutorial.carsim.model.CarFileUtil;
import se.pitch.hlatutorial.carsim.model.Position;

import java.io.File;
import java.io.IOException;


public class Controller {

   private HlaInterface _hlaInterface;
   private final CarSimulator _carSimulator = CarSimulator.Factory.newSimulator();

   private ScenarioThread _scenarioThread;

   private SimulationConfig _config;

   private State _state = State.getStartState();

   public void start() {
      try {
         _config = new SimulationConfig("Simulation.config");
      } catch (IOException e) {
         System.err.println("Could not read Simulation.config");
         return;
      }
      _hlaInterface = HlaInterface.Factory.newInterface(this);
      _hlaInterface.addInteractionListener(new InteractionListener() {
         public void receivedSetScenario(String scenarioName, int initialFuelLevel) {
            _state = _state.setScenario(scenarioName, initialFuelLevel, Controller.this);
         }

         public void receivedStart(float timeScaleFactor) {
            _state = _state.start(Controller.this, timeScaleFactor);
         }

         public void receivedStop() {
            _state = _state.stop(Controller.this);
         }
      });

      try {
         _hlaInterface.start(_config.getLocalSettingsDesignator(), _config.getFom(), _config.getFederationName(), _config.getFederateName());
      } catch (RTIexception e) {
         System.out.println("Could not connect to the RTI using the local settings designator \"" + _config.getLocalSettingsDesignator() + "\"");
         e.printStackTrace();
         return;
      }

      File dir = new File("cars/");
      if (dir.isDirectory()) {
         File[] files = dir.listFiles();
         if (files != null) {
            for (File file : files) {
               if (!file.isDirectory() && file.getName().endsWith(".car")) {
                  try {
                     Car configuration = CarFileUtil.createCar(file);
                     _carSimulator.simulateCar(configuration);
                     try {
                        _hlaInterface.createCar(configuration);
                     } catch (RTIexception e) {
                        System.out.println("Failed to register car");
                        e.printStackTrace();
                     }
                  } catch (IOException e) {
                     System.out.println("Failed to load Car from file " + file.getName());
                  }
               }
            }
         }
      }
      System.out.println(_config.getFederateName() + " has joined. Ready to receive scenario");
   }

   public void stop() {
      try {
         _hlaInterface.stop();
      } catch (RTIexception exception) {
         exception.printStackTrace();
      }
      if (_scenarioThread != null) {
         _scenarioThread.terminate();
      }
   }

   public boolean setScenario(String scenarioName, int initialFuelLevel) {
      String scenarioPath = _config.getScenarioDir() + File.separator + scenarioName + ".scenario";
      try {
         ScenarioFileUtil.Scenario scenario = ScenarioFileUtil.createScenario(new File(scenarioPath));
         _carSimulator.setScenario(new Position(scenario.getStartLat(), scenario.getStartLong()),
                                   new Position(scenario.getStopLat(), scenario.getStopLong()),
                                   initialFuelLevel);
         System.out.println("Loaded scenario: " + scenarioName);

         for (Car car : _carSimulator.getCars()) {
            try {
               _hlaInterface.updateCar(car);
            } catch (RTIexception e) {
               e.printStackTrace();
            }
         }

         _hlaInterface.sendScenarioLoaded();

         System.out.println("The following cars are standing by at the starting point:");
         for (Car car : _carSimulator.getCars()) {
            System.out.println(car.getName());
         }

         return true;
      } catch (IOException e) {
         System.out.println("Failed to load scenario: " + scenarioName + " from " + scenarioPath);
         try {
            _hlaInterface.sendScenarioLoadFailure("Failed to read scenario from disk.");
         } catch (RTIexception rtIexception) {
            System.out.println("Failed to send ScenarioLoadFailure interaction");
         }
      } catch (RTIexception e) {
         e.printStackTrace();
      }
      return false;
   }

   public void startSimulation(float timeScaleFactor) {
      if (_scenarioThread != null) {
         _scenarioThread.resumeSimulation(timeScaleFactor);
      } else {
         _scenarioThread = new ScenarioThread(_config.getUpdateRate());
         _scenarioThread.start();
         _scenarioThread.startSimulation(timeScaleFactor);
      }
      System.out.println("Started simulation");
   }

   public void stopSimulation() {
      _scenarioThread.pauseSimulation();
      System.out.println("Stopped simulation");
   }

   public Car getCar(String identifier) {
      return _carSimulator.getCar(identifier);
   }

   private class ScenarioThread extends Thread {

      private volatile boolean _terminated = false;

      private volatile boolean _isRunning = true;

      private int _advanceTimeStep;
      private final int _sleepTimeStep;

      private ScenarioThread(int frameRate) {
         _sleepTimeStep = 1000 / frameRate;
      }

      private void setTimeScaleFactor(float timeScaleFactor) {
         _advanceTimeStep = (int)timeScaleFactor * _sleepTimeStep;
      }

      void startSimulation(float timeScaleFactor) {
         setTimeScaleFactor(timeScaleFactor);
         _isRunning = true;
      }

      void pauseSimulation() {
         _isRunning = false;
      }

      void resumeSimulation(float timeScaleFactor) {
         setTimeScaleFactor(timeScaleFactor);
         _isRunning = true;
      }

      void terminate() {
         _terminated = true;
         interrupt();
      }

      public void run() {
         while (!_terminated) {

            if (_isRunning) {
               try {
                  _carSimulator.advanceTime(_advanceTimeStep);
               } catch (NoScenarioException ignored) {
                  //This should never happen since the States won't allow it
               }

               for (Car car : _carSimulator.getCars()) {
                  try {
                     _hlaInterface.updateCar(car);
                  } catch (RTIexception e) {
                     e.printStackTrace();
                  }
               }
            }

            try {
               Thread.sleep(_sleepTimeStep);
            } catch (InterruptedException ignored) {
            }
         }
         _isRunning = false;
      }
   }
}
