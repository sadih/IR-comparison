/*
 * Skeleton class for the Lucene search program implementation
 * Created on 2011-12-21
 * Jouni Tuominen <jouni.tuominen@aalto.fi>
 */
package fi.aalto.ir;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.document.DateTools;

public class LuceneSearchApp {
	
	private Directory index;
	
	public LuceneSearchApp() {

	}
	
	public void index(List<RssFeedDocument> docs) {

		// implement the Lucene indexing here
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);
		this.index = new RAMDirectory();

		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_42, analyzer);
		try {
			IndexWriter w = new IndexWriter(this.index, config);
			for (RssFeedDocument feed: docs) {
				addDoc(w, feed.getTitle(), feed.getDescription(), feed.getPubDate());
			}

			w.close();
		} catch (IOException e) {
			System.out.print(e);
		}
		
		
		
	}
	
	private static void addDoc(IndexWriter w, String title, String description, Date publication_date) throws IOException {
		Document doc = new Document();
		doc.add(new TextField("title", title, Field.Store.YES));
		doc.add(new TextField("description", description, Field.Store.YES));
		doc.add(new IntField("publication_date", new Integer(DateTools.dateToString(publication_date, DateTools.Resolution.DAY)), Field.Store.YES));

		w.addDocument(doc);
	}
	
	public List<String> search(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) throws ParseException, IOException {
		
		printQuery(inTitle, notInTitle, inDescription, notInDescription, startDate, endDate);

		List<String> results = new LinkedList<String>();

		// implement the Lucene search here
		IndexReader reader = IndexReader.open(this.index);
		IndexSearcher searcher = new IndexSearcher(reader);
		
		BooleanQuery bq = new BooleanQuery();
		
		if (inTitle != null) {
			for (String q: inTitle) {
				bq.add(new TermQuery(new Term("title", q)), BooleanClause.Occur.MUST);
			}
		}
		
		if (notInTitle != null) {
			for (String q: notInTitle) {
				bq.add(new TermQuery(new Term("title", q)), BooleanClause.Occur.MUST_NOT);
			}
		}
		
		if (inDescription != null) {
			for (String q: inDescription) {
				bq.add(new TermQuery(new Term("description", q)), BooleanClause.Occur.MUST);
			}
		}
		
		if (notInDescription != null) {
			for (String q: notInDescription) {
				bq.add(new TermQuery(new Term("description", q)), BooleanClause.Occur.MUST_NOT);
			}
		}
		
		
		if (startDate != null && endDate != null) {
			NumericRangeQuery date = NumericRangeQuery.newIntRange("publication_date", new Integer(startDate.replace("-", "")), new Integer(endDate.replace("-", "")), true, true);
			bq.add(new BooleanClause(date , BooleanClause.Occur.MUST));
		} else if (startDate != null) {
			bq.add(new BooleanClause(NumericRangeQuery.newIntRange("publication_date", new Integer(startDate.replace("-", "")), 20141231, true, true) , BooleanClause.Occur.MUST));
		} else if (endDate != null) {
			bq.add(new BooleanClause(NumericRangeQuery.newIntRange("publication_date", 20001231, new Integer(endDate.replace("-", "")), true, true) , BooleanClause.Occur.MUST));
		}
		
		TopDocs result = searcher.search(bq, 100);
		for (ScoreDoc doc: result.scoreDocs) {
			results.add(reader.document(doc.doc).get("title"));
		}
		
		return results;
	}
	
	public void printQuery(List<String> inTitle, List<String> notInTitle, List<String> inDescription, List<String> notInDescription, String startDate, String endDate) {
		System.out.print("Search (");
		if (inTitle != null) {
			System.out.print("in title: "+inTitle);
			if (notInTitle != null || inDescription != null || notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (notInTitle != null) {
			System.out.print("not in title: "+notInTitle);
			if (inDescription != null || notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (inDescription != null) {
			System.out.print("in description: "+inDescription);
			if (notInDescription != null || startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (notInDescription != null) {
			System.out.print("not in description: "+notInDescription);
			if (startDate != null || endDate != null)
				System.out.print("; ");
		}
		if (startDate != null) {
			System.out.print("startDate: "+startDate);
			if (endDate != null)
				System.out.print("; ");
		}
		if (endDate != null)
			System.out.print("endDate: "+endDate);
		System.out.println("):");
	}
	
	public void printResults(List<String> results) {
		if (results.size() > 0) {
			Collections.sort(results);
			for (int i=0; i<results.size(); i++)
				System.out.println(" " + (i+1) + ". " + results.get(i));
		}
		else
			System.out.println(" no results");
	}
	
	public static void main(String[] args) throws ParseException, IOException {
		if (args.length > 0) {
			LuceneSearchApp engine = new LuceneSearchApp();
			
			RssFeedParser parser = new RssFeedParser();
			parser.parse(args[0]);
			List<RssFeedDocument> docs = parser.getDocuments();
			
			engine.index(docs);

			List<String> inTitle;
			List<String> notInTitle;
			List<String> inDescription;
			List<String> notInDescription;
			List<String> results;
			
			// 1) search documents with words "kim" and "korea" in the title
			inTitle = new LinkedList<String>();
			inTitle.add("kim");
			inTitle.add("korea");
			results = engine.search(inTitle, null, null, null, null, null);
			engine.printResults(results);
			
			// 2) search documents with word "kim" in the title and no word "korea" in the description
			inTitle = new LinkedList<String>();
			notInDescription = new LinkedList<String>();
			inTitle.add("kim");
			notInDescription.add("korea");
			results = engine.search(inTitle, null, null, notInDescription, null, null);
			engine.printResults(results);

			// 3) search documents with word "us" in the title, no word "dawn" in the title and word "" and "" in the description
			inTitle = new LinkedList<String>();
			inTitle.add("us");
			notInTitle = new LinkedList<String>();
			notInTitle.add("dawn");
			inDescription = new LinkedList<String>();
			inDescription.add("american");
			inDescription.add("confession");
			results = engine.search(inTitle, notInTitle, inDescription, null, null, null);
			engine.printResults(results);
			
			// 4) search documents whose publication date is 2011-12-18
			results = engine.search(null, null, null, null, "2011-12-18", "2011-12-18");
			engine.printResults(results);
			
			// 5) search documents with word "video" in the title whose publication date is 2000-01-01 or later
			inTitle = new LinkedList<String>();
			inTitle.add("video");
			results = engine.search(inTitle, null, null, null, "2000-01-01", null);
			engine.printResults(results);
			
			// 6) search documents with no word "canada" or "iraq" or "israel" in the description whose publication date is 2011-12-18 or earlier
			notInDescription = new LinkedList<String>();
			notInDescription.add("canada");
			notInDescription.add("iraq");
			notInDescription.add("israel");
			results = engine.search(null, null, null, notInDescription, null, "2011-12-18");
			engine.printResults(results);
		}
		else
			System.out.println("ERROR: the path of a RSS Feed file has to be passed as a command line argument.");
	}
}
