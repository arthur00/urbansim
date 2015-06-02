
//Title:        Viewer Federate for Book: user interface
//Version:      
//Copyright:    Copyright (c) 1998
//Author:       Frederick Kuhl
//Company:      The MITRE corporation
//Description:  Your description

package se.pitch.sushifederation.viewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;

public class ViewerFrame extends JFrame {
  int _federateHandle;
	String _fedexName;
	Viewer _impl;
  private static String _fileSeparator = System.getProperty("file.separator");
  private static String _userDirectory = System.getProperty("user.dir");
	private static String _newline = System.getProperty("line.separator");
  private Toolkit _toolkit = Toolkit.getDefaultToolkit();
  private int _numberOfSushiTypes;
  private Image[] _sushiIcons;
  private Image _chefIcon;
  private Image _boatIcon;
  private Image _dinerIcon;
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel northPanel = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JButton clearLog = new JButton();
  JPanel logAreaPane = new JPanel();
  JScrollPane logAreaScrollPane = new JScrollPane();
  JTextArea logArea = new JTextArea();
  ViewerPanel viewerPanel = new ViewerPanel();
  BorderLayout borderLayout3 = new BorderLayout();
  JLabel jLabel1 = new JLabel();


  public ViewerFrame() {
    try  {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  //initialization factored out because JBuilder appears
  //to be happy only with a no-arg constructor
  public void finishConstruction(
    Viewer impl,
    Hashtable chefs,
    Hashtable servings,
    Hashtable boats,
    Hashtable diners)
	{
    _impl = impl;
		setTitle("Viewer");
    setSize(900,700);
    setLocation((Toolkit.getDefaultToolkit().getScreenSize()).width - getSize().width, 0);
    viewerPanel.setBorder(BorderFactory.createEtchedBorder());
    viewerPanel.setSimulationData(chefs, servings, boats, diners);
    this.getContentPane().add(viewerPanel, BorderLayout.CENTER);
    logAreaScrollPane.setHorizontalScrollBarPolicy(
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    logAreaScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    logArea.setRows(2000);
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); //we'll handle it
    loadIcons();
	}

  public void lastAdjustments() {
    //repaint();
  }

  public void updateView() {
    viewerPanel.repaint();
  }

  private final class Appender implements Runnable {
    private String thingToAppend;
    Appender(String aLine) { thingToAppend = aLine + _newline; }
    public void run() { logArea.append(thingToAppend); }
  }

	public void post(String line) {
	  //logArea.append(line + _newline);
    Runnable pkg = new Appender(line);
    try {
      SwingUtilities.invokeAndWait(pkg);
    }
    catch (Exception e) {
      System.err.println("Appender exception: " + e);
    }
	}

  void clearLog_actionPerformed(ActionEvent e) {
    logArea.setText("");
  }

  public void loadIcons()
  {
    try {
      MediaTracker tracker = new MediaTracker(this);
      int imageID = 0;
      String urlString;
      String configString = _impl.getProperty("CONFIG");
      //get icons for sushi types
      _numberOfSushiTypes = Integer.parseInt(_impl.getProperty("Federation.Sushi.numberOfTypes"));
      _sushiIcons = new Image[_numberOfSushiTypes];
      for (int i = 0; i < _numberOfSushiTypes; ++i) {
        urlString = configString + _impl.getProperty("Federation.Sushi.icon."+i);
        _sushiIcons[i] = _toolkit.getImage(new URL(urlString));
        tracker.addImage(_sushiIcons[i], imageID++);
      }
      _chefIcon = _toolkit.getImage(new URL(configString + "images" + _fileSeparator + "chef.gif"));
      tracker.addImage(_chefIcon, imageID++);
      _boatIcon = _toolkit.getImage(new URL(configString + "images" + _fileSeparator + "boat.gif"));
      tracker.addImage(_boatIcon, imageID++);
      _dinerIcon = _toolkit.getImage(new URL(configString + "images" + _fileSeparator + "diner.gif"));
      tracker.addImage(_dinerIcon, imageID++);
      try {
        tracker.waitForAll();
      }
      catch (InterruptedException e) { }
      viewerPanel.setConfigurationData(
        _sushiIcons,
        _chefIcon,
        _boatIcon,
        _dinerIcon);
    }
    catch (MalformedURLException e) {
      System.out.println("ERROR loading icons: " + e);
    }
  }

  private void jbInit() throws Exception {
    this.getContentPane().setLayout(borderLayout1);
    northPanel.setLayout(borderLayout2);
    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        this_windowClosing(e);
      }
    });
    logAreaPane.setLayout(borderLayout3);
    clearLog.setText("Clear Log");
    logAreaScrollPane.setPreferredSize(new Dimension(200, 100));
    logAreaScrollPane.setMinimumSize(new Dimension(200, 100));
    logArea.setPreferredSize(new Dimension(600, 1000));
    logArea.setMinimumSize(new Dimension(300, 200));
    jLabel1.setText("Messages");
    jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
    logArea.setMaximumSize(new Dimension(600, 1000));
    logArea.setText("");
    clearLog.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearLog_actionPerformed(e);
      }
    });
    this.getContentPane().add(northPanel, BorderLayout.NORTH);
    northPanel.add(clearLog, BorderLayout.EAST);
    this.getContentPane().add(logAreaPane, BorderLayout.EAST);
    logAreaPane.add(logAreaScrollPane, BorderLayout.CENTER);
    logAreaScrollPane.getViewport().add(logArea, null);
    logAreaPane.add(jLabel1, BorderLayout.NORTH);
    this.getContentPane().add(viewerPanel, BorderLayout.CENTER);
    logAreaScrollPane.getViewport();
    logAreaScrollPane.getViewport();
    logAreaScrollPane.getViewport();
  }

  void this_windowClosing(WindowEvent e) {
    String string1 = "Exit";
    String string2 = "Cancel";
    Object[] options = {string1, string2};
    Object[] message = {
      "This will exit the federate abruptly.    ",
      "Continue?"};
    if (_impl.isJoined()) {
      int n = JOptionPane.showOptionDialog(
        this,
        message,
        "Viewer - Exit",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE,
        null,     //don't use a custom Icon
        options,  //the titles of buttons
        string2); //the title of the default button
      if (n == JOptionPane.YES_OPTION) System.exit(0);
    }
    else  System.exit(0);
  }
}

                                            
