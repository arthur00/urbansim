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
#ifndef HLA_FUEL_TYPE_H
#define HLA_FUEL_TYPE_H

#include <RTI/VariableLengthData.h>
#include <RTI/encoding/BasicDataElements.h>
#include <boost/bimap.hpp>

#include "FuelType.h"

typedef boost::bimap<int, std::wstring> HLAfuelMap;
typedef HLAfuelMap::value_type HLAfuel;

/**
 * The HLAfuelType class handles encoding and decoding of FuelType data.
 *
 * @see FuelType
 * @author Steve Eriksson steve.eriksson@pitch.se
 */ 
class HLAfuelType {
public:
    HLAfuelType();

    /**
     * Encode the fuel type as HLA variable length data.
     *
     * @param fuelType - the fuel type to encode
     */
    rti1516e::VariableLengthData encode(const FuelType& fuelType);

    /**
     * Encode the fuel type shared pointer as HLA variable length data.
     *
     * @param fuelType - shared pointer to the fuel type to encode
     */
    rti1516e::VariableLengthData encode(FuelTypePtr fuelType);

    /**
     * Decode the HLA variable length data buffer to a shared pointer to 
     * Fuel type.
     *
     * @param buffer - the variable length data buffer
     * @return a shared pointer to a Fuel type object
     */
    FuelTypePtr decode(const rti1516e::VariableLengthData& buffer);

private:
    rti1516e::HLAinteger32BE _hlaFuelType;
    HLAfuelMap               _fuelMap; 
};

#endif
