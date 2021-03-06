package edu.dlut.software.cagetian.storagenode;

import edu.dlut.software.cagetian.FileInfo;

import java.io.*;
import java.math.RoundingMode;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Properties;

/**
 * Created by CageTian on 2017/7/6.
 */
public class StorageNode implements Serializable{
    private static DecimalFormat df = null;
    static {
        // 设置数字格式，保留一位有效小数
        df = new DecimalFormat("#0.0");
        df.setRoundingMode(RoundingMode.HALF_UP);
        df.setMinimumFractionDigits(1);
        df.setMaximumFractionDigits(1);
    }

    private String nodeName;
    private String nodeIP;
    private int nodePort;
    private String rootFolder;
    private long volume;
    private String fileServerIP;
    private int fileServerPort;
    private long restVolume;

    private HashMap<String,FileInfo> file_info_map;

    public StorageNode(String nodeName, String nodeIP, int nodePort, long restVolume) {
        this.nodeName = nodeName;
        this.nodeIP = nodeIP;
        this.nodePort = nodePort;
        this.restVolume = restVolume;
        this.file_info_map = new HashMap<>();
    }

    public StorageNode(String nodeIP, int nodePort, String rootFolder) {
        this.nodeIP = nodeIP;
        this.nodePort = nodePort;
        this.rootFolder = rootFolder;
        this.file_info_map = new HashMap<>();

    }
    public StorageNode(File f) throws IOException {
        getProperties(f);
        this.file_info_map = getAllFile(this.rootFolder, new HashMap<>());
    }
    public StorageNode(String nodeName){
        this.nodeName=nodeName;
        this.file_info_map = new HashMap<>();
    }

    public static void main(String[] args) throws IOException {
        StorageNode storageNode=new StorageNode(new File(args[0]));
        ServerSocket serverSocket=new ServerSocket(storageNode.nodePort);
        storageNode.notifyServer();
        while(true){
            Socket socket = serverSocket.accept();
            System.out.println(socket.getInetAddress().toString()+socket.getPort()+" access in:");
            storageNode.clientService(socket);
        }
//        System.out.println(storageNode);
    }

    private HashMap<String, FileInfo> getAllFile(String rootFolder, HashMap<String, FileInfo> map) {
        File directory = new File(rootFolder);
        if (directory.isDirectory()) {
            for (File file : directory.listFiles())
                getAllFile(file.getAbsolutePath(), map);
        } else if (directory.isFile()) {
            map.put(directory.getName(), FileInfo.getNodeInitInstance(directory.getName()
                    , directory.getParentFile().getName(), directory.length(), directory));
        }
        return map;
    }

    /**
     * 通知Server是否运行
     */
    public void notifyServer() {
        new Thread(new notifyService(this, 3000)).start();
    }

    /**
     * Download,Upload,BackUp,Remove in new Thread
     *
     * @param socket
     */
    public void clientService(Socket socket) {
        new Thread(new StorageClientService(this, socket)).start();
    }

    private void getProperties(File prop_file) throws IOException {
        Properties pps = new Properties();
        InputStream in = new BufferedInputStream(new FileInputStream(prop_file));
        pps.load(in);
        nodeName=pps.getProperty("NodeName");
        nodeIP=pps.getProperty("NodeIP");
        nodePort=Integer.parseInt(pps.getProperty("NodePort"));
        rootFolder=pps.getProperty("RootFolder");
        volume=Long.parseLong(pps.getProperty("Volume"));
        fileServerIP=pps.getProperty("FileServerIP");
        fileServerPort=Integer.parseInt(pps.getProperty("FileServerPort"));
    }
    @Override
    public boolean equals(Object o){
        if(o instanceof StorageNode){
            StorageNode s=(StorageNode)o;
            return s.getNodeName().equals(this.getNodeName()) ;
        }
        return false;
    }
    private String getFormatFileSize(long length) {
        double size = ((double) length) / (1 << 30);
        if(size >= 1) {
            return df.format(size) + "GB";
        }
        size = ((double) length) / (1 << 20);
        if(size >= 1) {
            return df.format(size) + "MB";
        }
        size = ((double) length) / (1 << 10);
        if(size >= 1) {
            return df.format(size) + "KB";
        }
        return length + "B";
    }
    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getNodeIP() {
        return nodeIP;
    }

    public void setNodeIP(String nodeIP) {
        this.nodeIP = nodeIP;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        this.volume = volume;
    }

    public String getFileServerIP() {
        return fileServerIP;
    }

    public void setFileServerIP(String fileServerIP) {
        this.fileServerIP = fileServerIP;
    }

    public int getFileServerPort() {
        return fileServerPort;
    }

    public void setFileServerPort(int fileServerPort) {
        this.fileServerPort = fileServerPort;
    }

    public long getRestVolume() {
        return restVolume;
    }

    public void setRestVolume(long restVolume) {
        this.restVolume = restVolume;
    }

    public HashMap<String, FileInfo> getFile_info_map() {
        return file_info_map;
    }

    public void setFile_info_map(HashMap<String, FileInfo> file_info_map) {
        this.file_info_map = file_info_map;
    }
}
