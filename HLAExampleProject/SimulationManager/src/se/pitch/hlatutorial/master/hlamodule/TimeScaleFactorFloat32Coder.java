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
package se.pitch.hlatutorial.master.hlamodule;

import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAfloat32BE;


class TimeScaleFactorFloat32Coder {
   private final HLAfloat32BE _coder;

   TimeScaleFactorFloat32Coder(EncoderFactory factory) {
      _coder = factory.createHLAfloat32BE();
   }

   byte[] encode(float timeScaleFactor) {
      _coder.setValue(timeScaleFactor);
      return _coder.toByteArray();
   }

   float decode(byte[] timeScaleFactor) throws DecoderException {
      _coder.decode(timeScaleFactor);
      return _coder.getValue();
   }
}
