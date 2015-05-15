package se.pitch.sushifederation.manager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.NumberFormat;

public class ManagerFrame extends JFrame {
  private int _federateHandle;
	private String _fedexName;
	private Manager _impl;
  private NumberFormat _timeFormat;
  private ToggleBarrier _pausedBarrier;
  private static String _fileSeparator = System.getProperty("file.separator");
  private static String _userDirectory = System.getProperty("user.dir");
	private static String _newline = System.getProperty("line.separator");
  BorderLayout borderLayout1 = new BorderLayout();
  JPanel northPanel = new JPanel();
  BorderLayout borderLayout2 = new BorderLayout();
  JSplitPane jSplitPane1 = new JSplitPane();
  JScrollPane federateTableScrollPane = new JScrollPane();
  JTable federateTable = new JTable();
  JButton clearLog = new JButton();
  JLabel jLabel1 = new JLabel();
  JLabel jLabel2 = new JLabel();
  JTextField timeStateTextField = new JTextField();
  JPanel jPanel3 = new JPanel();
  JTextField logicalTimeTextField = new JTextField();
  JToggleButton pauseButton = new JToggleButton();
  JPanel logAreaPane = new JPanel();
  JScrollPane logAreaScrollPane = new JScrollPane();
  JTextArea logArea = new JTextArea();
  JLabel jLabel3 = new JLabel();
  BorderLayout borderLayout3 = new BorderLayout();


  public ManagerFrame(ToggleBarrier pausedBarrier) {
    _pausedBarrier = pausedBarrier;
    try  {
      jbInit();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void finishConstruction(Manager impl, FederateTable theFederateTable)
	{
    _timeFormat = NumberFormat.getInstance(java.util.Locale.US);
    _timeFormat.setGroupingUsed(false);
    _timeFormat.setMinimumFractionDigits(4);
    _timeFormat.setMaximumFractionDigits(4);
    setSize(800,190);
		_impl = impl;
		setTitle("Manager");
    logArea.setRows(2000);
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); //we'll handle it
    federateTable.setModel(theFederateTable);
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
      //SwingUtilities.invokeLater(pkg);
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
    this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        this_windowClosing(e);
      }
    });
    logAreaPane.setLayout(borderLayout3);
    federateTableScrollPane.setPreferredSize(new Dimension(200, 100));
    federateTableScrollPane.setMinimumSize(new Dimension(200, 100));
    federateTable.setMinimumSize(new Dimension(200, 100));
    clearLog.setText("Clear Log");
    jLabel1.setText("Logical Time:");
    jLabel2.setText("Time State:");
    timeStateTextField.setEditable(false);
    logicalTimeTextField.setEditable(false);
    pauseButton.setText("Pause");
    logAreaScrollPane.setPreferredSize(new Dimension(200, 100));
    logAreaScrollPane.setMinimumSize(new Dimension(200, 100));
    logArea.setPreferredSize(new Dimension(600, 1000));
    logArea.setMinimumSize(new Dimension(300, 200));
    jLabel3.setText("Messages");
    jLabel3.setHorizontalAlignment(SwingConstants.CENTER);
    logArea.setMaximumSize(new Dimension(600, 1000));
    logArea.setText("");
    pauseButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        pause_actionPerformed(e);
      }
    });
    logicalTimeTextField.setText("Logical time not init");
    timeStateTextField.setText("time state not init");
    clearLog.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearLog_actionPerformed(e);
      }
    });
    federateTable.setPreferredSize(new Dimension(200, 100));
    federateTable.setPreferredScrollableViewportSize(new Dimension(300, 250));
    this.getContentPane().add(northPanel, BorderLayout.NORTH);
    northPanel.add(clearLog, BorderLayout.EAST);
    northPanel.add(jPanel3, BorderLayout.CENTER);
    jPanel3.add(jLabel1, null);
    jPanel3.add(logicalTimeTextField, null);
    jPanel3.add(jLabel2, null);
    jPanel3.add(timeStateTextField, null);
    northPanel.add(pauseButton, BorderLayout.WEST);
    this.getContentPane().add(jSplitPane1, BorderLayout.CENTER);
    jSplitPane1.add(federateTableScrollPane, JSplitPane.LEFT);
    federateTableScrollPane.getViewport().add(federateTable, null);
    jSplitPane1.add(logAreaPane, JSplitPane.RIGHT);
    logAreaPane.add(jLabel3, BorderLayout.NORTH);
    logAreaPane.add(logAreaScrollPane, BorderLayout.CENTER);
    logAreaScrollPane.getViewport().add(logArea, null);
    federateTableScrollPane.getViewport();
    logAreaScrollPane.getViewport();
    logAreaScrollPane.getViewport();
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
        "Manager - Exit",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE,
        null,     //don't use a custom Icon
        options,  //the titles of buttons
        string2); //the title of the default button
      if (n == JOptionPane.YES_OPTION) System.exit(0);
    }
    else  System.exit(0);
  }

  void pause_actionPerformed(ActionEvent e) {
    if (pauseButton.isSelected()) _pausedBarrier.raise();
    else _pausedBarrier.lower();
  }
}

                                            
