package eg.edu.alexu.csd.oop.db.cs30;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TableFactory {

    public static void createTable(String tablePath,String schemaPath,String[] columnNames,Integer[] columnTypes,String tableName) throws SQLException {
        createTableSchema(schemaPath, columnNames, columnTypes);

        // Create empty file
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document xmlDocument = documentBuilder.newDocument();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            DOMSource domSource = new DOMSource(xmlDocument);
            StreamResult streamResult = new StreamResult(new File(tablePath));

            transformer.transform(domSource, streamResult);
        }
        catch (Exception e) {
            throw new SQLException();
        }
    }

    /**
     * Load a table from xml
     */
    public static Table loadTable(String tablePath,String schemaPath) throws SQLException {
        Table table = loadSchema(schemaPath);
        return load(table, tablePath);
    }



    public static void saveTable(String tablePath, Table table) throws SQLException{

        try{
            // load the Document Builder and return a document
            Document doc = loadDocument();

            // load all the data from the table and put it in nodes in the doc object
            saveCellstoXml(table, doc);

            // send all the stuff to the transformer so it can transform the doc object to the file with that path
            writeToFile(tablePath, doc);

        } catch (ParserConfigurationException | TransformerException e) {

            throw new SQLException("CANT SAVE THAT FILE!!!!!!");
        }
    }

    /**
     * Create table schema.
     */
    private static void createTableSchema(String schemaPath, String[] columnNames, Integer[] columnTypes) throws SQLException {
        File file = new File(schemaPath);

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document xmlDocument = documentBuilder.newDocument();

            Element root = xmlDocument.createElement("schema");
            xmlDocument.appendChild(root);

            for (int i = 0, columns = columnNames.length; i < columns; i++)
            {
                // Create element and attributes
                Element columnElement = xmlDocument.createElement("element");
                columnElement.setAttribute("name", columnNames[i]);

                if (columnTypes[i] == 0)
                {
                    columnElement.setAttribute("type", "string");
                }
                else
                {
                    columnElement.setAttribute("type", "integer");
                }

                root.appendChild(columnElement);
            }

            // Write to file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource domSource = new DOMSource(xmlDocument);
            StreamResult streamResult = new StreamResult(new File(schemaPath));

            transformer.transform(domSource, streamResult);
        }
        catch (Exception e) {
            throw new SQLException("Error creating xsd schema");
        }
    }

    /**
     * Load an xml table.
     */
    private static Table load(Table table,String pathName) throws SQLException{
        File xmlFile =new File(pathName);
        DocumentBuilderFactory factory= DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        Document doc;
        try{
            builder=factory.newDocumentBuilder();
            doc=builder.parse(xmlFile);
        }catch (Exception e){
            throw new SQLException("failed to load file");
        }
        doc.getDocumentElement().normalize();
        Element root=doc.getDocumentElement();
        NodeList rows=root.getChildNodes();
        for(int i = 0 ;  i < rows.getLength()  ; i++) if (rows.item(i).getNodeType() == Node.ELEMENT_NODE){
            ArrayList<String> coulmnNames=new ArrayList<>();
            ArrayList<String> values=new ArrayList<>();
            Element row= (Element) rows.item(i);
            NodeList tags=row.getChildNodes();
            for(int j = 0; j < tags.getLength();  j++) if (tags.item(j).getNodeType() == Node.ELEMENT_NODE){
                Element tag= (Element) tags.item(j);
                coulmnNames.add(tag.getTagName());
                values.add(tag.getTextContent());
            }
            Object[][] result=parser(coulmnNames,values,table.getMap());
            table.insertRow(  (String[]) result[0] ,  result[1]  );
        }
        return table;
    }

    /**
     * @return Table containing data taken from a schema file.
     */
    private static Table loadSchema(String path) throws SQLException {
        Table table;
        List<String> columnNames = new ArrayList<>();
        List<Integer> columnTypes = new ArrayList<>();

        File file = new File(path);

        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document xmlDocument = documentBuilder.parse(file);

            // Find all elements
            NodeList elements = xmlDocument.getElementsByTagName("element");
            for (int i = 0; i < elements.getLength(); i++)
            {
                if (elements.item(i).getNodeType() == Node.ELEMENT_NODE)
                {
                    Element element = (Element) elements.item(i);
                    columnNames.add(element.getAttribute("name"));

                    String type = element.getAttribute("type");
                    if (type.equalsIgnoreCase("string"))
                    {
                        columnTypes.add(0);
                    }
                    else if (type.equalsIgnoreCase("integer"))
                    {
                        columnTypes.add(1);
                    }
                    else
                    {
                        throw new SQLException();
                    }
                }
            }
        }
        catch (Exception e) {
            throw new SQLException("Error while loading schema");
        }

        // Get table name
        String[] splitPath = path.replaceAll(".xsd", "").split("/");

        table = new Table(columnNames.toArray(new String[0]), columnTypes.toArray(new Integer[0]));
        table.setTableName(splitPath[splitPath.length - 1]);

        return table;
    }

    /**
     * Parse data taken from DOM object.
     * @return a 2d object array
     *      0: column names.
     *      1: values of objects.
     */
    private static Object[][] parser(List<String> columnNames, List<String> values, Map<String, Integer> columnType) throws SQLException {
        Object[] objectValues = new Object[columnNames.size()];

        // Parse data
        for (int i = 0, columns = columnNames.size(); i < columns; i++)
        {
            // Integer
            if (columnType.get(columnNames.get(i)).equals(1))
            {
                try {
                    objectValues[i] = Integer.parseInt(values.get(i));
                }
                catch (Exception e) {
                    throw new SQLException();
                }
            }
            // String
            else
            {
                objectValues[i] = values.get(i);
            }
        }

        Object[][] parsedValues = new Object[2][];
        parsedValues[0] = columnNames.toArray(new String[0]);
        parsedValues[1] = objectValues;

        return parsedValues;
    }

    private static Document loadDocument() throws ParserConfigurationException {


        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();

        return documentBuilder.newDocument();
    }

    private static void saveCellstoXml(Table table, Document doc)
    {
        Element rootElement = doc.createElement("rows");
        doc.appendChild(rootElement);

        Object[][] valuesOfTheTable = table.select();
        String[] tableColumnNames = table.getColumnNames();

        for (Object[] objects : valuesOfTheTable) {
            Element row = doc.createElement("row");
            for (int i = 0; i < objects.length; i++) {
                if (objects[i] != null) {

                    Element cell = doc.createElement(tableColumnNames[i]);
                    cell.appendChild(doc.createTextNode((String) objects[i]));
                    row.appendChild(cell);
                }
            }
        }
    }

    private static void writeToFile(String xmlPath, Document document) throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        DOMSource domSource = new DOMSource(document);
        StreamResult streamResult = new StreamResult(new File(xmlPath));

        transformer.transform(domSource, streamResult);
    }
}


