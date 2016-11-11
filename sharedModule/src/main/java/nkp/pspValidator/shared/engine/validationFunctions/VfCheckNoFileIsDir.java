package nkp.pspValidator.shared.engine.validationFunctions;

import nkp.pspValidator.shared.engine.Engine;
import nkp.pspValidator.shared.engine.ValueEvaluation;
import nkp.pspValidator.shared.engine.ValueType;
import nkp.pspValidator.shared.engine.exceptions.ContractException;

import java.io.File;
import java.util.List;


/**
 * Created by martin on 27.10.16.
 */
public class VfCheckNoFileIsDir extends ValidationFunction {

    public static final String PARAM_FILES = "files";


    public VfCheckNoFileIsDir(Engine engine) {
        super(engine, new Contract()
                .withValueParam(PARAM_FILES, ValueType.FILE_LIST, 1, 1)
        );
    }

    @Override
    public String getName() {
        return "checkNoFileIsDir";
    }

    @Override
    public ValidationResult validate() {
        try {
            checkContractCompliance();

            ValueEvaluation paramFiles = valueParams.getParams(PARAM_FILES).get(0).getEvaluation();
            List<File> files = (List<File>) paramFiles.getData();
            if (files == null) {
                return invalidValueParamNull(PARAM_FILES, paramFiles);
            }

            return validate(files);
        } catch (ContractException e) {
            return invalidContractNotMet(e);
        } catch (Throwable e) {
            return invalidUnexpectedError(e);
        }
    }

    private ValidationResult validate(List<File> files) {
        ValidationResult result = new ValidationResult();
        for (File file : files) {
            if (!file.exists()) {
                result.addError(invalidFileDoesNotExist(file));
            } else if (file.isDirectory()) {
                result.addError(invalidFileIsDir(file));
            }
        }
        return result;
    }


}
