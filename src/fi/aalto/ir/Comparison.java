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
				
				if (doc.getSearchTaskNumber() == 16) {
					String[] abs = doc.getAbstractText().split(" ");
					String stemmed = "";
					
					for (String word : abs) {
						stemmed += stem(word) + " ";
					}
					
					Document document = new Document();
					document.add(new Field("title", doc.getTitle(),
							TextField.TYPE_STORED));
					document.add(new Field("abstract", stemmed,
							TextField.TYPE_STORED));
					document.add(new Field("tasknumber", doc.getSearchTaskNumber() + "",
							TextField.TYPE_STORED));
					document.add(new Field("query", doc.getQuery(),
							TextField.TYPE_STORED));
					document.add(new Field("relevance", doc.isRelevant() ? "1" : "0",
							TextField.TYPE_STORED));
					
					writer.addDocument(document);
				}
			}
			writer.commit();
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public List<String> search(List<String> inTitle, List<String> inAbstract, String taskNumber, Similarity similarity, String type) {

		printQuery(inTitle, inAbstract, taskNumber, type);

		List<String> results = new LinkedList<String>();

		// implement the Lucene search here
		try {
			IndexReader reader = DirectoryReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(similarity);

			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.setMinimumNumberShouldMatch(1);
			if (inTitle != null) {
				for (String term : inTitle) {
					Query query = new TermQuery(new Term("title", term));
					booleanQuery.add(query, BooleanClause.Occur.SHOULD);
				}
			}
			
			if (inAbstract != null) {
				for (String term : inAbstract) {
					Query query = new TermQuery(new Term("abstract", term));
					booleanQuery.add(query, BooleanClause.Occur.SHOULD);
				}
			}
			
			if (taskNumber != null) {
				Query query = new TermQuery(new Term("tasknumber", taskNumber));
				booleanQuery.add(query, BooleanClause.Occur.MUST);
			}

			ScoreDoc[] docs = searcher.search(booleanQuery, 1000).scoreDocs;
			for (int i = 0; i < docs.length; i++) {
				results.add(searcher.doc(docs[i].doc).get("query") + ", taskNumber: " + searcher.doc(docs[i].doc).get("tasknumber") +
						", Relevant: " + searcher.doc(docs[i].doc).get("relevance") + " Title: " + searcher.doc(docs[i].doc).get("title"));
			}

			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return results;
	}

	public void printQuery(List<String> inTitle, List<String> inAbstract, String taskNumber, String type) {
		System.out.print("Search (");
		if (inTitle != null) {
			System.out.print("in title: " + inTitle);
		}
		if (inAbstract != null) {
			System.out.print(", in abstract: " + inAbstract);
		}
		if (taskNumber != null) {
			System.out.print(", taskNumber: " + taskNumber);
		}
		if (type != null) {
			System.out.print(", " + type);
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
	
	public String stem(String word) {
		Stemmer s = new Stemmer();
		s.add(word.toCharArray(), word.length());
		s.stem();
		return s.toString();
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			String indexDir = System.getProperty("user.dir") + "/indicies";

			try {
				directory = FSDirectory.open(new File(indexDir));
			} catch (IOException e) {
				e.printStackTrace();
			}

			Comparison comparison = new Comparison();
			DefaultSimilarity vsm = new DefaultSimilarity();
			BM25Similarity bm25 = new BM25Similarity();

			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();

			comparison.index(docs);

			List<String> inTitle;
			List<String> inAbstract;
			String taskNumber;
			List<String> results;

			// 1) search documents with word "tablets" in the title (BM25)
			inTitle = new LinkedList<String>();
			inTitle.add(comparison.stem("tablet"));
			results = comparison.search(inTitle, null, null, bm25, "BM25");
			comparison.printResults(results);
			
			// 1) search documents with word "tablets" in the title (VSM)
			results = comparison.search(inTitle, null, null, vsm, "VSM");
			comparison.printResults(results);
			
			
			
			
			// 2) search document with word "tablet" in abstract and taskNumber 16 (BM25)
			inTitle = new LinkedList<String>();
			inAbstract = new LinkedList<String>();
			taskNumber = "16";
			inTitle.add(comparison.stem("tablet"));
			inTitle.add(comparison.stem("ergonomics"));
			inTitle.add(comparison.stem("typing"));
			inAbstract.add(comparison.stem("tablet"));
			inAbstract.add(comparison.stem("ergonomics"));
			inAbstract.add(comparison.stem("typing"));
			results = comparison.search(inTitle, inAbstract, taskNumber, bm25, "BM25");
			comparison.printResults(results);
			
			// 2) search document with word "tablet" in abstract and taskNumber 16 (VSM)
			results = comparison.search(inTitle, inAbstract, taskNumber, vsm, "VSM");
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
