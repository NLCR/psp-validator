package nkp.pspValidator.shared.imageUtils;

import nkp.pspValidator.shared.OperatingSystem;
import nkp.pspValidator.shared.imageUtils.UtilHandler.Command;
import nkp.pspValidator.shared.imageUtils.UtilHandler.Parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by martin on 14.11.16.
 */
public class ImageUtilManager {

    //TODO: cachovani vysledku operaci, pripadne xml

    private final OperatingSystem os;
    private final Map<ImageUtil, UtilHandler> utilVersionDetectionHandlers;
    private final Map<ImageUtil, UtilHandler> utilExecutionHandlers;
    private final Map<ImageUtil, Boolean> utilAvaliable = new HashMap<>();


    public ImageUtilManager(OperatingSystem os, Map<ImageUtil, UtilHandler> utilVersionDetectionHandlers, Map<ImageUtil, UtilHandler> utilExecutionHandlers) {
        this.os = os;
        this.utilVersionDetectionHandlers = utilVersionDetectionHandlers;
        this.utilExecutionHandlers = utilExecutionHandlers;
        for (ImageUtil util : ImageUtil.values()) {
            utilAvaliable.put(util, false);
        }
    }

    public boolean isVersionDetectionDefined(ImageUtil type) {
        return utilVersionDetectionHandlers.containsKey(type);
    }

    public boolean isUtilExecutionDefined(ImageUtil type) {
        return utilVersionDetectionHandlers.containsKey(type);
    }

    public void setPaths(Map<ImageUtil, File> paths) {
        for (ImageUtil util : paths.keySet()) {
            File path = paths.get(util);
            UtilHandler versionDetection = utilVersionDetectionHandlers.get(util);
            if (versionDetection != null) {
                versionDetection.getCommand().setPath(path);
            }
            UtilHandler UtilHandler = utilExecutionHandlers.get(util);
            if (UtilHandler != null) {
                UtilHandler.getCommand().setPath(path);
            }
        }
    }

    public void setUtilAvailable(ImageUtil util, boolean available) {
        utilAvaliable.put(util, available);
    }

    public boolean isUtilAvailable(ImageUtil util) {
        return utilAvaliable.get(util);
    }

    public String runUtilVersionDetection(ImageUtil type) throws IOException, InterruptedException {
        UtilHandler versionDetection = utilVersionDetectionHandlers.get(type);
        String command = constructCommand(versionDetection.getCommand());
        CliCommand.Result result = new CliCommand(command).execute();
        String rawOutput = null;
        Stream stream = versionDetection.getParser().getStream();
        switch (stream) {
            case STDERR:
                rawOutput = result.getStderr();
                break;
            case STDOUT:
                rawOutput = result.getStdout();
                break;
            default:
                throw new IOException(String.format("empty response from '%s' (%s)", command, stream));
        }
        String parsed = parseData(rawOutput, versionDetection.getParser());
        return parsed == null || parsed.isEmpty() ? rawOutput.trim() : parsed.trim();
    }

    private String constructCommand(Command command) {
        File path = command.getPath();
        return path != null ?
                path.getAbsolutePath() + File.separator + command.getRawCommand() :
                command.getRawCommand();
    }


    private String constructCommand(Command ExecutionInfo, String imageFile) {
        File path = ExecutionInfo.getPath();
        String command = path != null ? path.getAbsolutePath() + File.separator + ExecutionInfo.getRawCommand() :
                ExecutionInfo.getRawCommand();
        command = command.replace("${IMAGE_FILE}", imageFile);
        System.out.println(command);
        return command;
    }

    private String parseData(String rawOutput, Parser parser) {
        if (parser.getRegexp() == null) {
            return rawOutput;
        } else {
            Matcher m = Pattern.compile(parser.getRegexp()).matcher(rawOutput);
            if (m.find()) {
                //first appearance
                return m.group(0);
            } else {
                // TODO: 29.9.16 not found
                return null;
            }
        }
    }

    public String runUtilExecution(ImageUtil utilType, String imageFile) throws IOException, InterruptedException {
        UtilHandler UtilHandler = utilExecutionHandlers.get(utilType);
        String command = constructCommand(UtilHandler.getCommand(), imageFile);
        CliCommand.Result result = new CliCommand(command).execute();
        String rawOutput = null;
        Stream stream = UtilHandler.getParser().getStream();
        switch (stream) {
            case STDERR:
                rawOutput = result.getStderr();
                break;
            case STDOUT:
                rawOutput = result.getStdout();
                break;
            default:
                throw new IOException(String.format("empty response from '%s' (%s)", command, stream));
        }
        String parsed = parseData(rawOutput, UtilHandler.getParser());
        return parsed == null || parsed.isEmpty() ? rawOutput.trim() : parsed.trim();
    }


}
