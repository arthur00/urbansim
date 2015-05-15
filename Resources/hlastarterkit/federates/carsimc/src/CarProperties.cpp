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
#include "CarProperties.h"
#include "FuelType.h"
#include "StringUtils.h"

using namespace std;

CarProperties::CarProperties(const wstring& file) : Properties(file) {}

std::wstring CarProperties::getName() const
{
    return getProperty(L"Name");	
}

std::wstring CarProperties::getLicensePlate() const
{
    return getProperty(L"LicensePlate");
}

FuelTypePtr CarProperties::getFuelType() const
{
    return FuelTypeFactory::createFuelType(getProperty(L"FuelType"));
}

float CarProperties::getNormalSpeed() const
{
    return StringUtils::fromWString<float>(getProperty(L"NormalSpeed"));
}

float CarProperties::getLitersPer100km1() const
{
    return  StringUtils::fromWString<float>(getProperty(L"LitersPer100km1"));
}

float CarProperties::getLitersPer100km2() const
{
    return  StringUtils::fromWString<float>(getProperty(L"LitersPer100km2"));
}

float CarProperties::getLitersPer100km3() const
{
    return  StringUtils::fromWString<float>(getProperty(L"LitersPer100km3"));
}
