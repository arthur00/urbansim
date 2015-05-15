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
#include "HLAposition.h"

HLAposition::HLAposition() : _lat(0.0), _long(0.0)
{
    _fixedRecord.appendElement(_lat);
    _fixedRecord.appendElement(_long);
}

rti1516e::VariableLengthData HLAposition::encode(const Position& position)
{
    _lat.set(position.getLatitude());
    _long.set(position.getLongitude());

    _fixedRecord.set(0, _lat);
    _fixedRecord.set(1, _long);

    return _fixedRecord.encode();
}

rti1516e::VariableLengthData HLAposition::encode(PositionPtr position)
{
    return encode(*position);
}


