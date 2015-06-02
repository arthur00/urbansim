package se.pitch.sushifederation.production;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;

public class ProductionFrame extends JFrame {
  private Barrier _buttonBarrier;
  private int _federateHandle;
	private String _fedexName;
	private Production _impl;
  private NumberFormat _timeFormat;
  private static String _fileSeparator = System.getProperty("file.separator");
  private static String _userDirectory = System.getProperty("user.dir");
	private static String _newline = System.getProperty("line.separator");
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel northPanel = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JSplitPane jSplitPane1 = new JSplitPane();
  JButton clearLog = new JButton();
  JLabel jLabel1 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JTextField timeStateTextField = new JTextField();
  JPanel jPanel3 = new JPanel();
  JTextField logicalTimeTextField = new JTextField();
  JPanel chefTablePane = new JPanel();
  BorderLayout borderLayout5 = new BorderLayout();
  javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
  javax.swing.JScrollPane chefTableScrollPane = new javax.swing.JScrollPane();
  javax.swing.JTable chefTableView = new javax.swing.JTable();
  javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
  javax.swing.JScrollPane logAreaScrollPane = new javax.swing.JScrollPane();
  BorderLayout borderLayout3 = new BorderLayout();
  javax.swing.JPanel logAreaPane = new javax.swing.JPanel();
  javax.swing.JTextArea logArea = new javax.swing.JTextArea();


  public ProductionFrame() {
    try  {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void finishConstruction(Production impl, ChefTable chefTable)
	{
    _timeFormat = NumberFormat.getInstance(java.util.Locale.US);
    _timeFormat.setGroupingUsed(false);
    _timeFormat.setMinimumFractionDigits(4);
    _timeFormat.setMaximumFractionDigits(4);
    setSize(800,190);
    setLocation(0, 190);
		_impl = impl;
		setTitle("Production");
    logAreaScrollPane.setHorizontalScrollBarPolicy(
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
    logAreaScrollPane.setVerticalScrollBarPolicy(
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    logArea.setRows(2000);
    chefTableView.setModel(chefTable);
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); //we'll handle it
	}

  public void lastAdjustments() {
    jSplitPane1.setDividerLocation(0.5);
  }

  private final class Appender implements Runnable {
    private String thingToAppend;
    Appender(String aLine) { thingToAppend = aLine + _newline; }
    public void run() { logArea.append(thingToAppend); }
  }

	public void post(String line) {
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

  private void jbInit() throws Exception {
    this.getContentPane().setLayout(borderLayout1);
    northPanel.setLayout(borderLayout2);
    this.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        this_windowClosing(e);
      }
    });
    chefTablePane.setLayout(borderLayout5);
    jSplitPane1.setRightComponent(logAreaPane);
    jSplitPane1.setBottomComponent(null);
    clearLog.setText("Clear Log");
    jLabel1.setText("Logical Time:");
    jLabel2.setText("Time State:");
    timeStateTextField.setEditable(false);
    logicalTimeTextField.setEditable(false);
    jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
    chefTableScrollPane.setMinimumSize(new Dimension(200, 100));
    chefTableView.setMinimumSize(new Dimension(200, 100));
    jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
    logAreaScrollPane.setMinimumSize(new Dimension(200, 100));
    logArea.setText("");
    logArea.setMaximumSize(new Dimension(600, 1000));
    logArea.setMinimumSize(new Dimension(300, 200));
    logArea.setPreferredSize(new Dimension(600, 1000));
    logAreaPane.setLayout(borderLayout3);
    logAreaScrollPane.setPreferredSize(new Dimension(200, 100));
    jLabel4.setText("Messages");
    jLabel4.setHorizontalTextPosition(SwingConstants.CENTER);
    chefTableView.setPreferredSize(new Dimension(200, 100));
    chefTableView.setPreferredScrollableViewportSize(new Dimension(300, 250));
    chefTableScrollPane.setPreferredSize(new Dimension(200, 100));
    jLabel3.setText("Chefs");
    logicalTimeTextField.setText("Logical time not init");
    timeStateTextField.setText("time state not init");
    clearLog.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearLog_actionPerformed(e);
      }
    });
    this.getContentPane().add(northPanel, BorderLayout.NORTH);
    northPanel.add(clearLog, BorderLayout.EAST);
    northPanel.add(jPanel3, BorderLayout.CENTER);
    jPanel3.add(jLabel1, null);
    jPanel3.add(logicalTimeTextField, null);
    jPanel3.add(jLabel2, null);
    jPanel3.add(timeStateTextField, null);
    this.getContentPane().add(jSplitPane1, BorderLayout.CENTER);
    jSplitPane1.add(chefTablePane, JSplitPane.LEFT);
    chefTablePane.add(jLabel3, BorderLayout.NORTH);
    chefTablePane.add(chefTableScrollPane, BorderLayout.CENTER);
    chefTableScrollPane.getViewport().add(chefTableView, null);
    jSplitPane1.add(logAreaPane, JSplitPane.RIGHT);
    logAreaPane.add(jLabel4, BorderLayout.NORTH);
    logAreaPane.add(logAreaScrollPane, BorderLayout.CENTER);
    logAreaScrollPane.getViewport().add(logArea, null);
    chefTableScrollPane.getViewport();
    chefTableScrollPane.getViewport();
    logAreaScrollPane.getViewport();
    logAreaScrollPane.getViewport();
    logAreaScrollPane.getViewport();
  }

  void allFederatesJoinedButton_actionPerformed(ActionEvent e) {
    _buttonBarrier.lower(null);
  }

  void setLogicalTime(double lt) {
    logicalTimeTextField.setText(_timeFormat.format(lt));
  }

  void setTimeStateAdvancing() {
    timeStateTextField.setText("Adv");
  }

  void setTimeStateGranted() {
    timeStateTextField.setText("Granted");
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
        "Production - Exit",
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



