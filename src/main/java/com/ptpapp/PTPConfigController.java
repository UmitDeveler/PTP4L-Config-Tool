package com.ptpapp;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class PTPConfigController {

    // Configuration File Management
    @FXML private TextField remoteConfigPathField;
    @FXML private Button saveConfigButton;

    // SSH Connection
    @FXML private TextField sshHostField;
    @FXML private TextField sshPortField;
    @FXML private TextField sshUsernameField;
    @FXML private PasswordField sshPasswordField;
    @FXML private TextField sshKeyPathField;
    @FXML private PasswordField sshKeyPassphraseField;
    @FXML private CheckBox useSSHKeyCheck;
    @FXML private Button browseKeyButton;
    @FXML private Button connectSSHButton;
    @FXML private Button disconnectSSHButton;
    @FXML private Label connectionStatusLabel;
    @FXML private CheckBox enableSSHCheck;

    // Global Options - Clock Configuration
    @FXML private ComboBox<String> clockTypeCombo;
    @FXML private TextField domainNumberField;
    @FXML private TextField priority1Field;
    @FXML private TextField priority2Field;
    @FXML private TextField clockClassField;
    @FXML private TextField clockAccuracyField;

    // Global Options - Servo Configuration
    @FXML private ComboBox<String> clockServoCombo;
    @FXML private TextField stepThresholdField;
    @FXML private TextField firstStepThresholdField;
    @FXML private TextField maxFrequencyField;

    // Global Options - Flags
    @FXML private CheckBox clientOnlyCheck;
    @FXML private CheckBox twoStepFlagCheck;
    @FXML private CheckBox freeRunningCheck;
    @FXML private CheckBox assumeTwoStepCheck;
    @FXML private CheckBox kernelLeapCheck;
    @FXML private CheckBox useSyslogCheck;

    // Port Options - Network Transport
    @FXML private ComboBox<String> networkTransportCombo;
    @FXML private ComboBox<String> timeStampingCombo;
    @FXML private ComboBox<String> delayMechanismCombo;

    // Port Options
    @FXML private TextField logAnnounceIntervalField;
    @FXML private TextField logSyncIntervalField;
    @FXML private TextField operLogSyncInterval;
    @FXML private TextField logMinDelayReqIntervalField;
    @FXML private TextField logMinPdelayReqIntervalField;
    @FXML private TextField operLogPdelayReqInterval;
    @FXML private TextField announceReceiptTimeout;
    @FXML private TextField syncReceiptTimeout;
    @FXML private TextField delay_response_timeout;
    @FXML private TextField delayAsymmetryField;
    @FXML private TextField fault_reset_interval;
    @FXML private TextField neighborPropDelayThresh;
    @FXML private TextField serverOnly;
    @FXML private TextField G_8275_portDS_localPriority;
    @FXML private TextField allowedLostResponses;
    @FXML private TextField asCapable;	
    @FXML private TextField BMCA;
    @FXML private TextField inhibit_announce;
    @FXML private TextField inhibit_delay_req;
    @FXML private TextField ignore_source_id;
    @FXML private TextField power_profile_2011_grandmasterTimeInaccuracy;
    @FXML private TextField power_profile_2011_networkTimeInaccuracy;
    @FXML private TextField power_profile_2017_totalTimeInaccuracy;
    @FXML private TextField power_profile_grandmasterID;
    @FXML private TextField power_profile_version;
    @FXML private TextField ptp_minor_version;
    @FXML private TextField spp;
    @FXML private TextField active_key_id;


    @FXML private TextField ingressLatencyField;
    @FXML private TextField egressLatencyField;

    // Interface & Execution
    @FXML private TextField interfaceNameField;
    @FXML private CheckBox masterModeCheck;
    @FXML private CheckBox useSudoCheck;
    @FXML private CheckBox verboseCheck;
    @FXML private CheckBox quietCheck;
    @FXML private TextField printLevelField;

    // Control Buttons
    @FXML private Button startButton;
    @FXML private Button stopButton;
    @FXML private Button clearButton;
    @FXML private TextArea outputArea;

    private Stage stage;
    private Process ptpProcess;
    private boolean isRunning = false;
    private SSHConnectionManager sshManager;
    private boolean isSSHEnabled = false;
    private Properties configProperties = new Properties();

    @FXML
    public void initialize() {
        setupComboBoxes();
        setupButtons();
        setupOutputArea();
        loadDefaultValues();
        setupSSH();
    }

    private void setupComboBoxes() {
        // Clock Type options
        clockTypeCombo.setItems(FXCollections.observableArrayList(
            "OC (Ordinary Clock)", "BC (Boundary Clock)", "P2P_TC (P2P Transparent)", "E2E_TC (E2E Transparent)"
        ));
        clockTypeCombo.setValue("OC (Ordinary Clock)");

        // Clock Servo options
        clockServoCombo.setItems(FXCollections.observableArrayList(
            "pi", "linreg", "ntpshm", "refclock_sock", "nullf"
        ));
        clockServoCombo.setValue("pi");

        // Network Transport options
        networkTransportCombo.setItems(FXCollections.observableArrayList("UDPv4", "UDPv6", "L2"));
        networkTransportCombo.setValue("UDPv4");

        // Time Stamping options
        timeStampingCombo.setItems(FXCollections.observableArrayList(
            "hardware", "software", "legacy", "onestep", "p2p1step"
        ));
        timeStampingCombo.setValue("hardware");

        // Delay Mechanism options
        delayMechanismCombo.setItems(FXCollections.observableArrayList(
            "E2E", "P2P", "Auto", "NONE"
        ));
        delayMechanismCombo.setValue("E2E");
    }

    private void setupButtons() {
        // Configuration file management
        saveConfigButton.setOnAction(e -> saveAndUploadConfig());

        // PTP control
        startButton.setOnAction(e -> startPTP());
        stopButton.setOnAction(e -> stopPTP());
        clearButton.setOnAction(e -> clearOutput());

        stopButton.setDisable(true);
    }

    private void setupOutputArea() {
        outputArea.setEditable(false);
        outputArea.setWrapText(true);
    }

    private void loadDefaultValues() {
        // Default values based on ptp4l manual
        domainNumberField.setText("0");
        priority1Field.setText("128");
        priority2Field.setText("128");
        clockClassField.setText("248");
        clockAccuracyField.setText("0xFE");
        
        stepThresholdField.setText("0.0");
        firstStepThresholdField.setText("0.00002");
        maxFrequencyField.setText("900000000");

        // Default Port Options

        logAnnounceIntervalField.setText("1");
        logSyncIntervalField.setText("0");
        operLogSyncInterval.setText("0");
        logMinDelayReqIntervalField.setText("0");
        logMinPdelayReqIntervalField.setText("0");
        operLogPdelayReqInterval.setText("0");
        announceReceiptTimeout.setText("3");
        syncReceiptTimeout.setText("0");
        delay_response_timeout.setText("0");
        delayAsymmetryField.setText("0");
        fault_reset_interval.setText("4");
        neighborPropDelayThresh.setText("200000000");
        serverOnly.setText("0");
        G_8275_portDS_localPriority.setText("128");
        allowedLostResponses.setText("3");
        asCapable.setText("auto");
        BMCA.setText("ptp");
        inhibit_announce.setText("0");
        inhibit_delay_req.setText("0");
        ignore_source_id.setText("0");
        power_profile_2011_grandmasterTimeInaccuracy.setText("-1");
        power_profile_2011_networkTimeInaccuracy.setText("-1");
        power_profile_2017_totalTimeInaccuracy.setText("-1");
        power_profile.grandmasterID.setText("0");
        power_profile_version.setText("none");
        ptp_minor_version.setText("1");
        spp.setText("-1");
        active_key_id.setText("0");

        
        ingressLatencyField.setText("0");
        egressLatencyField.setText("0");
        
        interfaceNameField.setText("eth0");
        printLevelField.setText("6");
        
        sshPortField.setText("22");
        remoteConfigPathField.setText("~/ptp4l.conf");
        
        // Default flags
        useSudoCheck.setSelected(true);
        twoStepFlagCheck.setSelected(true);
        kernelLeapCheck.setSelected(true);
        useSyslogCheck.setSelected(true);
    }
    
    private void setupSSH() {
        sshManager = new SSHConnectionManager();
        
        // Setup SSH UI event handlers
        enableSSHCheck.setOnAction(e -> toggleSSHMode());
        connectSSHButton.setOnAction(e -> connectSSH());
        disconnectSSHButton.setOnAction(e -> disconnectSSH());
        browseKeyButton.setOnAction(e -> browseSSHKey());
        useSSHKeyCheck.setOnAction(e -> toggleSSHAuthMode());
        
        // Initial state
        updateSSHUIState();
        updateConnectionStatus();
    }
    
    private void toggleSSHMode() {
        isSSHEnabled = enableSSHCheck.isSelected();
        updateSSHUIState();
        updateButtonStates();
    }
    
    private void toggleSSHAuthMode() {
        boolean useKey = useSSHKeyCheck.isSelected();
        sshPasswordField.setDisable(useKey);
        sshKeyPathField.setDisable(!useKey);
        sshKeyPassphraseField.setDisable(!useKey);
        browseKeyButton.setDisable(!useKey);
    }
    
    private void updateSSHUIState() {
        boolean sshEnabled = enableSSHCheck.isSelected();
        
        sshHostField.setDisable(!sshEnabled);
        sshPortField.setDisable(!sshEnabled);
        sshUsernameField.setDisable(!sshEnabled);
        sshPasswordField.setDisable(!sshEnabled || useSSHKeyCheck.isSelected());
        sshKeyPathField.setDisable(!sshEnabled || !useSSHKeyCheck.isSelected());
        sshKeyPassphraseField.setDisable(!sshEnabled || !useSSHKeyCheck.isSelected());
        useSSHKeyCheck.setDisable(!sshEnabled);
        browseKeyButton.setDisable(!sshEnabled || !useSSHKeyCheck.isSelected());
        connectSSHButton.setDisable(!sshEnabled);
        disconnectSSHButton.setDisable(!sshEnabled || !sshManager.isConnected());
    }
    
    private void connectSSH() {
        String hostname = sshHostField.getText().trim();
        String portText = sshPortField.getText().trim();
        String username = sshUsernameField.getText().trim();
        
        outputArea.appendText("🔄 Starting SSH connection process...\n");
        outputArea.appendText("📡 Target: " + username + "@" + hostname + ":" + portText + "\n");
        
        if (hostname.isEmpty() || username.isEmpty()) {
            outputArea.appendText("❌ SSH Configuration Error: Missing hostname or username\n");
            showAlert("SSH Configuration Error", "Please enter hostname and username.");
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portText);
            outputArea.appendText("🔧 Using port: " + port + "\n");
        } catch (NumberFormatException e) {
            port = 22;
            outputArea.appendText("⚠️ Invalid port, defaulting to 22\n");
        }
        
        connectSSHButton.setDisable(true);
        connectionStatusLabel.setText("Connecting...");
        
        CompletableFuture<Boolean> connectionFuture;
        
        if (useSSHKeyCheck.isSelected()) {
            String keyPath = sshKeyPathField.getText().trim();
            String passphrase = sshKeyPassphraseField.getText();
            
            outputArea.appendText("🔑 Using SSH key authentication\n");
            outputArea.appendText("🔑 Key path: " + keyPath + "\n");
            
            if (keyPath.isEmpty()) {
                outputArea.appendText("❌ SSH Key Error: No key path specified\n");
                showAlert("SSH Key Error", "Please select an SSH private key file.");
                connectSSHButton.setDisable(false);
                updateConnectionStatus();
                return;
            }
            
            connectionFuture = sshManager.connectWithKey(hostname, port, username, keyPath, passphrase);
        } else {
            String password = sshPasswordField.getText();
            outputArea.appendText("🔐 Using password authentication\n");
            
            if (password.isEmpty()) {
                outputArea.appendText("❌ SSH Password Error: No password entered\n");
                showAlert("SSH Password Error", "Please enter the SSH password.");
                connectSSHButton.setDisable(false);
                updateConnectionStatus();
                return;
            }
            
            connectionFuture = sshManager.connect(hostname, port, username, password);
        }
        
        outputArea.appendText("⏳ Attempting connection...\n");
        
        connectionFuture.thenAccept(success -> {
            Platform.runLater(() -> {
                if (success) {
                    outputArea.appendText("✅ SSH connection established: " + sshManager.getConnectionInfo() + "\n");
                    outputArea.appendText("🔍 Running connection test...\n");
                    
                    // Test basic SSH command
                    sshManager.executeCommand("whoami && pwd && echo 'SSH_TEST_SUCCESS'").thenAccept(testResult -> {
                        Platform.runLater(() -> {
                            outputArea.appendText("🔍 SSH Test Result:\n" + testResult + "\n");
                            if (testResult.contains("SSH_TEST_SUCCESS")) {
                                outputArea.appendText("✅ SSH connection fully operational!\n");
                            } else {
                                outputArea.appendText("⚠️ SSH connected but command execution may have issues\n");
                            }
                        });
                    });
                } else {
                    outputArea.appendText("❌ SSH connection failed - check credentials and network\n");
                    showAlert("SSH Connection Failed", "Could not connect to the remote host. Please check your credentials and network connection.");
                }
                updateConnectionStatus();
                updateSSHUIState();
                updateButtonStates();
            });
        }).exceptionally(throwable -> {
            Platform.runLater(() -> {
                outputArea.appendText("💥 SSH connection exception: " + throwable.getMessage() + "\n");
                connectSSHButton.setDisable(false);
                updateConnectionStatus();
            });
            return null;
        });
    }
    
    private void disconnectSSH() {
        outputArea.appendText("🔄 Disconnecting SSH...\n");
        sshManager.disconnect();
        outputArea.appendText("✅ SSH connection closed.\n");
        updateConnectionStatus();
        updateSSHUIState();
        updateButtonStates();
    }
    
    private void browseSSHKey() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select SSH Private Key");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("SSH Keys", "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519")
        );
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            sshKeyPathField.setText(selectedFile.getAbsolutePath());
        }
    }
    
    private void updateConnectionStatus() {
        if (isSSHEnabled && sshManager.isConnected()) {
            connectionStatusLabel.setText("Connected: " + sshManager.getConnectionInfo());
            connectionStatusLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
        } else if (isSSHEnabled) {
            connectionStatusLabel.setText("Not connected");
            connectionStatusLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        } else {
            connectionStatusLabel.setText("Local mode");
            connectionStatusLabel.setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
        }
    }

    private void savePTP4LConfigFile(String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write header comment
            writer.write("# PTP4L Configuration - Generated by PTP Configuration Tool\n");
            writer.write("# " + java.time.LocalDateTime.now().toString() + "\n\n");
            
            // Write [global] section
            writer.write("[global]\n");
            
            // Write all configuration properties with space format (key value)
            for (String key : configProperties.stringPropertyNames()) {
                String value = configProperties.getProperty(key);
                if (value != null && !value.trim().isEmpty()) {
                    writer.write(key + " " + value + "\n");
                }
            }
        }
    }

    private void saveAndUploadConfig() {
        outputArea.appendText("\n=== SAVE & UPLOAD CONFIG STARTED ===\n");
        
        // Create a temporary config file
        String tempConfigPath = System.getProperty("java.io.tmpdir") + "/ptp4l_temp.conf";
        outputArea.appendText("📁 Temp config path: " + tempConfigPath + "\n");
        
        // Update configuration from UI
        outputArea.appendText("🔄 Reading configuration from UI fields...\n");
        updateConfigFromUI();
        outputArea.appendText("✅ Configuration read from UI completed\n");
        
        try {
            // Save to temp file with proper PTP4L format
            outputArea.appendText("💾 Saving configuration to temp file...\n");
            savePTP4LConfigFile(tempConfigPath);
            outputArea.appendText("✅ Configuration saved locally to: " + tempConfigPath + "\n");
            
            // Show what was saved for debugging
            try {
                String savedContent = Files.readString(Paths.get(tempConfigPath));
                outputArea.appendText("📄 Generated config content (" + savedContent.length() + " chars):\n");
                outputArea.appendText("--- CONFIG START ---\n" + savedContent + "--- CONFIG END ---\n");
            } catch (IOException readError) {
                outputArea.appendText("⚠️ Could not read saved config for display: " + readError.getMessage() + "\n");
            }
        } catch (IOException e) {
            outputArea.appendText("❌ Error saving configuration: " + e.getMessage() + "\n");
            showAlert("Save Error", "Could not save configuration file: " + e.getMessage());
            return;
        }

        // If SSH is enabled and connected, also upload to remote
        outputArea.appendText("🔍 Checking SSH status for upload...\n");
        outputArea.appendText("📊 SSH Enabled: " + isSSHEnabled + "\n");
        outputArea.appendText("📊 SSH Connected: " + (sshManager != null ? sshManager.isConnected() : "null manager") + "\n");
        
        if (isSSHEnabled && sshManager.isConnected()) {
            String remotePath = remoteConfigPathField.getText().trim();
            outputArea.appendText("📍 Remote path: '" + remotePath + "'\n");

            if (remotePath.isEmpty()) {
                outputArea.appendText("⚠️ No remote path specified. Configuration saved locally only.\n");
                outputArea.appendText("=== SAVE & UPLOAD CONFIG COMPLETED (LOCAL ONLY) ===\n");
                return;
            }

            outputArea.appendText("🚀 Starting remote upload process...\n");
            
            // Do all file I/O in background thread, then switch to UI thread for SSH operations
            CompletableFuture.runAsync(() -> {
                try {
                    // Read temp file in background thread (this is the blocking I/O)
                    String configContent = Files.readString(Paths.get(tempConfigPath));
                    
                    // Now switch to UI thread for SSH operations and UI updates
                    Platform.runLater(() -> {
                        outputArea.appendText("📖 Read " + configContent.length() + " characters from temp file\n");
                        outputArea.appendText("🔄 Uploading config to remote: " + remotePath + "\n");
                        
                        // Check directory (this is async SSH, won't block UI)
                        String dirPath = remotePath.substring(0, remotePath.lastIndexOf('/'));
                        outputArea.appendText("📁 Checking directory: " + dirPath + "\n");
                        
                        sshManager.executeCommand("ls -la " + dirPath).thenAccept(dirResult -> {
                            Platform.runLater(() -> {
                                outputArea.appendText("📁 Directory check result:\n" + dirResult + "\n");
                                
                                // Prepare upload command
                                boolean needsSudo = remotePath.startsWith("/etc/") || remotePath.startsWith("/usr/") || 
                                                  remotePath.startsWith("/var/") || remotePath.startsWith("/opt/");
                                
                                outputArea.appendText("📤 Attempting file upload" + (needsSudo ? " (redirected to home)" : "") + "...\n");
                                
                                String uploadCommand;
                                String actualPath;
                                
                                if (needsSudo) {
                                    // For system directories, write to user's home directory instead
                                    actualPath = "~/ptp4l.conf";
                                    String escapedContent = configContent.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$");
                                    uploadCommand = "echo \"" + escapedContent + "\" > " + actualPath;
                                    outputArea.appendText("📤 System directory detected - writing to user home: " + actualPath + "\n");
                                    outputArea.appendText("💡 You can manually copy this file to " + remotePath + " with: sudo cp ~/ptp4l.conf " + remotePath + "\n");
                                } else {
                                    // Regular echo for user directories
                                    actualPath = remotePath;
                                    String escapedContent = configContent.replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$");
                                    uploadCommand = "echo \"" + escapedContent + "\" > " + actualPath;
                                    outputArea.appendText("📤 Using echo method for user directory\n");
                                }
                                
                                outputArea.appendText("📤 Upload command: " + uploadCommand + "\n");
                                
                                // Execute upload command (async SSH, won't block UI)
                                sshManager.executeCommand(uploadCommand).thenAccept(result -> {
                                    Platform.runLater(() -> {
                                        outputArea.appendText("📥 Upload Response (" + result.length() + " chars): '" + result + "'\n");
                                        
                                        // Verify the upload (async SSH, won't block UI)
                                        outputArea.appendText("🔍 Verifying upload...\n");
                                        sshManager.executeCommand("ls -la " + actualPath + " && echo '--- File Content ---' && cat " + actualPath).thenAccept(verifyResult -> {
                                            Platform.runLater(() -> {
                                                outputArea.appendText("🔍 File verification result:\n" + verifyResult + "\n");
                                                if (verifyResult.contains("No such file")) {
                                                    outputArea.appendText("❌ File upload failed - file does not exist\n");
                                                } else if (verifyResult.contains("Permission denied")) {
                                                    outputArea.appendText("❌ File upload failed - permission denied\n");
                                                } else if (verifyResult.contains("[global]")) {
                                                    outputArea.appendText("✅ Configuration uploaded successfully!\n");
                                                } else {
                                                    outputArea.appendText("⚠️ Upload may have issues - please check file content above\n");
                                                }
                                                outputArea.appendText("=== SAVE & UPLOAD CONFIG COMPLETED ===\n");
                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                } catch (IOException e) {
                    Platform.runLater(() -> {
                        outputArea.appendText("❌ Error reading config file for upload: " + e.getMessage() + "\n");
                        outputArea.appendText("=== SAVE & UPLOAD CONFIG FAILED ===\n");
                    });
                }
            });
        } else if (isSSHEnabled && !sshManager.isConnected()) {
            outputArea.appendText("⚠️ SSH enabled but not connected. Configuration saved locally only.\n");
            outputArea.appendText("💡 Connect to SSH to upload to remote machine.\n");
            outputArea.appendText("=== SAVE & UPLOAD CONFIG COMPLETED (LOCAL ONLY) ===\n");
        } else {
            outputArea.appendText("ℹ️ SSH not enabled. Configuration saved locally only.\n");
            outputArea.appendText("=== SAVE & UPLOAD CONFIG COMPLETED (LOCAL ONLY) ===\n");
        }
    }


    private void updateConfigFromUI() {
        outputArea.appendText("🔧 Updating config from UI fields...\n");
        
        // Update configuration properties from UI
        configProperties.setProperty("domainNumber", domainNumberField.getText().trim());
        configProperties.setProperty("priority1", priority1Field.getText().trim());
        configProperties.setProperty("priority2", priority2Field.getText().trim());
        configProperties.setProperty("clockClass", clockClassField.getText().trim());
        configProperties.setProperty("clockAccuracy", clockAccuracyField.getText().trim());
        
        outputArea.appendText("📝 Basic config: domain=" + domainNumberField.getText().trim() + 
                            ", priority1=" + priority1Field.getText().trim() + 
                            ", priority2=" + priority2Field.getText().trim() + "\n");
        
        configProperties.setProperty("step_threshold", stepThresholdField.getText().trim());
        configProperties.setProperty("first_step_threshold", firstStepThresholdField.getText().trim());
        configProperties.setProperty("max_frequency", maxFrequencyField.getText().trim());

        // Port Options
        configProperties.setProperty("logAnnounceInterval", logAnnounceIntervalField.getText().trim());
        configProperties.setProperty("logSyncInterval", logSyncIntervalField.getText().trim());
        configProperties.setProperty("operLogSyncInterval", operLogSyncInterval.getText().trim());
        configProperties.setProperty("logMinDelayReqInterval", logMinDelayReqIntervalField.getText().trim());
        configProperties.setProperty("logMinPdelayReqInterval", logMinPdelayReqIntervalField.getText().trim());
        configProperties.setProperty("operLogPdelayReqInterval", operLogPdelayReqInterval.getText().trim());
        configProperties.setProperty("announceReceiptTimeout", announceReceiptTimeout.getText().trim());
        configProperties.setProperty("syncReceiptTimeout", syncReceiptTimeout.getText().trim());
        configProperties.setProperty("delay_response_timeout", delay_response_timeout.getText().trim());
        configProperties.setProperty("delayAsymmetry", delayAsymmetry.getText().trim());
        configProperties.setProperty("fault_reset_interval", fault_reset_interval.getText().trim());
        configProperties.setProperty("neighborPropDelayThresh", neighborPropDelayThresh.getText().trim());
        configProperties.setProperty("serverOnly", serverOnly.getText().trim());
        configProperties.setProperty("G.8275.portDS.localPriority", G_8275_portDS_localPriority.getText().trim());
        configProperties.setProperty("allowedLostResponses", allowedLostResponses.getText().trim());
        configProperties.setProperty("asCapable", asCapable.getText().trim());
        configProperties.setProperty("BMCA", BMCA.getText().trim());
        configProperties.setProperty("inhibit_announce", inhibit_announce.getText().trim());
        configProperties.setProperty("inhibit_delay_req", inhibit_delay_req.getText().trim());
        configProperties.setProperty("ignore_source_id", ignore_source_id.getText().trim());
        configProperties.setProperty("power_profile.2011.grandmasterTimeInaccuracy", power_profile_2011_grandmasterTimeInaccuracy.getText().trim());
        configProperties.setProperty("power_profile.2011.networkTimeInaccuracy", power_profile_2011_networkTimeInaccuracy.getText().trim());
        configProperties.setProperty("power_profile.2017.totalTimeInaccuracy", power_profile_2017_totalTimeInaccuracy.getText().trim());
        configProperties.setProperty("power_profile.grandmasterID", power_profile_grandmasterID.getText().trim());
        configProperties.setProperty("power_profile.version", power_profile_version.getText().trim());
        configProperties.setProperty("ptp_minor_version", ptp_minor_version.getText().trim());
        configProperties.setProperty("spp", spp.getText().trim());
        configProperties.setProperty("active_key_id", active_key_id.getText().trim());


        
        
        configProperties.setProperty("ingressLatency", ingressLatencyField.getText().trim());
        configProperties.setProperty("egressLatency", egressLatencyField.getText().trim());
        
        // Set combo box values
        String clockType = clockTypeCombo.getValue();
        if (clockType != null) {
            if (clockType.contains("OC")) configProperties.setProperty("clock_type", "OC");
            else if (clockType.contains("BC")) configProperties.setProperty("clock_type", "BC");
            else if (clockType.contains("P2P_TC")) configProperties.setProperty("clock_type", "P2P_TC");
            else if (clockType.contains("E2E_TC")) configProperties.setProperty("clock_type", "E2E_TC");
        }
        
        if (clockServoCombo.getValue() != null) {
            configProperties.setProperty("clock_servo", clockServoCombo.getValue());
        }
        if (networkTransportCombo.getValue() != null) {
            configProperties.setProperty("network_transport", networkTransportCombo.getValue());
        }
        if (timeStampingCombo.getValue() != null) {
            configProperties.setProperty("time_stamping", timeStampingCombo.getValue());
        }
        if (delayMechanismCombo.getValue() != null) {
            configProperties.setProperty("delay_mechanism", delayMechanismCombo.getValue());
        }
        
        // Set checkboxes
        configProperties.setProperty("clientOnly", clientOnlyCheck.isSelected() ? "1" : "0");
        configProperties.setProperty("twoStepFlag", twoStepFlagCheck.isSelected() ? "1" : "0");
        configProperties.setProperty("free_running", freeRunningCheck.isSelected() ? "1" : "0");
        configProperties.setProperty("assume_two_step", assumeTwoStepCheck.isSelected() ? "1" : "0");
        configProperties.setProperty("kernel_leap", kernelLeapCheck.isSelected() ? "1" : "0");
        configProperties.setProperty("use_syslog", useSyslogCheck.isSelected() ? "1" : "0");
        
        outputArea.appendText("✅ Config update completed - " + configProperties.size() + " properties set\n");
    }

    @FXML
    private void startPTP() {
        outputArea.appendText("\n=== START PTP4L COMMAND ===\n");
        
        if (isRunning) {
            outputArea.appendText("❌ PTP is already running\n");
            showAlert("PTP is already running", "Please stop the current process first.");
            return;
        }

        outputArea.appendText("🔧 Building PTP command...\n");
        List<String> command = buildPTPCommand();
        
        if (command.isEmpty()) {
            outputArea.appendText("❌ Failed to build command - invalid configuration\n");
            showAlert("Invalid Configuration", "Please check your configuration settings.");
            return;
        }

        outputArea.appendText("✅ Command built successfully:\n");
        outputArea.appendText("📤 Command: " + String.join(" ", command) + "\n");
        outputArea.appendText("🔍 SSH Enabled: " + isSSHEnabled + "\n");
        outputArea.appendText("🔍 SSH Connected: " + (sshManager != null ? sshManager.isConnected() : "null manager") + "\n");

        if (isSSHEnabled && sshManager.isConnected()) {
            // Run via SSH
            String sshCommand = String.join(" ", command);
            outputArea.appendText("🔄 Executing via SSH: " + sshManager.getConnectionInfo() + "\n");
            outputArea.appendText("📤 SSH Command: " + sshCommand + "\n");
            outputArea.appendText("--- SSH Output Start ---\n");
            
            sshManager.executeCommandWithLiveOutput(sshCommand, output -> {
                outputArea.appendText("📥 " + output);
                outputArea.setScrollTop(Double.MAX_VALUE);
            });
            
        } else if (isSSHEnabled && !sshManager.isConnected()) {
            showAlert("SSH Not Connected", "Please connect to the remote host first.");
            return;
            
        } else {
            // Run locally
            CompletableFuture.runAsync(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(command);
                    pb.redirectErrorStream(true);
                    ptpProcess = pb.start();

                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(ptpProcess.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String outputLine = line;
                            Platform.runLater(() -> {
                                outputArea.appendText(outputLine + "\n");
                                outputArea.setScrollTop(Double.MAX_VALUE);
                            });
                        }
                    }

                    int exitCode = ptpProcess.waitFor();
                    Platform.runLater(() -> {
                        outputArea.appendText("\nPTP4L process exited with code: " + exitCode + "\n");
                        isRunning = false;
                        updateButtonStates();
                    });

                } catch (IOException | InterruptedException e) {
                    Platform.runLater(() -> {
                        outputArea.appendText("Error: " + e.getMessage() + "\n");
                        isRunning = false;
                        updateButtonStates();
                    });
                }
            });
        }

        isRunning = true;
        updateButtonStates();
    }

    @FXML
    private void stopPTP() {
        if (isSSHEnabled && sshManager.isConnected()) {
            // Stop via SSH
            sshManager.stopCurrentCommand();
            outputArea.appendText("PTP4L process stopped via SSH.\n");
        } else if (ptpProcess != null && ptpProcess.isAlive()) {
            // Stop local process
            ptpProcess.destroy();
            outputArea.appendText("PTP4L process stopped.\n");
        }
        isRunning = false;
        updateButtonStates();
    }

    @FXML
    private void clearOutput() {
        outputArea.clear();
    }

    private List<String> buildPTPCommand() {
        List<String> command = new ArrayList<>();
        
        // Add sudo if enabled
        if (useSudoCheck.isSelected()) {
            command.add("sudo");
        }
        
        command.add("ptp4l");

        // Add configuration file if specified
        String configPath = remoteConfigPathField.getText().trim();
        if (!configPath.isEmpty()) {
            command.add("-f");
            command.add(configPath);
        }

        // Add interface name if specified
        String interfaceName = interfaceNameField.getText().trim();
        if (!interfaceName.isEmpty()) {
            command.add("-i");
            command.add(interfaceName);
        }

        // Add master/slave mode
        if (masterModeCheck.isSelected()) {
            // Master mode: -m only
            command.add("-m");
        } else {
            // Slave mode: -m -s
            command.add("-m");
            command.add("-s");
        }

        // Add print level if specified
        String printLevel = printLevelField.getText().trim();
        if (!printLevel.isEmpty()) {
            command.add("-l");
            command.add(printLevel);
        }

        // Add quiet option
        if (quietCheck.isSelected()) {
            command.add("-q");
        }

        // Add verbose option
        if (verboseCheck.isSelected()) {
            command.add("-v");
        }

        return command;
    }

    private void updateButtonStates() {
        boolean canStart = !isRunning && (!isSSHEnabled || sshManager.isConnected());
        startButton.setDisable(!canStart);
        stopButton.setDisable(!isRunning);
        
        // Disable PTP controls when SSH is enabled but not connected
        if (isSSHEnabled && !sshManager.isConnected()) {
            startButton.setDisable(true);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }
} 
