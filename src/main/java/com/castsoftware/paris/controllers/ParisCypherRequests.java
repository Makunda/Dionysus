package com.castsoftware.paris.controllers;

import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.neo4j.Neo4JTemplateLanguageException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.metaLanguage.MetaLanguageProcessor;
import com.castsoftware.paris.metaLanguage.MetaRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParisCypherRequests {

	/**
	 * Check the validity of a query before the insertion in the database
	 * @param neo4jAL
	 * @param request
	 * @param awaitedReturn
	 * @return
	 */
	public static Boolean checkValidityRequest(Neo4jAL neo4jAL, String request, String awaitedReturn) {
		neo4jAL.logInfo(String.format("Submitting the query '%s' for verification.", request));
		// Add a first explain and after all the semi-colons to prevent the execution of the query);

    	String[] splitQuery = request.split("(?!\\B\"[^\"]*);(?![^\"]*\"\\B)(?![\\s]*$)");


		for(String req : splitQuery) {

			String toExecute = String.format("Explain %s", req);
      		// Verify the presence of a return value
			String regexReturn = "as[\\s]+([\\w]*)(?:[\\s]*(?:;|,|$))";
			Pattern pattern = Pattern.compile(regexReturn);
			Matcher matcher = pattern.matcher(toExecute);

			// If no return value is found, return invalid query
			boolean found = false;
			while (matcher.find()) {
				System.out.println("Full return match: " + matcher.group(0));

				for (int i = 1; i <= matcher.groupCount(); i++) {
					System.out.println("Group " + i + ": " + matcher.group(i));
					if (matcher.group(i).equals(awaitedReturn)) {
						found = true;
						break;
					}
				}
			}

			// Mark as failed if no return was found
			if(!found) {
				neo4jAL.logInfo(String.format("No return matching '%s' was found.", awaitedReturn));
				return false;
			}

			try {
				MetaRequest forged = MetaLanguageProcessor.forgeRequest(req, awaitedReturn, "Test");
				if(forged == null) return false;

				neo4jAL.executeQuery(forged.getRequest());
			} catch (Neo4JTemplateLanguageException | Neo4jQueryException e) {
				neo4jAL.logError(String.format("The query '%s' doesn't seem to be valid.", request), e);
				return false;
			}
		}

		// All the tests were passed successfully
		return true;
	}
}
