package nkp.pspValidator.gui;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;
import javafx.stage.WindowEvent;
import nkp.pspValidator.shared.FdmfRegistry;
import nkp.pspValidator.shared.engine.exceptions.ValidatorConfigurationException;
import nkp.pspValidator.shared.imageUtils.ImageUtilManager;
import nkp.pspValidator.shared.imageUtils.ImageUtilManagerFactory;

import java.io.File;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Created by martin on 9.12.16.
 */
public class ValidationDataInitializationController extends DialogController {

    private static Logger LOG = Logger.getLogger(ValidationDataInitializationController.class.getSimpleName());

    @FXML
    ProgressIndicator progressIndicator;

    @FXML
    ImageView imgOk;

    @FXML
    ImageView imgError;

    @FXML
    Label progressStatusLabel;

    @FXML
    Label errorLabel;

    @FXML
    Button btnContinue;

    @FXML
    Button btnClose;

    @FXML
    Button btnSetFdmfsRootDir;

    //other data
    private DialogState state = DialogState.RUNNING;

    @Override
    EventHandler<WindowEvent> getOnCloseEventHandler() {
        return new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                switch (state) {
                    case RUNNING:
                        event.consume();
                        break;
                    case ERROR:
                        closeApp();
                        break;
                    case FINISHED:
                        event.consume();
                        continueInApp(null);
                }
            }
        };
    }

    @Override
    void startNow() {
        LOG.info("startNow");
        state = DialogState.RUNNING;
        //show progress
        progressIndicator.setVisible(true);
        progressStatusLabel.setVisible(true);
        progressStatusLabel.setText("");
        //hide images & buttons
        errorLabel.setVisible(false);
        btnContinue.setVisible(false);
        imgOk.setVisible(false);
        imgError.setVisible(false);
        btnContinue.setVisible(false);
        btnClose.setVisible(false);
        btnSetFdmfsRootDir.setVisible(false);
        getConfigurationManager();

        Task task = new Task<Void>() {
            private ValidationDataManager validationDataManager;

            @Override
            protected Void call() throws Exception {
                try {
                    Thread.sleep(300);
                    validationDataManager = new ValidationDataManager(getConfigurationManager());
                    File fdmfsRoot = getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_DMF_DIR);
                    if (fdmfsRoot == null) {
                        processResult(new Result(false, "není definován kořenový adresář pro validační soubory"));
                    } else {
                        checkReadableDir(fdmfsRoot);
                        //System.out.println(fdmfsRoot.getAbsolutePath());
                        updateStatus("kontroluji " + fdmfsRoot.getAbsolutePath());
                        File imageUtilConfig = getImageUtilsConfigFile(fdmfsRoot);
                        //System.out.println(imageUtilConfig.getAbsolutePath());
                        ImageUtilManager imageUtilManager = new ImageUtilManagerFactory(imageUtilConfig).buildImageUtilManager(getConfigurationManager().getPlatform().getOperatingSystem());
                        validationDataManager.setImageUtilManager(imageUtilManager);
                        validationDataManager.setFdmfRegistry(new FdmfRegistry(fdmfsRoot));
                        //FdmfRegistry fdmfRegistry = new FdmfRegistry(fdmfsRoot);


                        //TODO: jeste dalsi veci
                        processResult(new Result(true, null));
                    }
                } catch (ValidatorConfigurationException e) {
                    processResult(new Result(false, e.getMessage()));
                    return null;
                } finally {
                    return null;
                }
            }

            private File getImageUtilsConfigFile(File fdmfsRoot) throws ValidatorConfigurationException {
                File file = new File(fdmfsRoot, "imageUtils.xml");
                if (!file.exists()) {
                    throw new ValidatorConfigurationException("chybí konfigurační soubor " + file.getAbsolutePath());
                } else if (!file.canRead()) {
                    throw new ValidatorConfigurationException("nelze číst konfigurační soubor " + file.getAbsolutePath());
                } else {
                    return file;
                }
            }

            private void checkReadableDir(File pspRoot) throws ValidatorConfigurationException {
                if (!pspRoot.exists()) {
                    throw new ValidatorConfigurationException(String.format("Soubor %s neexistuje", pspRoot.getAbsolutePath()));
                } else if (!pspRoot.isDirectory()) {
                    throw new ValidatorConfigurationException(String.format("Soubor %s není adresář", pspRoot.getAbsolutePath()));
                } else if (!pspRoot.canRead()) {
                    throw new ValidatorConfigurationException(String.format("Nelze číst adresář %s", pspRoot.getAbsolutePath()));
                }
            }

            private void updateStatus(String status) {
                Platform.runLater(() -> {
                    progressStatusLabel.setText(status);
                });
            }

            private void processResult(Result result) {
                progressIndicator.setVisible(false);
                Platform.runLater(() -> {
                    Random random = new Random();
                    progressIndicator.setVisible(false);
                    if (result.isOk()) {//ok
                        state = DialogState.FINISHED;
                        imgOk.setVisible(true);
                        btnContinue.setVisible(true);
                        progressStatusLabel.setText("OK");
                        /*app.setValidationDataManager(validationDataManager);
                        //TODO: pokud uz jednou zobrazeno, tak rovnou zavolat dalsi fazi
                        //ted pokracuju rovnou, pokud je vse ok
                        app.checkImageUtils();*/

                        main.setValidationDataManager(validationDataManager);
                        //TODO: pokud uz jednou zobrazeno, tak rovnou zavolat dalsi fazi
                        //ted pokracuju rovnou, pokud je vse ok
                        //mainController.checkImageUtils();
                        continueInApp(null);
                    } else {//error
                        state = DialogState.ERROR;
                        imgError.setVisible(true);
                        progressStatusLabel.setText("CHYBA");
                        errorLabel.setVisible(true);
                        errorLabel.setText(result.getError());
                        btnSetFdmfsRootDir.setVisible(true);
                        btnClose.setVisible(true);
                    }
                });
            }
        };
        new Thread(task).start();
    }

    public void continueInApp(ActionEvent actionEvent) {
        main.checkImageUtils();
    }

    public void closeApp(ActionEvent actionEvent) {
        closeApp();
    }

    public void setFdmfsRootDir(ActionEvent actionEvent) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Vyberte kořenový adresář s validačními soubory");
        File currentDir = getConfigurationManager().getFileOrNull(ConfigurationManager.PROP_DMF_DIR);
        if (currentDir != null) {
            chooser.setInitialDirectory(currentDir);
        }
        File selectedDirectory = chooser.showDialog(stage);
        if (selectedDirectory != null) {
            getConfigurationManager().setFile(ConfigurationManager.PROP_DMF_DIR, selectedDirectory);
            startNow();
        }
    }


    private static final class Result {
        private final boolean ok;
        private final String error;

        public Result(boolean available, String error) {
            this.ok = available;
            this.error = error;
        }

        public boolean isOk() {
            return ok;
        }

        public String getError() {
            return error;
        }
    }

    public static enum DialogState {
        RUNNING, FINISHED, ERROR;
    }

}
