/**
 * TellMeFirst - A Knowledge Discovery Application
 *
 * Copyright (C) 2012 Federico Cairo, Giuseppe Futia, Federico Benedetto
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

package it.polito.tellmefirst.classify;

import it.polito.tellmefirst.classify.threads.ClassiThread;
import it.polito.tellmefirst.lucene.IndexesUtil;
import it.polito.tellmefirst.lucene.LuceneManager;
import it.polito.tellmefirst.lucene.SimpleSearcher;
import it.polito.tellmefirst.util.TMFUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;
import static java.util.Optional.ofNullable;
import java.util.TreeMap;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;

public class Classifier {

	LuceneManager contextLuceneManager;
	SimpleSearcher searcher;
	static Log LOG = LogFactory.getLog(Classifier.class);

	public Classifier(String lang) {
		LOG.debug("[constructor] - BEGIN");
		if (lang.equals("it")) {
			LOG.info("[Initializing italian Classifier...");
			searcher = IndexesUtil.ITALIAN_CORPUS_INDEX_SEARCHER;
		} else {
			LOG.info("[Initializing english Classifier...");
			searcher = IndexesUtil.ENGLISH_CORPUS_INDEX_SEARCHER;
		}
		contextLuceneManager = searcher.getLuceneManager();
		LOG.debug("[constructor] - END");
	}

	public ArrayList<String[]> classify(String textString, int numOfTopics,
			String lang) throws InterruptedException, IOException,
			ParseException {
		LOG.debug("[classify] - BEGIN");

		ArrayList<String[]> result;
		Text text = new Text(textString);

		int totalNumWords = TMFUtils.countWords(textString);
		// no prod
		LOG.debug("TOTAL WORDS: " + totalNumWords);
		if (totalNumWords > 1000) {
			// no prod
			LOG.debug("Text contains " + totalNumWords
					+ " words. We'll use Classify for long texts.");
			result = classifyLongText(text, numOfTopics, lang);
		} else {
			// no prod
			LOG.debug("Text contains " + totalNumWords
					+ " words. We'll use Classify for short texts.");
			result = classifyShortText(text, numOfTopics, lang);
		}
		LOG.debug("[classify] - END");

		return result;
	}

	public ArrayList<String[]> classifyLongText(Text text, int numOfTopics,
			String lang) throws InterruptedException,
			IOException {
		LOG.debug("[classifyLongText] - BEGIN");
		ArrayList<String[]> result;
		// no prod
		LOG.debug("[classifyLongText] - We're using as analyzer: "
				+ contextLuceneManager.getLuceneDefaultAnalyzer());
		String longText = text.getText();
		ArrayList<String> pieces = new ArrayList<String>();

		// split long text in smaller parts and call
		// getContextQueryForKBasedDisambiguator() for each one
		int n = 0;
		while (TMFUtils.countWords(longText) > 1000) {
			String firstPart = StringUtils.join(longText.split(" "), " ", 0,
					1000);
			String secondPart = StringUtils.join(longText.split(" "), " ",
					1000, TMFUtils.countWords(longText));
			pieces.add(firstPart);
			// no prod
			LOG.debug("Piece n°" + n + " analyzing...");
			longText = secondPart;
			if (TMFUtils.countWords(longText) < 300) {
				// no prod
				LOG.debug("Final piece contains "
						+ TMFUtils.countWords(longText)
						+ " words. Discarded, because < " + "300 words.");
			} else if (TMFUtils.countWords(longText) < 1000) {
				// no prod
				LOG.debug("Final piece contains "
						+ TMFUtils.countWords(longText) + " words.");
				pieces.add(longText);
			}
			n++;
		}
		ArrayList<ScoreDoc> mergedHitList = new ArrayList<ScoreDoc>();
		ArrayList<ClassiThread> threadList = new ArrayList<ClassiThread>();
		for (String textPiece : pieces) {
			ClassiThread thread = new ClassiThread(contextLuceneManager,
					searcher, textPiece);
			thread.start();
			threadList.add(thread);
		}
		for (ClassiThread thread : threadList) {
			thread.join();
			ScoreDoc[] hits = thread.getHits();
			ArrayList<ScoreDoc> hitList = new ArrayList<ScoreDoc>();
			for (int b = 0; b < numOfTopics; b++) {
				hitList.add(hits[b]);
			}
			mergedHitList.addAll(hitList);
		}
		HashMap<Integer, Integer> scoreDocCount = new HashMap<Integer, Integer>();
		for (ScoreDoc scoreDoc : mergedHitList) {
			Integer count = scoreDocCount.get(scoreDoc.doc);
			scoreDocCount.put(scoreDoc.doc, (count == null) ? 1 : count + 1);
		}
		HashMap<Integer, Integer> sortedMap = TMFUtils
				.sortHashMapIntegers(scoreDocCount);
		LinkedHashMap<ScoreDoc, Integer> sortedMapWithScore = new LinkedHashMap<ScoreDoc, Integer>();
		for (int docnum : sortedMap.keySet()) {
			Document doc = searcher.getFullDocument(docnum); // XXX
			boolean flag = true;
			for (ScoreDoc sdoc : mergedHitList) {
				if (flag && sdoc.doc == docnum) {
					sortedMapWithScore.put(sdoc, sortedMap.get(docnum));
					flag = false;
				}
			}
		}
		ArrayList<ScoreDoc> finalHitsList = sortByRank(sortedMapWithScore);
		ScoreDoc[] hits = new ScoreDoc[finalHitsList.size()];
		for (int i = 0; i < finalHitsList.size(); i++) {
			hits[i] = finalHitsList.get(i);
		}
		result = classifyCore(hits, numOfTopics, lang);
		LOG.debug("[classifyLongText] - END");
		return result;
	}

	public ArrayList<String[]> classifyShortText(Text text, int numOfTopics,
			String lang) throws ParseException, IOException {
		LOG.debug("[classifyShortText] - BEGIN");
		ArrayList<String[]> result;
		// no prod
		LOG.debug("[classifyShortText] - We're using as analyzer: "
				+ contextLuceneManager.getLuceneDefaultAnalyzer());
		Query query = contextLuceneManager.getQueryForContext(text);
		ScoreDoc[] hits = searcher.getHits(query);
		result = classifyCore(hits, numOfTopics, lang);
		LOG.debug("[classifyShortText] - END");
		return result;
	}

	public ArrayList<String[]> classifyCore(ScoreDoc[] hits, int numOfTopics, String lang) throws IOException {
		LOG.debug("[classifyCore] - BEGIN");

		ArrayList<String[]> result = new ArrayList<String[]>();
		
		if (hits.length == 0) {
			LOG.error("No results given by Lucene query from Classify!!");
		} else {

			for (int i = 0; i < numOfTopics; i++) {

				String[] arrayOfFields = new String[7];

				Document doc = searcher.getFullDocument(hits[i].doc);
				String uri = "";
				String visLabel = "";
				String title = "";
				String mergedTypes = "";
				String image;
				String wikilink = "";

				if (lang.equals("italian")) {
					String italianUri = "http://it.dbpedia.org/resource/"
							+ doc.getField("URI").stringValue();
					wikilink = "http://it.wikipedia.org/wiki/"
							+ doc.getField("URI").stringValue();

					// Italian: resource without a corresponding in English DBpedia
					if (doc.getField("SAMEAS") == null) {
						uri = italianUri;
						title = doc.getField("TITLE").stringValue();
						visLabel = title.replaceAll("\\(.+?\\)", "").trim();
						Field[] types = doc.getFields("TYPE");
						StringBuilder typesString = new StringBuilder();
						for (Field value : types) {
							typesString.append(value.stringValue() + "#");
						}
						mergedTypes = typesString.toString();
						image = ofNullable(doc.getField("IMAGE"))
								.flatMap(y -> ofNullable(y.stringValue()))
								.orElse("");

					//
					// Italian: resource with a corresponding in English DBpedia.
					//
					// Note: in this case we use getImage() to get the image URL, rather
					// than the "IMAGE" field, under the assumption that the english
					// version of DBPedia is more rich.
					//
					} else {
						uri = doc.getField("SAMEAS").stringValue();
						title = IndexesUtil.getTitle(uri, "en");
						visLabel = doc.getField("TITLE").stringValue()
								.replaceAll("\\(.+?\\)", "").trim();
						image = IndexesUtil.getImage(uri, "en");
						ArrayList<String> typesArray = IndexesUtil.getTypes(uri, "en");
						StringBuilder typesString = new StringBuilder();
						for (String type : typesArray)
							typesString.append(type + "#");
						mergedTypes = typesString.toString();
					}

				// English
				} else {
					uri = "http://dbpedia.org/resource/"+doc.getField("URI").stringValue();
					wikilink = "http://en.wikipedia.org/wiki/"+doc.getField("URI").stringValue();
					title = doc.getField("TITLE").stringValue();
					visLabel = title.replaceAll("\\(.+?\\)", "").trim();
					image = ofNullable(doc.getField("IMAGE"))
								.flatMap(y -> ofNullable(y.stringValue()))
								.orElse("");
					Field[] types = doc.getFields("TYPE");
					StringBuilder typesString = new StringBuilder();
					for (Field value : types) {
						typesString.append(value.stringValue() + "#");
					}
					mergedTypes = typesString.toString();
				}

				LOG.debug("[classifyCore] - uri = " + uri);
				LOG.debug("[classifyCore] - title = " + title);
				LOG.debug("[classifyCore] - wikilink = " + wikilink);
				// LOG.debug("[classifyCore] - getWikiHtmlUrl = " +
				// getWikiHtmlUrl);

				String score = String.valueOf(hits[i].score);
				arrayOfFields[0] = uri;
				arrayOfFields[1] = visLabel;
				arrayOfFields[2] = title;
				arrayOfFields[3] = score;
				arrayOfFields[4] = mergedTypes;
				arrayOfFields[5] = image;
				arrayOfFields[6] = wikilink;
				

				result.add(arrayOfFields);
			}
		}

		LOG.debug("[classifyCore] - END size=" + result.size());
		return result;
	}

	public ArrayList<ScoreDoc> sortByRank(LinkedHashMap<ScoreDoc, Integer> inputList) {
		LOG.debug("[sortByRank] - BEGIN");
		ArrayList<ScoreDoc> result = new ArrayList<ScoreDoc>();
		LinkedMap apacheMap = new LinkedMap(inputList);
		for (int i = 0; i < apacheMap.size() - 1; i++) {
			TreeMap<Float, ScoreDoc> treeMap = new TreeMap<Float, ScoreDoc>(
					Collections.reverseOrder());
			do {
				i++;
				treeMap.put(((ScoreDoc) apacheMap.get(i - 1)).score,
						(ScoreDoc) apacheMap.get(i - 1));
			} while (i < apacheMap.size()
					&& apacheMap.getValue(i) == apacheMap.getValue(i - 1));
			i--;
			for (Float score : treeMap.keySet()) {
				result.add(treeMap.get(score));
			}
		}
		LOG.debug("[sortByRank] - END");
		return result;
	}
	
}
