package edu.dlut.software.cagetian.storagenode;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;


/**
 * Created by CageTian on 2017/7/7.
 */
public class notifyService implements Runnable {
    private StorageNode storageNode;
    private int serverPort;
    private String message;

    public notifyService(StorageNode storageNode,int serverPort) {
        this.serverPort=serverPort;
        this.storageNode=storageNode;
        this.message=storageNode.getNodeName()+'#'+storageNode.getNodeIP()
                +'#'+storageNode.getNodePort()+'#'+storageNode.getRestVolume();
    }

    @Override
    public void run() {
        try {
            DatagramSocket ds = new DatagramSocket();
            while (true){
                message=storageNode.getNodeName()+'#'+storageNode.getNodeIP()
                        +'#'+storageNode.getNodePort()+'#'+storageNode.getRestVolume();
                DatagramPacket dp = new DatagramPacket(message.getBytes(),
                        message.getBytes().length,
                        InetAddress.getByName(storageNode.getFileServerIP()), serverPort);
                ds.send(dp);
//                System.out.println("send");
                Thread.sleep(10000);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
