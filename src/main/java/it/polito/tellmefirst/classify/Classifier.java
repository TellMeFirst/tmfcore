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
package it.polito.tellmefirst.classify;

import it.polito.tellmefirst.classify.threads.ClassiThread;
import it.polito.tellmefirst.lucene.IndexesUtil;
import it.polito.tellmefirst.lucene.LuceneManager;
import it.polito.tellmefirst.lucene.SimpleSearcher;
import it.polito.tellmefirst.util.TMFUtils;
import static it.polito.tellmefirst.util.TMFUtils.unchecked;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
	String language;
	static Log LOG = LogFactory.getLog(Classifier.class);

	/**
	 * Instantiate the classifier.
	 * @param lang The classifier language ("it" or "en").
	 * @since 1.0.0.0.
	 */
	public Classifier(String lang) {
		LOG.debug("[constructor] - BEGIN");
		if (lang.equals("it")) {
			LOG.info("Initializing italian Classifier...");
			searcher = IndexesUtil.ITALIAN_CORPUS_INDEX_SEARCHER;
			language = "italian";
		} else {
			LOG.info("Initializing english Classifier...");
			searcher = IndexesUtil.ENGLISH_CORPUS_INDEX_SEARCHER;
			language = "english";
		}
		contextLuceneManager = searcher.getLuceneManager();
		LOG.debug("[constructor] - END");
	}

	/**
	 * Classify a piece of text. This function is implemented by different
	 * algorithms depending on the input text size; if the input text is
	 * less than 1,000 words, classifyShortText() is called, otherwise this
	 * function will invoke an algorithm that splits the text in chunks,
	 * calls classifyShortText() on each chunk, and then merge the results.
	 *
	 * @param textString the input piece of text.
	 * @param numOfTopics maximum number of topics to be returned (less
	 *        topics may be returned).
	 * @return A list of vectors of strings. Each vector shall be composed
	 *         of seven strings: the URI, the label, the title, the
	 *         score, the merged type, the image, and the wiki link.
	 *
	 * @since 1.0.0.0.
	 */
	public List<String[]> classify(String textString,
			int numOfTopics) {
		return unchecked(() -> {
			LOG.debug("[classify] - BEGIN");

			List<String[]> result;
			Text text = new Text(textString);

			int totalNumWords = TMFUtils.countWords(textString);
			LOG.debug("TOTAL WORDS: " + totalNumWords);
			if (totalNumWords > 1000) {
				LOG.debug("Text contains " + totalNumWords
						+ " words. We'll use Classify for long texts.");
				result = classifyLongText(text, numOfTopics);
			} else {
				LOG.debug("Text contains " + totalNumWords
						+ " words. We'll use Classify for short texts.");
				result = classifyShortText(text, numOfTopics);
			}
			LOG.debug("[classify] - END");

			return result;
		});
	}

	private List<String[]> classifyLongText(Text text, int numOfTopics)
			throws InterruptedException, IOException {
		LOG.debug("[classifyLongText] - BEGIN");
		List<String[]> result;
		LOG.debug("[classifyLongText] - We're using as analyzer: "
				+ contextLuceneManager.getLuceneDefaultAnalyzer());
		String longText = text.getText();
		List<String> pieces = new ArrayList<>();

		// split long text in smaller parts and call
		// getContextQueryForKBasedDisambiguator() for each one
		int n = 0;
		while (TMFUtils.countWords(longText) > 1000) {
			String firstPart = StringUtils.join(longText.split(" "), " ", 0,
					1000);
			String secondPart = StringUtils.join(longText.split(" "), " ",
					1000, TMFUtils.countWords(longText));
			pieces.add(firstPart);
			LOG.debug("Piece num" + n + " analyzing...");
			longText = secondPart;
			if (TMFUtils.countWords(longText) < 300) {
				LOG.debug("Final piece contains "
						+ TMFUtils.countWords(longText)
						+ " words. Discarded, because < " + "300 words.");
			} else if (TMFUtils.countWords(longText) < 1000) {
				LOG.debug("Final piece contains "
						+ TMFUtils.countWords(longText) + " words.");
				pieces.add(longText);
			}
			n++;
		}
		List<ScoreDoc> mergedHitList = new ArrayList<>();
		List<ClassiThread> threadList = new ArrayList<>();
		pieces.stream().map((textPiece) -> new ClassiThread(
				contextLuceneManager, searcher, textPiece)).map((thread) -> {
			thread.start();
			return thread;
		}).forEach((thread) -> {
			threadList.add(thread);
		});
		for (ClassiThread thread : threadList) {
			thread.join();
			ScoreDoc[] hits = thread.getHits();
			List<ScoreDoc> hitList = new ArrayList<>();
			for (int b = 0; b < numOfTopics && b < hits.length; b++) {
				hitList.add(hits[b]);
			}
			mergedHitList.addAll(hitList);
		}
		Map<Integer, Integer> scoreDocCount = new HashMap<>();
		mergedHitList.stream().forEach((scoreDoc) -> {
			Integer count = scoreDocCount.get(scoreDoc.doc);
			scoreDocCount.put(scoreDoc.doc, (count == null) ? 1 : count + 1);
		});
		Map<Integer, Integer> sortedMap = TMFUtils
				.sortIntegersMap(scoreDocCount);
		Map<ScoreDoc, Integer> sortedMapWithScore = new LinkedHashMap<>();
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
		List<ScoreDoc> finalHitsList = sortByRank(sortedMapWithScore);
		ScoreDoc[] hits = new ScoreDoc[finalHitsList.size()];
		for (int i = 0; i < finalHitsList.size(); i++) {
			hits[i] = finalHitsList.get(i);
		}
		result = postProcess(hits, numOfTopics);
		LOG.debug("[classifyLongText] - END");
		return result;
	}

	/**
	 * Classify a short piece of text. This function is used to bypass
	 * the policy by which TMF triggers the long or the short classification
	 * process depending on the text length. Note that if the text passed
	 * to this function is too long, lucene may throw an exception.
	 *
	 * @param textString the input piece of text.
	 * @param numOfTopics maximum number of topics to be returned (less
	 *        topics may be returned).
	 * @return A list of vectors of strings. Each vector shall be composed
	 *         of seven strings: the URI, the label, the title, the
	 *         score, the merged type, the image, and the wiki link.
	 *
	 * @since 2.0.0.0.
	 */
	public List<String[]> classifyShortText(String textString,
			int numOfTopics) {
		return unchecked(() -> {
			return classifyShortText(new Text(textString),
					numOfTopics);
		});
	}

	private List<String[]> classifyShortText(Text text, int numOfTopics)
			throws ParseException, IOException {
		LOG.debug("[classifyShortText] - BEGIN");
		List<String[]> result;
		LOG.debug("[classifyShortText] - We're using as analyzer: "
				+ contextLuceneManager.getLuceneDefaultAnalyzer());
		Query query = contextLuceneManager.getQueryForContext(text);
		ScoreDoc[] hits = searcher.getHits(query);
		result = postProcess(hits, numOfTopics);
		LOG.debug("[classifyShortText] - END");
		return result;
	}

	private List<String[]> postProcess(ScoreDoc[] hits, int numOfTopics)
			throws IOException {
		LOG.debug("[classifyCore] - BEGIN");

		List<String[]> result = new ArrayList<>();

		for (int i = 0; i < numOfTopics && i < hits.length; i++) {

			String[] arrayOfFields = new String[7];

			Document doc = searcher.getFullDocument(hits[i].doc);
			String uri;
			String visLabel;
			String title;
			String mergedTypes;
			String image;
			String wikilink;

			if (language.equals("italian")) {
				String italianUri = "http://it.dbpedia.org/resource/"
						+ doc.getField("URI").stringValue();
				wikilink = "http://it.wikipedia.org/wiki/"
						+ doc.getField("URI").stringValue();

				// Italian: resource without a corresponding
				// in-English DBpedia
				if (doc.getField("SAMEAS") == null) {
					uri = italianUri;
					title = doc.getField("TITLE").stringValue();
					visLabel = title.replaceAll("\\(.+?\\)", "").trim();
					Field[] types = doc.getFields("TYPE");
					StringBuilder typesString = new StringBuilder();
					for (Field value : types) {
						typesString.append(value.stringValue()).append("#");
					}
					mergedTypes = typesString.toString();
					image = ofNullable(doc.getField("IMAGE"))
							.flatMap(y -> ofNullable(y.stringValue()))
							.orElse("");

				//
				// Italian: resource with a corresponding in-English
				// DBpedia.
				//
				// Note: in this case we use getImage() to get the
				// image URL, rather than the "IMAGE" field, under the
				// assumption that the english version of DBPedia is
				// more rich.
				//
				} else {
					uri = doc.getField("SAMEAS").stringValue();
					title = IndexesUtil.getTitle(uri, "en");
					visLabel = doc.getField("TITLE").stringValue()
							.replaceAll("\\(.+?\\)", "").trim();
					image = IndexesUtil.getImage(uri, "en");
					List<String> typesArray = IndexesUtil.getTypes(
							uri, "en");
					StringBuilder typesString = new StringBuilder();
					typesArray.stream().forEach((type) -> {
						typesString.append(type).append("#");
					});
					mergedTypes = typesString.toString();
				}

			} else {
				uri = "http://dbpedia.org/resource/"
						+ doc.getField("URI").stringValue();
				wikilink = "http://en.wikipedia.org/wiki/"
						+ doc.getField("URI").stringValue();
				title = doc.getField("TITLE").stringValue();
				visLabel = title.replaceAll("\\(.+?\\)", "").trim();
				image = ofNullable(doc.getField("IMAGE"))
						.flatMap(y -> ofNullable(y.stringValue()))
						.orElse("");
				Field[] types = doc.getFields("TYPE");
				StringBuilder typesString = new StringBuilder();
				for (Field value : types) {
					typesString.append(value.stringValue()).append("#");
				}
				mergedTypes = typesString.toString();
			}

			LOG.debug("[classifyCore] - uri = " + uri);
			LOG.debug("[classifyCore] - title = " + title);
			LOG.debug("[classifyCore] - wikilink = " + wikilink);

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

		LOG.debug("[classifyCore] - END size=" + result.size());
		return result;
	}

	private List<ScoreDoc> sortByRank(Map<ScoreDoc, Integer> inputList) {
		LOG.debug("[sortByRank] - BEGIN");
		List<ScoreDoc> result = new ArrayList<>();
		LinkedMap apacheMap = new LinkedMap(inputList);
		for (int i = 0; i < apacheMap.size() - 1; i++) {
			Map<Float, ScoreDoc> treeMap = new TreeMap<>(
					Collections.reverseOrder());
			do {
				i++;
				treeMap.put(((ScoreDoc) apacheMap.get(i - 1)).score,
						(ScoreDoc) apacheMap.get(i - 1));
			} while (i < apacheMap.size()
					&& apacheMap.getValue(i) == apacheMap.getValue(i - 1));
			i--;
			treeMap.keySet().stream().forEach((score) -> {
				result.add(treeMap.get(score));
			});
		}
		LOG.debug("[sortByRank] - END");
		return result;
	}
}
