/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.polito.tellmefirst.http;

import it.polito.tellmefirst.classify.Classifier;
import java.util.List;
import static java.util.stream.Collectors.toList;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 *
 * @author RMuzzi
 */
@Path("core/classify")
public class ClassifyHTTPInterface {

	@POST
	@Consumes("application/json; charset=utf-8")
	@Produces("application/json; charset=utf-8")
	public List<ClassifyOutput> getClassifyText(ClassifyInput ci ) {
		return classifyCoreAdapter(
			new Classifier(ci.getLang()).classify(
				ci.getText(),
				ci.getNumTopics(),
				ci.getLang()));
	}

	public List<ClassifyOutput> classifyCoreAdapter(List<String[]> classifyCoreOutput) {
		return classifyCoreOutput.stream().map(coreOutputStrings -> {
			ClassifyOutput co = new ClassifyOutput();
			co.setUri(coreOutputStrings[0]);
			co.setLabel(coreOutputStrings[1]);
			co.setTitle(coreOutputStrings[2]);
			co.setScore(coreOutputStrings[3]);
			co.setMergedTypes(coreOutputStrings[4]);
			co.setImage(coreOutputStrings[5]);
			co.setWikilink(coreOutputStrings[6]);
			return co;
		}).collect(toList());
	}

	public static class ClassifyInput {
		String text;
		Integer numTopics;
		String lang;	
		public String getText() {
			return text;
		}
		public void setText(String text) {
			this.text = text;
		}
		public Integer getNumTopics() {
			return numTopics;
		}
		public void setNumTopics(Integer numTopics) {
			this.numTopics = numTopics;
		}
		public String getLang() {
			return lang;
		}
		public void setLang(String lang) {
			this.lang = lang;
		}
	}

	//XXX Esempio di classe statica che mappa il JSON di output. Andrebbe spostata altrove.
	public static class ClassifyOutput {

		private String uri,
			label,
			title,
			score,
			mergedTypes,
			image,
			wikilink;

		public String getUri() {
			return uri;
		}

		public void setUri(String uri) {
			this.uri = uri;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String label) {
			this.label = label;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getScore() {
			return score;
		}

		public void setScore(String score) {
			this.score = score;
		}

		public String getMergedTypes() {
			return mergedTypes;
		}

		public void setMergedTypes(String mergedTypes) {
			this.mergedTypes = mergedTypes;
		}

		public String getImage() {
			return image;
		}

		public void setImage(String image) {
			this.image = image;
		}

		public String getWikilink() {
			return wikilink;
		}

		public void setWikilink(String wikilink) {
			this.wikilink = wikilink;
		}
	}
}
