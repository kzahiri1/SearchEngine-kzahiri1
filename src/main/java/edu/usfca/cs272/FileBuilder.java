package edu.usfca.cs272;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class for building and processing files/directories to generate word counts
 * and an inverted index to write to JSON file
 */
public class FileBuilder {
	/**
	 * The InvertedIndex class used for storing word counts and the inverted index
	 */
	private final InvertedIndex indexer;

	/**
	 * Creates a new FileBuilder object with the InvertedIndex
	 *
	 * @param indexer the InvertedIndex object
	 */
	public FileBuilder(InvertedIndex indexer) {
		this.indexer = indexer;
	}

	/**
	 * Returns the InvertedIndex
	 *
	 * @return the InvertedIndex object
	 */
	public InvertedIndex getIndexer() {
		return indexer;
	}

	/**
	 * Builds word count and inverted index structures for the specified input path.
	 *
	 * @param inputPath The path of the file or directory to be processed
	 * @throws IOException If an I/O error occurs
	 */
	public void buildStructures(Path inputPath) throws IOException {
		if (Files.isDirectory(inputPath)) {
			processDirectory(inputPath);
		} else {
			processFile(inputPath);
		}
	}

	/**
	 * Processes the files in the specified directory to generate word counts and
	 * the inverted index
	 *
	 * @param directory The directory to process
	 * @throws IOException If an I/O error occurs
	 */
	public void processDirectory(Path directory) throws IOException {
		try (DirectoryStream<Path> listing = Files.newDirectoryStream(directory)) {
			for (Path path : listing) {
				if (Files.isDirectory(path)) {
					processDirectory(path);
				} else {
					if (isTextFile(path)) {
						processFile(path);
					}
				}
			}
		}
	}

	/**
	 * Processes the specified file to generate word counts and an inverted index
	 *
	 * @param location The path of the file to process
	 * @throws IOException If an I/O error occurs
	 */
	public void processFile(Path location) throws IOException {
		int position = 0;
		try (BufferedReader reader = Files.newBufferedReader(location)) {
			String line;
			while ((line = reader.readLine()) != null) {
				List<String> wordStems = FileStemmer.listStems(line);
				for (String stemmedWord : wordStems) {
					position += 1;
					indexer.addWord(stemmedWord, location.toString(), position);
				}
			}
		}
	}
	/**
	 * Determines if given a valid file
	 * 
	 * @param file The file to be checked
	 * @return True for a valid file, false otherwise
	 */
	public static boolean isTextFile(Path file) {
		String fileName = file.getFileName().toString().toLowerCase();
		return Files.isRegularFile(file) && (fileName.endsWith(".txt") || fileName.endsWith(".text"));
	}

	public Map<String, List<SearchResult>> conductSearch(List<List<String>> processedQueries) throws IOException {
	    Map<String, List<SearchResult>> searchResultsMap = new HashMap<>();

	    for (List<String> query : processedQueries) {
	        if (query.isEmpty()) {
	            continue;
	        }

	        String queryWord = String.join(" ", query);
	        Set<String> visitedLocations = new HashSet<>();
	        List<SearchResult> searchResults = new ArrayList<>();

	        for (String word : query) {
	            Map<String, TreeSet<Integer>> locations = indexer.getInvertedIndex().getOrDefault(word, new TreeMap<>());

	            for (Map.Entry<String, TreeSet<Integer>> entry : locations.entrySet()) {
	                String location = entry.getKey();
	                TreeSet<Integer> positions = entry.getValue();
	                int totalWords = indexer.getTotalWordCount(location);
	                int count = countMatches(query, positions);

	                if (!visitedLocations.contains(location)) {
	                    double score = calculateScore(count, totalWords);

	                    SearchResult result = new SearchResult(location, totalWords, count, score);
	                    searchResults.add(result);
	                    visitedLocations.add(location);
	                } else {
	                	for (SearchResult result : searchResults) {
	                	    if (result.getLocation().equals(location)) {
	                	        result.updateMatchCount(count);
	                	        break;
	                	    }
	                	}
	                }
	            }
	        }

	        searchResultsMap.put(queryWord, searchResults);
	    }

	    searchResultsMap = SearchResult.sortResults(searchResultsMap);

	    return searchResultsMap;
	}




    private int countMatches(List<String> query, TreeSet<Integer> positions) {
        int count = 0;
        for (int position : positions) {
            if (queryMatches(query, position)) {
                count++;
            }
        }
        return count;
    }

    private boolean queryMatches(List<String> query, int position) {
        return true;
    }

    private double calculateScore(int matchCount, int totalWords) {
        return (double) matchCount / totalWords;
    }




	public static List<List<String>> processQuery(Path queryPath) throws IOException {
		List<List<String>> processedQueries = new ArrayList<>();
		List<String> queryLines = Files.readAllLines(queryPath);

		for (String queryLine : queryLines) {
			List<String> stemmedWords = FileStemmer.listStems(queryLine);
			List<String> processedQuery = new ArrayList<>(new HashSet<>(stemmedWords));
			Collections.sort(processedQuery);
			processedQueries.add(processedQuery);
		}

		return processedQueries;
	}
}
