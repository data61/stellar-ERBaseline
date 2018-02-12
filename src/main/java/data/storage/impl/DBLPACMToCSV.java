package data.storage.impl;

import com.univocity.parsers.csv.CsvWriter;
import com.univocity.parsers.csv.CsvWriterSettings;
import data.Attribute;
import data.Record;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DBLPACMToCSV {
    private FileWriter csvfw, csvfw2;
    private CsvWriter writer, writer2;

    public DBLPACMToCSV(String location) throws IOException {
        csvfw = new FileWriter(location);
        writer = new CsvWriter(csvfw, new CsvWriterSettings());

        csvfw2 = new FileWriter(location+"2");
        writer2 = new CsvWriter(csvfw2, new CsvWriterSettings());
    }

    public void writeRecords(Set<Record> records) {
        writer.writeHeaders("idDBLP","idACM");
        writer2.writeHeaders("idDBLP","idACM");

        records.forEach((Record record) -> {
            Attribute attrID = record.getAttribute("id");
            if (attrID.getValuesCount() == 2) {
                Iterator i1 = attrID.iterator();
                String acmID, dblpID;

                String first = (String)i1.next();
                String second = (String)i1.next();

                Boolean isFirstNum = false;
                Boolean isSecondNum = false;

                try {
                    int id = Integer.parseInt(first);
                    isFirstNum = true;
                } catch (NumberFormatException e) {
                    isFirstNum = false;
                }

                try {
                    int id = Integer.parseInt(second);
                    isSecondNum = true;
                } catch (NumberFormatException e) {
                    isSecondNum = false;
                }

                if (isFirstNum && isSecondNum) {
                    acmID = first;
                    dblpID = "";
                } else if (!isFirstNum && !isSecondNum) {
                    acmID = "";
                    dblpID = second;
                } else {
                    try {
                        int id = Integer.parseInt(first);
                        acmID = first;
                        dblpID = second;
                    } catch (NumberFormatException e) {
                        dblpID = first;
                        acmID = second;
                    }

                }

                writer.writeRow(dblpID+','+acmID);
            } else if (attrID.getValuesCount() == 1) {
                Iterator i1 = attrID.iterator();
                String acmID, dblpID;

                String value = (String)i1.next();
                try {
                    int id = Integer.parseInt(value);
                    acmID = value;
                    dblpID = "";
                } catch (NumberFormatException e) {
                    dblpID = value;
                    acmID = "";
                }
                writer.writeRow(dblpID+','+acmID);
            } else {
                List<String> acmIDs = new ArrayList<String>();
                List<String> dblpIDs = new ArrayList<String>();

                String row = "";
                Iterator it = attrID.iterator();
                while (it.hasNext()) {
                    String value = (String)it.next();
                    row = value + "," + row;
                    try {
                        int id = Integer.parseInt(value);
                        acmIDs.add(value);
                    } catch (NumberFormatException e) {
                        dblpIDs.add(value);
                    }
                }

                if (acmIDs.size() > 0 && dblpIDs.size() > 0) {
                    Collections.sort(acmIDs);
                    Collections.sort(dblpIDs);

                    Iterator acmIter = acmIDs.iterator();
                    Iterator dblpIter = dblpIDs.iterator();
                    while (acmIter.hasNext() && dblpIter.hasNext()) {
                        String acm = (String) acmIter.next();
                        String dblp = (String) dblpIter.next();
                        writer.writeRow(dblp+','+acm);
                    }

                    while (acmIter.hasNext()){
                        writer.writeRow(","+(String) acmIter.next());
                    }

                    while (dblpIter.hasNext()) {
                        writer.writeRow((String) dblpIter.next()+",");
                    }
                } else if (acmIDs.size() > 0 && dblpIDs.size() == 0) {
                    for (String id : acmIDs)
                        writer.writeRow(","+id);
                } else if (dblpIDs.size() > 0 && acmIDs.size() == 0) {
                    for (String id : dblpIDs)
                        writer.writeRow(id+",");
                } else
                    writer2.writeRow(row);
            }
        });

        writer.close();
        writer2.close();
    }
}
