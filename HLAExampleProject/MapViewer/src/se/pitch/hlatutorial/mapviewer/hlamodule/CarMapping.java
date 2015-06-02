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
package se.pitch.hlatutorial.mapviewer.hlamodule;

import hla.rti1516e.ObjectInstanceHandle;

import java.util.HashMap;
import java.util.Map;


class CarMapping {

   private final Map<ObjectInstanceHandle, String> _map1 = new HashMap<ObjectInstanceHandle, String>();
   private final Map<String, ObjectInstanceHandle> _map2 = new HashMap<String, ObjectInstanceHandle>();

   public void put(ObjectInstanceHandle handle, String id) {
      _map1.put(handle, id);
      _map2.put(id, handle);
   }

   public String translate(ObjectInstanceHandle handle) {
      return _map1.get(handle);
   }

   public boolean hasCar(ObjectInstanceHandle handle) {
      return _map1.containsKey(handle);
   }

   public ObjectInstanceHandle translate(String id) {
      return _map2.get(id);
   }

   public void remove(ObjectInstanceHandle theObject) {
      _map2.remove(_map1.get(theObject));
      _map1.remove(theObject);
   }
}
