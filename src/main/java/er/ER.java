package er;

import data.CoraMatcherMerger;
import data.Record;
import data.ReferenceMatcherMerger;
import data.SpammerMatcherMerger;
import data.io.XMLifyYahooData;
import data.storage.impl.DBLPACMToCSV;
import data.storage.impl.GetRecordsFromCSV;
import data.storage.impl.GetRecordsFromYahooXML;
import data.storage.impl.EPGMRecordHandler;

import deduplication.RSwoosh;
import org.xml.sax.SAXException;
import utils.DataFileFormat;
import utils.JSONConfig;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import validation.Evaluator;

public class ER {
	static final String MATCHER_MERGER_INTERFACE = "data.MatcherMerger";
	private static final Map<String, String[]> defaultMatcherMergers = Collections.unmodifiableMap(
			new HashMap<String, String[]>() {{
				put("data.ReferenceMatcherMerger", ReferenceMatcherMerger.keyWords);
				put("data.CoraMatcherMerger", CoraMatcherMerger.keyWords);
				put("data.SpammerMatcherMerger", SpammerMatcherMerger.keyWords);
			}});

	Class matcherMerger;
	JSONConfig properties;
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
		return runRSwoosh(records, properties);
	}

	public Set<Record> runRSwoosh(Set<Record> records, JSONConfig parameters)
			throws SAXException, IOException, InvalidObjectException, ParserConfigurationException {
		try {
			Constructor mmConstructor = matcherMerger.getConstructor(JSONConfig.class);
			Object matcherMerger = mmConstructor.newInstance(parameters);
			System.out.println("Running Stellar-ER on " + records.size() + " records.");

			long mins = 0;
			long secs = 0;
			long runTime = 0;
			long startTime = System.currentTimeMillis();
//			Set<Record> result = RSwoosh.execute((data.MatcherMerger)matcherMerger, records);
			Set<Record> result = RSwoosh.execute_with_blocking((data.MatcherMerger)matcherMerger, parameters, records);
			runTime = System.currentTimeMillis() - startTime;
			mins = TimeUnit.MILLISECONDS.toMinutes(runTime);
			secs = TimeUnit.MILLISECONDS.toSeconds(runTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(runTime));
			System.out.println("Time taken: " + mins + " m, " + secs + " s");

			System.out.println("After running Stellar-ER, there are " + result.size() + " records.");

			Evaluator eval = new Evaluator(parameters.prefix + "/" + parameters.options.get("ground_truth"));
			eval.runEval(result);

			return result;
		} catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

	public Set<Record> parseConfigFile(String configFile) throws Exception {
		properties = JSONConfig.createConfig(new FileReader(configFile));
		System.out.println("#####: " + properties.toString());

		if (!properties.isValid()){
			throw (new Exception("Invalid JSON config!"));
		}

		return parseRecords(properties);
	}

	public Set<Record> parseRecords(JSONConfig parameters)
			throws Exception {

		if (parameters.matcherMerger == null) {
			if (parameters.attributes.size() < 1)
				parameters.matcherMerger = (String) defaultMatcherMergers.keySet().toArray()[0];
			else {
				Set<String> keys = parameters.attributes.keySet();
				AtomicReference<String> ret = new AtomicReference<>("");
				AtomicReference<Double> score = new AtomicReference<>(0.0);

				defaultMatcherMergers.forEach((k, v) -> {
					Set<String> keyWords = new HashSet<String>(Arrays.asList(v));
					Set<String> common = keyWords.stream().filter(keys::contains).collect(Collectors.toSet());
					double newScore = (double) common.size() / (double) keys.size();
					if (newScore > score.get()) {
						score.set(newScore);
						ret.set(k);
					}
				});

				parameters.matcherMerger = ret.get();
			}
		}

		System.out.println("matcherMerger: " + parameters.matcherMerger);
		matcherMerger = Class.forName(parameters.matcherMerger);
		if (matcherMerger == null){
			throw (new Exception("No MatcherMerger Class specified!"));
		}
		if (checkMatcherMergerInterface(matcherMerger) != true){
			throw (new Exception("Given MatcherMerger class does not implement SimpleMatcherMerger interface!"));

		}

		List<String> inputFiles = new ArrayList<>();
		inputFiles = parameters.fileSources;

		Map<Path, DataFileFormat> parsers = new HashMap();

		for (String path : inputFiles) {
			Path filepath = Paths.get(parameters.prefix+"/"+path);
			boolean exists = Files.exists(filepath);
			if (!exists)
				throw (new InvalidObjectException("Data source not found: "+ filepath));

			if(filepath.toString().lastIndexOf(".") != -1 && filepath.toString().lastIndexOf(".") != 0) {
				DataFileFormat dataFileFormat = DataFileFormat.fromString(filepath.toString().substring(filepath.toString().lastIndexOf(".") + 1));
				parsers.put(filepath, dataFileFormat);
			} else
				parsers.put(filepath, DataFileFormat.fromString("EPGM"));
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
				default:
					try {
						epgmParser.recordsFromEPGM(fileSrc);
						records.addAll(epgmParser.getAllRecords());
						break;
					} catch (IOException e) {
						throw new IllegalArgumentException("Invalid data format: " + format);
					}
			}
		}

//		System.out.println("Got records: " + records.size());
		return records;
	}

	public void writeResults(Set<Record> results)
			throws IOException {
		this.writeResults(results, properties);
	}

	public void writeResults(Set<Record> results, JSONConfig parameters)
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
					System.out.println("Write EPGM...");
					epgmParser.writeEPGMFromRecords(results, dir);
					System.out.println("Done.");
					break;
				case CSV:
					System.out.println("Write CSV...");
					DBLPACMToCSV write = new DBLPACMToCSV(dir);
					write.writeRecords(results);
					System.out.println("Done.");
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
