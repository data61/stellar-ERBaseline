package data;

import com.wcohen.ss.Jaro;
import com.wcohen.ss.BasicStringWrapper;

public class TitleMatcher implements AtomicMatch {

	float threshold = 0.9F;
	
	public TitleMatcher() {

	}

	public TitleMatcher(float th)
	{
		threshold = th;
	}
	
	/**
	 * Test if titles match
	 * In this implementation the jaro similarity metric is used to compare string values.
	 * Very similar strings will have a jaro score close to 1.  Different strings have a 
	 * jaro score close to 0.
	 * @param s1 title 1
	 * @param s2 title 2
	 * @return true if jaro.score(title1, title2) > threshold (threshold defaults to 0.9)
	 */
	public boolean valuesMatch(String s1, String s2) 
	{

		BasicStringWrapper sw1 = new BasicStringWrapper(s1);
		BasicStringWrapper sw2 = new BasicStringWrapper(s2);
	
		Jaro jro = new Jaro();
		double tmp = jro.score(sw1, sw2);

		if (tmp > threshold)
			return true;
		else
			return false;
		
	}

}
