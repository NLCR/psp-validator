package nkp.pspValidator.gui;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Created by martin on 9.12.16.
 */
public abstract class AbstractController extends Application {

    protected Stage stage;
    protected Main app;
    protected ConfigurationManager configurationManager;

    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
    }

    public void setApp(Main app) {
        this.app = app;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }
}
