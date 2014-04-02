IR-comparison
=============

#IR Comparison and Report

##Group assignment - Deadline 14.04.2014

In this part you will get familiar with the evaluation of information retrieval by calculating the precision and recall of queries and comparing different techniques used in the search methods.  

You should form into groups of three to four people and add your names and email addresses onto the Groups page of the wiki (Edit page). If you dont have a group, just add your name and the course assistants will form the rest of the groups. The groups should be formed or your name should be on the wiki page by the 10th of March. On the 15th of March the assistants will post the formed groups the following tasks.
Each group is given an information retrieval task to a document collection collected from the ACM digital library database. You are also given a parser for handling the document collection. 

You have to compare at least two of the following ranking methods, eg: VSM vs. BM25:
- VSM cosine similarity with tf-idf
- BM25 (implemented in Lucene)
- Language model

And use morphological analysis or stop words:
- Porter stemmer or other stemmers
- stop words vs. no analysis and stopwords

When you have a comparison scenario, you should implement a Java program which uses Lucene to perform the search with the given document collection and your search task (the program from part 1 might be of help here). Each comparison scenario has corresponding number in the document collection file, for example comparison scenario 1: Motion control, has all relevant documents marked with the search task number 1. Documents in the document collection are presented as <item>-elements and the actual query that has been used to find the document from the ACM-database is presented with the <query>-element. Queries in the <query>-elements should be good for document ranking, but you can also use your own query. You should use at least 2-3 queries in your comparison. Relevance of the document to the given comparison scenario is also pre-evaluated and presented as boolean in the <relevance>-element. 

The program should contain a main method which performs the indexing the abstracts of document collection, the search and prints the results in the standard output. The path to the XML file containing the document collection is passed to the main method as a command-line argument. You can decide whether to use a file- or memory-based search index. The queries for the search task can be hard-coded in the program.

Based on the results you get from the program (e.g. a ranked search result lists) you are supposed to produce 11-step non-interpolated precision recall curves (e.g. one for each of the compared techniques). Feel free to also use other statistical/quantitative methods to analyze the data as you see fit. You should also conduct some qualitative analysis, e.g. can the search results be characterized somehow, did the document collection contain some anomalies, why the chosen methods produced good/bad results, did the ranking work.

Finally, you must report your work in a 8-10 page document, using the Aalto LaTeX template. The report can be written in English or Finnish and must include at least the following parts:
- title page with the course code and course name, topic of the work, names and student IDs of the group members
- introduction
- description of the compared techniques in the search method
- evaluation of the information retrieval with the compared techniques (precision recall curves, etc.)
- conclusions / discussion
- references
- description of the contributions of each group member: did you have specific roles in the collaboration, which tasks did you do together, and which tasks were divided to each member to be done individually

Your group should send the report and the accompanying code to the course email t754400@list.aalto.fi. The title of the email should be "Assignment part 2", and your group number, for example "Assignment part 2, 12". If your code uses extermal libraries other than the ones linked to in this or previous parts, you should also attach them.
The course has a wiki page with an FAQ regarding the course assignment. Feel free post any questions straight to the FAQ or send them to the course email from which we will add them to the wiki.
