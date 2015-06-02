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


abstract class State {

   private State() {
   }

   abstract State setScenario(String scenarioName, int initialFuelAmount, Controller federate);

   abstract State start(Controller federate, float timeScaleFactor);

   abstract State stop(Controller federate);

   public static State getStartState() {
      return NoScenarioState.getState();
   }

   private static class NoScenarioState extends State {
      private static State _state;

      private static State getState() {
         if (_state == null) {
            _state = new NoScenarioState();
         }
         return _state;
      }

      private NoScenarioState() {
      }

      public State setScenario(String scenarioName, int initialFuelAmount, Controller federate) {
         if (federate.setScenario(scenarioName, initialFuelAmount)) {
            return ScenarioLoadedState.getState();
         } else {
            return this;
         }
      }

      public State start(Controller federate, float timeScaleFactor) {
         System.out.println("Cannot start without scenario");
         return this;
      }

      public State stop(Controller federate) {
         System.out.println("Cannot stop when not running");
         return this;
      }
   }

   private static class ScenarioLoadedState extends State {

      private static State _state;

      private static State getState() {
         if (_state == null) {
            _state = new ScenarioLoadedState();
         }
         return _state;
      }

      private ScenarioLoadedState() {
      }

      public State setScenario(String scenarioName, int initialFuelAmount, Controller federate) {
         federate.setScenario(scenarioName, initialFuelAmount);
         return this;
      }

      public State start(Controller federate, float timeScaleFactor) {
         federate.startSimulation(timeScaleFactor);
         return RunningState.getState();
      }

      public State stop(Controller federate) {
         System.out.println("Cannot stop when not running");
         return this;
      }
   }

   private static class RunningState extends State {

      private static State _state;

      private static State getState() {
         if (_state == null) {
            _state = new RunningState();
         }
         return _state;
      }

      public State setScenario(String scenarioName, int initialFuelAmount, Controller federate) {
         System.out.println("Cannot set scenario when running");
         return this;
      }

      public State start(Controller federate, float timeScaleFactor) {
         System.out.println("Cannot start when already running");
         return this;
      }

      public State stop(Controller federate) {
         federate.stopSimulation();
         return ScenarioLoadedState.getState();
      }
   }

}
