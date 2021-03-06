package edu.cmu.lti.f12.hw2.hw2_team08.qexpansion;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import edu.mit.jwi.*;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.morph.WordnetStemmer;

/**
 * 
 * The WordNetQueryExpander class uses the See <a
 * href="http://wordnet.princeton.edu">WordNet</a> to stem the query words and
 * find its synonyms.
 * 
 * @author <a href="mailto:yuangu@andrew.cmu.edu">Yuan Gu</a>
 */


public class WordNetQueryExpander extends AbstractQueryExpander {

	// Use singleton pattern
	private IDictionary dict;

	private WordnetStemmer stemmer;

	@Override
	public boolean init(Properties prop) {
		try {
			String dictPath = (String) prop.getProperty("parameter");
			File dictFile = new File(dictPath);
			dict = new Dictionary(dictFile);
			dict.open();
			stemmer = new WordnetStemmer(dict);
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	@Override
	public List<String> expandQuery(String query, int size) {

		List<String> stems = stemmer.findStems(query, POS.VERB);
		if (stems == null || stems.size() == 0)
			return null;

		IIndexWord idxWord = dict.getIndexWord(stems.get(0), POS.VERB);
		if (idxWord == null || idxWord.getWordIDs().isEmpty())
			return null;

		IWordID wordID = idxWord.getWordIDs().get(0); // 1st meaning
		IWord word = dict.getWord(wordID);
		ISynset synset = word.getSynset();
		List<IWord> words = synset.getWords();
		List<String> expandedQueries = new ArrayList<String>(words.size());
		for (IWord w : synset.getWords()) {
			String lemma = w.getLemma();
			lemma = lemma.replaceAll("_", " ");
			expandedQueries.add(lemma);
		}

		if (expandedQueries.size() > size)
			return expandedQueries.subList(0, size);

		return expandedQueries;
	}

	public static void main(String[] args) throws IOException {
		AbstractQueryExpander expander = new WordNetQueryExpander();
		Properties prop = new Properties();
		prop.setProperty("parameter", "/Users/htcbug/wordnet-dict");
		expander.init(prop);

		String query = "affected";
		List<String> expandedQueries = expander.expandQuery(query, 100);

		for (String expandedQuery : expandedQueries) {
			System.out.println(expandedQuery);
		}
	}
}