/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.chris.about;

import org.esa.snap.rcp.about.AboutBox;
import org.esa.snap.rcp.util.BrowserUtils;
import org.openide.modules.ModuleInfo;
import org.openide.modules.Modules;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author Marco Peters
 * @author Sabine Embacher
 */
@AboutBox(displayName = "Chris-Box", position = 120)
public class ChrisAboutBox extends JPanel {

    private final static String releaseNotesHTTP = "https://github.com/senbox-org/chris-box/blob/master/ReleaseNotes.md";

    public ChrisAboutBox() {
        super(new BorderLayout(4, 4));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        ImageIcon aboutImage = new ImageIcon(ChrisAboutBox.class.getResource("about_chris.png"));
        JLabel iconLabel = new JLabel(aboutImage);
        add(iconLabel, BorderLayout.CENTER);
        add(createVersionPanel(), BorderLayout.SOUTH);
    }

    private JPanel createVersionPanel() {
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        int year = utc.get(Calendar.YEAR);
        JLabel copyRightLabel = new JLabel("<html><b>© 2007-" + year + " Brockmann Consult GmbH, University of Valencia and Swansea University</b>", SwingConstants.CENTER);

        final ModuleInfo moduleInfo = Modules.getDefault().ownerOf(ChrisAboutBox.class);
        JLabel versionLabel = new JLabel("<html><b>Chris-Box version " + moduleInfo.getImplementationVersion() + "</b>", SwingConstants.CENTER);

        final JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.add(copyRightLabel);
        mainPanel.add(versionLabel);

        final URI releaseNotesURI = getReleaseNotesURI();
        if (releaseNotesURI != null) {
            final JLabel releaseNoteLabel = new JLabel("<html><a href=\"" + releaseNotesURI.toString() + "\">Release Notes</a>",
                                                       SwingConstants.CENTER);
            releaseNoteLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            releaseNoteLabel.addMouseListener(new BrowserUtils.URLClickAdaptor(releaseNotesHTTP));
            mainPanel.add(releaseNoteLabel);
        }

        return mainPanel;
    }

    private URI getReleaseNotesURI() {
        try {
            return new URI(releaseNotesHTTP);
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
