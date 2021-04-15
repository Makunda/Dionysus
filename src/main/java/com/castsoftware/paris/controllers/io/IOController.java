package com.castsoftware.paris.controllers.io;

import com.castsoftware.paris.database.Neo4jAL;
import com.castsoftware.paris.exceptions.file.FileIOException;
import com.castsoftware.paris.exceptions.neo4j.Neo4jQueryException;
import com.castsoftware.paris.io.ExportModel;
import com.castsoftware.paris.io.Exporter;
import com.castsoftware.paris.models.Case.Case;
import com.castsoftware.paris.models.Group.Group;
import com.castsoftware.paris.results.OutputMessage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class IOController {

	/**
	 * Export the all the paris node to a zip file, that you can re-import later
	 * @param neo4jAL Neo4j access layer
	 * @param path Path to the ZipFile
	 * @param fileName Name of the zip that will be created
	 * @return
	 * @throws FileIOException
	 * @throws Neo4jQueryException
	 */
	public static Path exportNodes(Neo4jAL neo4jAL, String path, String fileName) throws FileIOException, Neo4jQueryException {

		if(!path.endsWith("\\") || !path.endsWith("/")) {
			path += "\\";
		}

		Path target = Path.of(path);

		// List nodes to export
		// Case
		String caseLabel = Case.getLabelPropertyAsString();
		List<String> caseColumns = List.of(
				Case.getTitleProperty(),
				Case.getDescriptionProperty(),
				Case.getActiveProperty(),
				Case.getSelectedProperty(),
				Case.getCategoriesProperty());


		// Groups
		String groupLabel = Group.getLabelPropertyAsString();
		List<String> groupColumns = List.of(
				Group.getActiveProperty(),
				Group.getCategoriesProperty(),
				Group.getCreationDateProperty(),
				Group.getCypherRequestProperty(),
				Group.getCypherRequestReturnProperty(),
				Group.getDescriptionProperty(),
				Group.getGroupNameProperty(),
				Group.getNameProperty(),
				Group.getSelectedProperty(),
				Group.getTypeProperty());


		// Create export models
		ExportModel emCase = new ExportModel(caseLabel, caseColumns);
		emCase.setNeighborsLabel(List.of(groupLabel));
		emCase.setPk(List.of(Case.getTitleProperty(), Case.getDescriptionProperty()));

		ExportModel emGroup = new ExportModel(groupLabel, groupColumns);
		emGroup.setNeighborsLabel(List.of(groupLabel));
		emGroup.setPk(List.of(Group.getGroupNameProperty(), Group.getCypherRequestProperty()));

		List<ExportModel> toExport = List.of(emCase, emGroup);

		Exporter exporter = new Exporter(neo4jAL);
		return exporter.export(toExport, target, fileName);
	}
}
