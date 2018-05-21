package data.storage.impl;

import data.Attribute;
import data.Record;
import data.storage.DataSource;

import java.util.*;
import java.io.*;
import java.util.Collections;
import java.util.stream.Collectors;

import com.univocity.parsers.csv.*;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

public class GetRecordsFromCSV implements DataSource {
    private Set<Record> records;

    public Set<Record> getAllRecords() {
        return records;
    }

    public Iterator iterator() {
        return records.iterator();
    }

    private String getLastName(Object obj) {
        String name = (String) obj;

        if(name.split("\\w+").length>1){
            String lastname = name.substring(name.lastIndexOf(" ")+1);
            if (lastname != null) {
                try {
                    String newString = new String(lastname.getBytes("UTF-8"), "UTF-8");
                    return newString;
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            } else {
                System.out.println("NULL: "+ name);
                return null;
            }
        }

        if (name != null && !name.equals("?")) return name;
        return null;
    }

    public GetRecordsFromCSV(String file, Set<String> keys)
            throws UnsupportedEncodingException, FileNotFoundException {
        records = new HashSet<>();

        CsvParserSettings settings = new CsvParserSettings();
        settings.setMaxColumns(2048);
        CsvParser parser = new CsvParser(settings);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");

        List<String[]> allRows = parser.parseAll(reader);
        String[] attributes = allRows.remove(0);

        for(String[] model : allRows) {
            Set<Attribute> attrSet = new HashSet<>();
            int size = model.length;

            for (int i=0; i < size; i++) {
                if (keys.contains(attributes[i])) {
                    Attribute attr = new Attribute(attributes[i]);
                    if (model[i] == null) continue;

                    String str = model[i];
                    str = str.replaceAll("\n", " ")
                            .replaceAll("-", "")
                            .replaceAll("/", "")
                            .replaceAll("'", "")
                            .replaceAll(",", "")
                            .replaceAll(":", " ")
                            .replaceAll(" +", " ")
                            .replaceAll("^\"+", "")
                            .replaceAll("\"+$", "")
                            .toLowerCase();

                    attr.addValue(str);
                    attrSet.add(attr);
                }

                if (attributes[i].contains("id")) {
                    Attribute attr = new Attribute("__id");
                    attr.addValue(model[i]);
                    attrSet.add(attr);
                }
            }

            records.add(new Record(1.0, attrSet));
        }
    }
}