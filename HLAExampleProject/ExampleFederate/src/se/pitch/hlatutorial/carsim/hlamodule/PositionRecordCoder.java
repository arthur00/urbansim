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
package se.pitch.hlatutorial.carsim.hlamodule;

import hla.rti1516e.encoding.*;
import se.pitch.hlatutorial.carsim.model.Position;


class PositionRecordCoder {

   private final HLAfixedRecord _coder;
   private final HLAfloat64BE _latCoder;
   private final HLAfloat64BE _longCoder;

   PositionRecordCoder(EncoderFactory encoderFactory) {
      _coder = encoderFactory.createHLAfixedRecord();
      _latCoder = encoderFactory.createHLAfloat64BE();
      _coder.add(_latCoder);
      _longCoder = encoderFactory.createHLAfloat64BE();
      _coder.add(_longCoder);
   }

   Position decode(byte[] bytes) throws DecoderException {
      _coder.decode(bytes);
      return new Position(_latCoder.getValue(), _longCoder.getValue());
   }

   byte[] encode(Position position) {
      _latCoder.setValue(position.getLatitude());
      _longCoder.setValue(position.getLongitude());
      return _coder.toByteArray();
   }
}
