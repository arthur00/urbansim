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
#include <iostream>
#include <iterator>
#include <algorithm>

#include<boost/filesystem/fstream.hpp>
#include <boost/algorithm/string.hpp>

#include "Properties.h"
#include "StringUtils.h"

using namespace std;
using namespace StringUtils;

Properties::Properties(const wstring& file) : _comment(L"//"), _delimiter(L"="), _failedToLoad(false)
{
    parseConfig(file);
}

Properties::Properties(const wstring& file, const wstring& comment, const wstring& delimiter) : _comment(comment), _delimiter(delimiter), _failedToLoad(false)
{
    parseConfig(file);
}

Properties::~Properties() 
{ 
    /* void */ 
}

void Properties::parseConfig(const wstring& fileName) 
{
    string line;
    boost::filesystem::ifstream file(fileName);
    while (getline(file, line)) {
        wstring wline = StringUtils::str2wstr(line);
        if(wline.substr(0, _comment.size()) == _comment) { 
            continue; 
        }
        wstring key = getKey(wline, _delimiter);
        wstring value = getValue(wline, _delimiter);
        _config[key] = value;
    }
    if (!file.eof() && (file.bad() || file.fail())) { 
        _failedToLoad = true; 
    }
}

wstring Properties::getProperty(const wstring& key) const
{
    return _config[key];
}

wstring Properties::getKey(const wstring& line, const wstring& delimiter)
{
    return line.substr(0,line.find(delimiter));
}

wstring Properties::getValue(const wstring& line, const wstring& delimiter)
{
    wstring tmp = line.substr(line.find(delimiter) + 1);
    return boost::algorithm::trim_right_copy_if(tmp, boost::algorithm::is_cntrl());
}

wstring Properties::wstr() const
{
    wostringstream woss;
    transform(_config.begin(), _config.end(), ostream_iterator<wstring, wchar_t>(woss, L"\n"), pairToWString);
    return woss.str();
}

bool Properties::failedToLoad() const
{
    return _failedToLoad;
}

// helper functions

wstring pairToWString(const pair<const wstring, wstring>& entry)
{
    wostringstream woss;
    woss << entry.first << L"=" << entry.second;
    return woss.str();
}

wostream& operator << (wostream& wos, const pair<const wstring, wstring>& entry)
{
    return wos << entry.first << L"=" << entry.second;
}

wostream& operator << (wostream& wos, const Properties& properties)
{
    return wos << properties.wstr();
}
