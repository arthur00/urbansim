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
package se.pitch.hlatutorial.mapviewer.gui;

import se.pitch.hlatutorial.mapviewer.model.Car;
import se.pitch.hlatutorial.mapviewer.model.DataModel;
import se.pitch.hlatutorial.mapviewer.model.DataModelListener;
import se.pitch.hlatutorial.mapviewer.model.Scenario;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

class ScenarioTimeLabel extends JLabel {
   private final SimpleDateFormat _format;
   private final Timer _timer;
   private long _startTime = -1;
   private float _timeScaleFactor; // only valid of _startTime >= 0


   public ScenarioTimeLabel(DataModel dataModel) {
      super("", JLabel.RIGHT);
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      setFont(getFont().deriveFont(14f));

      _format = new SimpleDateFormat("HH:mm:ss");
      _format.setTimeZone(TimeZone.getTimeZone("GMT"));

      _timer = new Timer(100, new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            updateTime();
         }
      });

      dataModel.addListener(new DataModelListener() {
         public void carAdded(Car car) {
            //ignore
         }

         public void carRemoved(Car car) {
            //ignore
         }

         public void updated(Car car) {
            //ignore
         }

         public void setScenario(Scenario scenario) {
            //ignore
         }

         public void scenarioStared(float timeScaleFactor) {
            _timeScaleFactor = timeScaleFactor;
            _startTime = System.currentTimeMillis();

            _timer.start();
         }

         public void scenarioStopped() {
            _timer.stop();
            _startTime = -1;
         }
      });
      updateTime();
   }

   private void updateTime() {
      if (_startTime >= 0) {
         long time = Math.round((System.currentTimeMillis() - _startTime) * _timeScaleFactor);
         setText(_format.format(new Date(time)));
      } else {
         setText("-");
      }
   }
}
