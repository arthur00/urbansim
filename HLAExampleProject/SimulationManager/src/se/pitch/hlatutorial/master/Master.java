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
package se.pitch.hlatutorial.master;

import hla.rti1516e.exceptions.RTIexception;
import se.pitch.hlatutorial.master.hlamodule.HlaInterface;
import se.pitch.hlatutorial.master.hlamodule.InteractionListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class Master {

   public static void main(String[] args) {

      SimulationConfig config;
      try {
         config = new SimulationConfig("Simulation.config");
      } catch (IOException e) {
         System.out.println("Could not read Simulation.config");
         return;
      }
      HlaInterface hlaInterface = HlaInterface.Factory.newInterface();

      try {
         hlaInterface.start(config.getLocalSettingsDesignator(), config.getFom(), config.getFederationName(), config.getFederateName());
      } catch (RTIexception e) {
         System.out.println("Could not connect to CRC.");
         e.printStackTrace();
         return;
      }

      System.out.println("Welcome to the Master Federate of the Fuel Economy Federation");
      System.out.println("Make sure that your desired federates have joined the federation!");

      List<String> scenarios = new ArrayList<String>();
      File dir = new File(config.getScenarioDir() + File.separator);
      if (dir.isDirectory()) {
         File[] files = dir.listFiles();
         if (files != null) {
            for (File file : files) {
               if (!file.isDirectory() && file.getName().endsWith(".scenario")) {
                  scenarios.add(file.getName().substring(0, file.getName().length() - 9));
               }
            }
         }
      }

      System.out.println("");
      printOptions(scenarios);

      BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));

      hlaInterface.addInteractionListener(new InteractionListener() {
         public void receivedScenarioLoadedInteraction(String federateName) {
            System.out.println(federateName + " loaded scenario");
            System.out.print(">");
         }

         public void receivedScenarioLoadFailureInteraction(String federateName, String errorMessage) {
            System.out.println(federateName + " failed to load scenario: " + errorMessage);
            System.out.println(">");
         }
      });
      while (true) {
         System.out.print(">");
         try {
            String in = inputReader.readLine();
            if (in.equalsIgnoreCase("q")) {
               hlaInterface.stop();
               System.exit(0);
            }
            try {
               int number = Integer.parseInt(in);

               int numberOfScenarios = scenarios.size();
               if (number <= numberOfScenarios) {
                  while (true) {
                     System.out.println("Enter the initial fuel amount in liters:");
                     in = inputReader.readLine();
                     try {
                        hlaInterface.sendLoadScenario(scenarios.get(number-1), Integer.parseInt(in));
                        break;
                     } catch (NumberFormatException e) {
                        System.out.println("Fuel amount should be an integer.");
                     }
                  }
                  continue;
               } else if (number == numberOfScenarios + 1) {
                  while (true) {
                     System.out.println("Enter time scale: (Enter gives real time)");
                     in = inputReader.readLine();
                     try {
                        hlaInterface.sendStart(in.isEmpty() ? 1.0f : Float.parseFloat(in));
                        break;
                     } catch (NumberFormatException e) {
                        System.out.println("Time scale should be a decimal number.");
                     }
                  }
                  continue;
               } else if (number == numberOfScenarios + 2) {
                  hlaInterface.sendStop();
                  continue;
               }
            } catch (NumberFormatException ignore) {
            }
            System.out.println("Not a valid option.");
            printOptions(scenarios);
         } catch (IOException ignore) {
         } catch (RTIexception e) {
            System.out.println("Encountered exception: " + e.getMessage());
         }
      }
   }

   private static void printOptions(List<String> availableScenarios) {
      System.out.println("Select a command");
      int i = 1;
      for (String scenario : availableScenarios) {
         System.out.println(i + ". Load the " + scenario + " scenario");
         i++;
      }
      System.out.println(i + ". Start simulating");
      i++;
      System.out.println(i + ". Stop simulating");
      System.out.println("Q. Quit the Master Federate");
   }

}
