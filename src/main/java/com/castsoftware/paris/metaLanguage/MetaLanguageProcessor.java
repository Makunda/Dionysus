package com.castsoftware.paris.metaLanguage;

import com.castsoftware.paris.exceptions.neo4j.Neo4JTemplateLanguageException;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetaLanguageProcessor {

	public static final String ANCHOR_TAG_SET = "%%APPLICATION_NAME%%";
  public static final String ANCHOR_TAG_SET_VALUE =
      "SET @.Tags = CASE WHEN @.Tags IS NULL THEN [$tagName] ELSE @.Tags + $tagName END";

	public static final String ANCHOR_RETURN_NODE ="\\%\\%RETURN_AS_NODES\\(([A-Za-z0-9]*)\\)\\%\\%";

	public static final String ANCHOR_RETURN_RELATIONSHIP ="\\%\\%RETURN_AS_RELATIONSHIP\\(([A-Za-z0-9]*)\\)\\%\\%";
	public static final String ANCHOR_RETURN_RELATIONSHIP_VALUE = "RETURN @ AS nodes";

	public static final String ANCHOR_RETURN_STRING ="\\%\\%RETURN_AS_STRING\\(([A-Za-z0-9]*)\\)\\%\\%";
	public static final String ANCHOR_RETURN_STRING_VALUE = "RETURN @ AS string";


	public static final String ANCHOR_RETURN_VALUE = "RETURN @ AS val";
	public static final String ANCHOR_COUNT_RETURN_VALUE = "RETURN COUNT(DISTINCT @) AS num";

	// Label anchor is not present in this list. It is the only mandatory label that request a
	// replacement to allow the request to be functional
	public static final List<Pattern> RETURN_TAG_LIST = Stream.of(ANCHOR_RETURN_NODE, ANCHOR_RETURN_RELATIONSHIP, ANCHOR_RETURN_STRING)
			.map(Pattern::compile).collect(Collectors.toList());
	public static final Map<MetaRequestType, Pattern> PATTERN_MAP =
			Map.of(MetaRequestType.NODE, Pattern.compile(ANCHOR_RETURN_NODE), MetaRequestType.RELATIONSHIP, Pattern.compile(ANCHOR_RETURN_RELATIONSHIP));

	/**
	 * Clean residual tags in the request
	 * @param request
	 * @return
	 */
	public static String cleanResidualTags(String request) {
		List<String> tags = List.of(ANCHOR_TAG_SET, ANCHOR_RETURN_NODE, ANCHOR_RETURN_RELATIONSHIP, ANCHOR_RETURN_STRING);
		for(String t : tags) {
			request = request.replaceAll(t, "");
		}
		return request;
	}

	/**
	 * Replace the Context Anchor by the name of the application
	 *
	 * @return
	 */
	public static MetaRequest processApplicationContext(MetaRequest metaRequest) {
		String safeName = String.format("`%s`", metaRequest.getApplication());
		metaRequest.setRequest(metaRequest.getRequest().replaceAll(ANCHOR_TAG_SET, safeName));
		return metaRequest;
	}

	/**
	 * Replace the return anchor in a metaRequest
	 *
	 * @param metaRequest
	 * @return
	 */
	public static MetaRequest findAndReplaceReturnTag(MetaRequest metaRequest) throws Neo4JTemplateLanguageException {

		for(Map.Entry<MetaRequestType, Pattern> en : PATTERN_MAP.entrySet()) {
			Matcher m = en.getValue().matcher(metaRequest.getRequest());

			if (m.find() && m.groupCount() >= 1) {
				String o = m.group(1);

				// Set Type
				metaRequest.setType(en.getKey());

				// Forge new value

				String replacer = ANCHOR_RETURN_VALUE.replace("@", o);
				// Modify original request;
				metaRequest.setRequest(
						metaRequest.getRequest().replaceFirst( en.getValue().pattern(), replacer));
				metaRequest.setReturnValue("val");
				return metaRequest;
			}
		}

		throw new Neo4JTemplateLanguageException(
				"Invalid return tag usage.", metaRequest.getRequest(), "TAGPxPRRT01");
	}

	/**
	 * Forge the meta request
	 * @param request Request of the tag
	 * @param application Application used for the execution
	 * @return The meta request ( request enriched with information )
	 * @throws Neo4JTemplateLanguageException If the request isn't correct ( talking about templates )
	 */
	public static MetaRequest forgeRequest(String request, String application) throws Neo4JTemplateLanguageException {

		assert (request != null && application != null) : "The parameters provided cannot be null";
		if(request.isBlank() || application.isBlank()) return null;

		MetaRequest metaRequest = new MetaRequest();
		metaRequest.setApplication(application); // Assign application


		metaRequest.setRequest(request); // Assign request

		metaRequest = processApplicationContext(metaRequest); // Replace the application anchors
		//metaRequest = findAndReplaceReturnTag(metaRequest); // Find the return value and assign the type to the request

		metaRequest.setRequest(cleanResidualTags(metaRequest.getRequest())); // Clean the residual

		return metaRequest;
	}



}
