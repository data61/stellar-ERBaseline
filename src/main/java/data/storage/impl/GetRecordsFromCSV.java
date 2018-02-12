package data.storage.impl;

import data.Attribute;
import data.Record;
import data.storage.DataSource;

import java.util.*;
import java.io.*;
import java.util.Collections;
import java.util.stream.Collectors;

import com.univocity.parsers.csv.*;

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

    public GetRecordsFromCSV(String file)
            throws UnsupportedEncodingException, FileNotFoundException {
        records = new HashSet<>();

        CsvParserSettings settings = new CsvParserSettings();
        CsvParser parser = new CsvParser(settings);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");

        List<String[]> allRows = parser.parseAll(reader);
        String[] attributes = allRows.remove(0);

        for(String[] model : allRows) {
            Set<Attribute> attrSet = new HashSet<>();
            int size = model.length;
            for (int i=0; i < size; i++) {
                Attribute attr = new Attribute(attributes[i]);

                if (attributes[i].equals("authors")) {
                    if (model[i] == null) continue;
                    List authors = Arrays.asList(model[i].split(","));
                    List updated = (List) authors.stream().map(s->getLastName(s)).collect(Collectors.toList());

                    if (updated.size() > 0) {
                        Collections.sort(updated);
                        attr.addValue(String.join(",", updated));
                        attrSet.add(attr);
                    } else {
                        System.out.println("Null authors attributes!");
                        attr = null;
                    }
                } else {
                    attr.addValue(model[i]);
                    attrSet.add(attr);
                }
            }

            records.add(new Record(1.0, attrSet));
        }
    }
}