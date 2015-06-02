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
#ifndef PROPERTIES_H
#define PROPERTIES_H

#include <string>
#include <map>

typedef std::map<const std::wstring, std::wstring> config_map;

/**
 * The properties class parses a file with key/value pairs.
 * 
 * The Properties class is instantiated with the path to a file to
 * parse. The file is parsed one row at a time.
 * If the file couldn't be read properly the Properties::failedToLoad() method
 * will return true. Which string that should be considered a comment and the
 * delimiter between the key/value pairs can be configured in the constructor.
 * Rows starting with a comment will be ignored.
 *
 * @remark The class is thread safe through immutable state.
 * @author Steve Eriksson steve.eriksson@pitch.se
 */ 
class Properties {
public:

    /**
     * Create a Properties object based on the properties file.
     * The default configuration uses '=' and '//' as special symbols:
     * 
     * key=value
     * // comment
     *
     * @param file - the name of the properties file
     */
    explicit Properties(const std::wstring& file);

    Properties(const std::wstring& file, const std::wstring& comment, const std::wstring& delimiter);

    virtual ~Properties();

    /**
     * Get the string representation of the Properties object.
     *
     * @return the Properties object as wstring
     */
    std::wstring wstr() const;

    /**
     * Get the the value of a property.
     *
     * @param key - the key as wstring
     * @return the value of the key as wstring
     */
    std::wstring getProperty(const std::wstring& key) const;

    /**
     * Check if the Properties object was created correctly.
     * If the Properties object wasn't able to parse the properties file
     * this method will return false.
     *
     * @return true if the Properties object was able to load the properties file
     */
    bool failedToLoad() const;

private:

    /**
     * Parse the given properties file.
     * 
     * @param file - the name of the file to parse
     */
    void parseConfig(const std::wstring& file);

    /**
     * Get the key field of the given line.
     * 
     * @param line - a line from the properties file
     * @return the key of the line
     */
    std::wstring getKey(const std::wstring& line, const std::wstring& delimiter);

    /**
     * Get the value field of the given line.
     * 
     * @param line - a line from the properties file
     * @return the value of the line
     */
    std::wstring getValue(const std::wstring& line, const std::wstring& delimiter);

private:
    const std::wstring  _comment;
    const std::wstring  _delimiter;
    mutable config_map  _config;
    bool                _failedToLoad;
};

std::wstring pairToWString(const std::pair<const std::wstring, std::wstring>& entry);

std::wostream& operator << (std::wostream& wos, const Properties& Properties);

#endif
