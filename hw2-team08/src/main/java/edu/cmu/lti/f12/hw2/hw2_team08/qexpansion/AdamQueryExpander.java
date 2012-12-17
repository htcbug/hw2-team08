package edu.cmu.lti.f12.hw2.hw2_team08.qexpansion;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/*
 * The AdamQueryExpander wraps the ADAM database
 * (Another Database of Abbreviations in MEDLINE)
 * and retrieves long forms for abbreviated terms.
 * 
 * See <a href="http://arrowsmith.psych.uic.edu/arrowsmith_uic/adam.html">URL</a>
 *
 * @author <a href="mailto:norii@andrew.cmu.edu">Naoki Orii</a>
 */
public class AdamQueryExpander extends AbstractQueryExpander {

	private Map<String, List<String>> mTermVariantMap;

	@Override
	public boolean init(Properties prop) {
		mTermVariantMap = new HashMap<String, List<String>>();

		try {
			String adamFile = (String) prop.getProperty("parameter");
			loadMap(this.getClass().getResourceAsStream(adamFile));
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private void loadMap(InputStream file) throws FileNotFoundException,
			IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(file));
		String line;

		while ((line = br.readLine()) != null) {
			if (line.startsWith("#")) {
				continue;
			}
			String[] columns = line.split("\t");

			String term = columns[0];
			if (!mTermVariantMap.containsKey(term)) {
				mTermVariantMap.put(term, new ArrayList<String>());
			}

			String[] variants = columns[2].split("\\|");
			for (int i = 0; i < variants.length; i++) {
				String variant = variants[i];
				String[] tmp = variant.split(":");
				String var = tmp[0];

				mTermVariantMap.get(term).add(var);
			}
		}
	}

	@Override
	public List<String> expandQuery(String query, int size) {
		List<String> retval = new ArrayList<String>();
		if (mTermVariantMap.containsKey(query)) {
			retval = mTermVariantMap.get(query);
		}

		if (retval.size() > size)
			return retval.subList(0, size);

		return retval;
	}

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		AdamQueryExpander expander = new AdamQueryExpander();
		Properties prop = new Properties();
		prop.setProperty("database", "data/adam_database");
		expander.init(prop);

		String query = "BRCA1";
		List<String> expandedQueries = expander.expandQuery(query, 1);

		for (String expandedQuery : expandedQueries) {
			System.out.println(expandedQuery);
		}
	}
}
