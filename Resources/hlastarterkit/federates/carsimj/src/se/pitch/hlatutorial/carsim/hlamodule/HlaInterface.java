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
package se.pitch.hlatutorial.carsim.hlamodule;

import hla.rti1516e.exceptions.*;
import se.pitch.hlatutorial.carsim.Controller;
import se.pitch.hlatutorial.carsim.model.Car;


public interface HlaInterface {

   /**
    * Connect to a CRC and join federation
    *
    * @param localSettingsDesignator The name to load settings for or "" to load default settings
    * @param fomPath                 path to FOM file
    * @param federationName          Name of the federation to join
    * @param federateName            The name you want for your federate
    */
   void start(String localSettingsDesignator, String fomPath, String federationName, String federateName)
         throws FederateNotExecutionMember,
                RestoreInProgress,
                SaveInProgress,
                NotConnected,
                FederateServiceInvocationsAreBeingReportedViaMOM,
                RTIinternalError,
                ConnectionFailed,
                InvalidLocalSettingsDesignator,
                ErrorReadingFDD,
                CouldNotOpenFDD,
                InconsistentFDD;

   /**
    * Resign and disconnect from CRC
    */
   void stop() throws RTIinternalError;


   /*****************************************************************************
    * Interactions
    ****************************************************************************/

   /**
    * Add an InteractionListener to receive notifications when Interactions are received
    *
    * @param listener an InteractionListener
    */
   void addInteractionListener(InteractionListener listener);

   /**
    * Remove a previously added InteractionListener
    *
    * @param listener an InteractionListener
    */
   void removeInteractionListener(InteractionListener listener);

   /**
    * Send a ScenarioLoaded interaction
    */
   void sendScenarioLoaded()
         throws FederateNotExecutionMember,
                NotConnected,
                RestoreInProgress,
                SaveInProgress,
                RTIinternalError;

   /**
    * Send a ScenarioLoadFailure interaction to the federation
    *
    * @param errorMessage A string detailing why the scenario failed to load.
    */
   void sendScenarioLoadFailure(String errorMessage)
         throws FederateNotExecutionMember,
                NotConnected,
                RestoreInProgress,
                SaveInProgress,
                RTIinternalError;


   /*****************************************************************************
    * Objects
    ****************************************************************************/

   /**
    * Create a new Car in the federation
    *
    * @param state The static state of the Car
    */
   void createCar(Car state)
         throws FederateNotExecutionMember,
                RestoreInProgress,
                SaveInProgress,
                NotConnected,
                RTIinternalError;

   /**
    * Remove a new Car from the federation
    *
    * @param state The static state of the Car
    */
   void removeCar(Car state)
         throws FederateNotExecutionMember,
                RestoreInProgress,
                SaveInProgress,
                NotConnected,
                RTIinternalError;

   /**
    * Update the Car
    *
    * @param state The new dynamic state of the Car
    */
   void updateCar(Car state)
         throws FederateNotExecutionMember,
                NotConnected,
                RestoreInProgress,
                SaveInProgress,
                RTIinternalError;


   public static class Factory {
      public static HlaInterface newInterface(Controller controller) {
         return new HlaInterfaceImpl(controller);
      }
   }
}
