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

import javax.swing.*;
import java.awt.*;


class MapLabel extends JLabel {

   public MapLabel(String text) {
      super(text);
      setOpaque(false);
      setHorizontalAlignment(JLabel.CENTER);
      int width = getFontMetrics(getFont()).stringWidth(text);
      setSize(width + 10, 16);
   }

   @Override
   protected void paintComponent(Graphics g) {
      Graphics2D g2d = (Graphics2D)g;
      g2d.setColor(Color.white);
      g2d.fillOval(0, 0, getWidth() - 1, getHeight() - 1);
      g2d.setColor(Color.black);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
      super.paintComponent(g);
   }
}
