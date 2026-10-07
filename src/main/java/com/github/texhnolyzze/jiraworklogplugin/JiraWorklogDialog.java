package com.github.texhnolyzze.jiraworklogplugin;

import com.github.texhnolyzze.jiraworklogplugin.enums.AdjustEstimate;
import com.github.texhnolyzze.jiraworklogplugin.enums.AuthorizeWith;
import com.github.texhnolyzze.jiraworklogplugin.enums.HowToDetermineWhenUserStartedWorkingOnIssue;
import com.github.texhnolyzze.jiraworklogplugin.enums.WorklogGatherStrategyEnum;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.AddWorklogResponse;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.FindJiraIssuesResponse;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.JiraResponse;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.JiraIssue;
import com.github.texhnolyzze.jiraworklogplugin.jiraresponse.TodayWorklogSummaryResponse;
import com.github.texhnolyzze.jiraworklogplugin.timer.Timer;
import com.github.texhnolyzze.jiraworklogplugin.timer.TimerActionUtils;
import com.github.texhnolyzze.jiraworklogplugin.utils.EmailUtils;
import com.github.texhnolyzze.jiraworklogplugin.utils.JiraDurationUtils;
import com.github.texhnolyzze.jiraworklogplugin.utils.JiraKeyUtils;
import com.github.texhnolyzze.jiraworklogplugin.utils.JiraProfilesUtils;
import com.github.texhnolyzze.jiraworklogplugin.utils.Utils;
import com.google.common.html.HtmlEscapers;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import java.util.Arrays;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.github.texhnolyzze.jiraworklogplugin.utils.PluginCredentialsUtils.getCredentialAttributes;
import static java.util.function.Predicate.not;

public class JiraWorklogDialog extends JDialog {

    private final transient Project project;
    private final String branchName;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonReset;
    private JButton transferToButton;
    private JButton autoDialogsButton;
    private JTextField comment;
    private JTextField timeSpent;
    private JTextField remained;
    private JTextField todayTotalAfterLogging;
    private JTextField email;
    private JPasswordField password;
    private JButton testConnectionButton;
    private JLabel testConnectionResult;
    private JComboBox<String> jiraUrl;
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
    private Duration todayLoggedDuration;
    private boolean busy;
    private final String transferToBranch;

    public JiraWorklogDialog(
        final @NotNull Project project,
        final String branchName
    ) {
        this(project, branchName, null);
    }

    public JiraWorklogDialog(
        final @NotNull Project project,
        final String branchName,
        final String transferToBranch
    ) {
        this.project = project;
        this.branchName = branchName;
        this.transferToBranch = transferToBranch;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final Timer timer;
        synchronized (state) {
            timer = state.getTimer(branchName, project);
        }
        final String formatted = JiraDurationUtils.formatAsJiraDuration(timer.toDuration());
        this.timeSpent.setText(formatted);
        this.timeSpentSinceLastWorklogAdded.setText(
            "You spent " + formatted + " in " + branchName + " since you last logged from it "
        );
        setupListeners();
        for (final String knownUrl : JiraProfilesUtils.getKnownUrls()) {
            jiraUrl.addItem(knownUrl);
        }
        for (final AdjustEstimate estimate : AdjustEstimate.values()) {
            adjustEstimate.addItem(estimate);
        }
        adjustEstimate.setSelectedItem(AdjustEstimate.AUTO);
        updateAutoDialogsButton();
        if (transferToBranch != null && !transferToBranch.equals(branchName)) {
            transferToButton.setText("Transfer to " + transferToBranch);
            transferToButton.setToolTipText(
                "Move all time accumulated in " + branchName + " to " + transferToBranch
            );
            transferToButton.setEnabled(true);
            transferToButton.setVisible(true);
        }
    }

    public void init(final String jiraKey) {
        final boolean isJiraKey = JiraKeyUtils.isJiraKey(jiraKey);
        if (!setupJiraConnectionSettings()) {
            jiraUrl.requestFocus();
            if (isJiraKey) {
                getJiraIssueSearchField().setText(jiraKey);
            }
            return;
        }
        testConnection(jiraKey);
    }

