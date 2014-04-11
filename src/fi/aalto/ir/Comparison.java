package fi.aalto.ir;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Comparison {

	public static Directory directory = null;
	public Analyzer analyzer = null;

	public Comparison() {

	}

	public void index(List<DocumentInCollection> docs) {
		analyzer = new StandardAnalyzer(Version.LUCENE_42); // Uses default
															// StopAnalyzer.ENGLISH_STOP_WORDS_SET

		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_42,
				analyzer);
		conf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		try {
			IndexWriter writer = new IndexWriter(directory, conf);

			for (DocumentInCollection doc : docs) {
				/*
				 * private String title; private String abstractText; private
				 * int searchTaskNumber; private String query; private boolean
				 * relevant;
				 */

				Document document = new Document();
				document.add(new Field("title", doc.getTitle(),
						TextField.TYPE_STORED));
				writer.addDocument(document);
			}
			writer.commit();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public List<String> search(List<String> inTitle, Similarity similarity) {

		printQuery(inTitle);

		List<String> results = new LinkedList<String>();

		// implement the Lucene search here
		try {
			IndexReader reader = DirectoryReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(similarity);

			BooleanQuery booleanQuery = new BooleanQuery();
			if (inTitle != null) {
				for (String term : inTitle) {
					Query query = new TermQuery(new Term("title", term));
					booleanQuery.add(query, BooleanClause.Occur.MUST);
				}
			}

			ScoreDoc[] docs = searcher.search(booleanQuery, 100).scoreDocs;
			for (int i = 0; i < docs.length; i++) {
				results.add(searcher.doc(docs[i].doc).get("title"));
			}

			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return results;
	}

	public void printQuery(List<String> inTitle) {
		System.out.print("Search (");
		if (inTitle != null) {
			System.out.print("in title: " + inTitle);
		}
		System.out.println("):");
	}

	public void printResults(List<String> results) {
		if (results.size() > 0) {
			Collections.sort(results);
			for (int i = 0; i < results.size(); i++)
				System.out.println(" " + (i + 1) + ". " + results.get(i));
		} else
			System.out.println(" no results");
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			String indexDir = System.getProperty("user.dir") + "/indicies";

			try {
				directory = FSDirectory.open(new File(indexDir));
			} catch (IOException e) {
				e.printStackTrace();
			}

			// Example usage of stemmer "reading" -> "read"
			Stemmer s = new Stemmer();
			s.add("reading".toCharArray(), 7);
			s.stem();
			System.out.println("Stemmer converts 'reading' to --> '"
					+ s.toString() + "'");

			Comparison comparison = new Comparison();
			DefaultSimilarity vsm = new DefaultSimilarity();
			BM25Similarity bm25 = new BM25Similarity();

			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();

			comparison.index(docs);

			List<String> inTitle;
			List<String> results;

			// 1) search documents with word "tablets" in the title (BM25)
			inTitle = new LinkedList<String>();
			inTitle.add("tablet");
			// inTitle.add("ergonomics");
			results = comparison.search(inTitle, bm25);
			comparison.printResults(results);
			
			// 1) search documents with word "tablets" in the title (VSM)
			results = comparison.search(inTitle, vsm);
			comparison.printResults(results);

			try {
				directory.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("ERROR: the path of a ACM XML file has to be passed as a command line argument.");
		}

	}
}
