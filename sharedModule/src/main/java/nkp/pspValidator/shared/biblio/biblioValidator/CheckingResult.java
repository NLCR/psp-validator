package nkp.pspValidator.shared.biblio.biblioValidator;

/**
 * Created by Martin Řehánek on 27.1.17.
 */
public interface CheckingResult {
    public boolean matches();

    public String getErrorMessage();
}
