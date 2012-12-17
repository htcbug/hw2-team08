package edu.cmu.lti.f12.hw2.hw2_team08.retrieval;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;

import edu.cmu.lti.f12.hw2.hw2_team08.qexpansion.AbstractQueryExpander;
import edu.cmu.lti.f12.hw2.hw2_team08.qexpansion.ExpanderFactory;
import edu.cmu.lti.oaqa.core.provider.solr.SolrWrapper;
import edu.cmu.lti.oaqa.cse.basephase.retrieval.AbstractRetrievalStrategist;
import edu.cmu.lti.oaqa.framework.data.Keyterm;
import edu.cmu.lti.oaqa.framework.data.RetrievalResult;

/**
 * The QueryExpansionRetrievalStrategist utilizes multiple query expanders to
 * get the similar words of keyterms and use these words to formulate a new
 * Lucene query.
 * 
 * @author Yuan Gu <yuangu@andrew.cmu.edu>
 * 
 */
public class QueryExpansionRetrievalStrategist extends
		AbstractRetrievalStrategist {

	protected Integer hitListSize;

	private Integer expandFactor;

	protected SolrWrapper wrapper;

	private List<AbstractQueryExpander> expanderList;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			this.hitListSize = (Integer) aContext
					.getConfigParameterValue("hit-list-size");
		} catch (ClassCastException e) { // all cross-opts are strings?
			this.hitListSize = Integer.parseInt((String) aContext
					.getConfigParameterValue("hit-list-size"));
		}

		try {
			this.expandFactor = (Integer) aContext
					.getConfigParameterValue("expand-factor");
		} catch (ClassCastException e) { // all cross-opts are strings?
			this.expandFactor = Integer.parseInt((String) aContext
					.getConfigParameterValue("expand-factor"));
		}

		String serverUrl = (String) aContext.getConfigParameterValue("server");
		Integer serverPort = (Integer) aContext.getConfigParameterValue("port");
		Boolean embedded = (Boolean) aContext
				.getConfigParameterValue("embedded");
		String core = (String) aContext.getConfigParameterValue("core");
		try {
			this.wrapper = new SolrWrapper(serverUrl, serverPort, embedded,
					core);
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}

		String[] expanders = (String[]) aContext
				.getConfigParameterValue("expanders");

		Properties prop = new Properties();

		this.expanderList = new ArrayList<AbstractQueryExpander>();

		ExpanderFactory expanderFactory = new ExpanderFactory();
		for (String expander : expanders) {
			System.out.println(expander);
			String[] tmpArray = expander.split(":");
			String className = tmpArray[0];
			String classPara = tmpArray[1];
			AbstractQueryExpander queryExpander = expanderFactory
					.getQueryExpander(className);
			if (queryExpander == null) {
				System.out.println("Failed to create" + expander);
			}

			prop.setProperty("parameter", classPara);
			if (!queryExpander.init(prop)) {
				System.out.println("Failed to initialize " + expander);
			}
			this.expanderList.add(queryExpander);
		}

	}

	@Override
	protected final List<RetrievalResult> retrieveDocuments(
			String questionText, List<Keyterm> keyterms) {
		String query = formulateQuery(keyterms);
		return retrieveDocuments(query);
	}

	protected String formulateQuery(List<Keyterm> keyterms) {
		String query = "";

		for (Keyterm keyterm : keyterms) {
			String strKeyterm = keyterm.getText();
			List<String> expandedKeyterms = new ArrayList<String>();

			for (AbstractQueryExpander expander : this.expanderList) {
				List<String> terms = expander.expandQuery(strKeyterm,
						this.expandFactor);
				if (terms != null)
					expandedKeyterms.addAll(terms);
			}

			// Remove duplicate keyterms
			expandedKeyterms = new ArrayList<String>(new LinkedHashSet<String>(
					expandedKeyterms));

			String queryComponent = "(\"" + strKeyterm;
			for (String strExpandedTerm : expandedKeyterms) {
				queryComponent += "\" OR \"" + strExpandedTerm;
			}

			queryComponent += "\")";

			query += queryComponent + " AND ";
		}

		/* remove the last " AND " */
		query = query.substring(0, query.length() - 5);

		System.out.println(" I was called QUERY: " + query);
		return query;
	}

	protected List<RetrievalResult> retrieveDocuments(String query) {
		List<RetrievalResult> result = new ArrayList<RetrievalResult>();
		try {
			SolrDocumentList docs = wrapper.runQuery(query, hitListSize);
			for (SolrDocument doc : docs) {
				RetrievalResult r = new RetrievalResult(
						(String) doc.getFieldValue("id"),
						(Float) doc.getFieldValue("score"), query);
				result.add(r);
				System.out.println(doc.getFieldValue("id"));
			}
		} catch (Exception e) {
			System.err.println("Error retrieving documents from Solr: " + e);
		}
		return result;
	}

	@Override
	public void collectionProcessComplete()
			throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		wrapper.close();
	}
}