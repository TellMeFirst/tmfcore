/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package it.polito.tellmefirst.http;

import it.polito.tellmefirst.classify.Classifier;
import static java.util.Arrays.asList;
import java.util.List;
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
	@Produces("application/json; charset=utf-8")
	public List<ClassifyOutput> getClassifyText(	String text, 
													Integer numTopics,
													String lang){
		return classifyCoreAdapter(		
			new Classifier(lang).classify(  text, 
											numTopics, 
											lang) );
	}
	
	//TODO adapt classify core output to POJO
	public List<ClassifyOutput> classifyCoreAdapter( List<String[]> classifyCoreOutput ){
		//TODO ...
		
		return asList( new ClassifyOutput() );
	}
	
	//XXX Esempio di classe statica che mappa il JSON di output. Andrebbe spostata altrove.
	public static class ClassifyOutput {
		private String	uri,	
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
