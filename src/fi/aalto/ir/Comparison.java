package fi.aalto.ir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.DefaultSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class Comparison {

	//public static Directory directory = null;
	public Analyzer analyzer = null;

	public Comparison() {

	}

    public void toCSV() {
        File dataFile = new File(new File("curves"), "data.csv");
        try {
            FileWriter ostream = null;
            try {
                ostream = new FileWriter(dataFile);
                // Write header line
                ostream.write("recall, percision\n");
                for (double i = 0.0; i <= 1.0; i++) {
                    ostream.write(i + ", 0.35\n");
                }
                /*val dataMaps = dataSets.map(ds => ds.points.toMap)
                val nValues = dataMaps.foldLeft(Set[XT]())((result, m) => result union m.keySet)
                for (n <- nValues.toSeq.sorted) {
                    ostream.write(n + ", " + dataMaps.map(m => if (m.contains(n)) m(n).toString else "").mkString(", ") + "\n")
                }*/
            } finally {
                if (ostream != null) ostream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

	public void index(List<DocumentInCollection> docs, Similarity similarity, Directory directory) {
		analyzer = new StandardAnalyzer(Version.LUCENE_42); // Uses default
															// StopAnalyzer.ENGLISH_STOP_WORDS_SET

		IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_42, analyzer);
		conf.setSimilarity(similarity);
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

	public List<String> search(List<String> inAbstract, String relevance, Similarity similarity, String type, Directory directory) {

		printQuery(inAbstract, type);

		List<String> results = new LinkedList<String>();

		// implement the Lucene search here
		try {
			IndexReader reader = DirectoryReader.open(directory);
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(similarity);

			BooleanQuery booleanQuery = new BooleanQuery();
			booleanQuery.setMinimumNumberShouldMatch(1);

			if (inAbstract != null) {
				for (String term : inAbstract) {
					Term termm = new Term("abstract", term);
					Query query = new TermQuery(termm);
					if (term.contains("*")) {
						query = new WildcardQuery(termm);
					}
					booleanQuery.add(query, BooleanClause.Occur.SHOULD);
				}
			}
			
			if (relevance != null) {
				Query q = new TermQuery(new Term("relevance", "1"));
				booleanQuery.add(q, BooleanClause.Occur.SHOULD);
			}

			int total_results = 0;
			int relevant_results = 0;
			ScoreDoc[] docs = searcher.search(booleanQuery, 1000).scoreDocs;
			for (int i = 0; i < docs.length; i++) {
				results.add(searcher.doc(docs[i].doc).get("query") + ", taskNumber: " + searcher.doc(docs[i].doc).get("tasknumber") +
						", Relevant: " + searcher.doc(docs[i].doc).get("relevance"));
				if (searcher.doc(docs[i].doc).get("relevance").equals("1")) {
					relevant_results += 1;
				}
				total_results += 1;
			}
			
			System.out.println("Relevant results: " + relevant_results + ", total results: " + total_results);
			
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		return results;
	}

	public void printQuery(List<String> inAbstract, String type) {
		System.out.print("Search (");
		if (inAbstract != null) {
			System.out.print("in abstract: " + inAbstract);
		}
		if (type != null) {
			System.out.print(", " + type);
		}
		System.out.println("):");
	}

	public void printResults(List<String> results) {
		if (results.size() > 0) {
			for (int i = 0; i < results.size(); i++)
				System.out.println(" " + (i + 1) + ". " + results.get(i));
		} else
			System.out.println(" no results");
	}

	public static String stem(String word) {
		Stemmer s = new Stemmer();
		s.add(word.toCharArray(), word.length());
		s.stem();
		return s.toString();
	}

	public static void main(String[] args) {
		if (args.length > 0) {
			String indexDir = System.getProperty("user.dir") + "/indicies";
            String indexDir2 = System.getProperty("user.dir") + "/indicies2";
            Directory directory = null;
            Directory directory2 = null;

			try {
				directory = FSDirectory.open(new File(indexDir));
                directory2  = FSDirectory.open(new File(indexDir2));
			} catch (IOException e) {
				e.printStackTrace();
			}

			String[] first_query = "Ergonomics and modern devices".split(" ");
			String[] second_query = "ergonomics tablet typing".split(" ");
			String[] third_query = "tablet and ergo*".split(" ");

			List<String> stemmed_first_query = new ArrayList<String>();
			List<String> stemmed_second_query = new ArrayList<String>();
			List<String> stemmed_third_query = new ArrayList<String>();

			for (String word : first_query) {
				stemmed_first_query.add(stem(word));
			}
			for (String word : second_query) {
				stemmed_second_query.add(stem(word));
			}
			for (String word : third_query) {
				stemmed_third_query.add(stem(word));
			}

			Comparison comparisonVSM = new Comparison();
			Comparison comparisonBM25 = new Comparison();
			DefaultSimilarity vsm = new DefaultSimilarity();
			BM25Similarity bm25 = new BM25Similarity();

			DocumentCollectionParser parser = new DocumentCollectionParser();
			parser.parse(args[0]);
			List<DocumentInCollection> docs = parser.getDocuments();

			comparisonVSM.index(docs, vsm, directory);
			comparisonBM25.index(docs, bm25, directory2);

			List<String> inAbstract;
			List<String> results;
			List<String> results2;
			
			// First query
			inAbstract = new LinkedList<String>();
			for (String word : stemmed_first_query) {
				inAbstract.add(word);
			}

			results = comparisonBM25.search(inAbstract, null, bm25, "BM25", directory2);
			//comparisonBM25.printResults(results);
			
			results = comparisonVSM.search(inAbstract, null, vsm, "VSM", directory);
			//comparisonVSM.printResults(results);
			
			// Second query
			inAbstract = new LinkedList<String>();
			for (String word : stemmed_second_query) {
				inAbstract.add(word);
			}

			results = comparisonBM25.search(inAbstract, null, bm25, "BM25", directory2);
			//comparisonBM25.printResults(results);
			
			results = comparisonVSM.search(inAbstract, null, vsm, "VSM", directory);
			//comparisonVSM.printResults(results);
			
			// Third query
			inAbstract = new LinkedList<String>();
			for (String word : stemmed_third_query) {
				inAbstract.add(word);
			}
			results = comparisonBM25.search(inAbstract, null, bm25, "BM25", directory2);
			//comparisonBM25.printResults(results);
			
			results = comparisonVSM.search(inAbstract, null, vsm, "VSM", directory);
			//comparisonVSM.printResults(results);
			
			//Fetch ALL WITH RELEVANCE 1
			results = comparisonBM25.search(null, "1", bm25, "BM25", directory2);
			
			results = comparisonVSM.search(null, "1", vsm, "VSM", directory);

			try {
				if (directory != null) directory.close();
                if (directory2 != null) directory2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			System.out.println("ERROR: the path of a ACM XML file has to be passed as a command line argument.");
		}

	}
}
