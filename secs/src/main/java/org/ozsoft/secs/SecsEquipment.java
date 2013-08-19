package org.ozsoft.secs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.ozsoft.secs.format.A;
import org.ozsoft.secs.format.B;
import org.ozsoft.secs.format.Data;
import org.ozsoft.secs.format.L;
import org.ozsoft.secs.message.S1F1;
import org.ozsoft.secs.message.S1F13;
import org.ozsoft.secs.message.S1F15;
import org.ozsoft.secs.message.S1F17;
import org.ozsoft.secs.message.S2F25;

/**
 * SECS/GEM equipment. <br />
 * <br />
 * 
 * This is the core class of the SECS/GEM library.
 * 
 * @author Oscar Stigter
 */
public class SecsEquipment {
    
    private static final int MIN_DEVICE_ID = 0;

    private static final int MAX_DEVICE_ID = 32767;

    private static final int MIN_PORT = 1025;

    private static final int MAX_PORT = 65535;

    private static final long POLL_INTERVAL = 10L;

    private static final int BUFFER_SIZE = 8192;

    private static final Logger LOG = Logger.getLogger(SecsEquipment.class);

    private final Map<Integer, MessageHandler> messageHandlers;

    private final Set<SecsEquipmentListener> listeners;
    
    private final Map<Long, Transaction> transactions;
    
    private int deviceId = SecsConstants.DEFAULT_DEVICE_ID;

    private String modelName = SecsConstants.DEFAULT_MDLN;
    
    private String softRev = SecsConstants.DEFAULT_SOFTREV;

    private ConnectMode connectMode = SecsConstants.DEFAULT_CONNECT_MODE;

    private String host = SecsConstants.DEFAULT_HOST;

    private int port = SecsConstants.DEFAULT_PORT;

    private boolean isEnabled;

    private ConnectionState connectionState;

    private CommunicationState communicationState;

    private ControlState controlState;

    private Thread connectionThread;
    
    private Socket socket;
    
    private long nextTransactionId = 1L;
    
    public SecsEquipment() {
        listeners  = new HashSet<SecsEquipmentListener>();
        messageHandlers = new HashMap<Integer, MessageHandler>();
        transactions = new HashMap<Long, Transaction>();
        
        addDefaultMessageHandlers();
        
        isEnabled = false;
        setConnectionState(ConnectionState.NOT_CONNECTED);
        setCommunicationState(CommunicationState.NOT_ENABLED);
        setControlState(ControlState.EQUIPMENT_OFFLINE);
    }
    
    public int getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(int deviceId) throws SecsConfigurationException {
        if (deviceId < MIN_DEVICE_ID || deviceId > MAX_DEVICE_ID) {
            throw new SecsConfigurationException("Invalid device ID: " + deviceId);
        }
        this.deviceId = deviceId;
    }

    public String getModelName() {
        return modelName;
    }
    
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getSoftRev() {
        return softRev;
    }
    
    public void setSoftRev(String softRev) {
        this.softRev = softRev;
    }

    public ConnectMode getConnectMode() {
        return connectMode;
    }

    public void setConnectMode(ConnectMode connectMode) {
        this.connectMode = connectMode;
        LOG.info("Connect Mode set to " + connectMode);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) throws SecsConfigurationException {
        if (host == null || host.isEmpty()) {
            throw new SecsConfigurationException(String.format("Invalid host: '%s'", host));
        }
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) throws SecsConfigurationException {
        if (port < MIN_PORT || port > MAX_PORT) {
            throw new SecsConfigurationException("Invalid port number: " + port);
        }
        this.port = port;
    }
    
    public ConnectionState getConnectionState() {
        return connectionState;
    }

    public void setConnectionState(ConnectionState connectionState) {
        if (this.connectionState != connectionState) {
            this.connectionState = connectionState;
            for (SecsEquipmentListener listener : listeners) {
                listener.connectionStateChanged(connectionState);
            }
            LOG.info("Connection State set to " + connectionState);
        }
    }

