package er;

import java.io.UnsupportedEncodingException;
import java.util.*;

import data.Attribute;
import data.Record;

import java.lang.String;

import info.debatty.java.lsh.LSHMinHash;
import info.debatty.java.lsh.MinHash;
import utils.JSONConfig;

/*
*
*  This class performs simple blocking based on selected record field.
*  Each block (a.k.a bucket) contains a uniqe set of records with high similarity on the selected record field, measured based on LSH.
*  There's no redundancy across blocks, i.e. each record is assigned to only 1 block.
*  Tune the following parameters to see different results:  buckets, n, MinHash error/dict_size,
*
*  Note:
*      buckets = 500,  n = 256, minhash_error_rate = 0.9   can generate 2698 records in 6 m 41 s.
*      buckets = 1000, n = 256, minhash_error_rate = 0.9   can generate 3465 records in 1 m 34 s  ,   or
*                                                                       3719 records in 17 s,         or
*                                                                       2790 records in 11 m 43 s,    or
*                                                                       2682 records in 9 m 32 s
* */

public class SimpleBlocking {

    // LSH parameters
    private long initial_seed = -1;
    // Attention: to get relevant results, the number of elements per bucket
    // should be at least 100
    private int buckets = 100;                                              /* NOTE: Tune this to put entries sparsely into buckets */
    // Size of vectors
    private int n = 1024;                                                    /* NOTE: Tune this to create larger vector. n is the size of the signature (the number of hash functions that are used to produce the signature) */
    // MinHash error rate
    private double minhash_error_rate = 0.9;                                /* NOTE: Tune this to enforce higher similarity  */
    // the number of stages is also sometimes called thge number of bands
    private int stages = 1;                                                 /* TODO: Really simple. Only 1 stage! Instead of 2, or 4, or bla */

    Map<Integer, Set<Record>> recordBuckets = new HashMap();         /* TODO: only cater for 1 stage! 1 set of records per bucket*/

    public SimpleBlocking(JSONConfig props) {
        if (props.isValid() && props.options.size() > 0) {
            buckets = Integer.valueOf(props.options.get("bucketsize"));
            minhash_error_rate = Float.valueOf(props.options.get("similarity"));
            initial_seed = Long.valueOf(props.options.get("seed"));
            System.out.println("Simple Blocking: set bucket size to " + buckets + " with seed: " + initial_seed + ", similarity threshold: " + minhash_error_rate);
        }
    }

    public boolean perform_LSH_blocking(Set<Record> recordsOrig){

        // Create and configure LSH algorithm
        MinHash minhash;
        if (initial_seed >= 0){
            minhash = new MinHash(minhash_error_rate, 256, initial_seed);
        } else {
            minhash = new MinHash(minhash_error_rate, 256);
        }

        LSHMinHash lsh = new LSHMinHash(stages, buckets, n);

        // Count number of record assign to each bucket at each stage
        int[][] counts = new int[stages][buckets];

        // Browse through records, get their key attribute, use that key attribute value as the hash key, create block of similar entries
        while (!recordsOrig.isEmpty()) {
            Record current = recordsOrig.iterator().next();
            recordsOrig.remove(current);

            Map<String, Attribute> map = current.getAttributes();
            for(Map.Entry<String, Attribute> entry: map.entrySet()) {   /* TODO: really need to loop through all keys? */
                String propKey = entry.getKey();
                switch (propKey) {
                    case "title": case "full_name" : case "name":                                     /* TODO：don't hard code this. */
                        Attribute val = entry.getValue();
                        String attrstr = val.toString().trim();
                        String[] strTokens = attrstr.split(" |\\;");   /* TODO：remove propKey from the String! */

                        // Get signature of record in int[]
                        int[] vector = minhash.signature(getStringArrInTreeSetInt(strTokens));
                        // Create hash for signature
                        int[] hash = lsh.hashSignature(vector);

                        // Add records into respective bucket in each stage.
                        //  Each offset in "hash" represent the bucket ID of that record in each stage.
                        for (int i = 0; i < hash.length; i++) {
                            counts[i][hash[i]]++;
                            addRecordToBucket(current, hash[i]);
                        }

                        //Debug START -----
//                        System.out.println("record: \n" + current.toString());

//                        System.out.println(propKey + " - " + attrstr);
//                        print(vector);
//                        System.out.print(" : ");
//                        print(hash);
//                        System.out.print("\n");
//
//                        for(String str : strTokens)
//                            System.out.print(str + " *** ");
//                        System.out.print("\n");
                        //Debug END -----

                        break;
                }
            }
//            System.out.println();
        }

        //Debug START -----
//        System.out.println("Number of elements per bucket at each stage:");
//        for (int i = 0; i < stages; i++) {
//            print(counts[i]);
//            System.out.print("\n");
//        }
//        System.out.println("Length of recContainer at each stage:");
//        for (int i = 0; i < stages; i++) {
//            int count = 0;
//            for (int j=0; j< buckets; j++) {
//                Set<Record> bucket = getRecordFromBucket(j);
//                if (bucket != null) {
//                    System.out.println("Bucket " + j + ": " + bucket.size());
//                    count += bucket.size();
//                }
//            }
//            System.out.println("Total records: " + count);
//        }
        //Debug END -----

        return true;
    }

    public int getBucketSize(){
        return buckets;
    }

    public Set<Record> getRecordFromBucket(int bucketId) {    /* TODO: only work for 1 stage!!! */
        if(recordBuckets.get(bucketId) == null)
        {
            return null;
        }

        Set<Record> recContainer = recordBuckets.get(bucketId);
        return recContainer;
    }


    /* ----------------------- Private FUNC ----------------------- */
    private void addRecordToBucket(Record data, int bucketId) {
        if(recordBuckets.get(bucketId) == null)
        {
            // this means this is the first record added for this level
            // so create a container to hold the object
            recordBuckets.put(bucketId, new HashSet<Record>());
        }

        Set<Record> recContainer = recordBuckets.get(bucketId);
        recContainer.add(data);
    }

    private TreeSet<Integer> getStringArrInTreeSetInt(String[] strTokens){
        //Debug START -----
//        for(String str : strTokens)
//            System.out.print(str + " *** ");
//        System.out.println();
        //Debug END -----

        TreeSet<Integer> hashset = new TreeSet<Integer>();
        // For each token in the string, generate an integer hashcode
        for(String str : strTokens){
            try{
                // First, convert token to UTF-8 bytes
                byte[] b = str.getBytes("UTF-8");
                // Next, generate integer by hashing each byte
                int hashCode = 0;
                int offset = 0;
                for (int ii = 0; ii < b.length; ii++) {
                    // Note:    0xff to ensure result is always positive
                    hashCode = 31 * hashCode + b[offset++] & 0xff;             /* TODO: better way to generate hashCode ?? Change to 63, what's the differences? */
                }
                // Last, append each hash-byte to TreeSet
                hashset.add(hashCode);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return hashset;
    }

    private static void print(int[] array) {
        System.out.print("[");
        for (int v : array) {
            System.out.print("" + v + " ");
        }
        System.out.print("]");
    }
}
