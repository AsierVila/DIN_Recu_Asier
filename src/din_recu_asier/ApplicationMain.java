/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package din_recu_asier;

import din_recu_asier.controllers.VerMovimientosViewController;
import java.io.FileInputStream;
import java.io.File;
import java.util.Properties;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Clase principal del programa. Abre la vista de consulta de movimientos
 * @author Asier
 */
public class ApplicationMain extends Application {
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("views/VerMovimientosView.fxml"));
        Parent root = (Parent) fxmlLoader.load();
        VerMovimientosViewController verMovimientosController = ((VerMovimientosViewController) fxmlLoader.getController());
        verMovimientosController.setStage(primaryStage);
        
        String accountId; 
        
        // Intentar leer el número de cuenta del archivo de propiedades
        try {
            Properties properties = new Properties();

            // Obtener el path del jar
            File file = new File(ApplicationMain.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            
            // Añadir el nombre del archivo de propiedades
            String propertiesPath = file.getParentFile().getAbsolutePath();
            properties.load(new FileInputStream(propertiesPath + "/file.properties"));
            
            accountId = properties.getProperty("account");
            
            verMovimientosController.setAccountId(accountId);
            verMovimientosController.initStage(root);
            
        } catch (Exception exception) {
            createAlert("No se ha encontrado el archivo de propiedades.");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    /***
     * Método para crear un diálogo de alerta con el mensaje que recibe
     * @param message El mensaje que se muestra en pantalla
     */
    private void createAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
    
}
