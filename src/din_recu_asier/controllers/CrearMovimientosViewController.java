/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package din_recu_asier.controllers;

import din_recu_asier.classes.Account;
import din_recu_asier.classes.AccountType;
import din_recu_asier.classes.Movement;
import din_recu_asier.clients.AccountClient;
import din_recu_asier.clients.MovementClient;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javax.ws.rs.ClientErrorException;

/**
 * Clase para la vista que permite crear un nuevo movimiento para la cuenta
 * @author Asier
 */
public class CrearMovimientosViewController {
    
    /***
     * El cliente que se utilizará para gestionar las operaciones con los Movimientos
     */
    private final static MovementClient MOVEMENT_CLIENT = new MovementClient();
    
    /***
     * El cliente que se utilizará para gestionar las operaciones con las Cuentas
     */
    private static final AccountClient ACCOUNT_CLIENT = new AccountClient();
    
    /***
     * El Stage que utilizará la clase
     */
    private Stage stage;
    
    /***
     * El número de cuenta que se utiliza para buscar la cuenta
     */
    private String accountId;
    
    /***
     * La cuenta que utilizará la clase
     */
    private Account account;
    
    /***
     * El TextField donde se mostrará el número de cuenta
     */
    @FXML
    private TextField txtCuenta;
    
    /***
     * El TextField donde se mostrará el tipo de cuenta
     */
    @FXML
    private TextField txtTipo;
    
    /***
     * El TextField donde se mostrará la fecha del movimiento
     */
    @FXML
    private TextField txtFecha;
    
    /***
     * El TextField donde se mostrará la descripción del movimiento
     */
    @FXML
    private ComboBox cboxDescripcion;
    
    /***
     * El TextField donde se mostrará el importe del movimiento
     */
    @FXML
    private TextField txtImporte;
    
    /***
     * El TextField donde se mostrará el saldo después de la operación
     */
    @FXML
    private TextField txtSaldo;
    
    /***
     * El botón que se utilizará para crear el movimiento
     */
    @FXML
    private Button btnCrear;
    
    /***
     * El botón que se utilizará para volver a la vista de consulta
     */
    @FXML
    private Button btnConsultarMovimientos;
    
    private Double saldo;
    
    /***
     * Setter para el Stage que utiliza la clase
     * @param stage El Stage que se va a guardar
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }
    
    /***
     * Setter para el número de cuenta que se utiliza para buscar la clase
     * @param accountId 
     */
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    /***
     * El método que se ejecuta para abrir la ventana
     * @param root 
     */
    public void initStage(Parent root) {
        Scene scene = new Scene(root);
        
        stage.setScene(scene);
        
        stage.setTitle("Crear movimiento");
        stage.setResizable(false);
        
        boolean retry;
        
        do {
            try {
                // Coger la cuenta del servidor
                account = getAccountById(accountId);
                
                if (account == null) {
                    createError("La cuenta " + accountId
                            + " no se ha encontrado en la base de datos.");
                    // Si la cuenta no existe no se puede crear un movimiento
                    btnCrear.setDisable(true);
                }

                retry = false;
                
            } catch (Exception exception) {
                // Coger los datos de prueba si se pulsa el boton
                if (createRetryDialog()) {
                    account = createFakeAccount(accountId);
                    // Con los datos de prueba no se puede crear un movimiento
                    btnCrear.setDisable(true);
                    retry = false;
                    
                } else {
                    retry = true;
                }
            }
        } while (retry);
        
        if (account != null) {
            setElements();
        }

        setButtons();
        
        stage.show();
    }
    
