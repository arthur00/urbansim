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
#ifndef CAR_PROPERTIES_H
#define CAR_PROPERTIES_H

#include <string>

#include "Properties.h"
#include "FuelType.h"

/**
 * Represents the properties of a .car configuration file.
 *
 * @remark The class is thread safe through immutable state.
 * @see Properties
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class CarProperties : public Properties {
public:
    explicit CarProperties(const std::wstring& file);

    std::wstring getName()            const;
    std::wstring getLicensePlate()    const;
    FuelTypePtr  getFuelType()        const;
    float        getNormalSpeed()     const;
    float        getLitersPer100km1() const;
    float        getLitersPer100km2() const;
    float        getLitersPer100km3() const;
};

#endif

