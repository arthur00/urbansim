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


class FuelMeter extends JPanel {

   private final JPanel _fuelMeter = new JPanel() {
      @Override
      public void paintComponent(Graphics g) {
         int fuelMeterHeight = getHeight();
         int height = _initialFuelAmount == 0 ? 0 : (int)((((float)_fuelAmount) / ((float)_initialFuelAmount)) * fuelMeterHeight);
         Color originalColor = g.getColor();
         g.setColor(new Color(194, 78, 7));
         g.fillRect(0, fuelMeterHeight - height, 20, 50);
         g.setColor(originalColor);
      }
   };

   private final JLabel _fuelAmountLabel = new JLabel("0 l");

   private int _initialFuelAmount = 0;

   private int _fuelAmount = 0;

   FuelMeter() {
      super(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.weighty = 1;
      c.fill = GridBagConstraints.VERTICAL;
      _fuelMeter.setSize(20, 0);
      _fuelMeter.setBorder(BorderFactory.createLineBorder(Color.darkGray));
      _fuelMeter.setOpaque(false);
      add(_fuelMeter, c);
      c = new GridBagConstraints();
      c.gridy = 1;
      add(_fuelAmountLabel, c);
      setOpaque(false);
   }

   void setInitialFuelAmount(int initialAmount) {
      if (_initialFuelAmount != initialAmount) {
         _initialFuelAmount = initialAmount;
         _fuelMeter.repaint();
      }
   }

   void setFuelAmount(int amount) {
      if (_fuelAmount != amount) {
         _fuelAmount = amount;
         _fuelAmountLabel.setText(_fuelAmount + " l");
         _fuelMeter.repaint();
      }
   }
}
