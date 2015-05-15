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
#include "HLAfuelType.h"

HLAfuelType::HLAfuelType()
{
    _fuelMap.insert(HLAfuel(0, L"Unknown"));
    _fuelMap.insert(HLAfuel(1, L"Gasoline"));
    _fuelMap.insert(HLAfuel(2, L"Diesel"));
    _fuelMap.insert(HLAfuel(3, L"EthanolFlexibleFuel"));
    _fuelMap.insert(HLAfuel(4, L"NaturalGas"));
}

rti1516e::VariableLengthData HLAfuelType::encode(const FuelType& fuelType)
{
    int fuel = _fuelMap.right.at(fuelType.wstr());
    _hlaFuelType.set(fuel);
    return _hlaFuelType.encode();
}

rti1516e::VariableLengthData HLAfuelType::encode(FuelTypePtr fuelType)
{
    return encode(*fuelType);
}


FuelTypePtr HLAfuelType::decode(const rti1516e::VariableLengthData& buffer)
{
    _hlaFuelType.decode(buffer);
    std::wstring fuelType = _fuelMap.left.at(_hlaFuelType.get());
    return FuelTypeFactory::createFuelType(fuelType);
}