    public CommunicationState getCommunicationState() {
        return communicationState;
    }

    public void setCommunicationState(CommunicationState communicationState) {
        if (this.communicationState != communicationState) {
            this.communicationState = communicationState;
            LOG.info("Communication State set to " + communicationState);
            for (SecsEquipmentListener listener : listeners) {
                listener.communicationStateChanged(communicationState);
            }
        }
    }

    public ControlState getControlState() {
        return controlState;
    }

    public void setControlState(ControlState controlState) {
        if (this.controlState != controlState) {
            this.controlState = controlState;
            LOG.info("Control State set to " + controlState);
            for (SecsEquipmentListener listener : listeners) {
                listener.controlStateChanged(controlState);
            }
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean isEnabled) throws SecsException {
        if (isEnabled) {
            // Enable equipment.
            if (!isEnabled()) {
                enable();
            } else {
                throw new SecsException("Already enabled");
            }
        } else {
            // Disable equipment.
            if (isEnabled()) {
                disable();
            } else {
                throw new SecsException("Already disabled");
            }
        }
    }

    public void addMessageHandler(MessageHandler handler) {
        handler.setEquipment(this);
        int stream = handler.getStream();
        int function = handler.getFunction();
        int messageId = stream * 256 + function;
        messageHandlers.put(messageId, handler);
        LOG.info(String.format("Added message handler for %s", handler));
    }

    public void removeMessageHandler(MessageHandler handler) {
        int messageId = handler.getStream() * 256 + handler.getFunction();
        messageHandlers.remove(messageId);
    }
    
    public void addListener(SecsEquipmentListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(SecsEquipmentListener listener) {
        listeners.remove(listener);
    }
    
    public DataMessage sendDataMessage(int stream, int function, boolean withReply, Data<?> text) throws SecsException {
        if (communicationState != CommunicationState.COMMUNICATING) {
            throw new SecsException("Communication State not COMMUNICATING");
        }
        
        DataMessage requestMessage = new DataMessage(deviceId, stream, function, withReply, getNextTransactionId(), text);
        DataMessage replyMessage = null;
        try {
            replyMessage = (DataMessage) sendMessage(requestMessage, socket.getOutputStream(), false);
        } catch (IOException e) {
            String msg = "Internal error while sending message"; 
            LOG.error(msg, e);
            throw new SecsException(msg, e);
        }
        
        return replyMessage;
    }
    
    private Message sendMessage(Message message, OutputStream os, boolean asynchronous) throws SecsTimeoutException, IOException {
        LOG.trace(String.format("Send message %s", message));
        
        os.write(message.toByteArray());
        os.flush();
        
        for (SecsEquipmentListener listener : listeners) {
            listener.messageSent(message);
        }
        
        Message replyMessage = null;
        if (message instanceof DataMessage) {
            DataMessage dataMessage = (DataMessage) message;
            if (!asynchronous && dataMessage.withReply()) {
                long transactionId = dataMessage.getTransactionId();
                startTransaction(dataMessage);
                long startTime = System.currentTimeMillis();
                while (replyMessage == null) {
                    sleep(POLL_INTERVAL);
                    synchronized (transactions) {
                        Transaction transaction = transactions.get(transactionId);
                        if (transaction != null) {
                            replyMessage = transaction.getReplyMessage();
                        }
                    }
                    if (replyMessage == null && (System.currentTimeMillis() - startTime) > SecsConstants.DEFAULT_T3_TIMEOUT) {
                        String msg = String.format("T3 timeout for request message %s with transaction ID %d", dataMessage.getType(), transactionId); 
                        LOG.warn(msg);
                        throw new SecsTimeoutException(msg);
                    }
                }
            }
        }
        return replyMessage;
    }

    private void enable() {
        isEnabled = true;
        setCommunicationState(CommunicationState.NOT_COMMUNICATING);
        if (connectMode == ConnectMode.ACTIVE) {
            // Active mode; establish HSMS connection (client).
            connectionThread = new ActiveConnectionThread();
        } else {
            // Passive mode; accept incoming HSMS connection (server).
            connectionThread = new PassiveConnectionThread();
        }
        connectionThread.start();
    }

    private void disable() {
        if (connectionState != ConnectionState.NOT_CONNECTED) {
            Message message = new ControlMessage(deviceId, 0x00, 0x00, SType.SEPARATE, getNextTransactionId());
            try {
                OutputStream os = socket.getOutputStream();
                os.write(message.toByteArray());
                os.flush();
            } catch (IOException e) {
                LOG.error("Internal error while sending SEPARATE message", e);
            }
        }

        isEnabled = false;
        while (communicationState != CommunicationState.NOT_COMMUNICATING) {
            sleep(POLL_INTERVAL);
        }
        
        setCommunicationState(CommunicationState.NOT_ENABLED);
    }

    private void addDefaultMessageHandlers() {
        addMessageHandler(new S1F1());  // Are You There (R)
        addMessageHandler(new S1F13()); // Establish Communication Request // (CR)
        addMessageHandler(new S1F15()); // Request OFF-LINE (ROFL)
        addMessageHandler(new S1F17()); // Request ON-LINE (RONL)
        addMessageHandler(new S2F25()); // Request Loopback Diagnostic Request (LDR)
    }

    private void handleConnection() {
        String clientHost = socket.getInetAddress().getHostName();
        LOG.info(String.format("Connected with host '%s'", clientHost));
        setConnectionState(ConnectionState.NOT_SELECTED);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = socket.getInputStream();
            os = socket.getOutputStream();
            byte[] buf = new byte[BUFFER_SIZE];
            while (isEnabled && getConnectionState() != ConnectionState.NOT_CONNECTED) {
                if (connectMode == ConnectMode.ACTIVE && connectionState == ConnectionState.NOT_SELECTED) {
                    // Not selected yet; send SELECT_REQ.
                    Message message = new ControlMessage(deviceId, 0x00, 0x00, SType.SELECT_REQ, getNextTransactionId());
                    sendMessage(message, os, true);
                    startTransaction(message);
                    sleep(100L);
                } else if (connectMode == ConnectMode.ACTIVE && connectionState == ConnectionState.SELECTED && communicationState == CommunicationState.NOT_COMMUNICATING) {
                    // Not communicating yet; send S1F13 (Communication Request).
                    L text = new L();
                    text.addItem(new A(modelName));
                    text.addItem(new A(softRev));
                    Message message = new DataMessage(deviceId, 1, 13, true, getNextTransactionId(), text);
                    sendMessage(message, os, true);
                    sleep(100L);
                }
                
                if (is.available() > 0) {
                    int length = is.read(buf);
                    try {
                        Message requestMessage = MessageParser.parseMessage(buf, length);
                        LOG.trace(String.format("Received message: %s", requestMessage));
                        Message replyMessage = handleMessage(requestMessage);
                        if (replyMessage != null) {
                            sendMessage(replyMessage, os, true);
                        }
                        
                    } catch (SecsParseException e) {
                        LOG.error("Received invalid SECS message: " + e.getMessage());
                        
                    } catch (SecsException e) {
                        LOG.error("Internal SECS error while handling message", e);
                    }
                } else {
                    sleep(POLL_INTERVAL);
                }
                
//                checkTransactions();
            }
        } catch (Exception e) {
            // Internal error (should never happen).
            LOG.error("Internal error while handling connection", e);
        }

        // Disconnect.
        IOUtils.closeQuietly(os);
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(socket);
        disconnect();
    }

    private Message handleMessage(Message message) throws SecsException {
        int sessionId = message.getSessionId();
        long transactionId = message.getTransactionId();
        updateTransactionId(transactionId);

        Message replyMessage = null;

        if (message instanceof ControlMessage) {
            // HSMS control message.
            ControlMessage controlMessage = (ControlMessage) message;
            SType sType = controlMessage.getSType();
            int headerByte3 = controlMessage.getHeaderByte3();
            switch (sType) {
                case SELECT_REQ:
                    // Always accept SELECT_REQ.
                    if (getConnectionState() == ConnectionState.NOT_SELECTED) {
                        headerByte3 = 0x00; // SelectStatus: Communication Established
                        setConnectionState(ConnectionState.SELECTED);
                    } else {
                        headerByte3 = 0x01; // SelectStatus: Communication Already Active
                    }
                    
                    replyMessage = new ControlMessage(sessionId, 0x00, headerByte3, SType.SELECT_RSP, transactionId);
                    break;

                case SELECT_RSP:
                    if (endTransaction(transactionId)) {
                        int selectStatus = controlMessage.getHeaderByte3();
                        if (selectStatus == 0x00) { // SelectStatus: Communication Established
                            LOG.debug("Received SELECT_RSP message with SelectStatus: Communication Established");
                            setConnectionState(ConnectionState.SELECTED);
                        } else if (selectStatus == 0x01) { // SelectStatus: Communication Already Active
                            LOG.debug("Received SELECT_RSP message with SelectStatus: Communication Already Active");
                            setConnectionState(ConnectionState.SELECTED);
                        } else if (selectStatus == 0x02) { // SelectStatus: Connection Not Ready
                            LOG.warn("Received SELECT_RSP message with SelectStatus: Connection Not Ready -- Communication failed");
                            setConnectionState(ConnectionState.NOT_SELECTED);
                        } else if (selectStatus == 0x03) { // SelectStatus: Connect Exhaust
                            LOG.warn("Received SELECT_RSP message with SelectStatus: Connect Exhaust -- Communication failed");
                            setConnectionState(ConnectionState.NOT_SELECTED);
                        } else {
                            LOG.warn("Received SELECT_RSP message with invalid SelectStatus: " + selectStatus);
                        }
                    } else {
                        LOG.warn("Unexpected SELECT_RSP received -- ignored");
                    }
                    break;

                case DESELECT_REQ:
                    // Acknowledge DESELECT_REQ if selected, otherwise fail.
                    if (getConnectionState() == ConnectionState.SELECTED) {
                        headerByte3 = 0x00; // DeselectStatus: Success
                        setConnectionState(ConnectionState.NOT_SELECTED);
                    } else {
                        headerByte3 = 0x01; // DeselectStatus: Failed
                    }
                    replyMessage = new ControlMessage(sessionId, 0x00, headerByte3, SType.DESELECT_RSP, transactionId);
                    break;

                case DESELECT_RSP:
                    if (endTransaction(transactionId)) {
                        int deselectStatus = controlMessage.getHeaderByte3();
                        if (deselectStatus == 0x00) { // Accept
                            LOG.debug("Received DESELECT_RSP message with DeselectStatus: Success");
                            setConnectionState(ConnectionState.NOT_SELECTED);
                        } else {
                            LOG.debug("Received DESELECT_RSP message with DeselectStatus: Failed");
                        }
                    } else {
                        LOG.warn("Unexpected DESELECT_RSP received -- ignored");
                    }
                    break;

                case SEPARATE:
                    // Immediately disconnect on SEPARATE message.
                    LOG.debug("Received SEPARATE message");
                    disconnect();
                    break;

                case LINKTEST_REQ:
                    // Send LINKTEST_RSP message.
                    replyMessage = new ControlMessage(sessionId, 0x00, 0x00, SType.LINKTEST_RSP, transactionId);
                    break;

                case LINKTEST_RSP:
                    if (!endTransaction(transactionId)) {
                        LOG.warn("Unexpected LINKTEST_RSP received -- ignored");
                    }
                    break;

                case REJECT:
                    // Always accept REJECT; no action required.
                    LOG.warn("Received REJECT message");
                    break;

                default:
                    LOG.error("Unsupported control message type: " + sType);
            }
        } else {
            // Data message (standard GEM or custom).
            DataMessage dataMessage = (DataMessage) message;
            int stream = dataMessage.getStream();
            int function = dataMessage.getFunction();

            if (function == 0) {
                // Received FxS0 (ABORT) message; nothing to do.
            } else if (stream == 9) {
                // Steam 9 is reserved for generic errors.
                if (endTransaction(transactionId)) {
                    LOG.warn(String.format("Received %s for transaction %d", dataMessage.getType(), transactionId));
                } else {
                    LOG.warn(String.format("Received unexpected %s -- ignored", dataMessage.getType()));
                }
            } else if (stream == 1 && function == 14) {
                // S1F14 (Communication Request Acknowledge).
                handleS1F14(dataMessage);
            } else {
                // Check whether message is reply for active request.
                synchronized (transactions) {
                    Transaction transaction = transactions.get(transactionId);
                    if (transaction != null) {
                        // Active request message found; set reply message to be processed.
                        transaction.setReplyMessage(dataMessage);
                    } else {
                        // Request message; process by specific message handler.
                        MessageHandler handler = messageHandlers.get(stream * 256 + function);
                        if (handler != null) {
                            LOG.trace("Handle data message " + handler);
                            replyMessage = handler.handle(dataMessage);
                        } else {
                            // Unsupported message; send ABORT.
                            LOG.warn(String.format("Unsupported data message (%s) -- ABORT", dataMessage.getType()));
                            replyMessage = new DataMessage(sessionId, stream, 0, false, transactionId, null);
                        }
                    }
                }
            }
        }

        return replyMessage;
    }
    
    /**
     * Handle S1F14 (Communication Request Acknowledge). <br />
     * <br />
     * 
     * Format:
     * <pre>
     * <L:2
     *     COMMACK        // <B:1>
     *     <L:2
     *          MDLN      // <A:20>
     *          SOFTREV   // <A:20>
     *     >
     * >
     * </pre>   
     * 
     * @param message
     *            The message.
     * 
     * @throws SecsParseException
     *             If the message is invalid.
     */
    private void handleS1F14(DataMessage message) throws SecsParseException {
        Data<?> text = message.getText();
        if (text == null) {
            throw new SecsParseException("Invalid S1F14 message: empty text");
        }
        if (!(text instanceof L)) {
            throw new SecsParseException("Invalid S1F14 message: text not L");
        }
        L l = (L) text;
        if (l.length() != 2) {
            throw new SecsParseException("Invalid S1F14 message: L must have length of 2");
        }
        text = l.getItem(0);
        if (!(text instanceof B)) {
            throw new SecsParseException("Invalid S1F14 message: First element of L must be B");
        }
        B b = (B) text;
        if (b.length() != 1) {
            throw new SecsParseException("Invalid S1F14 message: B must have length of 1 (COMMACK)");
        }
        int commAck = b.get(0);
        if (commAck == 0x00) { // Accepted
            LOG.debug("Received S1F14 message with COMMACK: Accepted");
            setCommunicationState(CommunicationState.COMMUNICATING);
        } else if (commAck == 0x01) { // Denied, Try Again
            LOG.warn("Received S1F14 message with COMMACK: Denied, Try Again");
        } else {
            LOG.warn("Received S1F14 with invalid COMMACK value");
        }
    }
    
    private void disconnect() {
        setCommunicationState(CommunicationState.NOT_COMMUNICATING);
        setConnectionState(ConnectionState.NOT_CONNECTED);
        connectionThread.interrupt();
        LOG.info("Disconnected");
    }
    
    private long getNextTransactionId() {
        return nextTransactionId++;
    }
    
    private void updateTransactionId(long transactionId) {
        if (transactionId > nextTransactionId) {
            nextTransactionId = transactionId + 1;
        }
    }
    
    private void startTransaction(Message requestMessage) {
        synchronized (transactions) {
            long transactionId = requestMessage.getTransactionId();
            transactions.put(transactionId, new Transaction(requestMessage));
            LOG.trace(String.format("Transaction %d started", transactionId));
        }
    }
    
    private boolean endTransaction(long transactionId) {
        synchronized (transactions) {
            if (transactions.containsKey(transactionId)) {
                transactions.remove(transactionId);
                LOG.trace(String.format("Transaction %d ended", transactionId));
                return true;
            } else {
                LOG.warn(String.format("Reply message received for unknown transaction %d", transactionId));
                return false;
            }
        }
    }
    
//    private Message getRequestMessage(long transactionId) {
//        Message requestMessage = null;
//        synchronized (transactions) {
//            Transaction transaction = transactions.get(transactionId);
//            if (transaction != null) {
//                requestMessage = transaction.getRequestMessage();
//            }
//        }
//        return requestMessage;
//    }
//    
//    private Message getReplyMessage(long transactionId) {
//        Message replyMessage = null;
//        synchronized (transactions) {
//            Transaction transaction = transactions.get(transactionId);
//            if (transaction != null) {
//                replyMessage = transaction.getReplyMessage();
//            }
//        }
//        return replyMessage;
//    }
//    
//    private void checkTransactions() {
//        long now = System.currentTimeMillis();
//        synchronized (transactions) {
//            for (Transaction transaction : transactions.values()) {
//                long duration = now - transaction.getTimestamp();
//                Message message = transaction.getRequestMessage();
//                long transactionId = message.getTransactionId();
//                if (message instanceof DataMessage) {
//                    //FIXME: Use configured T3 value.
//                    if (duration > SecsConstants.DEFAULT_T3_TIMEOUT) {
//                        LOG.warn(String.format("T3 timeout for transaction %d -- aborted", transactionId));
//                        endTransaction(transactionId);
//                    }
//                } else { // ControlMessage
//                    //FIXME: Use configured T6 value.
//                    if (duration > SecsConstants.DEFAULT_T6_TIMEOUT) {
//                        // Control message time-out; treat as connection failure.
//                        LOG.warn(String.format("T6 timeout for transaction %d -- disconnect", transactionId));
//                        endTransaction(transactionId);
//                        disconnect();
//                    }
//                }
//            }
//        }
//    }

    private static void sleep(long duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            // Safe to ignore.
        }
    }

