/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package din_recu_asier.controllers;

import din_recu_asier.classes.Account;
import din_recu_asier.classes.AccountType;
import din_recu_asier.classes.Movement;
import din_recu_asier.clients.MovementClient;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.GenericType;

/**
 * Clase para la vista que permite consultar todos los movimientos asociados a una cuenta
 * 
 * @author Asier
 */
public class VerMovimientosViewController {
    
    /***
     * El cliente que se utilizará para gestionar las operaciones
     */
    private static final MovementClient MOVEMENT_CLIENT = new MovementClient();
    
    /***
     * El Stage que utilizará la clase
     */
    private Stage stage;
    
    /***
     * El número de cuenta que se va a utilizar para buscar la cuenta
     */
    private String accountId;
    
    /***
     * Label en la que se muestra el número de la cuenta guardada
     * en el archivo de propiedades
     */
    @FXML
    private Label lblNumeroCuenta;
    
    /***
     * El botón que se utilizará para abrir la vista de "Crear movimiento"
     */
    @FXML
    private Button btnCrearMovimiento;
    
    /***
     * La tabla donde se muestran todos los movimientos asiciados a la cuenta
     */
    @FXML
    private TableView tableMovimientos;
    
    /***
     * La columna en la que se mostrará la fecha del movimiento
     */
    @FXML
    private TableColumn columnFecha;
    
    /***
     * La columna en la que se mostrará la descripción del movimiento
     */
    @FXML
    private TableColumn columnDescripcion;
    
    /***
     * La columna en la que se mostrará el saldo después del movimiento
     */
    @FXML
    private TableColumn columnSaldo;
    
    /***
     * La columna en la que se mostrará el importe del movimiento
     */
    @FXML
    private TableColumn columnImporte;
    
    /***
     * Setter para el Stage que contiene la clase
     * @param stage El Stage que se va a guardar
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /***
     * Setter para el AccountId de la clase
     * @param accountId El número de cuenta que se va a guardar
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /***
     * Iniciar la vista
     * @param root 
     */
    public void initStage(Parent root) {
        
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        
        stage.setTitle("Consultar movimientos");
        stage.setResizable(false);
        
        // Prepara los elementos
        setElements();
        
        stage.show();
        
        // Rellenar la tabla
        fillTableMovements();
    }
    
    /***
     * Prepara los elementos de la ventana para su correcto funcionamiento
     */
    private void setElements() {
        
        lblNumeroCuenta.setText(accountId);
        
        btnCrearMovimiento.setOnAction(this::btnCrearMovimientoPressed);
    }
    
    /***
     * Rellena la tabla con los datos que reciba, ya sean del servidor o de prueba.
     */
    private void fillTableMovements() {
        
        columnFecha.setCellValueFactory(new PropertyValueFactory("timestamp"));
        columnDescripcion.setCellValueFactory(new PropertyValueFactory("description"));
        columnSaldo.setCellValueFactory(new PropertyValueFactory("balance"));
        columnImporte.setCellValueFactory(new PropertyValueFactory("amount"));
        
        // Añade la propiedad de css para alinear a la derecha
        columnSaldo.setStyle("-fx-alignment: CENTER-RIGHT;");
        columnImporte.setStyle("-fx-alignment: CENTER-RIGHT;");
        
        boolean retry;
        
        ObservableList<Movement> movements = FXCollections.observableArrayList();
        
        do {
            try {
                // Coger los datos del servidor
                movements.addAll(getServerData());
                if (movements.isEmpty()) {
                    createAlert("No se ha encontrado ningún movimiento para la cuenta "
                            + accountId +". Compruebe que el número es correcto");
                }
                
                retry = false;
                
            } catch (Exception exception) {
                // Coges los datos de prueba si se pulsa el boton
                if (createRetryDialog()) {
                    movements.addAll(getFakeData());
                    retry = false;
                } else {
                    retry = true;
                }
            }
        } while (retry);
        
        tableMovimientos.setItems(movements);
    }
    