    /***
     * Prepara todos los elementos de la vista para que funcione correctamente
     */
    private void setElements() {
        
        txtCuenta.setDisable(true);
        txtTipo.setDisable(true);
        txtFecha.setDisable(true);
        txtSaldo.setDisable(true);
        
        txtCuenta.setText(account.getId().toString());
        txtTipo.setText(account.getType().toString());
        saldo = account.getBalance();
        txtSaldo.setText(account.getBalance().toString());
        
        txtImporte.textProperty().addListener(this::onImporteChanged);
        
        // Recoger la fecha actual y darle formato con el locale
        Date movementDate = new Date();
        DateFormat format = DateFormat.getDateInstance(DateFormat.LONG, Locale.ENGLISH);
        txtFecha.setText(format.format(movementDate));
        
        // Crear una ObservableList para las opciones
        ObservableList<String> descriptionOptions = FXCollections.observableArrayList();
        ArrayList<String> options = new ArrayList<String>();
        
        options.add("Deposit");
        options.add("Payment");
        
        descriptionOptions.addAll(options);
        // Guardar las opciones en la ComboBox
        cboxDescripcion.setItems(descriptionOptions);
    }
    
    /***
     * Prepara las acciones de los botones que contiene la vista
     */
    private void setButtons() {
        // Añadir las acciones de los botones
        btnConsultarMovimientos.setOnAction(this::btnConsultarMovimientosPressed);
        btnCrear.setOnAction(this::btnCrearMovimientoPressed);
    }
    
    private void onImporteChanged(ObservableValue observable, String oldValue, String newValue) {
        
        try {
            Double importe = Double.valueOf(txtImporte.getText());

            Double resultado = saldo + importe;

            txtSaldo.setText(resultado.toString());
            
        } catch (NumberFormatException numberFormatException) {
            txtSaldo.setText(saldo.toString());
        }
    }
    
