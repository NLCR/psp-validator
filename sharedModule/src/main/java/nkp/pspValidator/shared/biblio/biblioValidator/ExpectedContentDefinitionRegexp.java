package nkp.pspValidator.shared.biblio.biblioValidator;

import java.util.regex.Pattern;

/**
 * Created by Martin Řehánek on 10.1.17.
 */
public class ExpectedContentDefinitionRegexp implements ExpectedContentDefinition {
    private final String regexp;

    public ExpectedContentDefinitionRegexp(String regexp) {
        this.regexp = regexp;
    }

    @Override
    public CheckingResult checkAgainst(String valueFound) {
        if (valueFound == null || valueFound.isEmpty()) { //value empty
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return "prázdná hodnota";
                }
            };
        }
        if (Pattern.matches(regexp, valueFound)) {
            return new CheckingResultMatch();
        } else {
            return new CheckingResultFail() {
                @Override
                public String getErrorMessage() {
                    return String.format("hodnota '%s' neodpovídá regulárním výrazu '%s'", valueFound, regexp);
                }
            };
        }
    }

    public String toString() {
        return String.format("regulární výraz '%s'", regexp);
    }
}
