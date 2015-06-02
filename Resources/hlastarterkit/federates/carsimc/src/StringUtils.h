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
#ifndef STRING_UTILS_H
#define STRING_UTILS_H

#include <sstream>
#include <algorithm>
#include <string>

/**
 * Utility functions for string convertions.
 *
 * @author Steve Eriksson steve.eriksson@pitch.se
 */ 
namespace StringUtils {

    // convert string to type T
    template<class T>
    T fromString(const std::string& s)
    {
        std::istringstream stream(s);
        T t;
        stream >> t;
        return t;
    }

    // convert string to type T
    template<class T>
    T fromWString(const std::wstring& ws)
    {
        std::wistringstream wstream(ws);
        T t;
        wstream >> t;
        return t;
    }

    // convert type T to wstring
    template<class T>
    std::wstring toWString(T t)
    {
        std::wstringstream wss;
        wss << t;
        return wss.str();
    }

    // convert a string to a wstring
    std::wstring str2wstr(const std::string& s);
}
    
#endif
