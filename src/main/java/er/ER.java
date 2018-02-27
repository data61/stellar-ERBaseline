package er;

import data.Record;
import data.io.XMLifyYahooData;
import data.storage.impl.DBLPACMToCSV;
import data.storage.impl.GetRecordsFromCSV;
import data.storage.impl.GetRecordsFromYahooXML;
import data.storage.impl.EPGMRecordHandler;

import deduplication.RSwoosh;
import er.rest.api.RestParameters;
import org.xml.sax.SAXException;
import utils.DataFileFormat;

import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;


public class ER {
	static final String MATCHER_MERGER_INTERFACE = "data.MatcherMerger";
	Class matcherMerger;
	Properties properties;
	EPGMRecordHandler epgmParser;

	//Recursively checks if testClass or any of its ancestor class implements MathcerMerger interface
	private boolean checkMatcherMergerInterface(Class testClass){
		Class[] interfaces = testClass.getInterfaces();
		Class superClass = testClass.getSuperclass();
		try{
			for (int i = 0; i < interfaces.length; i++){
				if (interfaces[i] == Class.forName(MATCHER_MERGER_INTERFACE)){
					return true;
				}
			}
			if (superClass!= null && checkMatcherMergerInterface(superClass)){
				return true;
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	public Set<Record> runRSwoosh(Set<Record> records)
			throws SAXException, IOException, InvalidObjectException, ParserConfigurationException {
		try {
		    Constructor mmConstructor = matcherMerger.getConstructor(Properties.class);
			Object matcherMerger = mmConstructor.newInstance(properties);
			System.out.println("Running RSwoosh on " + records.size() + " records.");

			Set<String> venues = new HashSet<>();
			records.forEach(r -> {
				Iterator it = r.getAttribute("venue").iterator();
				venues.add((String)it.next());
			});

			long mins = 0;
			long secs = 0;
			long runTime = 0;
			long startTime = System.currentTimeMillis();
//			Set<Record> result = RSwoosh.execute((data.MatcherMerger)matcherMerger, records);
			Set<Record> result = RSwoosh.execute_with_blocking((data.MatcherMerger)matcherMerger, records);
			runTime = System.currentTimeMillis() - startTime;
			mins = TimeUnit.MILLISECONDS.toMinutes(runTime);
			secs = TimeUnit.MILLISECONDS.toSeconds(runTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runTime));
			System.out.println("\t" + mins + " m, " + secs + " s");

			System.out.println("After running RSwoosh, there are " + result.size() + " records.");

			return result;
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	public Set<Record> runRSwoosh(Set<Record> records, RestParameters parameters)
			throws SAXException, IOException, InvalidObjectException, ParserConfigurationException {
		try {
			Constructor mmConstructor = matcherMerger.getConstructor(RestParameters.class);
			Object matcherMerger = mmConstructor.newInstance(parameters);
			System.out.println("Running RSwoosh on " + records.size() + " records.");

			Set<String> venues = new HashSet<>();
			records.forEach(r -> {
				Iterator it = r.getAttribute("venue").iterator();
				venues.add((String)it.next());
			});

			long mins = 0;
			long secs = 0;
			long runTime = 0;
			long startTime = System.currentTimeMillis();
//			Set<Record> result = RSwoosh.execute((data.MatcherMerger)matcherMerger, records);
			Set<Record> result = RSwoosh.execute_with_blocking((data.MatcherMerger)matcherMerger, records);
			runTime = System.currentTimeMillis() - startTime;
			mins = TimeUnit.MILLISECONDS.toMinutes(runTime);
			secs = TimeUnit.MILLISECONDS.toSeconds(runTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runTime));
			System.out.println("\t" + mins + " m, " + secs + " s");

			System.out.println("After running RSwoosh, there are " + result.size() + " records.");

			return result;
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	public Set<Record> parseConfigFile(String configFile) throws Exception {
		properties = new Properties();
		properties.load(new FileInputStream(configFile));
		String fileSources = properties.getProperty("FileSources");
		if (fileSources == null){
			throw (new Exception("No File Sources specified!"));
		}

		String matcherMerger = properties.getProperty("MatcherMerger");
		RestParameters parameters = new RestParameters();
		parameters.setPropertiesFromConfigFile(properties);

		return parseRecords(parameters, matcherMerger);
	}

	public Set<Record> parseRecords(RestParameters parameters, String mm)
			throws Exception {

		List<String> inputFiles = new ArrayList<>();
		inputFiles = Arrays.asList(parameters.fileSources.split(","));

		matcherMerger = Class.forName(mm);
		if (matcherMerger == null){
			throw (new Exception("No MatcherMerger Class specified!"));
		}
		if (checkMatcherMergerInterface(matcherMerger) != true){
			throw (new Exception("Given MatcherMerger class does not implement SimpleMatcherMerger interface!"));

		}

		Map<Path, DataFileFormat> parsers = new HashMap();

		for (String path : inputFiles) {
			Path filepath = Paths.get(parameters.prefix+"/"+path);
			boolean exists = Files.exists(filepath);
			if (!exists)
				throw (new InvalidObjectException("Data source must be a file. "+ filepath));

			if(filepath.toString().lastIndexOf(".") != -1 && filepath.toString().lastIndexOf(".") != 0) {
				DataFileFormat dataFileFormat = DataFileFormat.fromString(filepath.toString().substring(filepath.toString().lastIndexOf(".") + 1));
				parsers.put(filepath, dataFileFormat);
			}
		}

		Set<Record> records = new HashSet();
		epgmParser = new EPGMRecordHandler(null);

		for (Map.Entry<Path,DataFileFormat> pair : parsers.entrySet()){
			String fileSrc = pair.getKey().toString();
			DataFileFormat format = pair.getValue();

			switch (format) {
				case XML:
					GetRecordsFromYahooXML yds = new GetRecordsFromYahooXML(fileSrc);
					yds.parseXML();
					records.addAll(yds.getAllRecords());
					break;
				case CSV:
					GetRecordsFromCSV csvParser = new GetRecordsFromCSV(fileSrc);
					records.addAll(csvParser.getAllRecords());
					break;
				case EPGM:

					epgmParser.recordsFromEPGM(fileSrc);
					records.addAll(epgmParser.getAllRecords());
					break;
				default:
					throw new IllegalArgumentException("Invalid data format: " + format);
			}
		}

		System.out.println("Read records: " + records.size());
		return records;
	}

	public void writeResults(Set<Record> results)
			throws IOException {

		String outputFile = properties.getProperty("OutputFile");
		RestParameters para = new RestParameters();
		para.outputFile = outputFile;
		para.prefix = properties.getProperty("Prefix");

		this.writeResults(results, para);
	}

	public void writeResults(Set<Record> results, RestParameters parameters)
			throws IOException {

		String outputFile = parameters.outputFile;
		if (results != null && results.size() > 0 && outputFile != null) {
			String ext[] = outputFile.split("\\.");
			DataFileFormat format = DataFileFormat.EPGM;
			if (ext.length == 2) {
				try {
					format = DataFileFormat.fromString(ext[1]);
				} catch (IllegalArgumentException e) {
					System.out.println(e.getCause());
				}
			}

			String dir = parameters.prefix+"/"+outputFile;
			switch (format){
				case EPGM:
					System.out.println("write epgm");
					epgmParser.writeEPGMFromRecords(results, dir);
					break;
				case CSV:
					System.out.println("write csv");

					DBLPACMToCSV write = new DBLPACMToCSV(dir);
					write.writeRecords(results);
					break;
				default:
					System.out.println("default " + format.toString());

					FileWriter fw = new FileWriter(dir);
					fw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
					XMLifyYahooData.openRecordSet(fw);
					for (Record r : results)
						XMLifyYahooData.serializeRecord(r, fw);
					XMLifyYahooData.closeRecordSet(fw);
					fw.close();
			}
		}
	}
}
