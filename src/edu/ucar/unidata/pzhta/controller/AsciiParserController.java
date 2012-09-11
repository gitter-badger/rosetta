package edu.ucar.unidata.pzhta.controller;

import org.grlea.log.SimpleLogger;

import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import edu.ucar.unidata.pzhta.domain.AsciiFile;

import edu.ucar.unidata.pzhta.Pzhta;

/**
 * Controller to parse ASCII file data.
 *
 */

@Controller
public class AsciiParserController {

    private static final SimpleLogger log = new SimpleLogger(AsciiParserController.class);

    @RequestMapping(value="/parse", method=RequestMethod.POST)
    @ResponseBody
    public String parseFile(AsciiFile file, BindingResult result) {
        String filePath = System.getProperty("java.io.tmpdir") + "/" + file.getUniqueId() + "/" + file.getFileName();
        StringBuffer sBuffer = new StringBuffer();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String sCurrentLine;            
            int lineCount = 0;
            int delimiterRunningTotal = 0;
            boolean dataLine = false;
            List <String> delimiterList = file.getDelimiterList(); 
            String selectedDelimiter = "";    
            ArrayList<ArrayList> outerList = new ArrayList<ArrayList>();  
            if (!file.getDelimiterList().isEmpty()) {
                selectedDelimiter = delimiterList.get(0);  
            }             
            while ((sCurrentLine = reader.readLine()) != null) {
                if (file.getHeaderLineNumberList().isEmpty()) {
                    sBuffer.append(sCurrentLine + "\n");     
                } else {
                    if (file.getDelimiterList().isEmpty()) {
                        log.error("ASCII file header lines are present, but no delimiters are specified.");
                        return null;
                    } else {
                        if (file.getHeaderLineNumberList().contains(new Integer(lineCount).toString())) {
                            sBuffer.append(sCurrentLine + "\n");    
                        } else {             
                            Iterator<String> delimiterIterator = delimiterList.iterator();
                            while (delimiterIterator.hasNext()) {  
                                String delimiter = delimiterIterator.next();
                                int delimiterCount = StringUtils.countMatches(sCurrentLine, delimiter);
                                if (!dataLine) {
                                    delimiterRunningTotal = delimiterCount;
                                    dataLine = true;
                                } else {       
                                    if (delimiterRunningTotal != delimiterCount) {
                                        log.error("ASCII file line of data contains an irregular delimiter count at line number: " + new Integer(lineCount).toString() + " for delimiter: " + delimiter);
                                        return null;
                                    }
                                }                               
                            }
                            if (delimiterList.size() != 1) {
                                String[] delimiters = (String[])delimiterList.toArray(new String[delimiterList.size()]);
                                for(int i = 1; i< delimiters.length; i++){ 
		                             String updatedLineData = sCurrentLine.replaceAll(delimiters[i],  selectedDelimiter);
                                     sBuffer.append(updatedLineData + "\n");   
                                     String[] lineComponents = sBuffer.toString().split("selectedDelimiter");
                                }                            
                            } else {
                                sBuffer.append(sCurrentLine + "\n");  
                                String[] lineComponents = sBuffer.toString().split("selectedDelimiter"); 
                                ArrayList<String> innerList = new ArrayList<String>(Arrays.asList(lineComponents));
                                outerList.add(innerList);
                            }
                        }
                    }
                }
                lineCount++;
            }   
            if (!file.getDelimiterList().isEmpty()) {
                selectedDelimiter = selectedDelimiter + "\n";
            }
            if (file.getDone() != null) {          
                Pzhta ncWriter = new Pzhta();
                String ncmlFile = System.getProperty("java.io.tmpdir") + "/" + file.getUniqueId() + "/" + "file.getUniqueId().ncml";
                String fileOutName = System.getProperty("java.io.tmpdir") + "/" + file.getUniqueId() + "/" + "file.getUniqueId().nc";
                if (ncWriter.convert(ncmlFile, fileOutName, outerList)) {
                    return fileOutName;
                } else {
                    log.error("netCDF file not created.");
                    return null;
                }
            } else {
                return selectedDelimiter + sBuffer.toString();
            }
        } catch (IOException e) {
            log.error(e.getMessage());
            return null;
	    }
    }
}