    /***
     * Listener para controlar la acción que sucede cuando se pulsa el botón 
     * Crear movimiento
     * 
     * @param actionEvent 
     */
    private void btnCrearMovimientoPressed(ActionEvent actionEvent) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/din_recu_asier/views/CrearMovimientosView.fxml"));
            Parent root = (Parent) fxmlLoader.load();
            
            CrearMovimientosViewController crearMovimientosController = ((CrearMovimientosViewController) fxmlLoader.getController());
            crearMovimientosController.setStage(new Stage());
            crearMovimientosController.setAccountId(accountId);
            crearMovimientosController.initStage(root);
            
            stage.hide();
            
        } catch (Exception exception) {
            createAlert("Ha ocurrido un error al abrir la ventana");
            exception.printStackTrace();
        }
    }
    
    /***
     * Selecciona los movimientos que contiene la cuenta.
     * 
     * @return 
     */
    private Set<Movement> getServerData() {
        
        Set<Movement> movements;
        
        try {
            // Intenta buscar los movimientos de la cuenta
            movements = MOVEMENT_CLIENT.findMovementByAccount_XML(new GenericType<Set<Movement>>() {}, accountId);
            
        } catch (ClientErrorException clientException) {
            // Si no existe ninguno, salta una excepción
            movements = null;
        }
        
        // Devuelve el Set como nulo
        return movements;
    }
    
    /***
     * Crea movimientos con información falsa para desplegar los datos a modo de prueba.
     * 
     * @return ArrayList de Movement
     */
    private ArrayList<Movement> getFakeData() {
        
        ArrayList<Movement> movements = new ArrayList<Movement>();
        
        Account fakeAccount = createFakeAccount(accountId);
        
        for (int i = 0; i<=10; i++) {
            Movement movement = new Movement();
            
            movement.setId(Long.valueOf(i));            
            movement.setAccount(fakeAccount);
            movement.setTimestamp(new Timestamp(System.currentTimeMillis()));
            movement.setAmount(1999.0);
            movement.setBalance(2999.0);
            movement.setDescription("Deposit");
            
            movements.add(movement);
        }
        
        return movements;
    }
    
    /***
     * Crear una cuenta con información falsa para desplegar los datos a modo de prueba.
     * 
     * @param accountId El número que se va a utilizar para crear la cuenta falsa
     * @return La cuenta que crea
     */
    private Account createFakeAccount(String accountId) {
        Account account = new Account();
        
        account.setId(Long.valueOf(accountId));
        account.setType(AccountType.CREDIT);
        account.setDescription("Check Account with Credit Line");
        account.setBalance(3999.0);
        account.setCreditLine(2000.0);
        account.setBeginBalance(2000.0);
        account.setBeginBalanceTimestamp(new Timestamp(System.currentTimeMillis()));
        //account.setCustomers();
        
        return account;
    }
    
    /***
     * Crear un cuadro de diálogo con el mensaje que reciba.
     * 
     * @param message 
     */
    private void createAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message, ButtonType.OK);
        alert.showAndWait();
    }
    
    /***
     * Crear un diálogo para reintentar la operación de leer del servidor.
     * 
     * @return true cuando se quiere reintentar
     */
    private boolean createRetryDialog() {
        boolean ret;
        
        // Crear los botones "personalizados"
        ButtonType reintentar = new ButtonType("Reintentar", ButtonBar.ButtonData.OK_DONE);
        ButtonType datosPrueba = new ButtonType("Cargar datos de prueba", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        // Se crea el Alert con los botones anteriores
        Alert alert = new Alert(Alert.AlertType.WARNING, "No se ha podido conectar al servidor", reintentar, datosPrueba);
        alert.setTitle("Ha ocurrido un error");
        Optional<ButtonType> result = alert.showAndWait();

        // Si se decide cargar los datos de prueba
        if (result.orElse(reintentar) == datosPrueba) {
            ret = true;
        } else {
            ret = false;
        }
        
        return ret;
    }
}