    private void findIssues(final String input) {
        if (busy) {
            return;
        }
        final JiraClient client = JiraClient.getInstance(project);
        final JiraIssue.Criteria criteria = new JiraIssue.Criteria();
        final boolean isJiraKey = JiraKeyUtils.isJiraKey(input);
        if (isJiraKey) {
            criteria.setKey(input);
        } else {
            criteria.setSummary(input);
        }
        final String url = getJiraUrlText();
        final String emailText = email.getText();
        final char[] pass = password.getPassword();
        final String passText = new String(pass);
        Arrays.fill(pass, '\0');
        setBusy(true);
        jiraIssue.removeAllItems();
        ApplicationManager.getApplication().executeOnPooledThread(
            () -> {
                final FindJiraIssuesResponse response = callJiraOrError(
                    () -> client.findIssues(url, emailText, passText, criteria),
                    FindJiraIssuesResponse::error
                );
                ApplicationManager.getApplication().invokeLater(
                    () -> applyFindIssuesResult(input, response)
                );
            }
        );
    }

    private void applyFindIssuesResult(final String input, final FindJiraIssuesResponse response) {
        setBusy(false);
        if (!isShowing()) {
            return;
        }
        if (response != null && StringUtils.isBlank(response.getError())) {
            final NavigableSet<JiraIssue> issues = response.getIssues();
            for (final JiraIssue issue : issues.descendingSet()) {
                jiraIssue.addItem(issue);
            }
            findIssuesError.setText(null);
            findIssuesError.setVisible(false);
            jiraIssue.requestFocus();
            if (issues.size() > 1) {
                jiraIssue.showPopup();
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
        fitContent();
        findIssuesError.setForeground(JBColor.RED);
        issueSummary.setVisible(false);
        timeEstimate.setText(null);
    }

    private boolean setupJiraConnectionSettings() {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final String url = state.getJiraUrl();
        if (!StringUtils.isBlank(url)) {
            getJiraUrlField().setText(url);
            final CredentialAttributes attributes = getCredentialAttributes(url);
            final Credentials credentials = PasswordSafe.getInstance().get(attributes);
            if (credentials != null) {
                email.setText(credentials.getUserName());
                password.setText(credentials.getPasswordAsString());
                return true;
            }
        }
        return false;
    }

    private void prefillCredentialsFromKeychain() {
        final String url = getJiraUrlText();
        if (StringUtils.isBlank(url)) {
            return;
        }
        final Credentials credentials = PasswordSafe.getInstance().get(getCredentialAttributes(url));
        if (credentials != null) {
            email.setText(credentials.getUserName());
            password.setText(credentials.getPasswordAsString());
        }
    }

    private void setupListeners() {
        buttonCancel.addActionListener(unused -> onCancel());
        buttonOK.addActionListener(unused -> onOK());
        buttonReset.addActionListener(unused -> onReset());
        transferToButton.addActionListener(unused -> onTransfer());
        autoDialogsButton.addActionListener(unused -> toggleAllDialogs());
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
        testConnectionButton.addActionListener(unused -> testConnection(null));
        jiraUrl.addActionListener(unused -> prefillCredentialsFromKeychain());
        final TextFieldListener textFieldListener = new TextFieldListener();
        email.getDocument().addDocumentListener(textFieldListener);
        password.getDocument().addDocumentListener(textFieldListener);
        getJiraUrlField().getDocument().addDocumentListener(textFieldListener);
        timeSpent.getDocument().addDocumentListener(textFieldListener);
        adjustmentDuration.getDocument().addDocumentListener(textFieldListener);
        jiraIssue.addItemListener(
            unused -> {
                checkEnablingConditions();
                final Object selectedItem = jiraIssue.getSelectedItem();
                if (selectedItem instanceof final JiraIssue issue) {
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
                if (selectedItem instanceof final AdjustEstimate estimate) {
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

    private void onReset() {
        TimerActionUtils.resetTimer(branchName, project);
        dispose();
    }

    private void toggleAllDialogs() {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final boolean enableAll;
        synchronized (state) {
            enableAll = !(
                state.isShowDialogOnExit() ||
                state.isShowDialogOnBranchChange() ||
                state.isShowDialogOnGitPush()
            );
            state.setShowDialogOnExit(enableAll);
            state.setShowDialogOnBranchChange(enableAll);
            state.setShowDialogOnGitPush(enableAll);
        }
        updateAutoDialogsButton();
    }

    private void updateAutoDialogsButton() {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final boolean anyDialogEnabled;
        synchronized (state) {
            anyDialogEnabled = (
                state.isShowDialogOnExit() ||
                state.isShowDialogOnBranchChange() ||
                state.isShowDialogOnGitPush()
            );
        }
        autoDialogsButton.setText(
            anyDialogEnabled ? "Don't show dialogs automatically" : "Show dialogs automatically"
        );
    }

    private void onTransfer() {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        synchronized (state) {
            final Timer sourceTimer = state.getTimer(branchName, project);
            final Timer targetTimer = state.getTimer(transferToBranch, project);
            targetTimer.transfer(sourceTimer);
            state.getActiveTimers().remove(sourceTimer);
            state.getTimers().remove(branchName);
            for (final UnitOfWork unit : state.getTimeSeries()) {
                if (Objects.equals(unit.getBranch(), branchName)) {
                    unit.setBranch(transferToBranch);
                }
            }
        }
        dispose();
    }

    private void updateTodayTotalAfterLogging() {
        final Duration toLog = JiraDurationUtils.parseJiraDuration(timeSpent.getText());
        if (todayLoggedDuration == null || toLog == null) {
            todayTotalAfterLogging.setText(null);
            return;
        }
        todayTotalAfterLogging.setText(
            JiraDurationUtils.formatAsJiraDuration(todayLoggedDuration.plus(toLog))
        );
    }

    private void updateEstimate() {
        final Object jiraIssueSelectedItem = jiraIssue.getSelectedItem();
        if (jiraIssueSelectedItem instanceof final JiraIssue issue && issue.getTimeEstimateSeconds() != null) {
                final Duration currentEstimate = Duration.ofSeconds(issue.getTimeEstimateSeconds());
                timeEstimate.setText(JiraDurationUtils.formatAsJiraDuration(currentEstimate));
                final Object adjustEstimateSelectedItem = adjustEstimate.getSelectedItem();
                if (adjustEstimateSelectedItem instanceof final AdjustEstimate adjEstimate) {
                    final Duration adjusted = adjEstimate.adjust(
                        currentEstimate,
                        JiraDurationUtils.parseJiraDuration(adjustmentDuration.getText()),
                        JiraDurationUtils.parseJiraDuration(timeSpent.getText())
                    );
                    if (adjusted != null) {
                        timeEstimate.setText(
                            timeEstimate.getText() + " (" + JiraDurationUtils.formatAsJiraDuration(adjusted) + " after adjustment)"
                        );
                    }
                }
            }

    }

    private JTextField getJiraIssueSearchField() {
        return (JTextField) jiraIssue.getEditor().getEditorComponent();
    }

    /**
     * Runs a Jira request, converting any unexpected exception into an error response,
     * so the dialog always gets a result back and can never be stuck in a busy state
     */
    private <T extends JiraResponse> T callJiraOrError(
        final Supplier<T> call,
        final Function<String, T> errorFactory
    ) {
        try {
            return call.get();
        } catch (final Exception e) {
            return errorFactory.apply(ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private JTextField getJiraUrlField() {
        return (JTextField) jiraUrl.getEditor().getEditorComponent();
    }

    private String getJiraUrlText() {
        final Object item = jiraUrl.getEditor().getItem();
        return item == null ? null : item.toString();
    }

    private void testConnection(final String pendingJiraKey) {
        if (busy) {
            return;
        }
        final JiraClient client = JiraClient.getInstance(project);
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final String url = getJiraUrlText();
        final String emailText = email.getText();
        final char[] pass = password.getPassword();
        final String passText = new String(pass);
        Arrays.fill(pass, (char) 0);
        final WorklogGatherStrategyEnum strategy = state.getWorklogSummaryGatherStrategy();
        final HowToDetermineWhenUserStartedWorkingOnIssue how = state.getHowToDetermineWhenUserStartedWorkingOnIssue();
        setBusy(true);
        testConnectionResult.setText("Testing connection…");
        testConnectionResult.setForeground(JBColor.GRAY);
        testConnectionResult.setVisible(true);
        ApplicationManager.getApplication().executeOnPooledThread(
            () -> {
                final TodayWorklogSummaryResponse summary = callJiraOrError(
                    () -> client.getTodayWorklogSummary(url, emailText, passText, strategy, how),
                    TodayWorklogSummaryResponse::error
                );
                ApplicationManager.getApplication().invokeLater(
                    () -> applyTestConnectionResult(summary, pendingJiraKey, passText)
                );
            }
        );
    }

    private void applyTestConnectionResult(
        final TodayWorklogSummaryResponse summary,
        final String pendingJiraKey,
        final String passText
    ) {
        setBusy(false);
        if (!isShowing()) {
            return;
        }
        final JiraClient client = JiraClient.getInstance(project);
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        final String url = getJiraUrlText();
        final String emailText = email.getText();
        if (summary != null && StringUtils.isBlank(summary.getError())) {
            final AuthorizeWith authorizeWith = client.getAuthorizeWith(emailText, url);
            testConnectionResult.setText(
                    "<html>" +
                            "Connection ok" +
                            (
                                    authorizeWith == AuthorizeWith.USERNAME ?
                                    "<br>You are authorized with username (without @ and domains)" +
                                            "<br>Nothing wrong, just make sure you typed email correctly" :
                                    ""
                            ) +
                    "</html>"
            );
            testConnectionResult.setForeground(authorizeWith == AuthorizeWith.EMAIL ? JBColor.GREEN : JBColor.YELLOW);
            logged.setText(String.valueOf(summary.getSpentPretty()));
            remained.setText(String.valueOf(summary.getRemainedToLogPretty()));
            todayLoggedDuration = summary.getTimeSpent();
            updateTodayTotalAfterLogging();
            final Duration timeSpentViaExternalWorklogs = findTimeSpentViaExternalWorklogs(summary);
            if (timeSpentViaExternalWorklogs.compareTo(Duration.ZERO) > 0) {
                adjustTimeSpentForExternalWorklogs(state, timeSpentViaExternalWorklogs);
            }
            synchronized (state) {
                state.setJiraUrl(url);
            }
            if (url != null) {
                JiraProfilesUtils.addKnownUrl(url);
            }
            final CredentialAttributes credentialAttributes = getCredentialAttributes(url);
            final Credentials credentials = new Credentials(emailText, passText);
            PasswordSafe.getInstance().set(credentialAttributes, credentials);
            if (pendingJiraKey != null) {
                if (JiraKeyUtils.isJiraKey(pendingJiraKey)) {
                    findIssues(pendingJiraKey);
                } else {
                    jiraIssue.requestFocus();
                }
            }
        } else {
            testConnectionResult.setText(
                "<html>" +
                    "Error" + (
                        summary != null && !StringUtils.isBlank(summary.getError()) ?
                        ": " + summary.getError() :
                        ""
                    ) + "<br>" +
                    "Try to change worklog gather strategy<br>" +
                    "Tools -> Jira Worklog Plugin -> Worklog Gather Strategy<br>" +
                    "Make sure your credentials are correct<br>" +
                    "New versions of JIRA use API token" +
                "</html>"
            );
            testConnectionResult.setForeground(JBColor.RED);
            logged.setText(null);
            remained.setText(null);
            todayLoggedDuration = null;
            updateTodayTotalAfterLogging();
            jiraUrl.requestFocus();
            if (pendingJiraKey != null && JiraKeyUtils.isJiraKey(pendingJiraKey)) {
                getJiraIssueSearchField().setText(pendingJiraKey);
            }
        }
        testConnectionResult.setVisible(true);
        fitContent();
    }

    /**
     * Grows the dialog when its content no longer fits the base size defined in the form
     * (e.g. the long "Plugin also detected ..." message). A plain pack() would not help,
     * because the content pane has a fixed preferred size from the form, so the natural
     * size is measured with that fixed size temporarily removed
     */
    private void fitContent() {
        final Dimension fixedSize = contentPane.getPreferredSize();
        contentPane.setPreferredSize(null);
        final Dimension naturalSize = contentPane.getPreferredSize();
        contentPane.setPreferredSize(fixedSize);
        final Insets insets = getInsets();
        final int width = naturalSize.width + insets.left + insets.right;
        final int height = naturalSize.height + insets.top + insets.bottom;
        if (width > getWidth() || height > getHeight()) {
            setSize(Math.max(width, getWidth()), Math.max(height, getHeight()));
        }
    }

    private void adjustTimeSpentForExternalWorklogs(final JiraWorklogPluginState state, final Duration timeSpentViaExternalWorklogs) {
        final Timer timer;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (state) { // NOSONAR
            timer = state.getTimer(branchName, project);
        }
        final Duration timerDuration = timer.toDuration();
        final Duration adjusted = timerDuration.minus(timeSpentViaExternalWorklogs);
        timeSpent.setText(JiraDurationUtils.formatAsJiraDuration(adjusted));
        timeSpentSinceLastWorklogAdded.setText(
            "<html>" +
                "You spent " + JiraDurationUtils.formatAsJiraDuration(timerDuration) + " in " + branchName + " since you last logged from it.<br>" +
                "Plugin also detected today worklogs that intersect with current branch timer, " +
                "not created by it with total time " + JiraDurationUtils.formatAsJiraDuration(timeSpentViaExternalWorklogs) + ".<br>" +
                "This time was automatically subtracted from Time Spent" +
            "</html>"
        );
    }

    private Duration findTimeSpentViaExternalWorklogs(final TodayWorklogSummaryResponse summary) {
        final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
        Duration externalWorklogsTotal = Duration.ZERO;
        synchronized (state) {
            final List<UnitOfWork> currentBranchUnitsOfWork = state.getTimeSeries().stream().filter(
                work -> work.getBranch().equals(branchName)
            ).collect(Collectors.toList());
            currentBranchUnitsOfWork.add(state.actualUnitOfWorkForBranch(branchName, project));
            final List<JiraWorklog> externalWorklogs = summary.getWorklogs().stream().filter(
                not(worklog -> worklog.isIssuedByPlugin(project.getName()))
            ).toList();
            for (final UnitOfWork work : currentBranchUnitsOfWork) {
                for (final JiraWorklog worklog : externalWorklogs) {
                    final Duration intersection = work.findIntersection(worklog);
                    if (intersection.compareTo(Duration.ZERO) > 0) {
                        externalWorklogsTotal = externalWorklogsTotal.plus(intersection);
                    }
                }
            }
        }
        return externalWorklogsTotal;
    }

    private void onOK() {
        final JDialog dialog = new JDialog();
        try {
            final Object selectedItem = jiraIssue.getSelectedItem();
            final Duration duration = JiraDurationUtils.parseJiraDuration(timeSpent.getText());
            if (
                selectedItem instanceof JiraIssue issue &&
                duration != null &&
                !duration.isZero()
            ) {
                dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                dialog.setVisible(true);
                dialog.setLocationRelativeTo(this);
                final int selected = JOptionPane.showConfirmDialog(
                    dialog,
                    "Log " + JiraDurationUtils.formatAsJiraDuration(duration) + " to " + ((JiraIssue) selectedItem).getKey() + "?",
                    "Confirm",
                    JOptionPane.OK_CANCEL_OPTION
                );
                if (selected == JOptionPane.OK_OPTION) {
                    final String url = getJiraUrlText();
                    final String emailText = email.getText();
                    final String commentText = comment.getText();
                    final char[] pass = password.getPassword();
                    final String passText = new String(pass);
                    Arrays.fill(pass, (char) 0);
                    final Object adjustEstimateSelectedItem = adjustEstimate.getSelectedItem();
                    final AdjustEstimate estimate = adjustEstimateSelectedItem instanceof AdjustEstimate ?
                        (AdjustEstimate) adjustEstimateSelectedItem : null;
                    final Duration adjDuration = JiraDurationUtils.parseJiraDuration(adjustmentDuration.getText());
                    final HowToDetermineWhenUserStartedWorkingOnIssue how =
                        JiraWorklogPluginState.getInstance(project).getHowToDetermineWhenUserStartedWorkingOnIssue();
                    setBusy(true);
                    buttonOK.setText("Logging…");
                    ApplicationManager.getApplication().executeOnPooledThread(
                        () -> {
                            final AddWorklogResponse response = callJiraOrError(
                                () -> JiraClient.getInstance(project).addWorklog(
                                    url,
                                    emailText,
                                    passText,
                                    issue,
                                    duration,
                                    commentText,
                                    estimate,
                                    estimate != null && estimate.getAdjustmentDurationLabel() != null ? adjDuration : null,
                                    how
                                ),
                                AddWorklogResponse::error
                            );
                            ApplicationManager.getApplication().invokeLater(
                                () -> applyAddWorklogResult(response)
                            );
                        }
                    );
                }
            }
        } finally {
            dialog.dispose();
        }
    }

    private void applyAddWorklogResult(final AddWorklogResponse response) {
        setBusy(false);
        buttonOK.setText("OK");
        if (!isShowing()) {
            return;
        }
        addWorklogError.setVisible(false);
        if (response != null && StringUtils.isBlank(response.getError())) {
            final JiraWorklogPluginState state = JiraWorklogPluginState.getInstance(project);
            synchronized (state) {
                final Timer timer = state.getTimer(branchName, project);
                timer.reset(project);
                state.getTimeSeries().removeIf(work -> work.getBranch().equals(branchName));
            }
            final String url = getJiraUrlText();
            if (url != null) {
                JiraProfilesUtils.addKnownUrl(url);
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
            fitContent();
        }
    }

    private void onCancel() {
        dispose();
    }

    private void checkEnablingConditions() {
        final String url = getJiraUrlText();
        final String emailText = JiraWorklogDialog.this.email.getText();
        final char[] pass = JiraWorklogDialog.this.password.getPassword();
        final boolean jiraConnectionSettingsOk;
        final boolean jiraWorklogParamsOk;
        jiraConnectionSettingsOk = (
                !StringUtils.isBlank(url) &&
                        Utils.isValidUrl(url) &&
                        url.startsWith("https://") &&
                        !StringUtils.isBlank(emailText) &&
                        EmailUtils.isEmail(emailText) &&
                        pass != null &&
                        pass.length >= 8
        );
        final Object jiraIssueSelectedItem = jiraIssue.getSelectedItem();
        final Object adjustEstimateSelectedItem = adjustEstimate.getSelectedItem();
        jiraWorklogParamsOk = (
            jiraIssueSelectedItem instanceof JiraIssue &&
            JiraDurationUtils.isJiraDuration(timeSpent.getText()) &&
            !JiraDurationUtils.parseJiraDuration(timeSpent.getText()).isZero() &&
            adjustEstimateSelectedItem instanceof AdjustEstimate &&
            (
                ((AdjustEstimate) adjustEstimateSelectedItem).getAdjustmentDurationLabel() == null ||
                JiraDurationUtils.isJiraDuration(adjustmentDuration.getText())
            )
        );
        testConnectionButton.setEnabled(!busy && jiraConnectionSettingsOk);
        buttonOK.setEnabled(!busy && jiraWorklogParamsOk && jiraConnectionSettingsOk);
        if (pass != null) {
            Arrays.fill(pass, (char) 0);
        }
    }

    private void setBusy(final boolean busy) {
        this.busy = busy;
        checkEnablingConditions();
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
                builder.append(HtmlEscapers.htmlEscaper().escape(word));
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
            updateTodayTotalAfterLogging();
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
