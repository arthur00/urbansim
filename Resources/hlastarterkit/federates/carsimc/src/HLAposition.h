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
#ifndef HLA_POSITION_H
#define HLA_POSITION_H

#include <RTI/encoding/BasicDataElements.h>
#include <RTI/encoding/HLAvariantRecord.h>
#include <RTI/encoding/HLAfixedRecord.h>
#include <RTI/VariableLengthData.h>

#include "Position.h"

/**
 * The HLAfuelType class handles encoding and decoding of Position data.
 *
 * @see Position
 * @author Steve Eriksson steve.eriksson@pitch.se
 */ 
class HLAposition {
public:
    HLAposition();

    /**
     *  Encode the Position as HLA variable length data.
     *
     * @param position - the position to encode
     */
    rti1516e::VariableLengthData encode(const Position& position);

    /**
     * Encode the Position as HLA variable length data.
     *
     * @param position - a shared pointer to a position to encode
     */
    rti1516e::VariableLengthData encode(PositionPtr position);

private:
    rti1516e::HLAfloat64BE   _lat;
    rti1516e::HLAfloat64BE   _long;
    rti1516e::HLAfixedRecord _fixedRecord;
};

#endif
