package nkp.pspValidator.shared.biblio.biblioValidator;

/**
 * Created by Martin Řehánek on 27.1.17.
 */
public class CheckingResultMatch implements CheckingResult {
    @Override
    public boolean matches() {
        return true;
    }

    @Override
    public String getErrorMessage() {
        return null;
    }
}