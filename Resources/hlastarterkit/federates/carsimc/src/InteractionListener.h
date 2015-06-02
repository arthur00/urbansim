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
#ifndef INTERACTION_LISTENER
#define INTERACTION_LISTENER

/**
 * Interface specification for handling a set of HLA interactions.
 *
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
#include <string>

#include <boost/shared_ptr.hpp>

class InteractionListener;
typedef boost::shared_ptr<InteractionListener> InteractionListenerPtr;

class InteractionListener {
public:

    /**
     * The listener received a Start Interaction.
     * Be careful to do time consuming work in this callback. Consider using
     * a new thread instead.
     *
     * @param timeScaleFactor - a positive double precision value
     */
    virtual void receivedStart(double timeScaleFactor) = 0;

    /**
     * The listener received a Stop Interaction.
     * Be careful to do time consuming work in this callback. Consider using
     * a new thread instead.
     */
    virtual void receivedStop() = 0;

    /**
     * The listener received a Quit Interaction.
     * Be careful to do time consuming work in this callback. Consider using
     * a new thread instead.
     */
    virtual void receivedQuit() = 0;

    /**
     * The listener received a Load Scenario Interaction.
     * Be careful to do time consuming work in this callback. Consider using
     * a new thread instead.
     *
     * @param scenario - the name of the scenario to load
     * @param initialFuelLevel - the amount of fuel to start with
     */
    virtual void receivedLoadScenario(const std::wstring& scenario, double initialFuelLevel) = 0;
};

#endif
