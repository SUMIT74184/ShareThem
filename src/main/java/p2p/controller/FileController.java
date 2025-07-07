package p2p.controller;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.IOUtils;
import p2p.service.FileSharer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileController {
    private final FileSharer fileSharer;
    private final HttpServer server;
    private final String uploadDir;
    private final ExecutorService executorService;


    public FileController(int port) throws IOException {
        this.fileSharer=new FileSharer();
        this.server=HttpServer.create(new InetSocketAddress(port),0);
        this.uploadDir=System.getProperty("java.io.tmpdir")+ File.separator+"peerlink-uploads";
        this.executorService= Executors.newFixedThreadPool(10); // at a time only 10 people can handle the link

        File uploadDirFile=new File(uploadDir);
        if(!uploadDirFile.exists()){
            uploadDirFile.mkdirs();
        }

        server.createContext("/uploads",new UploadHandler());
        server.createContext("/downloads",new DownloadHandler());
        server.createContext("/",new CorsHandler());//allow cross-origin request to connect client and server
        server.setExecutor(executorService);
    }

    public void start(){
        server.start();
        System.out.println("API server started on port " + server.getAddress().getPort());
    }

    public void stop(){
        server.stop(0);
        executorService.shutdown();
        System.out.println("API server Stopped");
    }
    private class CorsHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException{
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin","*");
            headers.add("Access-Control-Allow-Methods","GET,POST,OPTIONS");
            headers.add("Access-Control-Allow-Headers","Content-Type,Authorization");

            if(exchange.getResponseHeaders().equals("OPTIONS")){
                exchange.sendResponseHeaders(204,-1);
            }
            String response="NOT FOUND";
            exchange.sendResponseHeaders(404,response.getBytes().length);
            try(OutputStream oos = exchange.getResponseBody()){
                oos.write(response.getBytes());

            }
        }
    }
    private class DownloadHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange){

        }
    }

    private class UploadHandler implements HttpHandler{
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Headers headers=exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin","*");
            if(!exchange.getRequestMethod().equals("POST")){
                String response="Method not allowed";
                exchange.sendResponseHeaders(405,response.getBytes().length);
                try(OutputStream oos=exchange.getResponseBody()){
                    oos.write(response.getBytes());
                }
                return;

            }
            Headers requestHeaders=exchange.getRequestHeaders();
            String contentType=requestHeaders.getFirst("Content-Type");
            if(contentType == null || !contentType.startsWith("Multipart/form-data")){
                String response = "Bad Request:Content-Type must be multi-part/form-data";
                exchange.sendResponseHeaders(400,response.getBytes().length);
                try(OutputStream oos=exchange.getResponseBody()){
                    oos.write(response.getBytes());

                }
                return;
            }
            try{
            String boundary=contentType.substring(contentType.indexOf("boundary=")+9);
                ByteArrayOutputStream baos=new ByteArrayOutputStream();
                IOUtils.copy(exchange.getRequestBody(),baos);
                byte[] requestData=baos.toByteArray();

                Multiparser parser=new Multiparser(requestData,boundary);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    private static class Multiparser{
        private final byte[] data;
        private final String boundary;

        public Multiparser(byte[]data,String boundary){
            this.data=data;
            this.boundary=boundary;
        }
        public ParseResult parse(){
            try{
                String dataAsString=new String(data);
                String fileNameMarker="filename=\"";
                int filenameStart=dataAsString.indexOf(fileNameMarker);
                if(filenameStart == -1){
                    return null;
                }
                int filenameEnd=dataAsString.indexOf("\"",filenameStart);
                String fileName=dataAsString.substring(filenameStart,filenameEnd);

                String contentTypeMarker="Content-Type: ";
                String contentType="application/octet-stream";
                int contentTypeStart = dataAsString.indexOf(contentTypeMarker,filenameEnd);

                if(contentTypeStart!=-1) {
                    contentTypeStart += contentTypeMarker.length();
                    int contentTypeEnd=dataAsString.indexOf("\r\n",contentTypeStart);
                    contentType=dataAsString.substring(contentTypeStart,contentTypeEnd);
                }

                String headerEndMarker="\r\n\r\n";


            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return contentType;
        }
    }
    public static class ParseResult{
        public final String fileName;
        public final byte[] fileContent;


        public ParseResult(String fileName,byte[] fileContent){
            this.fileContent=fileContent;
            this.fileName=fileName;
        }
    }

}
