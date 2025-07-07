package p2p.service;

import p2p.utils.UploadUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class FileSharer {
    private HashMap<Integer,String>AvailableFiles;

    public FileSharer(){
        AvailableFiles=new HashMap<>();
    }

    public int offerFile(String filePath){
        int port;
        while(true){
        port= UploadUtils.generateCode();
       if(AvailableFiles.containsKey(port)){
           AvailableFiles.put(port,filePath);
           return port;
       }
     }
    }
    public void startFileServer(int port){
        String filePath=AvailableFiles.get(port);
        if(filePath == null){
            System.out.println("NO file is associated with port: "+ port);
            return;
        }
        try(ServerSocket serverSocket=new ServerSocket(port)){
            System.out.println("Serving file " + new File(filePath).getName()+ "on port" + port);
            Socket clientSocket=serverSocket.accept();
            System.out.println("Client connection: "+ clientSocket.getInetAddress());
            new Thread(new FileSenderHandler(clientSocket,filePath)).start();

        }catch (IOException ex){
            System.out.println("Error handling file server on port: " +port);
        }
    }

    private static class FileSenderHandler implements Runnable{

        private final Socket clientSocket;
        private final String filePath;

        public FileSenderHandler(Socket clientSocket,String filePath){
            this.clientSocket=clientSocket;
            this.filePath=filePath;
        }

        @Override
        public void run(){
            try(FileInputStream fis =new FileInputStream(filePath)) {
                OutputStream oos = clientSocket.getOutputStream();
                String fileName = new File(filePath).getName();
                String header = "Filename: "+fileName+"\n";
                oos.write(header.getBytes());

                byte[] buffer = new byte[4096];
                int byteRead;
                while((byteRead = fis.read(buffer))!=-1){
                    oos.write(buffer,0,byteRead);
                }
                System.out.println("file " + fileName + "sent to " + clientSocket.getInetAddress());

            }catch (Exception ex){
                System.out.println("Error sending file to client" +ex.getMessage());
            }finally {
                try{
                    clientSocket.close();;
                }catch (Exception e){
                    System.out.println("Error in closing socket: " + e.getMessage());
                }
            }

        }
    }

}
