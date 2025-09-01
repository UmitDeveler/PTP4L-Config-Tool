package com.ptpapp;

import com.jcraft.jsch.*;
import javafx.application.Platform;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SSHConnectionManager {
    
    private Session session;
    private Channel channel;
    private boolean isConnected = false;
    private String hostname;
    private String username;
    private int port;
    
    public SSHConnectionManager() {
        JSch.setConfig("StrictHostKeyChecking", "no");
    }
    
    public CompletableFuture<Boolean> connect(String hostname, int port, String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.hostname = hostname;
                this.username = username;
                this.port = port;
                
                JSch jsch = new JSch();
                session = jsch.getSession(username, hostname, port);
                session.setPassword(password);
                
                // Avoid asking for key confirmation
                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);
                
                session.connect(30000); // 30 second timeout
                isConnected = true;
                
                return true;
            } catch (JSchException e) {
                isConnected = false;
                return false;
            }
        });
    }
    
    public CompletableFuture<Boolean> connectWithKey(String hostname, int port, String username, String privateKeyPath, String passphrase) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                this.hostname = hostname;
                this.username = username;
                this.port = port;
                
                JSch jsch = new JSch();
                
                if (passphrase != null && !passphrase.isEmpty()) {
                    jsch.addIdentity(privateKeyPath, passphrase);
                } else {
                    jsch.addIdentity(privateKeyPath);
                }
                
                session = jsch.getSession(username, hostname, port);
                
                Properties prop = new Properties();
                prop.put("StrictHostKeyChecking", "no");
                session.setConfig(prop);
                
                session.connect(30000);
                isConnected = true;
                
                return true;
            } catch (JSchException e) {
                isConnected = false;
                return false;
            }
        });
    }
    
    public CompletableFuture<String> executeCommand(String command) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isConnected || session == null) {
                return "Error: Not connected to remote host";
            }
            
            try {
                Channel channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                
                channel.setInputStream(null);
                
                // Capture both stdout and stderr
                InputStream in = channel.getInputStream();
                InputStream err = ((ChannelExec) channel).getErrStream();
                
                channel.connect();
                
                StringBuilder output = new StringBuilder();
                StringBuilder errorOutput = new StringBuilder();
                byte[] tmp = new byte[1024];
                
                while (true) {
                    // Read stdout
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) break;
                        output.append(new String(tmp, 0, i));
                    }
                    
                    // Read stderr
                    while (err.available() > 0) {
                        int i = err.read(tmp, 0, 1024);
                        if (i < 0) break;
                        errorOutput.append(new String(tmp, 0, i));
                    }
                    
                    if (channel.isClosed()) {
                        // Read any remaining output
                        if (in.available() > 0 || err.available() > 0) continue;
                        break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (Exception ee) {}
                }
                
                int exitCode = channel.getExitStatus();
                channel.disconnect();
                
                // Combine output and error, include exit code info
                StringBuilder result = new StringBuilder();
                if (output.length() > 0) {
                    result.append(output.toString());
                }
                if (errorOutput.length() > 0) {
                    if (result.length() > 0) result.append("\n");
                    result.append("STDERR: ").append(errorOutput.toString());
                }
                if (exitCode != 0) {
                    if (result.length() > 0) result.append("\n");
                    result.append("Exit Code: ").append(exitCode);
                }
                
                return result.toString();
            } catch (JSchException | IOException e) {
                return "Error executing command: " + e.getMessage();
            }
        });
    }
    
    public void executeCommandWithLiveOutput(String command, Consumer<String> outputConsumer) {
        CompletableFuture.runAsync(() -> {
            if (!isConnected || session == null) {
                Platform.runLater(() -> outputConsumer.accept("Error: Not connected to remote host\n"));
                return;
            }
            
            try {
                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                
                channel.setInputStream(null);
                
                // Capture both stdout and stderr for live output
                InputStream in = channel.getInputStream();
                InputStream err = ((ChannelExec) channel).getErrStream();
                
                channel.connect();
                
                byte[] tmp = new byte[1024];
                
                while (true) {
                    // Read stdout
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i > 0) {
                            final String output = new String(tmp, 0, i);
                            Platform.runLater(() -> outputConsumer.accept(output));
                        }
                    }
                    
                    // Read stderr
                    while (err.available() > 0) {
                        int i = err.read(tmp, 0, 1024);
                        if (i > 0) {
                            final String errorOutput = "STDERR: " + new String(tmp, 0, i);
                            Platform.runLater(() -> outputConsumer.accept(errorOutput));
                        }
                    }
                    
                    if (channel.isClosed()) {
                        // Read any remaining output
                        if (in.available() > 0 || err.available() > 0) continue;
                        
                        // Report exit code
                        int exitCode = channel.getExitStatus();
                        if (exitCode != 0) {
                            Platform.runLater(() -> outputConsumer.accept("\n--- Command finished with exit code: " + exitCode + " ---\n"));
                        } else {
                            Platform.runLater(() -> outputConsumer.accept("\n--- Command completed successfully ---\n"));
                        }
                        break;
                    }
                    
                    try {
                        Thread.sleep(100);
                    } catch (Exception ee) {}
                }
                
            } catch (JSchException | IOException e) {
                Platform.runLater(() -> outputConsumer.accept("Error executing command: " + e.getMessage() + "\n"));
            }
        });
    }
    
    public void stopCurrentCommand() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
    }
    
    public void disconnect() {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        isConnected = false;
    }
    
    public boolean isConnected() {
        return isConnected && session != null && session.isConnected();
    }
    
    public String getConnectionInfo() {
        if (isConnected) {
            return username + "@" + hostname + ":" + port;
        }
        return "Not connected";
    }
} 