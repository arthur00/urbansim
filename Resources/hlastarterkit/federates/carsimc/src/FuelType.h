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
#ifndef FUEL_TYPE_H
#define FUEL_TYPE_H

#include <stdexcept>
#include <string>
#include <iostream>

#include <boost/shared_ptr.hpp>

/**
 * The class represents a kind of fuel.
 * 
 * The FuelType class is abstract.
 *
 * @remark The class is thread safe through immutable state.
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class FuelType;
typedef boost::shared_ptr<FuelType> FuelTypePtr;

class FuelType {
public:
    virtual ~FuelType() {};

    virtual bool operator == (const FuelType& rhs) const;
    virtual bool operator == (FuelTypePtr rhs) const;

    virtual bool operator != (const FuelType& rhs) const;
    virtual bool operator != (FuelTypePtr rhs) const;

    std::wstring wstr() const { return toWString(); }

private:
    virtual std::wstring toWString() const = 0;
};


// Implementations

class Diesel : public FuelType {
private:
    std::wstring toWString() const { return L"Diesel"; }
};

class Gasoline : public FuelType {
private:
    std::wstring toWString() const { return L"Gasoline"; }
};

class NaturalGas : public FuelType {
private:
    std::wstring toWString() const { return L"NaturalGas"; }
};

class EthanolFlexibleFuel : public FuelType {
private:
    std::wstring toWString() const { return L"EthanolFlexibleFuel"; }
};

/**
 * A static factory for creating fuel types. 
 * 
 * A fuel type is instantiated by providing the factory a string 
 * representing it. If an erronous string is provided an invalid_argument
 * exception will be thrown.
 *
 * @remark The class is thread safe through immutable state.
 * @see FuelType
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class FuelTypeFactory {
public:
    static FuelTypePtr createFuelType(const std::wstring& type)
    {
      if (type.compare(L"Gasoline"))            return FuelTypePtr(new Gasoline());
      if (type.compare(L"Diesel"))              return FuelTypePtr(new Diesel());
      if (type.compare(L"NaturalGas"))          return FuelTypePtr(new NaturalGas());
      if (type.compare(L"EthanolFlexibleFuel")) return FuelTypePtr(new EthanolFlexibleFuel());

      throw new std::invalid_argument("FuelType is not defined");
    }
};


// helper function
std::wostream& operator << (std::wostream& os, const FuelType& type);

#endif
