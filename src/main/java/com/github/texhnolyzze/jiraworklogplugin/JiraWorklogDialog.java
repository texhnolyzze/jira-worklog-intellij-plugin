package com.github.texhnolyzze.jiraworklogplugin;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.*;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

public class JiraWorklogDialog extends JDialog {

    private final transient Project project;
    private final boolean pauseTimerAfterReset;
    private final String branchName;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField comment;
    private JTextField timeSpent;
    private JTextField remained;
    private JTextField username;
    private JPasswordField password;
    private JButton testConnectionButton;
    private JLabel testConnectionResult;
    private JTextField jiraUrl;
    private JTextField logged;
    private JComboBox<JiraIssue> jiraIssue;
    private JLabel findIssuesError;
    private JLabel issueSummary;
    private JLabel addWorklogError;
    private JComboBox<AdjustEstimate> adjustEstimate;
    private JTextField timeEstimate;
    private JLabel adjustmentDurationLabel;
    private JTextField adjustmentDuration;
    private JLabel timeSpentSinceLastWorklogAdded;

    private int maxIssueSummaryWidth;

    public JiraWorklogDialog(
        final @NotNull Project project,
        final String branchName,
        final boolean pauseTimerAfterReset
    ) {
        this.project = project;
        this.pauseTimerAfterReset = pauseTimerAfterReset;
        this.branchName = branchName;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final Timer timer;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) {
            timer = state.getTimer(branchName, project);
        }
        final String formatted = Util.formatAsJiraDuration(timer.toDuration());
        this.timeSpent.setText(formatted);
        this.timeSpentSinceLastWorklogAdded.setText(
            "You spent " + formatted + " in " + branchName + " since you last logged from it "
        );
        setupListeners();
        for (final AdjustEstimate estimate : AdjustEstimate.values()) {
            adjustEstimate.addItem(estimate);
        }
        adjustEstimate.setSelectedItem(AdjustEstimate.AUTO);
    }

    void init(final String jiraKey) {
        final boolean connectionSettingsOk = setupJiraConnectionSettings();
        final boolean connectionOk;
        if (connectionSettingsOk) {
            connectionOk = testConnection();
        } else {
            connectionOk = false;
        }
        final boolean isJiraKey = Util.isJiraKey(jiraKey);
        if (connectionOk) {
            if (isJiraKey) {
                findIssues(jiraKey);
            } else {
                jiraIssue.requestFocus();
            }
        } else {
            jiraUrl.requestFocus();
            if (isJiraKey) {
                getJiraIssueSearchField().setText(jiraKey);
            }
        }
    }

    private synchronized void findIssues(final String input) {
        jiraIssue.removeAllItems();
        final JiraClient client = JiraClient.getInstance(project);
        final JiraIssue.Criteria criteria = new JiraIssue.Criteria();
        final boolean isJiraKey = Util.isJiraKey(input);
        if (isJiraKey) {
            criteria.setKey(input);
        } else {
            criteria.setSummary(input);
        }
        final char[] pass = password.getPassword();
        final FindJiraIssuesResponse response = client.findIssues(
            jiraUrl.getText(),
            username.getText(),
            new String(pass),
            criteria
        );
        Arrays.fill(pass, '\0');
        if (response != null && StringUtils.isBlank(response.getError())) {
            final NavigableSet<JiraIssue> issues = response.getIssues();
            for (final JiraIssue issue : issues.descendingSet()) {
                jiraIssue.addItem(issue);
            }
            findIssuesError.setText(null);
            findIssuesError.setVisible(false);
            jiraIssue.requestFocus();
            if (issues.size() > 1) {
                SwingUtilities.invokeLater(
                    () -> jiraIssue.showPopup()
                );
            }
            if (issues.isEmpty()) {
                getJiraIssueSearchField().setText(input);
            }
        } else {
            setFindIssuesError(response);
            getJiraIssueSearchField().setText(input);
        }
    }

    private void setFindIssuesError(final FindJiraIssuesResponse response) {
        findIssuesError.setText(
            "Error searching Jira issues" + (
                response == null || StringUtils.isBlank(response.getError()) ?
                "" :
                ": " + response.getError()
            )
        );
        findIssuesError.setVisible(true);
        findIssuesError.setForeground(JBColor.RED);
        issueSummary.setVisible(false);
        timeEstimate.setText(null);
    }

    private boolean setupJiraConnectionSettings() {
        final String url = JiraWorklogPluginState.getInstance(project).getJiraUrl();
        if (!StringUtils.isBlank(url)) {
            jiraUrl.setText(url);
            final CredentialAttributes attributes = getCredentialAttributes(url);
            final Credentials credentials = PasswordSafe.getInstance().get(attributes);
            if (credentials != null) {
                username.setText(credentials.getUserName());
                password.setText(credentials.getPasswordAsString());
                return true;
            }
        }
        return false;
    }

    private void setupListeners() {
        buttonCancel.addActionListener(unused -> onCancel());
        buttonOK.addActionListener(unused -> onOK());
        addWindowListener(
            new WindowAdapter() {
                @Override
                public void windowClosing(final WindowEvent unused) {
                    onCancel();
                }
            }
        );
        contentPane.registerKeyboardAction(
            unused -> onCancel(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );
        testConnectionButton.addActionListener(unused -> testConnection());
        final TextFieldListener textFieldListener = new TextFieldListener();
        username.getDocument().addDocumentListener(textFieldListener);
        password.getDocument().addDocumentListener(textFieldListener);
        jiraUrl.getDocument().addDocumentListener(textFieldListener);
        timeSpent.getDocument().addDocumentListener(textFieldListener);
        adjustmentDuration.getDocument().addDocumentListener(textFieldListener);
        jiraIssue.addItemListener(
            unused -> {
                checkEnablingConditions();
                final Object selectedItem = jiraIssue.getSelectedItem();
                if (selectedItem instanceof JiraIssue) {
                    final JiraIssue issue = (JiraIssue) selectedItem;
                    issueSummary.setText(
                        getJiraIssueHtml(
                            maxIssueSummaryWidth,
                            jiraUrl,
                            issue.prettySummary()
                        )
                    );
                    issueSummary.setVisible(true);
                    updateEstimate();
                } else {
                    issueSummary.setVisible(false);
                    timeEstimate.setText(null);
                }
            }
        );
        getJiraIssueSearchField().addKeyListener(new JiraIssueKeyListener());
        adjustEstimate.addItemListener(
            unused -> {
                final Object selectedItem = adjustEstimate.getSelectedItem();
                if (selectedItem instanceof AdjustEstimate) {
                    final AdjustEstimate estimate = (AdjustEstimate) selectedItem;
                    final String label = estimate.getAdjustmentDurationLabel();
                    if (label != null) {
                        adjustmentDuration.setText(null);
                        adjustmentDuration.setVisible(true);
                        adjustmentDurationLabel.setVisible(true);
                        adjustmentDurationLabel.setText(label);
                    } else {
                        adjustmentDuration.setVisible(false);
                        adjustmentDurationLabel.setVisible(false);
                    }
                } else {
                    adjustmentDuration.setVisible(false);
                    adjustmentDurationLabel.setVisible(false);
                }
                updateEstimate();
            }
        );
    }

    private void updateEstimate() {
        final Object jiraIssueSelectedItem = jiraIssue.getSelectedItem();
        if (jiraIssueSelectedItem instanceof JiraIssue) {
            final JiraIssue issue = (JiraIssue) jiraIssueSelectedItem;
            if (issue.getTimeEstimateSeconds() != null) {
                final Duration currentEstimate = Duration.ofSeconds(issue.getTimeEstimateSeconds());
                timeEstimate.setText(Util.formatAsJiraDuration(currentEstimate));
                final Object adjustEstimateSelectedItem = adjustEstimate.getSelectedItem();
                if (adjustEstimateSelectedItem instanceof AdjustEstimate) {
                    final AdjustEstimate adjEstimate = (AdjustEstimate) adjustEstimateSelectedItem;
                    final Duration adjusted = adjEstimate.adjust(
                        currentEstimate,
                        Util.parseJiraDuration(adjustmentDuration.getText()),
                        Util.parseJiraDuration(timeSpent.getText())
                    );
                    if (adjusted != null) {
                        timeEstimate.setText(
                            timeEstimate.getText() + " (" + Util.formatAsJiraDuration(adjusted) + " after adjustment)"
                        );
                    }
                }
            }
        }
    }

    private JTextField getJiraIssueSearchField() {
        return (JTextField) jiraIssue.getEditor().getEditorComponent();
    }

    private boolean testConnection() {
        final JiraClient client = JiraClient.getInstance(project);
        final char[] pass = password.getPassword();
        final String url = jiraUrl.getText();
        final String name = username.getText();
        final TodayWorklogSummaryResponse summary = client.getTodayWorklogSummary(
            url,
            name,
            new String(pass)
        );
        final boolean connectionOk;
        if (summary != null && StringUtils.isBlank(summary.getError())) {
            testConnectionResult.setText("Connection ok");
            testConnectionResult.setForeground(JBColor.GREEN);
            logged.setText(String.valueOf(summary.getSpentPretty()));
            remained.setText(String.valueOf(summary.getRemainedToLogPretty()));
            final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (state) {
                state.setJiraUrl(url);
            }
            final CredentialAttributes credentialAttributes = getCredentialAttributes(url);
            final Credentials credentials = new Credentials(username.getText(), new String(pass));
            PasswordSafe.getInstance().set(credentialAttributes, credentials);
            connectionOk = true;
        } else {
            testConnectionResult.setText(
                "Error" + (
                    summary != null && !StringUtils.isBlank(summary.getError()) ?
                    ": " + summary.getError() :
                    ""
                )
            );
            testConnectionResult.setForeground(JBColor.RED);
            logged.setText(null);
            remained.setText(null);
            connectionOk = false;
        }
        testConnectionResult.setVisible(true);
        Arrays.fill(pass, (char) 0);
        return connectionOk;
    }

    @NotNull
    private static CredentialAttributes getCredentialAttributes(final String url) {
        return new CredentialAttributes(
            CredentialAttributesKt.generateServiceName("JiraWorklogPlugin", url)
        );
    }

    private void onOK() {
        final JDialog dialog = new JDialog();
        try {
            final Object selectedItem = jiraIssue.getSelectedItem();
            final Duration duration = Util.parseJiraDuration(timeSpent.getText());
            if (
                selectedItem instanceof JiraIssue &&
                duration != null &&
                !duration.isZero()
            ) {
                dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
                dialog.setLocationRelativeTo(this);
                final int selected = JOptionPane.showConfirmDialog(
                    dialog,
                    "Log " + Util.formatAsJiraDuration(duration) + " to " + ((JiraIssue) selectedItem).getKey() + "?",
                    "Confirm",
                    JOptionPane.OK_CANCEL_OPTION
                );
                if (selected == JOptionPane.OK_OPTION) {
                    final char[] pass = password.getPassword();
                    final Object adjustEstimateSelectedItem = adjustEstimate.getSelectedItem();
                    final Duration adjDuration = Util.parseJiraDuration(adjustmentDuration.getText());
                    final AddWorklogResponse response = JiraClient.getInstance(project).addWorklog(
                        jiraUrl.getText(),
                        username.getText(),
                        new String(pass),
                        ((JiraIssue) selectedItem),
                        duration,
                        comment.getText(),
                        adjustEstimateSelectedItem instanceof AdjustEstimate ? ((AdjustEstimate) adjustEstimateSelectedItem) : null,
                        adjustEstimateSelectedItem instanceof AdjustEstimate && ((AdjustEstimate) adjustEstimateSelectedItem).getAdjustmentDurationLabel() != null ? adjDuration : null
                    );
                    addWorklogError.setVisible(false);
                    if (response != null && StringUtils.isBlank(response.getError())) {
                        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
                        //noinspection SynchronizationOnLocalVariableOrMethodParameter
                        synchronized (state) {
                            final Timer timer = state.getTimer(branchName, project);
                            timer.reset(project);
                            if (pauseTimerAfterReset) {
                                timer.pause(project);
                            }
                        }
                        dispose();
                    } else {
                        addWorklogError.setText(
                            "Error adding worklog" + (
                                response != null && !StringUtils.isBlank(response.getError()) ?
                                ": " + response.getError() :
                                ""
                            )
                        );
                        addWorklogError.setForeground(JBColor.RED);
                        addWorklogError.setVisible(true);
                    }
                    Arrays.fill(pass, (char) 0);
                }
            }
        } finally {
            dialog.dispose();
        }
    }

    private void onCancel() {
        dispose();
    }

    private void checkEnablingConditions() {
        final String url = JiraWorklogDialog.this.jiraUrl.getText();
        final String name = JiraWorklogDialog.this.username.getText();
        final char[] pass = JiraWorklogDialog.this.password.getPassword();
        final boolean jiraConnectionSettingsOk;
        final boolean jiraWorklogParamsOk;
        jiraConnectionSettingsOk = (
            !StringUtils.isBlank(url) &&
            Util.isValidUrl(url) &&
            url.startsWith("https://") &&
            !StringUtils.isBlank(name) &&
            pass != null &&
            pass.length >= 8
        );
        final Object jiraIssueSelectedItem = jiraIssue.getSelectedItem();
        final Object adjustEstimateSelectedItem = adjustEstimate.getSelectedItem();
        jiraWorklogParamsOk = (
            jiraIssueSelectedItem instanceof JiraIssue &&
            Util.isJiraDuration(timeSpent.getText()) &&
            !Util.parseJiraDuration(timeSpent.getText()).isZero() &&
            adjustEstimateSelectedItem instanceof AdjustEstimate &&
            (
                ((AdjustEstimate) adjustEstimateSelectedItem).getAdjustmentDurationLabel() == null ||
                Util.isJiraDuration(adjustmentDuration.getText())
            )
        );
        testConnectionButton.setEnabled(jiraConnectionSettingsOk);
        buttonOK.setEnabled(jiraWorklogParamsOk && jiraConnectionSettingsOk);
        if (pass != null) {
            Arrays.fill(pass, (char) 0);
        }
    }

    @NotNull
    private String getJiraIssueHtml(
        final int maxJiraIssueWidth,
        final Component component,
        final String text
    ) {
        final Font font = component.getFont();
        final FontMetrics metrics = component.getFontMetrics(font);
        final int spaceWidth = metrics.stringWidth(" ");
        final String[] words = text.split("\\s");
        int currentWidth = 0;
        final StringBuilder builder = new StringBuilder();
        builder.append("<html>");
        for (int i = 0; i < words.length; i++) {
            final String word = words[i];
            final int wordWidth = metrics.stringWidth(word);
            final boolean lastWord = i == words.length - 1;
            if (currentWidth + wordWidth + (lastWord ? 0 : spaceWidth) > maxJiraIssueWidth) {
                currentWidth = wordWidth;
                builder.append("<br>");
                builder.append(escapeHtml(word));
            } else {
                builder.append(word);
                currentWidth += wordWidth;
            }
            if (!lastWord) {
                builder.append(" ");
                currentWidth += spaceWidth;
            }
        }
        builder.append("</html>");
        return builder.toString();
    }

    public void afterPack() {
        this.maxIssueSummaryWidth = jiraIssue.getWidth() - 15;
    }

    private class TextFieldListener implements DocumentListener {

        @Override
        public void insertUpdate(final DocumentEvent e) {
            onEvent();
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            onEvent();
        }

        @Override
        public void changedUpdate(final DocumentEvent e) {
            onEvent();
        }

        private void onEvent() {
            checkEnablingConditions();
            updateEstimate();
        }

    }

    private class JiraIssueKeyListener implements KeyListener {

        @Override
        public void keyTyped(final KeyEvent e) {
//          Ничего не делаем
        }

        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                final Object selectedItem = jiraIssue.getSelectedItem();
                final String text = getJiraIssueSearchField().getText();
                if (
                    !(selectedItem instanceof JiraIssue) ||
                    !selectedItem.toString().equals(text)
                ) {
                    e.consume();
                    findIssues(text);
                }
            }
        }

        @Override
        public void keyReleased(final KeyEvent e) {
//          Ничего не делаем
        }

    }

}
