package data.storage.impl;

import java.io.IOException;
import java.io.OutputStreamWriter;

import data.Record;
import data.io.ParseYahooData;
import data.io.RecordFilter;
import data.io.XMLifyYahooData;

public class PrintRecordsFromYahooData extends ParseYahooData {
	
	OutputStreamWriter outWriter = new OutputStreamWriter(System.out);
	int count=0;
	
	public PrintRecordsFromYahooData(String[] srcs, RecordFilter rf, int maxcount) 
	throws IOException
	{
		super(srcs, rf);
				
		parseDataSources(maxcount);
	}
	
	public PrintRecordsFromYahooData(String[] srcs, RecordFilter rf) 
	throws IOException
	{
		super(srcs, rf);
			
		parseDataSources();
	}
	
	
	public PrintRecordsFromYahooData(String[] srcs) 
	throws IOException
	{
		super(srcs);
			
		parseDataSources();
	}
	
	public PrintRecordsFromYahooData(String[] srcs, int maxcount) 
	throws IOException
	{
		super(srcs);
		parseDataSources(maxcount);
	}
	
	public PrintRecordsFromYahooData(String[] srcs, int[] mcPerFile) 
	throws IOException
	{
		super(srcs);
		parseDataSources(mcPerFile);
	}
	
	@Override
	protected void action(Record r) 
	throws IOException
	{
		XMLifyYahooData.serializeRecord(r, outWriter);
		count++;
	}
	
	protected void parseBegin() throws IOException 
	{
		outWriter.write("<recordSet>\n");
		outWriter.close();
		System.err.println("Wrote "+count+" records to standard output.\n");		
	}
	
	protected void parseEnd() throws IOException 
	{
		outWriter.write("\n</recordSet>\n");
		outWriter.close();
		System.err.println("Wrote "+count+" records to standard output.\n");
		
	}
}
