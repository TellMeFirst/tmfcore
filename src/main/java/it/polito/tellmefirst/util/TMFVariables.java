/*-
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
package it.polito.tellmefirst.util;

import static it.polito.tellmefirst.util.TMFUtils.unchecked;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;

public class TMFVariables {

	static Log LOG = LogFactory.getLog(TMFVariables.class);

	public static String CORPUS_INDEX_IT;
	public static String KB_IT;
	public static String RESIDUAL_KB_IT;
	public static Set<String> STOPWORDS_IT;

	public static String CORPUS_INDEX_EN;
	public static String KB_EN;
	public static String RESIDUAL_KB_EN;
	public static Set<String> STOPWORDS_EN;

	/**
	 * Initialize the internal variables. Yes, this is crazy but you
	 * must instantiate this class to initialize internal variables that
	 * are later used to instantiate the classifiers. If you don't call
	 * this constructor, you are likely to get a NullPointer error
	 * later when you construct the classifier.
	 *
	 * @since 3.0.0.0.
	 */
	public TMFVariables() {
		unchecked(() -> {
			InputStream configStream = Thread.currentThread()
				.getContextClassLoader()
				.getResourceAsStream("tmfcore.properties");
			init(configStream);
		});
	}

	/**
	 * Initialize the internal variables. Yes, this is crazy but you
	 * must instantiate this class to initialize internal variables that
	 * are later used to instantiate the classifiers. If you don't call
	 * this constructor, you are likely to get a NullPointer error
	 * later when you construct the classifier.
	 *
	 * @since 1.0.0.0.
	 */
	@Deprecated
	public TMFVariables(String confFile) {
		unchecked(() -> {
			InputStream configStream = new FileInputStream(new File(confFile));
			init(configStream);
		});
	}

	private void init(InputStream confStream) {
		unchecked(() -> {
			LOG.debug("[constructor] - BEGIN");
			Properties config = new Properties();

			config.load(confStream);

			CORPUS_INDEX_IT = config.getProperty("corpus.index.it", "").trim();
			KB_IT = config.getProperty("kb.it", "").trim();
			RESIDUAL_KB_IT = config.getProperty("residualkb.it", "").trim();
			STOPWORDS_IT = TMFUtils.getStopWords(config.getProperty("stopWords.it", "").trim());

			CORPUS_INDEX_EN = config.getProperty("corpus.index.en", "").trim();
			KB_EN = config.getProperty("kb.en", "").trim();
			RESIDUAL_KB_EN = config.getProperty("residualkb.en", "").trim();
			STOPWORDS_EN = TMFUtils.getStopWords(config.getProperty("stopWords.en", "").trim());

			LOG.debug("[constructor] - END");
		});
	}
}
