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
#include <iostream>
#include <memory>

#include "Controller.h"
#include "SimulatorConfig.h"
#include "CarFactory.h"

using namespace std;

/**
 * The main file for the C++ car simulator.
 * CarSimC creates and initializes all required modules and
 * starts the simulator.
 *
 * @author Steve Eriksson steve.eriksson@pitch.se 
 * @author Mikael Karlsson mikael.karlsson@pitch.se
 */


void startSimulation(SimulatorConfig const & simConf) 
{
   boost::shared_ptr<Simulator>  simulator(new Simulator(simConf));
   boost::shared_ptr<HLAmodule>  hlaModule(new HLAmodule(simConf));
   boost::shared_ptr<CarFactory> carFactory(new CarFactory(simConf.getCarsDir()));

   Controller ctr(carFactory, simulator, hlaModule);
   ctr.start();
}

void waitForKey()
{
   int in;
   cin >> in;
}

int loadConfigAndRunSimulation()
{
   SimulatorConfig simConf(L"simulator.config");

   if (simConf.failedToLoad()) {
      cout << "Simulator configuration is bad" << endl;
      return 1;
   } else {
      startSimulation(simConf);
      return 0;
   }
}

int main(int argc, char** argv)
{
    srand(static_cast<unsigned int>(time(0))); // initialize random generator
    int result = loadConfigAndRunSimulation();
    return result;
}

