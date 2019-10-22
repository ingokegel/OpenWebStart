package com.openwebstart.launcher;

import com.openwebstart.install4j.Install4JUtils;
import com.openwebstart.jvm.ui.dialogs.DialogFactory;
import net.adoptopenjdk.icedteaweb.client.controlpanel.ControlPanel;
import net.adoptopenjdk.icedteaweb.i18n.Translator;
import net.adoptopenjdk.icedteaweb.logging.Logger;
import net.adoptopenjdk.icedteaweb.logging.LoggerFactory;
import net.adoptopenjdk.icedteaweb.ui.swing.SwingUtils;
import net.sourceforge.jnlp.config.DeploymentConfiguration;

import javax.naming.ConfigurationException;
import javax.swing.UIManager;

public class ControlPanelLauncher {

    private final static Logger LOG = LoggerFactory.getLogger(ControlPanelLauncher.class);


    public static void main(final String[] args) {
        Translator.addBundle("i18n");

        final DeploymentConfiguration config = new DeploymentConfiguration();
        try {
            config.load();
        } catch (final ConfigurationException e) {
            DialogFactory.showErrorDialog(Translator.getInstance().translate("error.loadConfig"), e);
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }

        Install4JUtils.applicationVersion().ifPresent(v ->  LOG.info("Starting OpenWebStart {}", v));

        SwingUtils.invokeLater(() -> {
            final ControlPanel editor = new ControlPanel(config, new OpenWebStartControlPanelStyle());
            editor.setVisible(true);
        });
    }
}
