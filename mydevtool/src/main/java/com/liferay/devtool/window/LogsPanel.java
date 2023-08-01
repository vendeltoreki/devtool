package com.liferay.devtool.window;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import com.liferay.devtool.eventlog.LogEntry;
import com.liferay.devtool.eventlog.LogEventListener;

public class LogsPanel extends JPanel implements MouseWheelListener, LogEventListener {
	private static final long serialVersionUID = -3140427339966486122L;
	private JPanel logEntryLister;
	private int fontSize = 12;
	private Font labelFont = new Font("Dialog", Font.BOLD, fontSize);
	private Font textFont = new Font("Dialog", Font.PLAIN, fontSize);

	public void init() {
		this.setLayout(new BorderLayout());

		JButton clearButton = new JButton("Clear logs");

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		
		topPanel.add(clearButton);
		this.add(topPanel, BorderLayout.NORTH);

		logEntryLister = new JPanel();
		BoxLayout layout = new BoxLayout(logEntryLister, BoxLayout.Y_AXIS);
		logEntryLister.setLayout(layout);

		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				logEntryLister.removeAll();
			}
		});


		JScrollPane listScrollPane = new JScrollPane(logEntryLister);
		listScrollPane.getVerticalScrollBar().setUnitIncrement(5);
		listScrollPane.getVerticalScrollBar().getModel().getValue();

		this.add(listScrollPane, BorderLayout.CENTER);
		
		listScrollPane.addMouseWheelListener(this);
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		boolean controlDown = (e.getModifiers() & InputEvent.CTRL_MASK) != 0;
		
		if (controlDown) {
			fontSize -= e.getWheelRotation();
			if (fontSize < 12) {
				fontSize = 12;
			}
			
			if (fontSize > 100) {
				fontSize = 100;
			}
			
			updateFontSize();
		}
	}

	private void updateFontSize() {
		labelFont = new Font(labelFont.getName(), labelFont.getStyle(), fontSize);
		textFont = new Font(textFont.getName(), textFont.getStyle(), fontSize);
		
		/*for (BundlePanel p : bundlePanelList) {
			p.refreshEntry();
		}
		bundleLister.revalidate();*/
	}


	@Override
	public void onLogEventReceived(LogEntry logEntry) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				addLogEntry(logEntry);
			}

		});		
	}

	private void addLogEntry(LogEntry logEntry) {
		logEntryLister.add(createLogEntryPanel(logEntry));
	}
	
	private LogEntryPanel createLogEntryPanel(LogEntry entry) {
		LogEntryPanel res = new LogEntryPanel(entry);
		res.init();
		return res;
	}

	class LogEntryPanel extends JPanel {
		private static final long serialVersionUID = -228638912029061421L;
		private LogEntry entry;
		private JTextArea textArea;

		public LogEntryPanel(LogEntry entry) {
			super();
			this.entry = entry;
		}

		public void init() {
			this.setBorder(BorderFactory.createLineBorder(Color.black));
			BorderLayout layout = new BorderLayout();
			this.setLayout(layout);

			/*label = new JLabel(createLabelText());
			label.setHorizontalAlignment(JLabel.LEFT);
			this.add(label, BorderLayout.CENTER);*/

			refreshEntry();
		}
		
		public LogEntry getEntry() {
			return entry;
		}

		public void setEntry(LogEntry entry) {
			this.entry = entry;
		}

		public void refreshEntry() {
			if (entry != null) {
				
				if (textArea == null) {
			        textArea = new JTextArea(getText());
			        
			        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
			        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			        
			        textArea.setOpaque(true);
			        textArea.setBorder(null);
			        textArea.setEditable(false);
			        
			        textArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			        textArea.setFont(textFont);			        
			        
					this.add(textArea, BorderLayout.SOUTH);
				} else {
					
					textArea.setText(getText());
					textArea.setFont(textFont);
				}
			}
		}

		private String getText() {
			if (entry.getException() != null) {
				StringWriter writer = new StringWriter();
				PrintWriter printWriter= new PrintWriter(writer);
				entry.getException().printStackTrace(printWriter);				
				
				return "["+entry.getTime()+"] ("+entry.getThread()+") "+entry.getMessage()+"\n"+writer.toString();
			} else {
				return "["+entry.getTime()+"] ("+entry.getThread()+") "+entry.getMessage();
			}
		}
	}
	
}
