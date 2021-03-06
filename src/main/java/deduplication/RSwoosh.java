package deduplication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import data.MatcherMerger;
import data.Record;
import er.SimpleBlocking;
import utils.JSONConfig;

public class RSwoosh
{
	public static Set<Record> execute_with_blocking(MatcherMerger mm, JSONConfig props, Set<Record> recordsOrig){

		Set<Record> records = new HashSet<Record>(recordsOrig);
		Set<Record> rmatch = new HashSet<Record>();
		Map<Integer, Set<Record>> recordSetInBuckets = new HashMap();

		// Initialize simple blocking
		SimpleBlocking sb = new SimpleBlocking(props);

		if (!records.isEmpty()) {
			// Step 1: performing blocking
			if (sb.perform_LSH_blocking(recordsOrig)) {
				if (sb.getBucketSize() <= 0) {
					System.out.println("Bucket is empty. No blocking is performed.");
				}
				// Step 2: loop through all blocks
				for (int j = 0; j < sb.getBucketSize(); j++) {
					Set<Record> bucket = sb.getRecordFromBucket(j);
					if (bucket != null) {
						System.out.print("Bucket " + j + ": " + bucket.size());
						// Step 3: perform matching within the same blocks only, then get and concantenate buckets
						rmatch.addAll(execute((data.MatcherMerger) mm, bucket));
					}
				}
			}
		}

		return rmatch;
	}

	public static Set<Record> execute(MatcherMerger mm, Set<Record> recordsOrig)
	{
		Set<Record> records = new HashSet<Record>(recordsOrig);
		Set<Record> rprime = new HashSet<Record>();

		int count = 0;
		while (!records.isEmpty())
		{
//			System.out.println("R size: " + records.size() + ", R' size: " + rprime.size());

			// Remove one element from R
			Record current = records.iterator().next();
			records.remove(current);
			count += 1;

			Record buddy = null;
			for (Record r : rprime)
			{
				if (mm.match(current, r))
				{
					buddy = r;
					break;
				}
			}

			if (buddy == null)
			{
				rprime.add(current);
			}
			else
			{
				rprime.remove(buddy);
				records.add(mm.merge(current, buddy));
			}
		}

		System.out.println("\t Matching: " + count + "\t New Size: " + rprime.size());
		return rprime;
	}


}
