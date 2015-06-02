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


class Marker extends JPanel {

   private Color _color = Color.white;
   static final int SIZE = 10;

   Marker() {
      setSize(SIZE, SIZE);
      setOpaque(false);
   }

   Marker(Color color) {
      this();
      setColor(color);
   }

   void setColor(Color color) {
      _color = color;
   }

   @Override
   protected void paintComponent(Graphics g) {
      Graphics2D g2d = (Graphics2D)g;
      Color originalColor = g2d.getColor();
      g2d.setColor(_color);

      g2d.fillOval(0, 0, SIZE - 1, SIZE - 1);
      g2d.setColor(Color.darkGray);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.drawOval(0, 0, SIZE - 1, SIZE - 1);
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
      g2d.setColor(originalColor);
   }

   @Override
   public void setLocation(int x, int y) {
      super.setLocation(x - SIZE / 2, y - SIZE / 2);
   }
}
