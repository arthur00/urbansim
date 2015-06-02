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
#ifndef SIMULATOR_LISTENER
#define SIMULATOR_LISTENER

#include <boost/shared_ptr.hpp>

class Simulator;
class SimulatorListener;
typedef boost::shared_ptr<SimulatorListener> SimulatorListenerPtr;

/**
 * Interface specification for handling Simulator events.
 *
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class SimulatorListener {
public:

    /**
     * Notification from a Simulator telling it has started simulating.
     * Be careful to do time consuming work in this callback. Consider using
     * a new thread instead.
     *
     * @param simulator - pointer to the Simulator object
     */
    virtual void stoppedSimulating(Simulator* simulator) = 0;

    /**
     * Notification from a Simulator telling it has stopped simulating.
     * Be careful to do time consuming work in this callback. Consider using
     * a new thread instead.
     *
     * @param simulator - pointer to the Simulator object
     */
    virtual void startedSimulating(Simulator* simulator) = 0;

    /**
     * Notification from a Simulator telling it's state has been updated.
     * Be careful to do time consuming work in this callback. Consider using
     * a new thread instead.
     *
     * @param simulator - pointer to the Simulator object
     */
    virtual void updatedState(Simulator* simulator)      = 0;
};

#endif
