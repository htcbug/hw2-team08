package edu.cmu.lti.f12.hw2.hw2_team08.qexpansion;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import edu.mit.jwi.Dictionary;

/**
 * 
 * The MeshQueryExpander class uses the See <a
 * href="http://www.nlm.nih.gov/mesh/meshhome.html">MeSH</a> to find the gene's
 * synonyms.
 * 
 * @author <a href="mailto:norii@andrew.cmu.edu">Naoki Orii</a>
 */
public class MeshQueryExpander extends AbstractQueryExpander {

	private IndexSearcher searcher;

	private StandardAnalyzer analyzer;

	private IndexReader reader;

	@Override
	public boolean init(Properties prop) {
		try {
			String indexDir = (String) prop.getProperty("parameter");
			this.reader = IndexReader.open(FSDirectory.open(new File(indexDir)));
		} catch (IOException e) {
			return false;
		}
		this.searcher = new IndexSearcher(reader);
		this.analyzer = new StandardAnalyzer(Version.LUCENE_36);
		return true;
	}

	@Override
	public List<String> expandQuery(String q, int size) {
		List<String> retval = new ArrayList<String>();

		try {
			QueryParser parser = new QueryParser(Version.LUCENE_36, "synonym",
					analyzer);
			Query query = parser.parse(q);

			TopDocs results = searcher.search(query, null, 10);
			ScoreDoc[] hits = results.scoreDocs;

			int numTotalHits = results.totalHits;
			if (numTotalHits == 0) {
				return retval;
			}

			Document doc = searcher.doc(hits[0].doc);
			String[] synonyms = doc.getValues("synonym");
			for (String synonym : synonyms) {
				retval.add(synonym);
			}

			String hypernym = doc.get("hypernym");
			retval.add(hypernym);

			String[] hyponyms = doc.getValues("hyponym");
			for (String hyponym : hyponyms) {
				retval.add(hyponym);
			}

		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		// Remove repeated terms
		List<String> retlist = new ArrayList<String>(new LinkedHashSet<String>(
				retval));

		if (retlist.size() > size)
			return retlist.subList(0, size);

		return retlist;
	}

	public static void main(String[] args) throws CorruptIndexException,
			IOException {
		MeshQueryExpander expander = new MeshQueryExpander();
		Properties prop = new Properties();
		prop.setProperty("parameter", "/Users/htcbug/mesh.lucene.index");
		expander.init(prop);

		String query = "head";
		List<String> expandedQueries = expander.expandQuery(query, 2);

		for (String expandedQuery : expandedQueries) {
			System.out.println(expandedQuery);
		}
	}
}
