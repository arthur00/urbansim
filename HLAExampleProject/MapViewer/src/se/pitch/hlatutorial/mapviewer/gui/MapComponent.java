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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class MapComponent extends JLayeredPane {

   private final static Integer BACKGROUND_LEVEL = 100;
   private final static Integer LINE_LEVEL = 200;
   private final static Integer CAR_LEVEL = 300;

   private BufferedImage _backgroundMap;

   private Point _startPosition;
   private final JLabel _startLabel = new MapLabel("Start");
   private final JPanel _startMarker = new Marker(Color.red);

   private Point _goalPosition;
   private final JLabel _goalLabel = new MapLabel("Goal");
   private final JPanel _goalMarker = new Marker(Color.red);

   private double _normal;
   private double _scale = 1.0;

   private final Map<String, CarPanel> _cars = new HashMap<String, CarPanel>();
   private final Map<String, CarLinePanel> _carLines = new HashMap<String, CarLinePanel>();
   private final JScrollPane _pane;

   private final JLabel _noScenarioYetLabel = new JLabel("No scenario loaded yet");

   MapComponent(DataModel dataModel, JScrollPane pane) {
      super();
      _pane = pane;

      _startLabel.setLocation(-50, -50);
      add(_startLabel, BACKGROUND_LEVEL);
      _goalLabel.setLocation(-50, -50);
      add(_goalLabel, BACKGROUND_LEVEL);

      _startMarker.setLocation(-50, -50);
      add(_startMarker, BACKGROUND_LEVEL);
      _goalMarker.setLocation(-50, -50);
      add(_goalMarker, BACKGROUND_LEVEL);

      _noScenarioYetLabel.setLocation(10, 10);
      _noScenarioYetLabel.setSize(_noScenarioYetLabel.getPreferredSize());
      add(_noScenarioYetLabel);

      dataModel.addListener(new DataModelListener() {
         public void carAdded(Car car) {
            invalidate();
            String carIdentifier = car.getIdentifier();
            _cars.put(carIdentifier, new CarPanel(car));
            _carLines.put(carIdentifier, new CarLinePanel());
            add(_cars.get(carIdentifier), CAR_LEVEL);
            add(_carLines.get(carIdentifier), LINE_LEVEL);
            revalidate();
         }

         public void carRemoved(Car car) {
            invalidate();
            String id = car.getIdentifier();
            remove(_cars.get(id));
            _cars.remove(id);
            remove(_carLines.get(id));
            _carLines.remove(id);
            revalidate();
         }

         public void updated(Car car) {
            _cars.get(car.getIdentifier()).updated();
            layoutCars();
         }

         public void setScenario(Scenario scenario) {
            invalidate();
            remove(_noScenarioYetLabel);
            _backgroundMap = scenario.getMap();
            double heightScale = ((double)_pane.getViewport().getHeight()) / ((double)_backgroundMap.getHeight());
            double widthScale = ((double)_pane.getViewport().getWidth()) / ((double)_backgroundMap.getWidth());
            _scale = Math.max(widthScale, heightScale);
            _startPosition = scenario.getStartPosition();
            _goalPosition = scenario.getGoalPosition();

            double slope = calculateSlope(_startPosition, _goalPosition);
            if (slope == 0) {
               _normal = 0;
            } else {
               _normal = -(1 / slope);
            }

            resize();
            revalidate();
            repaint();
         }

         public void scenarioStared(float timeScaleFactor) {
            //ignore
         }

         public void scenarioStopped() {
            //ignore
         }
      });

      _pane.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            resize();
         }
      });
   }

   private void resize() {
      if (_backgroundMap != null) {
         double heightScale = ((double)_pane.getViewport().getHeight()) / ((double)_backgroundMap.getHeight());
         double widthScale = ((double)_pane.getViewport().getWidth()) / ((double)_backgroundMap.getWidth());
         _scale = Math.max(widthScale, heightScale);

         if (heightScale < widthScale) {
            _pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            _pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
         } else {
            _pane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            _pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
         }

         layoutCars();

         placeStartAndGoal();

         repaint();
      }
   }

   private void placeStartAndGoal() {
      Point scaledStartPoint = scaledPoint(_startPosition);
      Point scaledGoalPoint = scaledPoint(_goalPosition);

      _startMarker.setLocation(scaledStartPoint);
      _startLabel.setLocation(calculateLabelPosition(
            scaledStartPoint,
            scaledGoalPoint,
            _startLabel.getWidth(),
            _startLabel.getHeight()));

      _goalMarker.setLocation(scaledGoalPoint);
      _goalLabel.setLocation(calculateLabelPosition(
            scaledGoalPoint,
            scaledStartPoint,
            _goalLabel.getWidth(),
            _goalLabel.getHeight()));
   }

   @Override
   public Dimension getPreferredSize() {
      if (_backgroundMap == null) {
         return super.getPreferredSize();
      }
      return new Dimension((int)(_backgroundMap.getWidth() * _scale), (int)(_backgroundMap.getHeight() * _scale));
   }

   @Override
   protected void paintComponent(Graphics g) {
      Color originalColor = g.getColor();

      g.setColor(Color.white);
      g.fillRect(0, 0, getWidth(), getHeight());

      if (_backgroundMap != null) {
         g.drawImage(_backgroundMap, 0, 0, (int)(_backgroundMap.getWidth() * _scale), (int)(_backgroundMap.getHeight() * _scale), null);
      }

      Graphics2D g2d = (Graphics2D)g;
      if (_startPosition != null && _goalPosition != null) {
         Stroke originalStroke = g2d.getStroke();
         g2d.setColor(Color.red);
         g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {3}, 0));
         g2d.drawLine((int)(_startPosition.x * _scale), (int)(_startPosition.y * _scale), (int)(_goalPosition.x * _scale), (int)(_goalPosition.y * _scale));
         g2d.setStroke(originalStroke);

      }
      g2d.setColor(originalColor);
   }

   private static class CarPanel extends Marker {
      private final Car _car;

      int getOriginalX() {
         return _car.getX();
      }

      int getOriginalY() {
         return _car.getY();
      }

      CarPanel(Car car) {
         _car = car;
      }

      void updated() {
         setColor(_car.getColor());
      }
   }

   private static class CarLinePanel extends JPanel {
      private Point _originalPoint;
      private Point _displayPoint;

      CarLinePanel() {
         setOpaque(false);
      }

      void setLine(Point originalPoint, Point displayPoint) {
         setLocation(Math.min(originalPoint.x, displayPoint.x) - 10, Math.min(originalPoint.y, displayPoint.y) - 10);
         setSize(Math.abs(displayPoint.x - originalPoint.x) + 20, Math.abs(displayPoint.y - originalPoint.y) + 20);
         _originalPoint = new Point(originalPoint.x - getX(), originalPoint.y - getY());
         _displayPoint = new Point(displayPoint.x - getX(), displayPoint.y - getY());
      }

      @Override
      protected void paintComponent(Graphics g) {
         Color originalColor = g.getColor();
         Graphics2D g2d = (Graphics2D)g;
         g.setColor(Color.black);
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g.drawLine(_originalPoint.x, _originalPoint.y, _displayPoint.x, _displayPoint.y);
         g.fillOval(_originalPoint.x - 2, _originalPoint.y - 2, 4, 4);
         g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT);
         g.setColor(originalColor);
      }
   }

   private void layoutCars() {
      List<Point> _takenPositions = new ArrayList<Point>();
      for (String id : _cars.keySet()) {
         CarPanel carPanel = _cars.get(id);
         Point scaledOriginalPoint = scaledPoint(carPanel.getOriginalX(), carPanel.getOriginalY());
         int sub = 0;
         int add = 0;
         boolean okPosition = false;
         Point position = new Point(0, 0);
         while (!okPosition) {
            if (sub == 0 && add == 0) {
               position = scaledOriginalPoint;
            } else if (sub > add) {
               int x = solveForX(_normal);
               position = new Point((int)(scaledOriginalPoint.x - sub * x * _normal), scaledOriginalPoint.y - sub * x);
            } else {
               int x = solveForX(_normal);
               position = new Point((int)(scaledOriginalPoint.x + add * x * _normal), scaledOriginalPoint.y + add * x);
            }
            okPosition = true;
            for (Point p : _takenPositions) {
               if (overlap(position, p)) {
                  okPosition = false;
                  if (sub <= add) {
                     sub++;
                  } else {
                     add++;
                  }
                  break;
               }
            }
         }

         _takenPositions.add(position);
         carPanel.setLocation(position.x, position.y);
         _carLines.get(id).setLine(scaledOriginalPoint, position);
      }
   }

   private int solveForX(double slope) {
      return (int)(((double)Marker.SIZE * 1.5) / (Math.sqrt(1 + Math.abs(slope) * Math.abs(slope))));
   }

   private static boolean overlap(Point p1, Point p2) {
      return Math.abs(p1.x - p2.x) < Marker.SIZE && Math.abs(p1.y - p2.y) < Marker.SIZE;
   }

   private static double calculateSlope(Point _startPosition, Point _goalPosition) {
      return ((double)(_startPosition.x - _goalPosition.x)) / ((double)(_startPosition.y - _goalPosition.y));
   }

   private static final int LABEL_OFFSET = 6;

   private Point calculateLabelPosition(Point position, Point otherPosition, int labelWidth, int labelHeight) {
      Point testPosition = new Point(position.x - labelWidth - LABEL_OFFSET, position.y - labelHeight - LABEL_OFFSET);

      if (testPosition.x > 0 && testPosition.y > 0 && (otherPosition.x > position.x || otherPosition.y > position.y)) {
         return testPosition;
      }

      testPosition = new Point(position.x + LABEL_OFFSET, position.y - labelHeight - LABEL_OFFSET);
      if (testPosition.x > 0 && testPosition.y > 0 && (otherPosition.x < position.x || otherPosition.y > position.y)) {
         return testPosition;
      }

      testPosition = new Point(position.x + LABEL_OFFSET, position.y + LABEL_OFFSET);
      if (testPosition.x > 0 && testPosition.y > 0 && (otherPosition.x < position.x || otherPosition.y < position.y)) {
         return testPosition;
      }

      testPosition = new Point(position.x - labelWidth - LABEL_OFFSET, position.y + LABEL_OFFSET);
      if (testPosition.x > 0 && testPosition.y > 0 && (otherPosition.x > position.x || otherPosition.y < position.y)) {
         return testPosition;
      }

      // If impossible to place label nicely place it outside view
      return new Point(-50, -50);
   }

   private Point scaledPoint(Point p) {
      return scaledPoint(p.x, p.y);
   }

   private Point scaledPoint(int x, int y) {
      return new Point((int)(x * _scale), (int)(y * _scale));
   }
}
