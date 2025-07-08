package p2p;

import p2p.controller.FileController;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        try {
            FileController fileController = new FileController(8080);
            fileController.start();
            System.out.println("PeerLink server started on port 8080");
            System.out.println("UI available at localhost:3000");
            Runtime.getRuntime().addShutdownHook(
                    new Thread(
                            ()->{
                                System.out.println("Shutting down the server");
                                fileController.stop();
                            }
                    )
            );

            System.out.println("Press enter to stop the server");
            System.in.read(); //todo:stopping the server


        }catch(Exception e){
            System.out.println("Failed to start the server at port 8080");
            e.printStackTrace();
        }
    }
}
