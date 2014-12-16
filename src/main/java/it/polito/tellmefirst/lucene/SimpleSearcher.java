/*-
 * Copyright (C) 2012 Federico Cairo, Giuseppe Futia, Federico Benedetto.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.polito.tellmefirst.lucene;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;

public class SimpleSearcher {

	static Log LOG = LogFactory.getLog(SimpleSearcher.class);

	LuceneManager luceneManager;
	IndexSearcher indexSearcher;
	IndexReader indexReader;

	public SimpleSearcher(LuceneManager lucene) throws IOException {
		LOG.debug("[constructor] - BEGIN");
		luceneManager = lucene;
		LOG.debug("Opening IndexSearcher for Lucene directory "
				+ luceneManager.getLuceneCorpusIndexDirectory());
		indexReader = IndexReader.open(luceneManager
				.getLuceneCorpusIndexDirectory(), true);
		indexSearcher = new IndexSearcher(indexReader);
		LOG.debug("[constructor] - END");
	}

	public Document getFullDocument(int docNo) throws IOException {
		LOG.debug("[getFullDocument] - BEGIN");
		Document document;
		document = indexReader.document(docNo);
		LOG.debug("[getFullDocument] - END");
		return document;
	}

	public ScoreDoc[] getHits(Query query) throws IOException {
		LOG.debug("[getHits] - BEGIN");
		ScoreDoc[] result = getTopResults(query,
				luceneManager.getLimitForQueryResult());
		LOG.debug("[getHits] - END");
		return result;
	}

	private ScoreDoc[] getTopResults(Query query, int numResults)
			throws IOException {
		ScoreDoc[] hits;
		LOG.debug("[getTopResults] - BEGIN");
		TopScoreDocCollector collector = TopScoreDocCollector.create(
				numResults, false);
		indexSearcher.search(query, collector);
		hits = collector.topDocs().scoreDocs;
		LOG.debug("[getTopResults] - END");
		return hits;
	}

	public LuceneManager getLuceneManager() {
		return luceneManager;
	}

	public IndexSearcher getIndexSearcher() {
		return indexSearcher;
	}

	public String getTitle(String uri) throws IOException {
		LOG.debug("[getTitle] - BEGIN");
		String result = "";
		String cleanUri = uri.replace("http://it.dbpedia.org/resource/", "")
				.replace("http://dbpedia.org/resource/", "");
		Query q = new TermQuery(new Term("URI", cleanUri));
		TopDocs hits = indexSearcher.search(q, 1);
		if (hits.totalHits != 0) {
			int docId = hits.scoreDocs[0].doc;
			Document doc = getFullDocument(docId);
			if (doc.getField("TITLE").stringValue() != null) {
				result = doc.getField("TITLE").stringValue();
			}
		}
		LOG.debug("[getTitle] - END");
		return result;
	}

	public String getImage(String uri) throws IOException {
		LOG.debug("[getImage] - BEGIN");
		String result = "";
		String cleanUri = uri.replace("http://it.dbpedia.org/resource/", "")
				.replace("http://dbpedia.org/resource/", "");
		Query q = new TermQuery(new Term("URI", cleanUri));
		TopDocs hits = indexSearcher.search(q, 1);
		if (hits.totalHits != 0) {
			int docId = hits.scoreDocs[0].doc;
			Document doc = getFullDocument(docId);
			if (doc.getField("IMAGE") != null) {
				result = doc.getField("IMAGE").stringValue();
			}
		}
		LOG.debug("[getImage] - END");
		return result;
	}

	public List getTypes(String uri) throws IOException {
		LOG.debug("[getTypes] - BEGIN");
		List<String> result = new ArrayList<>();
		String cleanUri = uri.replace("http://it.dbpedia.org/resource/", "")
				.replace("http://dbpedia.org/resource/", "");
		Query q = new TermQuery(new Term("URI", cleanUri));
		TopDocs hits = getIndexSearcher().search(q, 1);
		if (hits.totalHits != 0) {
			int docId = hits.scoreDocs[0].doc;
			Document doc = getFullDocument(docId);
			Field[] types = doc.getFields("TYPE");
			for (Field type : types) {
				result.add(type.stringValue());
			}
		}
		LOG.debug("[getTypes] - END");
		return result;
	}
}
