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

import se.pitch.hlatutorial.mapviewer.model.DataModel;

import javax.swing.*;
import java.awt.*;


public class MapFrame extends JFrame {

   public MapFrame(DataModel dataModel) {
      final JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      mainSplit.add(new JScrollPane(new CarList(dataModel)));

      JScrollPane pane = new JScrollPane();
      MapComponent mapComponent = new MapComponent(dataModel, pane);
      pane.getViewport().setView(mapComponent);
      mainSplit.add(pane);
      mainSplit.setDividerLocation(200);

      Container contentPane = getRootPane().getContentPane();
      contentPane.setLayout(new BorderLayout());
      contentPane.add(mainSplit, BorderLayout.CENTER);

      contentPane.add(new ScenarioTimeLabel(dataModel), BorderLayout.NORTH);

      setTitle("HLA Tutorial by Pitch - Map Viewer");
      setSize(600, 400);
   }
}
