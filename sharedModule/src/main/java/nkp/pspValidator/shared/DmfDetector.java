package nkp.pspValidator.shared;

import nkp.pspValidator.shared.engine.exceptions.InvalidXPathExpressionException;
import nkp.pspValidator.shared.engine.exceptions.PspDataException;
import nkp.pspValidator.shared.engine.exceptions.XmlFileParsingException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

import static nkp.pspValidator.shared.Dmf.Type.*;

/**
 * Created by Martin Řehánek on 2.11.16.
 */
public class DmfDetector {


    public static final String DEFAULT_MONOGRAPH_VERSION = "1.0";
    public static final String DEFAULT_PERIODICAL_VERSION = "1.4";
    public static final String DEFAULT_SOUND_RECORDING_VERSION = "0.2";


    /**
     * Validátor zkontroluje hlavní mets soubor, konkrétně kořenový element <mets:mets> na hodnotu atributu TYPE. Možné hodnoty jsou „Monograph“ nebo „Periodical“. Platí:
     * Pokud nenalezne atribut TYPE – chyba.
     * Pokud se vyskytne jiná než povolená hodnota atributu – chyba.
     * Pokud se vyskytuje hodnota „Monograph“, zachází validátor s balíčkem jako s monografií.
     * Pokud se vyskytuje hodnota „Periodical“, zachází validátor s balíčkem jako s periodikem.
     */
    public Dmf.Type detectDmfType(File pspRootDir) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {

        try {
            File primaryMetsFile = findPrimaryMetsFile(pspRootDir);
            Document metsDoc = loadDocument(primaryMetsFile);
            XPathExpression xPathExpression = buildXpathIgnoringNamespaces("/mets/@TYPE");
            String docType = ((String) xPathExpression.evaluate(metsDoc, XPathConstants.STRING)).trim();
            if ("Monograph".equals(docType)) {
                return MONOGRAPH;
            } else if ("Periodical".equals(docType)) {
                return PERIODICAL;
            } else if ("sound recording".equals(docType)) {
                return SOUND_RECORDING;
            } else {
                throw new PspDataException(pspRootDir, String.format("atribut TYPE elementu mods neobsahuje korektní typ (Monograph/Periodical), ale hodnotu '%s'", docType));
            }
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException("", String.format("chyba v zápisu Xpath: %s", e.getMessage()));
        }
    }

    private File findPrimaryMetsFile(File pspRootDir) throws PspDataException {
        Pattern pattern = Pattern.compile("mets.*\\.xml", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);
        File[] metsCandidates = pspRootDir.listFiles((dir, name) -> pattern.matcher(name).matches());
        if (metsCandidates.length >= 2) {
            throw new PspDataException(pspRootDir,
                    String.format("nalezeno více možných souborů PRIMARY-METS, není jasné, který použít pro zjištění typu dokumentu (monografie/periodikum)"));
        } else if (metsCandidates.length == 0) {
            throw new PspDataException(pspRootDir,
                    String.format("nenalezen soubor PRIMARY-METS pro zjištění typu dokumentu (monografie/periodikum)"));
        } else {
            return metsCandidates[0];
        }
    }

    private Document loadDocument(File file) throws XmlFileParsingException {
        try {
            return XmlUtils.buildDocumentFromFile(file, false);
        } catch (SAXException e) {
            throw new XmlFileParsingException(file, String.format("chyba parsování xml v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (IOException e) {
            throw new XmlFileParsingException(file, String.format("chyba čtení v souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        } catch (ParserConfigurationException e) {
            throw new XmlFileParsingException(file, String.format("chyba konfigurace parseru při zpracování souboru %s: %s", file.getAbsolutePath(), e.getMessage()));
        }
    }

    private XPathExpression buildXpathIgnoringNamespaces(String xpathExpression) throws InvalidXPathExpressionException {
        try {
            XPathFactory xPathfactory = XPathFactory.newInstance();
            XPath xpath = xPathfactory.newXPath();
            return xpath.compile(xpathExpression);
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException(xpathExpression, String.format("chyba v zápisu Xpath '%s': %s", xpathExpression, e.getMessage()));
        }
    }

    /**
     * @param dmfType
     * @param pspRootDir
     * @return dmf version found in file INFO or null
     */
    public String detectDmfVersionFromInfoFile(Dmf.Type dmfType, File pspRootDir) throws PspDataException, XmlFileParsingException, InvalidXPathExpressionException {
        try {
            File infoFile = findInfoFile(pspRootDir);
            Document infoDoc = loadDocument(infoFile);
            XPathExpression xPathExpression = buildXpathIgnoringNamespaces("/info/metadataversion|/info/metadataVersion");
            String versionFound = ((String) xPathExpression.evaluate(infoDoc, XPathConstants.STRING)).trim();
            return versionFound == null || versionFound.isEmpty() ? null : versionFound;
        } catch (XPathExpressionException e) {
            throw new InvalidXPathExpressionException("", String.format("chyba v zápisu Xpath: %s", e.getMessage()));
        }
    }

    private File findInfoFile(File pspRootDir) throws PspDataException {
        Pattern pattern = Pattern.compile("info.*\\.xml", java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE);

        File[] infoCandidates = pspRootDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return pattern.matcher(name).matches();
            }
        });
        if (infoCandidates.length >= 2) {
            throw new PspDataException(pspRootDir,
                    String.format("nalezeno více možných souborů INFO, není jasné, který použít pro zjištění verze standardu DMF"));
        } else if (infoCandidates.length == 0) {
            throw new PspDataException(pspRootDir,
                    String.format("nenalezen soubor INFO pro zjištění verze standardu DMF"));
        } else {
            return infoCandidates[0];
        }
    }

    public Dmf resolveDmf(File pspRoot,
                          String preferredDmfMonVersion, String preferredDmfPerVersion, String preferredDmfSRVersion,
                          String forcedDmfMonVersion, String forcedDmfPerVersion, String forcedDmfSRVersion) throws PspDataException, InvalidXPathExpressionException, XmlFileParsingException {
        Dmf.Type type = detectDmfType(pspRoot);
        switch (type) {
            case MONOGRAPH: {
                return chooseVersion(MONOGRAPH, forcedDmfMonVersion, pspRoot, preferredDmfMonVersion, DEFAULT_MONOGRAPH_VERSION);
            }
            case PERIODICAL: {
                return chooseVersion(PERIODICAL, forcedDmfPerVersion, pspRoot, preferredDmfPerVersion, DEFAULT_PERIODICAL_VERSION);
            }
            case SOUND_RECORDING: {
                return chooseVersion(SOUND_RECORDING, forcedDmfSRVersion, pspRoot, preferredDmfSRVersion, DEFAULT_SOUND_RECORDING_VERSION);

            }
            default:
                throw new IllegalStateException();
        }
    }

    private Dmf chooseVersion(Dmf.Type type, String forcedVersion, File pspRoot, String preferredVersion, String defaultVersion) throws PspDataException, InvalidXPathExpressionException, XmlFileParsingException {
        if (forcedVersion != null) {
            return new Dmf(type, forcedVersion);
        } else {
            String versionFromInfo = detectDmfVersionFromInfoFile(type, pspRoot);
            if (versionFromInfo != null) {
                return new Dmf(type, versionFromInfo);
            } else if (preferredVersion != null) {
                return new Dmf(type, preferredVersion);
            } else {
                return new Dmf(type, defaultVersion);
            }
        }
    }

}
