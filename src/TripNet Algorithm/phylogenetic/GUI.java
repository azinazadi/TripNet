package phylogenetic;

import graphlab.platform.StaticUtils;
import graphlab.platform.core.exception.ExceptionHandler;
import graphlab.platform.preferences.lastsettings.Settings;
import graphlab.platform.preferences.lastsettings.StorableOnExit;
import graphlab.platform.preferences.lastsettings.UserModifiableProperty;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * @author Azin azadi Date: Nov 18, 2009
 */
public class GUI implements StorableOnExit {
	JTextField tripletFileTxt;
	JPanel panel1;
	JButton browseButton;
	JTextArea algOutTxt;
	JTextField outputfileTxt;
	JButton browseButton1;
	JButton executeTheAlgorithmButton;
	JButton convertToImageButton;
	// private JButton aboutTripNetButton;
	private JTabbedPane tabbedPane1;
	private JTextArea tripletsTxt;
	private JTextField itxt;
	private JTextField jtxt;
	private JTextField ktxt;
	private JButton checkTripletButton;
	private JLabel chkTripletLbl;
	private JEditorPane helpPane;

	{
		SETTINGS.registerSetting(this, "TripNet");
	}

	@UserModifiableProperty()
	public static String lastPath = "";
	JFileChooser jfc;

