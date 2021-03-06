package edu.dlut.software.cagetian.storagenode;

import edu.dlut.software.cagetian.FileInfo;

import java.io.*;
import java.net.Socket;

/**
 * Created by CageTian on 2017/7/8.
 */
public class StorageClientService implements Runnable {
    private StorageNode storageNode;
    private Socket socket;

    /**
     * Constructor
     * @param storageNode
     * @param socket
     */
    public StorageClientService(StorageNode storageNode, Socket socket) {
        this.storageNode = storageNode;
        this.socket = socket;
    }

    /**
     * respond client download request
     * done
     * @throws IOException
     */
    public void clientDownload(ObjectInputStream ois) throws Exception {
        String file_uuid = ois.readUTF();
        String client_name = ois.readUTF();
        String file_path=storageNode.getRootFolder()+
                File.separatorChar+client_name+ File.separatorChar+file_uuid;
        File file=new File(file_path);
        if(file.isFile())
            send(file, new ObjectOutputStream(socket.getOutputStream()));
    }



    /**
     * ask other Node for backup request
     *
     * @throws Exception
     */
    private void backUpToBNode(FileInfo fileInfo) throws Exception {
        StorageNode bNode=fileInfo.getSec_node();
        Socket node_socket=new Socket(bNode.getNodeIP(),bNode.getNodePort());
        ObjectOutputStream oos=new ObjectOutputStream(node_socket.getOutputStream());
        oos.writeChar('b');
        oos.writeObject(fileInfo);
        send(fileInfo.getFile(), oos);
    }

    private void receiveBackUp(ObjectInputStream ois) throws Exception {
        FileInfo fileInfo = receive(ois);
        System.out.println("======== 节点成功接收备份文件 [File Name：" +
                fileInfo.getFile_id() + "] [Size：" + fileInfo.getFile_size() + "] ========");
        storageNode.getFile_info_map().put(fileInfo.getFile_id(),fileInfo);
    }
    /**
     * respond client upload request
     *
     * @throws IOException
     */
    public void clientUpload(ObjectInputStream ois) throws Exception {
        FileInfo fileInfo = receive(ois);
        System.out.println("======== 节点成功接收上传文件 [File Name：" +
                fileInfo.getFile_id() + "] [Size：" + fileInfo.getFile_size() + "] ========");
        socket.close();
        storageNode.getFile_info_map().put(fileInfo.getFile_id(),fileInfo);
        backUpToBNode(fileInfo);
    }
    /**
     * respond client remove request
     *
     * @throws Exception
     */
    public void clientRemove(ObjectInputStream ois) throws Exception {
        String client_name = ois.readUTF();
        FileInfo fileInfo = (FileInfo) ois.readObject();
        String uuid = fileInfo.getFile_id();
        FileInfo local_file = storageNode.getFile_info_map().get(uuid);
        if (storageNode.getFile_info_map().keySet().contains(uuid) &&
                fileInfo.getClient_name().equals(client_name) && local_file.getFile().delete()) {
            System.out.println(client_name + " deleted " + uuid);
            storageNode.getFile_info_map().remove(uuid);
            if (storageNode.equals(fileInfo.getMain_node())) {
                Socket bSocket = new Socket(fileInfo.getSec_node()
                        .getNodeIP(), fileInfo.getSec_node().getNodePort());
                ObjectOutputStream oos = new ObjectOutputStream(bSocket.getOutputStream());
                oos.writeChar('r');
                oos.flush();
                oos.writeUTF(client_name);
                oos.flush();
                oos.writeObject(fileInfo);
                oos.flush();
                oos.close();
                bSocket.close();
            }
        }
    }

    @Override
    public void run() {
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            char ch = ois.readChar();
            switch (ch){
                case 'd':
                    clientDownload(ois);
                    break;
                case 'u':
                    clientUpload(ois);
                    break;
                case 'r':
                    clientRemove(ois);
                    break;
                case 'b':
                    receiveBackUp(ois);
                default:
                    break;//do somethings
            }
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private FileInfo receive(ObjectInputStream ois) throws Exception {
//        ObjectInputStream ois=new ObjectInputStream(socket.getInputStream());
        FileInfo fileInfo=(FileInfo)ois.readObject();
        String file_uuid = fileInfo.getFile_id();
        String client_name=fileInfo.getClient_name();
        File directory=new File(storageNode.getRootFolder()
                + File.separatorChar +client_name);
        if (!directory.exists()) {
            directory.mkdir();
        }
        File file = new File(directory.getAbsolutePath()+File.separatorChar + file_uuid);
        FileOutputStream fos = new FileOutputStream(file);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = ois.read(bytes, 0, bytes.length)) != -1) {
            fos.write(bytes, 0, length);
            fos.flush();
        }
        fileInfo.setFile(file);
        ois.close();
        fos.close();
        return fileInfo;
    }

    private void send(File file, ObjectOutputStream oos) throws Exception {
        FileInputStream fis=new FileInputStream(file);
//        =new ObjectOutputStream(socket.getOutputStream());

        // 文件名和长度

        oos.writeUTF(file.getName());
        oos.flush();
        oos.writeLong(file.length());
        oos.flush();

        // 开始传输文件
        System.out.println("======== 开始传输文件 ========");
        byte[] bytes = new byte[1024];
        int length;
        long progress = 0;
        while((length = fis.read(bytes, 0, bytes.length)) != -1) {
            oos.write(bytes, 0, length);
            oos.flush();
            progress += length;
            System.out.print("| " + (100*progress/file.length()) + "% |");
        }
        System.out.println();
        System.out.println("======== 文件传输成功 ========");
        fis.close();
        oos.close();

    }
}
