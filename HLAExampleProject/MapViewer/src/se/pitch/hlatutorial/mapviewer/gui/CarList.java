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
import java.awt.*;


class CarList extends JList {

   private class CarRenderer extends JPanel implements ListCellRenderer {

      private final JLabel _nameLabel = new JLabel();
      private final Marker _carMarker = new Marker();

      private final JLabel _detailsLabel = new JLabel();
      private final FuelMeter _fuelMeter = new FuelMeter();

      public CarRenderer() {
         super(new GridBagLayout());
         GridBagConstraints c = new GridBagConstraints();
         c.gridheight = 2;
         c.insets = new Insets(5, 5, 5, 5);
         add(_carMarker, c);
         c.insets = new Insets(5, 5, 5, 0);
         c.fill = GridBagConstraints.HORIZONTAL;
         c.weightx = 1;
         c.gridheight = 1;
         c.gridx = 1;
         _nameLabel.setFont(_nameLabel.getFont().deriveFont(12f).deriveFont(Font.BOLD));
         add(_nameLabel, c);
         c.gridy = 1;
         c.insets = new Insets(0, 5, 5, 0);
         add(_detailsLabel, c);
         c.insets = new Insets(4, 5, 2, 5);
         c.gridheight = 2;
         c.gridx = 2;
         c.gridy = 0;
         c.fill = GridBagConstraints.VERTICAL;
         c.weighty = 1;
         c.weightx = 0;
         add(_fuelMeter, c);

         setOpaque(false);
         setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.gray));
      }

      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         Car car = (Car)value;

         _nameLabel.setText(car.getName());
         _detailsLabel.setText(car.getLicensePlateNumber() + " " + car.getFuelType().getName());
         _carMarker.setColor(car.getColor());

         _fuelMeter.setFuelAmount(car.getFuelLevel());
         return this;
      }
   }

   private class CarModel extends DefaultListModel {
      void carUpdated(Car car) {
         int index = indexOf(car);
         fireContentsChanged(this, index, index);
      }
   }

   private final CarModel _carModel = new CarModel();
   private CarRenderer _carRenderer;

   CarList(DataModel dataModel) {
      setModel(_carModel);
      dataModel.addListener(new DataModelListener() {
         public void carAdded(Car car) {
            _carModel.addElement(car);
         }

         public void carRemoved(final Car car) {
            SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                  _carModel.removeElement(car);
               }
            });
         }

         public void updated(Car car) {
            _carModel.carUpdated(car);
         }

         public void setScenario(Scenario scenario) {
            _carRenderer._fuelMeter.setInitialFuelAmount(scenario.getInitialFuelAmount());
         }

         public void scenarioStared(float timeScaleFactor) {
            //ignore
         }

         public void scenarioStopped() {
            //ignore
         }
      });

      _carRenderer = new CarRenderer();
      setCellRenderer(_carRenderer);
   }
}