    /***
     * Acción para cuando se pulsa el botón "Consultar movimientos"
     * @param actionEvent 
     */
    private void btnConsultarMovimientosPressed(ActionEvent actionEvent) {
        
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/din_recu_asier/views/VerMovimientosView.fxml"));
            Parent root = (Parent) fxmlLoader.load();
            VerMovimientosViewController verMovimientosController = ((VerMovimientosViewController) fxmlLoader.getController());
            verMovimientosController.setStage(new Stage());
            verMovimientosController.setAccountId(accountId);
            
            verMovimientosController.initStage(root);
            stage.hide();

        } catch (Exception exception) {
            createError("Ha ocurrido un error");
        }
    }
    
    /***
     * Acción para cuando se pulsa el botón "Crear movimiento"
     * @param actionEvent 
     */
    private void btnCrearMovimientoPressed(ActionEvent actionEvent) {
        
        try {
            if (validateFields()) {
                // Se crea e inserta el movimiento
                Movement movement = getMovementData();
                MOVEMENT_CLIENT.create_XML(movement);
                
                // Se actualiza el saldo de la cuenta
                account.setBalance(Double.valueOf(txtSaldo.getText()));
                ACCOUNT_CLIENT.updateAccount_XML(account);
                
                // Si no hay errores, se informa y vuelve al inicio
                createInformation("El movimiento se ha insertado");
                this.btnConsultarMovimientosPressed(actionEvent);
            }
            
        } catch(Exception exception) {
            createError("Ha ocurrido un error");
        }
    }
    
    /***
     * Método que llama al servidor para seleccionar la cuenta con el número que reciba.
     * @param accountId El número de cuenta que se utiliza para la búsqueda
     * @return 
     */
    private Account getAccountById(String accountId) {

        Account ret;
        
        try {
            // Intenta seleccionar la cuenta de la base de datos
            ret = ACCOUNT_CLIENT.find_XML(Account.class, accountId);
            
        } catch (ClientErrorException clientException) {
            // Si no se puede seleccionar la cuenta de la base de datos
            ret = null;
        }
        
        return ret;
    }
    
    /**
     * Recoge los valores que contienen los campos para 
     * guardarlos en el movimiento que se crea
     * @return 
     */
    private Movement getMovementData() {
        
        Movement ret = new Movement();
        
        boolean repeated = false;
        Long movementId;
        
        /*
        // Parte de código que no funciona. Se queda en el do-while sin entrar en la excepción
        // Genera un id aleatorio
        do {
            movementId = ThreadLocalRandom.current().nextLong(0, 999999999);
            
            try {
                // Si no encuentra el id es porque no existe
                Movement movement = MOVEMENT_CLIENT.find_XML(Movement.class, movementId.toString());
                // Si el id ya existe, se vuelve a lanzar
                repeated = true;
                
            } catch (Exception clientException) {
                // Lanza una excepcion porque no ha podido encontrarlo
                repeated = false;
            }    
        } while (repeated);
        */
        
        movementId = ThreadLocalRandom.current().nextLong(0, 999999999);
        
        ret.setId(movementId);
        ret.setAccount(account); // El account del cliente o de prueba
        ret.setDescription(cboxDescripcion.getSelectionModel().getSelectedItem().toString());
        ret.setTimestamp(new Timestamp(System.currentTimeMillis()));
        ret.setAmount(Double.valueOf(txtImporte.getText()));
        ret.setBalance(Double.valueOf(txtSaldo.getText()));
        ret.setDescription(cboxDescripcion.getPromptText());
        
        return ret;
    }

    /***
     * Crea una cuenta de prueba 
     * @param accountId El id que se va a utilizar para crear una cuenta de prueba
     * @return La cuenta de prueba que se ha creado
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
     * Valida que todos los datos introducidos son correctos y válidos. 
     * Genera un diálogo cuando alguno de los datos no es válido
     * @return false cuando un dato no es válido, true cuando todos son correctos
     */
    private boolean validateFields() {
        
        if (txtCuenta.getText().isEmpty()) {
            createError("Ha ocurrido un error con el número de cuenta");
            return false;
        }
        if (txtTipo.getText().isEmpty()) {
            createError("Ha ocurrido un error al determinar el tipo de cuenta");
            return false;
        }
        if (txtFecha.getText().isEmpty()) {
            createError("Ha ocurrido un error con la fecha del movimiento");
            return false;
        }
        if (cboxDescripcion.getSelectionModel().isEmpty()) {
            createError("La descripcion no puede estar vacía");
            return false;
        }
        if (!txtImporte.getText().isEmpty()) {
            // Comprobar que es numérico si contiene texto
            if (Pattern.matches("-?[0-9]+", txtImporte.getText())) {
                Double importe = Double.valueOf(txtImporte.getText());

                if (cboxDescripcion.getSelectionModel().getSelectedItem().toString().equals("Deposit")) {
                    // validar que el importe es positivo en el Deposit
                    if (importe < 0.0) {
                        createError("El importe debe ser positivo para los movimientos 'Deposit'");
                        return false;
                    }
                } else if (cboxDescripcion.getSelectionModel().getSelectedItem().toString().equals("Payment")) {
                    // validar que el importe es negativo en el Payment
                    if (importe > 0.0) {
                        createError("El importe debe ser negativo para los movimientos 'Payment'");
                        return false;
                    }
                }
            } else {
                createError("El importe debe ser numérico");
                return false;
            }
        } else {
            createError("El importe no puede estar vacío");
            return false;
        }
        if (txtSaldo.getText().isEmpty()) {
            createError("Ha ocurrido un error con el saldo de la cuenta. "
                    + "Pruebe a reiniciar el programa");
            return false;
        } else {
            if (Pattern.matches("-[0-9]+", txtSaldo.getText())) {
                createError("El saldo no puede ser negativo tras la operación");
                return false;
            }
        }
        
        return true;
    }
    
    /***
     * Método para crear un diálogo de alerta con el mensaje que recibe
     * @param message El mensaje que se muestra en pantalla
     */
    private void createError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
    
    
    /***
     * Método para crear un diálogo y reintentar una conexión con el servidor
     * @return true cuando se quiere reintentar, false cuando se debe crear datos de prueba
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

        // Si se decide cargar los datos de prueba, devuelve true
        if (result.orElse(reintentar) == datosPrueba) {
            ret = true;
        } else {
            ret = false;
        }
        
        return ret;
    }
    
    
    /***
     * Método para crear un diálogo de información con el mensaje que recibe
     * @param message El mensaje que se muestra en pantalla
     */
    private void createInformation(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.showAndWait();
    }
}
