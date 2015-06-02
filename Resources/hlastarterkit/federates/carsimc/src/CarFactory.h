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
#ifndef CAR_FACTORY_H
#define CAR_FACTORY_H

#include <string>

#include <boost/filesystem.hpp>
#include <boost/shared_ptr.hpp>

#include "Car.h"

/**
 * Creates cars based on .car files in given path.
 * 
 * The factory is created with a path that points to a directory
 * containing .car configuration files. The cars are returned as a map 
 * with Boost shared pointers to Car objects. If no .car files could be
 * read the map will be empty.
 *
 * @remark The class is not thread safe.
 * @author Steve Eriksson steve.eriksson@pitch.se
 */
class CarFactory;
typedef boost::shared_ptr<CarFactory> CarFactoryPtr;

class CarFactory {
public:
    explicit CarFactory(const std::wstring& carConfigDir);

    /**
     * Get the Car objects created from configurations files.
     *
     * @return a map with shared pointers to Car objects
     */
    CarPtrMap getCars() const;

    /**
     * Set the path to the desired directory containing Car object 
     * configurations.
     * 
     * @param carConfigDir - a unicode string representing the path
     */
    void setCarConfigDir(const std::wstring& carConfigDir);

private:
    boost::filesystem::path _carConfigDir;
};

#endif