	public GUI() {
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (jfc == null) {
					jfc = new JFileChooser();
					jfc.setSelectedFile(new File(lastPath));
				}
				jfc.setDialogTitle("Open a Triplets file");
				jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				jfc.setMultiSelectionEnabled(false);
				int x = jfc.showOpenDialog(null);
				if (x == JFileChooser.APPROVE_OPTION) {
					lastPath = jfc.getSelectedFile().getAbsolutePath();
					updateTripletfileTxt();

					SETTINGS.saveSettings();
					setHighlight(executeTheAlgorithmButton);
				}
			}
		});
		executeTheAlgorithmButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				TripNet.t = algOutTxt;
				TripNet.gui = GUI.this;
				new Thread() {
					public void run() {
						if (!running) {
							running = true;
						}
						executeTheAlgorithmButton.setEnabled(false);
						convertToImageButton.setEnabled(false);
						try {
							TripNet.main(new String[] { tripletFileTxt.getText(), "-v" , "-notexact"});
						} catch (Exception e1) {
							StaticUtils.addExceptionLog(e1);
						}
						executeTheAlgorithmButton.setEnabled(true);
						convertToImageButton.setEnabled(true);
						running = false;
						setHighlight(convertToImageButton);
					}
				}.start();
			}
		});
		convertToImageButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Utils.viewDotFile(outputfileTxt.getText(), outputfileTxt.getText() + ".png");
					setHighlight(browseButton);
				} catch (IOException e1) {
					TripNet.println("Err 10: Error accessing the files!");
					StaticUtils.addExceptionLog(e1);
				} catch (InterruptedException e1) {
					TripNet.println("Err 11: Operation interrupted!");
					StaticUtils.addExceptionLog(e1);
				}
			}
		});
		browseButton1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = new File(outputfileTxt.getText());
				try {
					Runtime.getRuntime().exec("explorer " + f.getParentFile());
				} catch (IOException e1) {
					TripNet.println("Err 12: Error accessing some files");
					StaticUtils.addExceptionLog(e1);
				}
			}
		});
		// aboutTripNetButton.addActionListener(new ActionListener() {
		// public void actionPerformed(ActionEvent e) {
		// TripNet.println("TripNet Version 1.0 \n" +
		// "TripNet is an algorithm for constructing phylogenetic networks from sparse\n"
		// +
		// "sets of rooted triplets.\n" +
		// "TripNet is a word made of Triplet and Network.\n" +
		// "-----------------------------------------------------------------------\n"
		// +
		// "* The code\n" +
		// "The code is written in Java and is thus platform independent.\n" +
		// "The source codes are places under the src directory. Main class is \n"
		// +
		// "TripNet, while some classes may not be used in this version like\n"
		// +
		// "CreateGueskInput, but they are usefull if you want to use another\n"
		// +
		// "integer programming tools.\n" +
		// "\n" +
		// "\n" +
		// "The code can be compiled using jar files located in the lib directory.\n"
		// +
		// "\n" +
		// "-------------------------\n" +
		// "\n" +
		// "* How to run the algorithm\n" +
		// "1- Select the Triplets file.\n" +
		// "2- Press the 'Execute the algorithm' button.\n" +
		// "    The algorithm will be executed writing some information about how it is running.\n"
		// +
		// "3- Press the 'Convert to image' button, when the algorithm has finished running. \n"
		// +
		// "\n\n\n" +
		// "The program uses GraphViz's dot package to draw the result network, so install it on \n"
		// +
		// "your system to see the resulting networks. (It can be downloaded\n"
		// +
		// "from: http://www.graphviz.org/Download.php\")\n" +
		// "\n" +
		// "*. The code is still in development. If for some reason you encounter a \n"
		// +
		// "bug or a problem, please inform me on: aazadi [at sign] gmail [dot] com\n"
		// +
		// "\n" +
		// "-------------------------\n" +
		// "\n" +
		// "Have fun!");
		// }
		// });

		setDefaultText(itxt, "i");
		setDefaultText(jtxt, "j");
		setDefaultText(ktxt, "k");
		makeInt(itxt);
		makeInt(jtxt);
		makeInt(ktxt);
		checkTripletButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// String triplets = tripletsTxt.getText();
				// triplets = triplets.replaceAll(",", " ").replaceAll("\\|" ,
				// " ");
				// Scanner s = new Scanner(triplets);
				Vector<Triplet> tow = TripNet.readTriplets();
				Integer i = _int(itxt);
				Integer j = _int(jtxt);
				Integer k = _int(ktxt);
				Vector<Triplet> filteredTriplets = new Vector<Triplet>();
				for (Triplet t : tow) {
					if ((i == null || t.has(i)) && (j == null || t.has(j))
							&& (k == null || t.has(k))) {
						filteredTriplets.add(t);
					}
				}

				String triplets = "";
				for (Triplet t : filteredTriplets) {
					triplets += t + "\n";
				}
				tripletsTxt.setText(triplets);
				chkTripletLbl.setText("");
				if (i != null && j != null && k != null) {
					Triplet t = new Triplet(i, j, k);
					if (tow.contains(t)) {
						chkTripletLbl.setText(t + " exists.");
					} else {
						Triplet t1 = new Triplet(i, k, j);
						Triplet t2 = new Triplet(j, k, i);
						if (tow.contains(t1)) {
							chkTripletLbl.setText(t + " does not exists, but " + t1
									+ (tow.contains(t2) ? " and " + t2 : "") + " does");
						} else if (tow.contains(t2)) {
							chkTripletLbl.setText(t + " does not exists, but " + t2 + " does");
						} else {
							chkTripletLbl.setText("none of " + t + ", " + t1 + ", " + t2
									+ " does not exist");
						}
					}
				}
			}
		});
		highlight = browseButton;
		new Thread() {

			public void run() {
				while (true) {
					if (highlight != null) {
						highlight.setForeground(Color.blue);
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						StaticUtils.addExceptionLog(e);
					}
					if (highlight != null) {
						highlight.setForeground(Color.black);
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						StaticUtils.addExceptionLog(e);
					}
				}
			}
		}.start();
	}

	JButton highlight;

	private void setHighlight(JButton highlight) {
		this.highlight.setForeground(Color.black);
		this.highlight = highlight;
	}

	private void updateTripletfileTxt() {
		tripletFileTxt.setText(lastPath);
		tripletFileTxt.setCaretPosition(lastPath.length());
		try {
			TripNet.tripletsFile = new File(lastPath);
		} catch (Exception e) {
			StaticUtils.addExceptionLog(e);
		}
	}

	boolean running = false;

	public static Integer _int(JTextField t) {
		int i = 0;
		try {
			i = Integer.parseInt(t.getText());
		} catch (NumberFormatException e) {
			return null;
		}
		return i;
	}

	public static void makeInt(final JTextField t) {
		t.addKeyListener(new KeyListener() {
			public void keyTyped(KeyEvent e) {
				setIntError(t);
			}

			public void keyPressed(KeyEvent e) {
				setIntError(t);
			}

			public void keyReleased(KeyEvent e) {
				setIntError(t);
			}
		});
		t.setInputVerifier(new InputVerifier() {

			public boolean verify(JComponent input) {
				String s = t.getText();
				if (s.equals(""))
					return true;
				try {
					Integer.parseInt(s);
				} catch (NumberFormatException e1) {
					return false;
					// t.setForeground(Color.red);
					// StaticUtils.addExceptionLog(e1);
				}

				return true;
			}
		});
		// t.addFocusListener(new FocusListener() {
		// public void focusGained(FocusEvent e) {
		// t.sett.getText();
		// }
		//
		// public void focusLost(FocusEvent e) {
		// }
		// });
	}

	private static void setIntError(JTextField t) {
		String s = t.getText();
		t.setForeground(Color.black);
		try {
			Integer.parseInt(s);
		} catch (NumberFormatException e1) {
			t.setForeground(Color.red);
			// StaticUtils.addExceptionLog(e1);
		}
	}

	public static void setDefaultText(final JTextField t, final String text) {
		if (t.getText().equals("")) {
			t.setForeground(Color.gray);
			t.setText(text);
		}
		t.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				if (t.getForeground() == Color.gray) {
					t.setForeground(Color.black);
					t.setText("");
				}
			}

			public void focusLost(FocusEvent e) {
				if (t.getText().equals("")) {
					t.setForeground(Color.gray);
					t.setText(text);
				}
			}
		});
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			ExceptionHandler.catchException(e);
		}

		JFrame f = new JFrame("TripNet 1.0");

		GUI g = new GUI();
		TripNet.gui = g;
		TripNet.t = g.algOutTxt;
		TripNet.verbose = true;
		g.tripletFileTxt.setText(lastPath);
		g.tripletFileTxt.setCaretPosition(lastPath.length());
		g.updateTripletfileTxt();
		f.add(g.panel1);
		f.setSize(600, 400);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				SETTINGS.saveSettings();
			}

			public void windowClosed(WindowEvent e) {
				SETTINGS.saveSettings();
			}
		});

	}

	{
		// GUI initializer generated by IntelliJ IDEA GUI Designer
		// >>> IMPORTANT!! <<<
		// DO NOT EDIT OR ADD ANY CODE HERE!
		$$$setupUI$$$();
	}

	/**
	 * Method generated by IntelliJ IDEA GUI Designer >>> IMPORTANT!! <<< DO NOT
	 * edit this method OR call it in your code!
	 * 
	 * @noinspection ALL
	 */
	private void $$$setupUI$$$() {
		panel1 = new JPanel();
		panel1.setLayout(new GridBagLayout());
		tripletFileTxt = new JTextField();
		GridBagConstraints gbc;
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel1.add(tripletFileTxt, gbc);
		final JLabel label1 = new JLabel();
		label1.setText("Triplets File:");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.anchor = GridBagConstraints.WEST;
		panel1.add(label1, gbc);
		browseButton = new JButton();
		browseButton.setText("1- Select triplets file");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridwidth = 2;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel1.add(browseButton, gbc);
		final JLabel label2 = new JLabel();
		label2.setText("Algorithm Output:");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.WEST;
		panel1.add(label2, gbc);
		outputfileTxt = new JTextField();
		outputfileTxt.setEditable(false);
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel1.add(outputfileTxt, gbc);
		browseButton1 = new JButton();
		browseButton1.setText("Browse ...");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel1.add(browseButton1, gbc);
		executeTheAlgorithmButton = new JButton();
		executeTheAlgorithmButton.setFont(new Font(executeTheAlgorithmButton.getFont().getName(),
				Font.BOLD, 16));
		executeTheAlgorithmButton.setText("2- Execute The Algorithm");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 4;
		gbc.weighty = 0.1;
		gbc.fill = GridBagConstraints.BOTH;
		panel1.add(executeTheAlgorithmButton, gbc);
		convertToImageButton = new JButton();
		convertToImageButton.setText("3- Convert To Image");
		gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel1.add(convertToImageButton, gbc);
		tabbedPane1 = new JTabbedPane();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 4;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		panel1.add(tabbedPane1, gbc);
		final JScrollPane scrollPane1 = new JScrollPane();
		scrollPane1.setHorizontalScrollBarPolicy(31);
		scrollPane1.setVerticalScrollBarPolicy(22);
		tabbedPane1.addTab("Algorithm Console", scrollPane1);
		algOutTxt = new JTextArea();
		algOutTxt.setDoubleBuffered(true);
		algOutTxt.setLineWrap(true);
		algOutTxt.setTabSize(4);
		algOutTxt.setWrapStyleWord(true);
		scrollPane1.setViewportView(algOutTxt);
		final JPanel panel2 = new JPanel();
		panel2.setLayout(new GridBagLayout());
		tabbedPane1.addTab("Input Triplets", panel2);
		final JScrollPane scrollPane2 = new JScrollPane();
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 6;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		panel2.add(scrollPane2, gbc);
		tripletsTxt = new JTextArea();
		tripletsTxt.setDoubleBuffered(true);
		tripletsTxt.setEditable(false);
		tripletsTxt.setLineWrap(true);
		tripletsTxt.setTabSize(4);
		tripletsTxt.setWrapStyleWord(true);
		scrollPane2.setViewportView(tripletsTxt);
		itxt = new JTextField();
		itxt.setHorizontalAlignment(0);
		itxt.setToolTipText("i");
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.weightx = 0.1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel2.add(itxt, gbc);
		jtxt = new JTextField();
		jtxt.setHorizontalAlignment(0);
		jtxt.setToolTipText("j");
		gbc = new GridBagConstraints();
		gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.weightx = 0.1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel2.add(jtxt, gbc);
		ktxt = new JTextField();
		ktxt.setHorizontalAlignment(0);
		ktxt.setToolTipText("k");
		gbc = new GridBagConstraints();
		gbc.gridx = 2;
		gbc.gridy = 1;
		gbc.weightx = 0.1;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel2.add(ktxt, gbc);
		checkTripletButton = new JButton();
		checkTripletButton.setText("Check Triplet");
		gbc = new GridBagConstraints();
		gbc.gridx = 3;
		gbc.gridy = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel2.add(checkTripletButton, gbc);
		final JPanel spacer1 = new JPanel();
		gbc = new GridBagConstraints();
		gbc.gridx = 5;
		gbc.gridy = 1;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		panel2.add(spacer1, gbc);
		chkTripletLbl = new JLabel();
		chkTripletLbl.setText("");
		gbc = new GridBagConstraints();
		gbc.gridx = 4;
		gbc.gridy = 1;
		gbc.anchor = GridBagConstraints.WEST;
		panel2.add(chkTripletLbl, gbc);
		final JScrollPane scrollPane3 = new JScrollPane();
		scrollPane3.setHorizontalScrollBarPolicy(31);
		tabbedPane1.addTab("Help", scrollPane3);
		helpPane = new JEditorPane();
		helpPane.setContentType("text/html");
		helpPane
				.setText("<html>\n  <head>\n    \n  </head>\n  <body>\n    <h1>\n      TripNet Version 1.0\n    </h1>\n    <h2>\n      What is TripNet\n    </h2>\n    TripNet is an algorithm for constructing phylogenetic networks from sparse \n    sets of rooted triplets. TripNet is a word made of Triplet and Network.\n\n    <h2>\n      How to run the algorithm\n    </h2>\n    1- Select the Triplets file.<br>2- Press the 'Execute the algorithm' \n    button. The algorithm will be executed writing some information about how \n    it is running.<br>3- Press the 'Convert to image' button, when the \n    algorithm has finished running.<br>The program uses GraphViz's dot package \n    to draw the result network, so install it on your system to see the \n    resulting networks. (It can be downloaded from: \n    http://www.graphviz.org/Download.php). If you can't use this feature you \n    may may manually convert the algorithm .dot output to an image using \n    GraphViz's tools.<br>*. This code is still under development. If for some \n    reason you encounter a bug or a problem, please inform me on: aazadi [at \n    sign] gmail [dot] com<hr>\n\n    <h2>\n      The code\n    </h2>\n    The code is written in Java and is thus platform independent. The source \n    codes are places under the src directory. Main class is TripNet, while \n    some classes may not be used in this version like CreateGueskInput, but \n    they are usefull if you want to use another integer programming tools.<br>This \n    program is written based on Graph<sup>Lab</sup> \n    (http://graphlab.sharif.edu) which is a visual graph theory platfrom.The \n    code can be compiled using jar files located in the lib directory.<hr>Have \n    fun!\n  </body>\n</html>\n");
		scrollPane3.setViewportView(helpPane);
	}

	/**
	 * @noinspection ALL
	 */
	public JComponent $$$getRootComponent$$$() {
		return panel1;
	}
}