    /**
     * Thread to establish a TCP/IP connection to another equipment (ACTIVE
     * connection mode).
     * 
     * @author Oscar Stigter
     */
    private class ActiveConnectionThread extends Thread {

        @Override
        public void run() {
            while (isEnabled) {
                if (getCommunicationState() == CommunicationState.NOT_COMMUNICATING) {
                    LOG.debug(String.format("Connecting to equipment '%s' on port %d", host, port));
                    try {
                        socket = new Socket(host, port);
                        handleConnection();
                    } catch (IOException e) {
                        LOG.debug(String.format("Failed to connect to equipment '%s' on port %d", host, port));
                        SecsEquipment.sleep(SecsConstants.DEFAULT_T5_TIMEOUT);
                    }
                } else {
                    SecsEquipment.sleep(POLL_INTERVAL);
                }
            }
        }
    }

    /**
     * Thread to listen for incoming TCP/IP connections from another equipment
     * (PASSIVE connection mode).
     * 
     * @author Oscar Stigter
     */
    private class PassiveConnectionThread extends Thread {

        private static final int SOCKET_TIMEOUT = 100;

        @Override
        public void run() {
            LOG.info(String.format("Listening for incoming connections on port %d", port));
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(port);
                serverSocket.setSoTimeout(SOCKET_TIMEOUT);
                while (isEnabled) {
                    if (getCommunicationState() == CommunicationState.NOT_COMMUNICATING) {
                        try {
                            socket = serverSocket.accept();
                            handleConnection();
                        } catch (SocketTimeoutException e) {
                            // No incoming connections, just continue waiting.
                        } catch (IOException e) {
                            LOG.error("Socket connection error: " + e.getMessage());
                            disconnect();
                        }
                    } else {
                        SecsEquipment.sleep(POLL_INTERVAL);
                    }
                }
            } catch (IOException e) {
                LOG.error("Could not start server: " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(serverSocket);
            }
        }
    }

}