package validation;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import data.Attribute;
import data.Record;

import java.io.*;
import java.util.*;

public class Evaluator {

    private String gfile;
    private int true_positive;
    private int numberResults;
    private int numberGroundTruth;
    private double beta;
    private HashMap<List<String>, Integer> groundTruth;

    public Evaluator(String fname)
    {
        gfile = fname;
        true_positive = 0;
        numberResults = 0;
        numberGroundTruth = 0;
        groundTruth = new HashMap<List<String>, Integer>();
        beta = 1.0;   // beta == 1.0 means recall and precision are equally important.
    }

    public boolean runEval(Set<Record> results) {
        System.out.println("=====================================");
        System.out.println("Evaluating Precision/Recall/F-Score");

        // Step 1: Read ground truth from file
        try {
            loadGroundTruthFromCSV(gfile);

            // Step 2: Populate results
            populateTruePositiveFromResults(results);

            // Step 3: Compute precision/recall
            computePrecisionRecall();

        } catch(UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        } catch(FileNotFoundException e) {
            System.out.println("Ground truth file not found: " + gfile);
            System.out.println("=====================================");
            return false;
        }
        return true;
    }

    private void computePrecisionRecall(){
        float precision = (float) true_positive / numberResults;
        float recall = (float) true_positive / numberGroundTruth;
        double beta2 = beta * beta;
        double fscore = (1 + beta2) * (precision * recall) / (beta2 * precision + recall);
        System.out.printf("Precision: %.4f\n", precision);
        System.out.printf("Recall: %.4f\n", recall);
        System.out.printf("F-score: %.4f (beta set to %.4f)\n", fscore, beta);
        System.out.println("=====================================");
    }

    private void checkAttr(Attribute attr){
        if (attr.getValuesCount() > 1) {
            Iterator iterId = attr.iterator();
            List<String> idStack = new ArrayList<>();

            while (iterId.hasNext())
                idStack.add((String) iterId.next());

            System.out.println(idStack.toString());
        }
    }

    private boolean populateTruePositiveFromResults(Set<Record> results){
        results.forEach(record -> {
//            checkAttr(record.getAttribute("full_name"));
//            checkAttr(record.getAttribute("address"));
            Attribute ids = record.getAttribute("__id");
            if (ids.getValuesCount() > 1) {
                Iterator iterId = ids.iterator();
                List<String> idStack = new ArrayList<>();

                while (iterId.hasNext())
                    idStack.add((String) iterId.next());

                numberResults += (float) idStack.size() * ((float) idStack.size() - 1) / 2;

                for (int i = 0; i < idStack.size(); ++i) {
                    for (int j = i + 1; j < idStack.size(); ++j) {
//                        System.out.println("[" + i + " " + j + "] - " + idStack.get(i) + " " + idStack.get(j));
                        if (groundTruth.get(Arrays.asList(idStack.get(i), idStack.get(j))) != null) {
//                            System.out.println("********* TP found! ");
                            true_positive += 1;
                        }
                    }
                }
//            }
            } else {
                numberResults += 1;
            }
        });

        System.out.println("Number of Results Found: " + numberResults);
        System.out.println("Number of True Positive: " + true_positive);

        return true;
    }

    private boolean loadGroundTruthFromCSV(String file)
            throws UnsupportedEncodingException, FileNotFoundException {

        // Read ground truth from CSV which contain only true positive pair (A,B), but not (B,A).
        // Create a hashmap which add (A,B) and (B,A) as key.
        // If either of the key is found in the result, then true positive + 1.

        CsvParserSettings settings = new CsvParserSettings();
        CsvParser parser = new CsvParser(settings);
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
        List<String[]> allRows = parser.parseAll(reader);

        numberGroundTruth = allRows.size();

        for(String[] model : allRows) {
            // Create keys for both directions, so either direction is true positive.
            groundTruth.put(
                    // unmodifiable so key cannot change hash code
                    Collections.unmodifiableList(Arrays.asList(model[0], model[1])),
                    1
            );
            groundTruth.put(
                    // unmodifiable so key cannot change hash code
                    Collections.unmodifiableList(Arrays.asList(model[1], model[0])),
                    1
            );
        }
        System.out.println("Number of Ground Truth: " + numberGroundTruth);

        return true;
    }
}
