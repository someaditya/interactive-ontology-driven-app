package com.kde.group2.model;

import static java.lang.Double.parseDouble;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.kde.group2.Application;
import com.kde.group2.Coordinate;
import com.kde.group2.rdf.RDFFile;
import com.kde.group2.variables.AccommodationType;
import com.kde.group2.variables.County;

import constants.Constants;
import constants.Queries;

@Component
public class HousingModel {

    static Logger logger = LoggerFactory.getLogger(Application.class);

    Model model;

    private List<HashMap<String, Object>> printAndExecuteQuery(String queryString) {
        Query query = QueryFactory.create(queryString);

        QueryExecution qe = QueryExecutionFactory.create(query, this.model);
        ResultSet results = qe.execSelect();
        List<String> resultVars = results.getResultVars();
        List<HashMap<String, Object>> resultsList = new ArrayList<>();
        
        for ( ; results.hasNext() ; )
        {
            QuerySolution solution = results.next() ;
            HashMap<String, Object> resultMap = new HashMap<>();

            for (String resultName: resultVars) {
                if (resultName.equals("households")  || resultName.equals("persons")) {
                    resultMap.put(resultName, solution.getLiteral(resultName).getInt());
                } else if (resultName.equals("geometry")) {
                    List<Coordinate> coordinates = extractCoordinatedFromPolygonResult(solution.getLiteral("geometry").getString());
                    double area = calculateAreaFromPolygon(coordinates);
                    resultMap.put("coordinates", coordinates);
                    resultMap.put("area", area);
                } else {
                    resultMap.put(resultName, solution.get(resultName));
                }

                resultsList.add(resultMap);
            }
        }

        // Execute the Query AGAIN so it can print (needs to be removed / improved)
        QueryExecution qe2 = QueryExecutionFactory.create(query, this.model);
        ResultSet printableResults = qe2.execSelect();
        ResultSetFormatter.out(System.out, printableResults, query);
        qe.close();

        return resultsList;
    }

    public HousingModel() {
        String executionPath = System.getProperty("user.dir");
        System.out.println(executionPath);
        try {
            System.out.println("current dir = " + System.getProperty("user.dir"));
            RDFFile rdfFile = new RDFFile(Constants.RDF_FILE_PATH, Constants.RDF_MODEL_NAME);
            this.model = rdfFile.getModel();
        } catch (IOException e) {
            logger.error("Unable to open RDF file.",e);
            throw new RuntimeException("Unable to open file: "+Constants.RDF_FILE_PATH);
        }
    }

    private List<Coordinate> extractCoordinatedFromPolygonResult(String polygon) {
        String pattern = "([-]?\\d+\\.\\d+ [-]?\\d+\\.\\d+)";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(polygon);

        List<Coordinate> coordinates = new ArrayList<>();
        while (matcher.find( )) {
            String[] values = matcher.group(0).split(" ");

            Coordinate coordinate = new Coordinate(parseDouble(values[1]), parseDouble(values[0]));
            coordinates.add(coordinate);
        }

        return coordinates;
    }

    private double calculateAreaFromPolygon(List<Coordinate> coordinates) {
        // TODO: do the area calculation
        return 0.0;
    }

    public List<HashMap<String, Object>> getHouseholdsAndPersonsByType(AccommodationType type) {
        String queryString = String.format(Queries.HOUSEHOLDS_POPULATION_BY_TYPE, type.getValue());
        return printAndExecuteQuery(queryString);
    }

    public List<HashMap<String, Object>> getHouseholdsAndPersonsByCountyAndType(County county, AccommodationType type) {
        String queryString = String.format(Queries.HOUSEHOLDS_POPULATION_BY_COUNTY_AND_TYPE, county.getValue(), type.getValue());
        return printAndExecuteQuery(queryString);
    }

    public List<HashMap<String, Object>> getHouseholdsByCountyAndType(County county, AccommodationType type) {
        String queryString = String.format(Queries.HOUSEHOLDS_BY_COUNTY_AND_TYPE, county.getValue(), type.getValue());
        return printAndExecuteQuery(queryString);
    }

    public List<HashMap<String, Object>> getPersonsByCountyAndType(County county, AccommodationType type) {
        String queryString = String.format(Queries.PERSONS_BY_COUNTY_AND_TYPE, county.getValue(), type.getValue());
        return printAndExecuteQuery(queryString);
    }

    public List<HashMap<String, Object>> getPersonsByCounty(County county) {
        String queryString = String.format(Queries.PERSONS_BY_COUNTY, county.getValue());
        return printAndExecuteQuery(queryString);
    }

    public List<HashMap<String, Object>> getHouseholdsByCounty(County county) {
        String queryString = String.format(Queries.HOUSEHOLDS_BY_COUNTY, county.getValue());
        return printAndExecuteQuery(queryString);
    }

    public List<HashMap<String, Object>> getPersonsByType(AccommodationType type) {
        String queryString = String.format(Queries.PERSONS_BY_TYPE, type.getValue());
        return printAndExecuteQuery(queryString);
    }

    public List<HashMap<String, Object>> getHouseholdsByType(AccommodationType type) {
        String queryString = String.format(Queries.HOUSEHOLDS_BY_TYPE, type.getValue());
        return printAndExecuteQuery(queryString);
    }

    public List<HashMap<String, Object>> getHouseholdsGreaterThanXByType(AccommodationType type, int lowerLimit) {
        String queryString = String.format(Queries.HOUSEHOLDS_GREATHER_THAN_X_BY_TYPE, type.getValue(), lowerLimit);
        return printAndExecuteQuery(queryString);
    }
}
