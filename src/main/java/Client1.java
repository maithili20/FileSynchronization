import org.apache.log4j.Logger;
import utilities.Log;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;


public class Client1 implements Runnable{
    private static boolean reader = false;
    private static boolean writer = false;
    public boolean sender = false;
    public static String localDir;
    public static String backupDir;
    private static final Logger logger = Log.getLogger("Client1");//    private static final Logger logger = LogManager.getLogger(Client.class);
    public static Socket tcpSocket;
    public static int receiverUDPPort;
    public static int senderUDPPOrt;
    private static final int clientNum = 1;
    private static WatchFolder watcher;

    public void getNewWatcher() {
        try {
            watcher = new WatchFolder(localDir, logger);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Client1(){
    }

    @Override
    public void run(){
        try {
            switch (Thread.currentThread().getName()) {
                case "pollingAgent":
                    this.polling();
                    break;
                case "watcher":
                    this.watchOverFiles();
                    break;
            }
        } catch (IOException e) {
            logger.error("Exception", e);
        } catch (InterruptedException e) {
            logger.error("Exception", e);
        } catch (Exception e) {
            logger.error("Exception", e);
        }
    }
    public static Socket createTcpConn(){
        try {
            InetAddress serverIp = InetAddress.getByName("localhost");
            tcpSocket = new Socket(serverIp, Constants.SERVER_TCP_PORT);
        } catch (Exception e) {
            logger.info("Server isn't available");
            System.exit(1);
        }
        return tcpSocket;
    }

    public boolean canRead(){
        if(writer) return false;
        return true;
    }
    public boolean canWrite(){
        if(reader) return false;
        return true;
    }

    public void watchOverFiles() throws Exception {
        logger.info("Watching directory for changes"+localDir);
        this.getNewWatcher();
        FileTransferUtility transfer;
        int startBlock;
        // STEP4: Poll for events
        while (true) {
            if(this.canRead()) {
                reader = true;
                this.watcher.watchFolder();
                if (watcher.newFile != null) {
                    logger.info("A new file is created : " + watcher.newFile);
                    // Send the file over UDP
                    Socket tcpSocket = createTcpConn();
                    PrintWriter sender = new PrintWriter(tcpSocket.getOutputStream(), true);
                    Scanner receiver = new Scanner(tcpSocket.getInputStream());
                    String action = clientNum + Constants.CRLF + "GET NEW FILE";
                    sender.println(action);
                    transfer = new FileTransferUtility(false, 0, senderUDPPOrt, receiverUDPPort, tcpSocket, localDir, watcher.newFile.toString(), logger, clientNum, sender, receiver);
                    transfer.sendfile();
                    Helper.moveToBackupFolder(localDir,backupDir,watcher.newFile.toString(), logger);
                    watcher.newFile = null;
                    tcpSocket.close();
                    sender.close();
                    receiver.close();
                } else if (watcher.modifiedFile != null) {
                    logger.info("A file is modified : " + watcher.modifiedFile);
                    // Send the update over UDP
                    Path origin = Paths.get(localDir, String.valueOf(watcher.modifiedFile));
                    Path backup = Paths.get(backupDir, String.valueOf(watcher.modifiedFile));
                    Thread.sleep(100);
                    startBlock = FileComparison.compareFileByByte(origin, backup, logger);
                    logger.info("Change detected at Block no "+startBlock);
                    if(startBlock>-1) {

                        Socket tcpSocket = createTcpConn();
                        PrintWriter sender = new PrintWriter(tcpSocket.getOutputStream(), true);
                        Scanner receiver = new Scanner(tcpSocket.getInputStream());
                        String action = clientNum + Constants.CRLF + "GET UPDATE FILE";
                        sender.println(action);

                        transfer = new FileTransferUtility(true, startBlock, senderUDPPOrt, receiverUDPPort, tcpSocket, localDir, watcher.modifiedFile.toString(), logger, clientNum, sender, receiver);
                        transfer.sendfile();

                        tcpSocket.close();
                        sender.close();
                        receiver.close();
                        Helper.moveToBackupFolder(localDir,backupDir,watcher.modifiedFile.toString(), logger);
                    }else {
                        logger.info("No difference observed between against backup file");
                    }
                    watcher.modifiedFile = null;
                } else if (watcher.deletedFile != null) {
                    logger.info("A new file is deleted : " + watcher.deletedFile);
                    // Send the msg to server
                    Socket tcpSocket = createTcpConn();
                    PrintWriter sender = new PrintWriter(tcpSocket.getOutputStream(), true);
                    Helper.DeleteSingleFile(backupDir, watcher.deletedFile.toString(), logger);
                    String action = clientNum + Constants.CRLF + "DELETE FILE" + "\n" + watcher.deletedFile.toString();
                    sender.println(action);
                    watcher.deletedFile = null;
                    tcpSocket.close();
                    sender.close();
                }
                Thread.sleep(100);
                boolean valid = watcher.watchKey.reset();
                if (!valid) {
                    break;
                }
            }else {
                reader = false;
                Thread.sleep(20);
            }
        }
    }
    public void polling() throws IOException, InterruptedException {
        logger.info("Preparing to poll the server");
        while (true) {
            tcpSocket = createTcpConn();
            logger.info("Client Socket created");
            PrintWriter sender = new PrintWriter(tcpSocket.getOutputStream(), true);
            Scanner receiver = new Scanner(tcpSocket.getInputStream());
            logger.info("Polling the server started");
            String line = clientNum + "\nPOLLING";
            // send the message
            sender.println(line);
            line = receiver.nextLine();
            logger.info(line);
            if (!line.equals("NONE")) {
                // The horror
                logger.info("Server has some update " + line);
                tcpSocket.close();
                sender.close();
                receiver.close();
                writer = true;
                while(!this.canWrite()){Thread.sleep(1);}

                if (line.startsWith("ADD")) {
                    String command = clientNum+Constants.CRLF+"SEND NEW FILE";
                    this.getFile(command, line.split("#")[1], "0");
                    Helper.moveToBackupFolder(localDir,backupDir,line.split("#")[1], logger);
                }else if (line.startsWith("UPDATE")) {
                    String command = clientNum+Constants.CRLF+"SEND UPDATE FILE";
                    this.getFile(command, line.split("#")[1], line.split("#")[2]);
                    Helper.moveToBackupFolder(localDir,backupDir,line.split("#")[1], logger);
                }else if (line.startsWith("DELETE")) {
                    Helper.DeleteSingleFile(localDir, line.split("#")[1], logger);
                    Helper.DeleteSingleFile(backupDir, line.split("#")[1], logger);
                }
                this.getNewWatcher();
                writer = false;
            }else {
                tcpSocket.close();
                sender.close();
                receiver.close();
            }
            logger.info("Polling the server Finished");
            Thread.sleep(20000);
        }
    }

    public void getFile(String command, String fileName, String startBlock) throws IOException {
        tcpSocket = createTcpConn();
        logger.info("Client Socket created");
        PrintWriter sender = new PrintWriter(tcpSocket.getOutputStream(), true);
        Scanner receiver = new Scanner(tcpSocket.getInputStream());

        try {
            // send the file name
            sender.println(command);
            FileTransferUtility transfer = new FileTransferUtility(command.contains("UPDATE"),Integer.parseInt(startBlock), senderUDPPOrt, receiverUDPPort, tcpSocket, localDir, fileName, logger, clientNum, sender, receiver);
            transfer.receiveHandleClient();

            tcpSocket.close();
            sender.close();
            receiver.close();

        }catch (Exception e){
            logger.error("Failed to chat", e);
        }
    }


    private static void setProperties(){
        if(clientNum==1){
            receiverUDPPort = Constants.SERVER_UDP_PORT_CLIENT_ONE;
            senderUDPPOrt = Constants.CLIENT_ONE_UDP_PORT;
        }else{
            receiverUDPPort = Constants.SERVER_UDP_PORT_CLIENT_TWO;
            senderUDPPOrt = Constants.CLIENT_TWO_UDP_PORT;
        }
        localDir = Constants.LOCAL_DIRS[0][clientNum];
        backupDir = Constants.LOCAL_DIRS[1][clientNum];
    }

    public static void main(String[] args) throws IOException {
        // Expectation is Client.class file will be executed with an identifier like 0, 1, 2 etc
        // And localDir will be decided using that client_no
        setProperties();
        // First cleanup the clients' local and backup dirs.
        Helper.deleteAllFiles(logger, localDir, backupDir);
        tcpSocket = createTcpConn();
        PrintWriter sender = new PrintWriter(tcpSocket.getOutputStream(), true);
        sender.println(clientNum+"\nSTARTING");
        tcpSocket.close();
        // Create threads
        Thread watcher = new Thread(new Client1());
        Thread pollingAgent = new Thread(new Client1());
        watcher.setName("watcher");
        pollingAgent.setName("pollingAgent");
        // Start them
        watcher.start();
        pollingAgent.start();

    }

}