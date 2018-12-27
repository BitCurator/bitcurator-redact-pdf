package bca.redact;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.Charset;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.commons.io.IOUtils;

public class SimpleAboutDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public SimpleAboutDialog(JFrame parent) {
	    super(parent, "About BitCurator PDF Redactor", true);

	    Box b = Box.createVerticalBox();
	    b.add(Box.createGlue());
	    String aboutHTML = null;
	    try {
			aboutHTML = IOUtils.resourceToString("/about.html", Charset.forName("UTF-8"));
		} catch (IOException e) {
			throw new Error("Unexpected IO error", e);
		}
	    b.add(new JLabel(aboutHTML));
	    b.add(Box.createGlue());
	    getContentPane().add(b, "Center");

	    JPanel p2 = new JPanel();
	    JButton ok = new JButton("Ok");
	    p2.add(ok);
	    getContentPane().add(p2, "South");

	    ok.addActionListener(new ActionListener() {
	      public void actionPerformed(ActionEvent evt) {
	        setVisible(false);
	      }
	    });

	    setSize(350, 250);
	  }
}
